package com.lucky3d.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.DrawSyncMetadata
import com.lucky3d.app.data.repository.LiveContentRepository
import com.lucky3d.app.data.repository.AnnouncementRefreshState
import com.lucky3d.app.data.repository.OfficialDataSyncCoordinator
import com.lucky3d.app.data.repository.OfficialDataSyncTrigger
import com.lucky3d.app.data.repository.YunnanAnnouncementRepository
import com.lucky3d.app.domain.livecontent.LiveContentRefreshResult
import com.lucky3d.app.domain.livecontent.LiveRefreshTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DrawRepository,
    private val liveContentRepository: LiveContentRepository,
    private val clock: Clock,
    private val yunnanRepository: YunnanAnnouncementRepository,
    private val officialDataSyncCoordinator: OfficialDataSyncCoordinator,
) : ViewModel() {
    constructor(
        repository: DrawRepository,
        liveContentRepository: LiveContentRepository,
        clock: Clock,
    ) : this(
        repository,
        liveContentRepository,
        clock,
        EmptyHomeYunnanRepository,
        OfficialDataSyncCoordinator(repository, EmptyHomeYunnanRepository),
    )

    internal constructor(
        repository: DrawRepository,
        liveContentRepository: LiveContentRepository,
        clock: Clock,
        yunnanRepository: YunnanAnnouncementRepository,
    ) : this(
        repository,
        liveContentRepository,
        clock,
        yunnanRepository,
        OfficialDataSyncCoordinator(repository, yunnanRepository),
    )
    private val isRefreshing = MutableStateFlow(false)
    private val beforeTrialReleaseWindow = MutableStateFlow(isBeforeTrialRefreshWindow())
    private val currentBeijingDate = MutableStateFlow(
        clock.instant().atZone(BEIJING).toLocalDate(),
    )
    private val manualTrialFailureDate = MutableStateFlow<LocalDate?>(null)
    private var homeVisibleRefreshJob: Job? = null

    private val drawState = combine(
        repository.latestDraw,
        repository.allDrawsAscending,
        repository.syncMetadata,
        isRefreshing,
    ) { latest, allDrawsAscending, metadata, refreshing ->
        val drawsDescending = latest
            ?.let { current ->
                listOf(current) + allDrawsAscending
                    .asReversed()
                    .filterNot { it.issue == current.issue }
            }
            .orEmpty()
        HomeUiState(
            latest = latest,
            insights = buildHomeInsights(drawsDescending),
            syncState = if (refreshing) HomeSyncState.UPDATING else metadata.defaultState(),
            lastSuccessEpochMillis = metadata?.lastSuccessEpochMillis,
            failureType = metadata?.lastFailureType,
        )
    }

    private val trialFailureDateState = combine(
        currentBeijingDate,
        manualTrialFailureDate,
    ) { today, manualFailureDate ->
        today to manualFailureDate
    }

    private val announcementState = combine(
        drawState,
        yunnanRepository.latestAnnouncement,
        officialDataSyncCoordinator.announcementRefreshState,
    ) { home, announcement, refreshState ->
        home.copy(
            yunnanAnnouncement = announcement,
            announcementRefreshFailed = refreshState == AnnouncementRefreshState.ERROR ||
                refreshState == AnnouncementRefreshState.DRAW_ERROR,
            syncState = if (refreshState == AnnouncementRefreshState.REFRESHING) {
                HomeSyncState.UPDATING
            } else {
                home.syncState
            },
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        announcementState,
        liveContentRepository.trialNumber,
        liveContentRepository.trialRefreshState,
        beforeTrialReleaseWindow,
        trialFailureDateState,
    ) { home, trial, trialState, beforeRelease, dateState ->
        val (today, manualFailureDate) = dateState
        val currentTrial = trial?.takeIf { candidate ->
            candidate.sourceLocalDate == today ||
                (beforeRelease && candidate.sourceLocalDate == today.minusDays(1))
        }
        home.copy(
            trialNumber = currentTrial,
            trialState = trialState,
            isBeforeTrialReleaseWindow = beforeRelease,
            trialManualRefreshFailed =
                manualFailureDate == today && !beforeRelease && currentTrial == null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeUiState(
            isBeforeTrialReleaseWindow = beforeTrialReleaseWindow.value,
        ),
    )

    fun onHomeVisible() {
        if (homeVisibleRefreshJob?.isActive == true) return
        homeVisibleRefreshJob = viewModelScope.launch {
            runTrialRefreshSchedule()
        }
    }

    fun onHomeHidden() {
        homeVisibleRefreshJob?.cancel()
        homeVisibleRefreshJob = null
    }

    fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        viewModelScope.launch {
            try {
                officialDataSyncCoordinator.sync(OfficialDataSyncTrigger.MANUAL)
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun refreshTrial() {
        viewModelScope.launch {
            manualTrialFailureDate.value = null
            val result = liveContentRepository.refreshTrial(LiveRefreshTrigger.MANUAL)
            val now = clock.instant().atZone(BEIJING)
            if (result is LiveContentRefreshResult.Failed && now.toLocalTime() >= TRIAL_REFRESH_TIME) {
                manualTrialFailureDate.value = now.toLocalDate()
            }
        }
    }

    private fun isBeforeTrialRefreshWindow(): Boolean {
        val localTime = clock.instant().atZone(BEIJING).toLocalTime()
        return localTime < TRIAL_REFRESH_TIME
    }

    private suspend fun runTrialRefreshSchedule() {
        while (currentCoroutineContext().isActive) {
            val now = clock.instant().atZone(BEIJING)
            currentBeijingDate.value = now.toLocalDate()
            val beforeRelease = now.toLocalTime() < TRIAL_REFRESH_TIME
            beforeTrialReleaseWindow.value = beforeRelease
            if (beforeRelease) {
                val release = now.toLocalDate().atTime(TRIAL_REFRESH_TIME).atZone(BEIJING)
                delay(
                    minOf(
                        Duration.between(now, release).toMillis().coerceAtLeast(1L),
                        millisUntilNextMidnight(now),
                    ),
                )
                continue
            }
            val result = liveContentRepository.refreshTrial(LiveRefreshTrigger.HOME_VISIBLE)
            val waitMillis = when (result) {
                LiveContentRefreshResult.Success -> millisUntilNextMidnight(now)
                is LiveContentRefreshResult.Failed -> TRIAL_RETRY_MILLIS
                is LiveContentRefreshResult.Skipped -> when (result.reason) {
                    com.lucky3d.app.domain.livecontent.SkipReason.ALREADY_SUCCEEDED_TODAY,
                    com.lucky3d.app.domain.livecontent.SkipReason.DAILY_AUTO_LIMIT,
                    -> millisUntilNextMidnight(now)
                    com.lucky3d.app.domain.livecontent.SkipReason.COOLDOWN -> TRIAL_RETRY_MILLIS
                    com.lucky3d.app.domain.livecontent.SkipReason.BEFORE_RELEASE_WINDOW -> 1_000L
                    com.lucky3d.app.domain.livecontent.SkipReason.TRIGGER_NOT_APPLICABLE -> return
                }
            }
            delay(minOf(waitMillis, millisUntilNextMidnight(now)).coerceAtLeast(1L))
        }
    }

    private fun millisUntilNextMidnight(now: java.time.ZonedDateTime): Long {
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay(BEIJING)
        return Duration.between(now, midnight).toMillis().coerceAtLeast(1L)
    }

    private companion object {
        val BEIJING: ZoneId = ZoneId.of("Asia/Shanghai")
        val TRIAL_REFRESH_TIME: LocalTime = LocalTime.of(16, 30)
        const val TRIAL_RETRY_MILLIS = 30 * 60 * 1000L
    }
}

private object EmptyHomeYunnanRepository : YunnanAnnouncementRepository {
    override val latestAnnouncement = flowOf<com.lucky3d.app.core.model.YunnanAnnouncement?>(null)
    override fun observeByIssue(issue: String) =
        flowOf<com.lucky3d.app.core.model.YunnanAnnouncement?>(null)
    override suspend fun refreshRecent(limit: Int) =
        com.lucky3d.app.data.remote.YunnanAnnouncementDataResult.EmptyResponse
    override suspend fun refreshIssue(issue: String) =
        com.lucky3d.app.data.remote.YunnanAnnouncementDataResult.EmptyResponse
}

private fun DrawSyncMetadata?.defaultState(): HomeSyncState = when {
    this?.lastFailureType != null -> HomeSyncState.ERROR
    this?.correctedIssues?.isNotEmpty() == true -> HomeSyncState.CORRECTED
    this?.lastSuccessEpochMillis != null -> HomeSyncState.UP_TO_DATE
    else -> HomeSyncState.LOCAL
}
