package com.lucky3d.app.feature.caibao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucky3d.app.core.model.CaibaoDocument
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.data.repository.CaibaoImageReadResult
import com.lucky3d.app.data.repository.DrawQuery
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.DrawSyncMetadata
import com.lucky3d.app.data.repository.LiveContentRepository
import com.lucky3d.app.data.repository.SyncResult
import com.lucky3d.app.domain.livecontent.LiveRefreshTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CaibaoViewModel @Inject constructor(
    private val repository: LiveContentRepository,
    private val drawRepository: DrawRepository,
) : ViewModel() {
    constructor(repository: LiveContentRepository) : this(repository, EmptyCaibaoDrawRepository)

    private var visibleRefreshScheduled = false
    private val selectedIssue = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CaibaoUiState> = combine(
        combine(repository.caibaoDocuments, drawRepository.allDrawsAscending) { documents, draws ->
            documents to draws
        }.combine(selectedIssue) { (documents, draws), issue ->
            Triple(documents, draws, issue ?: documents.firstOrNull()?.issue)
        }.mapLatest { (documents, draws, issue) ->
            val document = documents.firstOrNull { it.issue == issue }
            loadContent(document).copy(
                documents = documents,
                selectedIssue = issue,
                issueOptions = buildCaibaoIssueOptions(draws, documents),
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
            repository.cleanCaibaoCache()
            repository.refreshCaibao(LiveRefreshTrigger.CAIBAO_VISIBLE)
        }
    }

    fun refresh() {
        viewModelScope.launch { repository.refreshCaibao(LiveRefreshTrigger.MANUAL) }
    }

    fun selectIssue(issue: String) {
        if (issue !in uiState.value.issueOptions) return
        selectedIssue.value = issue
        if (uiState.value.documents.none { it.issue == issue }) {
            viewModelScope.launch { repository.refreshCaibaoIssue(issue) }
        }
    }

    fun selectPrevious() = selectRelativeIssue(-1)

    fun selectNext() = selectRelativeIssue(1)

    private fun selectRelativeIssue(delta: Int) {
        val options = uiState.value.issueOptions
        val index = options.indexOf(uiState.value.selectedIssue)
        options.getOrNull(index + delta)?.let(::selectIssue)
    }

    fun onImageDecodeFailed(document: CaibaoDocument) {
        viewModelScope.launch { repository.invalidateCaibaoImage(document) }
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
}

internal fun buildCaibaoIssueOptions(
    draws: List<DrawRecord>,
    documents: List<CaibaoDocument>,
    today: LocalDate = LocalDate.now(BEIJING),
): List<String> {
    val cutoff = today.minusDays(29)
    return (draws.asSequence()
        .filter { it.drawDate.toLocalDateOrNull()?.let { date -> date in cutoff..today } == true }
        .map(DrawRecord::issue) + documents.asSequence()
        .filter { it.cachedLocalDate in cutoff..today }
        .map(CaibaoDocument::issue))
        .distinct()
        .sortedDescending()
        .toList()
}

private val BEIJING: ZoneId = ZoneId.of("Asia/Shanghai")

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private object EmptyCaibaoDrawRepository : DrawRepository {
    override val latestDraw = flowOf<DrawRecord?>(null)
    override val allDrawsAscending = flowOf(emptyList<DrawRecord>())
    override val syncMetadata = flowOf<DrawSyncMetadata?>(null)
    override fun observeRecent(limit: Int) = flowOf(emptyList<DrawRecord>())
    override fun observe(query: DrawQuery) = flowOf(emptyList<DrawRecord>())
    override suspend fun refresh(): SyncResult = SyncResult.Throttled
    override suspend fun syncOnForeground(): SyncResult = SyncResult.Throttled
}
