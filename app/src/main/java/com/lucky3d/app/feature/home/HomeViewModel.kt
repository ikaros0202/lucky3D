package com.lucky3d.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.DrawSyncMetadata
import com.lucky3d.app.data.repository.LiveContentRepository
import com.lucky3d.app.domain.livecontent.LiveRefreshTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.delay
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
    private var homeVisibleRefreshScheduled = false

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
    ) { home, trial, trialState, beforeRelease ->
        home.copy(
            trialNumber = trial,
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
        if (homeVisibleRefreshScheduled) return
        homeVisibleRefreshScheduled = true
        viewModelScope.launch {
            val waitMillis = millisUntilTrialRelease()
            if (waitMillis > 0) {
                delay(waitMillis)
                beforeTrialReleaseWindow.value = false
            }
            liveContentRepository.refreshTrial(LiveRefreshTrigger.HOME_VISIBLE)
        }
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

    private fun millisUntilTrialRelease(): Long {
        val now = clock.instant().atZone(BEIJING)
        if (now.toLocalTime() >= TRIAL_RELEASE_TIME) return 0L
        val release = now.toLocalDate().atTime(TRIAL_RELEASE_TIME).atZone(BEIJING)
        return Duration.between(now, release).toMillis().coerceAtLeast(0L)
    }

    private companion object {
        val BEIJING: ZoneId = ZoneId.of("Asia/Shanghai")
        val TRIAL_RELEASE_TIME: LocalTime = LocalTime.of(16, 35)
    }
}

private fun DrawSyncMetadata?.defaultState(): HomeSyncState = when {
    this?.lastFailureType != null -> HomeSyncState.ERROR
    this?.correctedIssues?.isNotEmpty() == true -> HomeSyncState.CORRECTED
    this?.lastSuccessEpochMillis != null -> HomeSyncState.UP_TO_DATE
    else -> HomeSyncState.LOCAL
}
