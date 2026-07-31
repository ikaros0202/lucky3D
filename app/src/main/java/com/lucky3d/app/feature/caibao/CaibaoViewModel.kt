package com.lucky3d.app.feature.caibao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucky3d.app.data.repository.LiveContentRepository
import com.lucky3d.app.domain.livecontent.LiveRefreshTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CaibaoViewModel @Inject constructor(
    private val repository: LiveContentRepository,
) : ViewModel() {
    private var visibleRefreshScheduled = false

    val uiState: StateFlow<CaibaoUiState> = combine(
        repository.caibaoDocument,
        repository.caibaoRefreshState,
    ) { document, refreshState ->
        CaibaoUiState(
            document = document,
            refreshState = refreshState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CaibaoUiState(),
    )

    fun onVisible() {
        if (visibleRefreshScheduled) return
        visibleRefreshScheduled = true
        viewModelScope.launch {
            repository.refreshCaibao(LiveRefreshTrigger.CAIBAO_VISIBLE)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshCaibao(LiveRefreshTrigger.MANUAL)
        }
    }
}
