package com.lucky3d.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.DrawSyncMetadata
import com.lucky3d.app.data.repository.LiveContentRepository
import com.lucky3d.app.domain.livecontent.LiveContentRefreshResult
import com.lucky3d.app.domain.livecontent.LiveRefreshTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DrawRepository,
    private val liveContentRepository: LiveContentRepository,
    private val clock: Clock,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val beforeTrialReleaseWindow = MutableStateFlow(isBeforeTrialReleaseWindow())
    private val currentBeijingDate = MutableStateFlow(
        clock.instant().atZone(BEIJING).toLocalDate(),
    )
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

    val uiState: StateFlow<HomeUiState> = combine(
        drawState,
        liveContentRepository.trialNumber,
        liveContentRepository.trialRefreshState,
        beforeTrialReleaseWindow,
        currentBeijingDate,
    ) { home, trial, trialState, beforeRelease, today ->
        val currentTrial = trial?.takeIf { candidate ->
            candidate.sourceLocalDate == today &&
                home.latest?.issue?.let { candidate.issue > it } != false
        }
        home.copy(
            trialNumber = currentTrial,
            trialState = trialState,
            isBeforeTrialReleaseWindow = beforeRelease,
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
        homeVisibleRefreshJob = viewModelScope.launch { runTrialRefreshSchedule() }
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
                repository.refresh()
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun refreshTrial() {
        viewModelScope.launch {
            liveContentRepository.refreshTrial(LiveRefreshTrigger.MANUAL)
        }
    }

    private fun isBeforeTrialReleaseWindow(): Boolean {
        val localTime = clock.instant().atZone(BEIJING).toLocalTime()
        return localTime < TRIAL_RELEASE_TIME
    }

    private suspend fun runTrialRefreshSchedule() {
        while (currentCoroutineContext().isActive) {
            val now = clock.instant().atZone(BEIJING)
            currentBeijingDate.value = now.toLocalDate()
            val beforeRelease = now.toLocalTime() < TRIAL_RELEASE_TIME
            beforeTrialReleaseWindow.value = beforeRelease
            if (beforeRelease) {
                val release = now.toLocalDate().atTime(TRIAL_RELEASE_TIME).atZone(BEIJING)
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
        val TRIAL_RELEASE_TIME: LocalTime = LocalTime.of(18, 30)
        const val TRIAL_RETRY_MILLIS = 30 * 60 * 1000L
    }
}

private fun DrawSyncMetadata?.defaultState(): HomeSyncState = when {
    this?.lastFailureType != null -> HomeSyncState.ERROR
    this?.correctedIssues?.isNotEmpty() == true -> HomeSyncState.CORRECTED
    this?.lastSuccessEpochMillis != null -> HomeSyncState.UP_TO_DATE
    else -> HomeSyncState.LOCAL
}
