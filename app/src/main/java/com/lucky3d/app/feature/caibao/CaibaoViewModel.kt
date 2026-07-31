package com.lucky3d.app.feature.caibao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucky3d.app.core.model.CaibaoDocument
import com.lucky3d.app.data.repository.CaibaoImageReadResult
import com.lucky3d.app.data.repository.LiveContentRepository
import com.lucky3d.app.domain.livecontent.LiveRefreshTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CaibaoViewModel @Inject constructor(
    private val repository: LiveContentRepository,
) : ViewModel() {
    private var visibleRefreshScheduled = false

    val uiState: StateFlow<CaibaoUiState> = combine(
        repository.caibaoDocument.mapLatest(::loadContent),
        repository.caibaoRefreshState,
    ) { content, refreshState ->
        CaibaoUiState(
            document = content.document,
            imageBytes = content.imageBytes,
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

    private suspend fun loadContent(document: CaibaoDocument?): CaibaoContent {
        if (document == null) return CaibaoContent()
        return when (val result = repository.readCaibaoImage(document)) {
            is CaibaoImageReadResult.Loaded -> CaibaoContent(document, result.bytes)
            is CaibaoImageReadResult.Unavailable -> CaibaoContent()
        }
    }

    private data class CaibaoContent(
        val document: CaibaoDocument? = null,
        val imageBytes: ByteArray? = null,
    )
}
