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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CaibaoViewModel @Inject constructor(
    private val repository: LiveContentRepository,
) : ViewModel() {
    private var visibleRefreshScheduled = false
    private val selectedIssue = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CaibaoUiState> = combine(
        combine(repository.caibaoDocuments, selectedIssue) { documents, issue ->
            documents to (issue ?: documents.firstOrNull()?.issue)
        }.mapLatest { (documents, issue) ->
            val document = documents.firstOrNull { it.issue == issue } ?: documents.firstOrNull()
            loadContent(document).copy(
                documents = documents,
                selectedIssue = document?.issue,
                issueOptions = issueOptions(documents.firstOrNull()?.issue),
            )
        },
        repository.caibaoRefreshState,
    ) { content, refreshState ->
        CaibaoUiState(
            document = content.document,
            documents = content.documents,
            selectedIssue = content.selectedIssue,
            issueOptions = content.issueOptions,
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

    fun selectIssue(issue: String) {
        selectedIssue.value = issue
        if (uiState.value.documents.none { it.issue == issue }) {
            viewModelScope.launch {
                repository.refreshCaibaoIssue(issue)
            }
        }
    }

    fun onImageDecodeFailed(document: CaibaoDocument) {
        viewModelScope.launch {
            repository.invalidateCaibaoImage(document)
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
        val documents: List<CaibaoDocument> = emptyList(),
        val selectedIssue: String? = null,
        val issueOptions: List<String> = emptyList(),
    )

    private fun issueOptions(latestIssue: String?): List<String> {
        val latest = latestIssue?.toLongOrNull() ?: return emptyList()
        return (0 until 30).map { offset -> (latest - offset).toString() }
    }
}
