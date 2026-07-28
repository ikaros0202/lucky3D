package com.lucky3d.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucky3d.app.data.repository.DrawSyncMetadata
import com.lucky3d.app.data.repository.DrawRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DrawRepository,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.latestDraw,
        repository.observeRecent(HOME_RECENT_LIMIT),
        repository.syncMetadata,
        isRefreshing,
    ) { latest, recent, metadata, refreshing ->
        HomeUiState(
            latest = latest,
            recent = recent,
            syncState = if (refreshing) HomeSyncState.UPDATING else metadata.defaultState(),
            lastSuccessEpochMillis = metadata?.lastSuccessEpochMillis,
            failureType = metadata?.lastFailureType,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeUiState(),
    )

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

    private companion object {
        const val HOME_RECENT_LIMIT = 5
    }
}

private fun DrawSyncMetadata?.defaultState(): HomeSyncState = when {
    this?.lastFailureType != null -> HomeSyncState.ERROR
    this?.correctedIssues?.isNotEmpty() == true -> HomeSyncState.CORRECTED
    this?.lastSuccessEpochMillis != null -> HomeSyncState.UP_TO_DATE
    else -> HomeSyncState.LOCAL
}
