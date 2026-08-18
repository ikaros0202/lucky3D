package com.lucky3d.app.feature.caibao

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.MainDispatcherRule
import com.lucky3d.app.core.model.CaibaoDocument
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.data.repository.CaibaoImageReadResult
import com.lucky3d.app.data.repository.DrawQuery
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.DrawSyncMetadata
import com.lucky3d.app.data.repository.LiveContentRepository
import com.lucky3d.app.data.repository.SyncResult
import com.lucky3d.app.domain.livecontent.LiveContentFailure
import com.lucky3d.app.domain.livecontent.LiveContentRefreshResult
import com.lucky3d.app.domain.livecontent.LiveContentRefreshState
import com.lucky3d.app.domain.livecontent.LiveRefreshTrigger
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaibaoViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `issue options use valid local draw issues in thirty day window`() {
        val today = LocalDate.of(2026, 8, 3)
        val draws = listOf(
            draw("2026198", "2026-08-03"),
            draw("2026197", "2026-08-01"),
            draw("2026196", "2026-07-04"),
        )

        assertThat(buildCaibaoIssueOptions(draws, emptyList(), today))
            .containsExactly("2026198", "2026197")
            .inOrder()
    }

    @Test
    fun `previous selects smaller older issue and next selects larger newer issue`() = runTest {
        val repository = FakeLiveContentRepository().apply {
            caibao.value = caibao()
        }
        val viewModel = CaibaoViewModel(repository, adjacentIssueDrawRepository())
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedIssue).isEqualTo("2026204")

        viewModel.selectPrevious()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedIssue).isEqualTo("2026203")

        viewModel.selectNext()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedIssue).isEqualTo("2026204")
    }

    @Test
    fun `relative issue selection stays put when requested neighbor is absent`() = runTest {
        val repository = FakeLiveContentRepository().apply {
            caibao.value = caibao()
        }
        val viewModel = CaibaoViewModel(repository, adjacentIssueDrawRepository())
        advanceUntilIdle()

        viewModel.selectNext()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedIssue).isEqualTo("2026204")

        viewModel.selectIssue("2026202")
        advanceUntilIdle()
        viewModel.selectPrevious()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedIssue).isEqualTo("2026202")
    }

    @Test
    fun `first visible event triggers exactly one automatic caibao refresh`() = runTest {
        val repository = FakeLiveContentRepository()
        val viewModel = CaibaoViewModel(repository)
        advanceUntilIdle()

        viewModel.onVisible()
        viewModel.onVisible()
        advanceUntilIdle()

        assertThat(repository.caibaoRefreshTriggers)
            .containsExactly(LiveRefreshTrigger.CAIBAO_VISIBLE)
    }

    @Test
    fun `manual refresh uses manual trigger`() = runTest {
        val repository = FakeLiveContentRepository()
        val viewModel = CaibaoViewModel(repository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertThat(repository.caibaoRefreshTriggers)
            .containsExactly(LiveRefreshTrigger.MANUAL)
    }

    @Test
    fun `refreshing state is exposed without clearing cached document`() = runTest {
        val cached = caibao()
        val repository = FakeLiveContentRepository().apply {
            caibao.value = cached
            caibaoState.value = LiveContentRefreshState.Refreshing
        }

        val viewModel = CaibaoViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.document).isEqualTo(cached)
        assertThat(viewModel.uiState.value.imageBytes).isEqualTo(IMAGE_BYTES)
        assertThat(viewModel.uiState.value.refreshState)
            .isEqualTo(LiveContentRefreshState.Refreshing)
    }

    @Test
    fun `refresh failure keeps cached document readable`() = runTest {
        val cached = caibao()
        val repository = FakeLiveContentRepository().apply {
            caibao.value = cached
            caibaoState.value = LiveContentRefreshState.Failed(LiveContentFailure.NETWORK)
        }

        val viewModel = CaibaoViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.document).isEqualTo(cached)
        assertThat(viewModel.uiState.value.imageBytes).isEqualTo(IMAGE_BYTES)
        assertThat(viewModel.uiState.value.hasCachedContent).isTrue()
        assertThat(viewModel.uiState.value.refreshState)
            .isEqualTo(LiveContentRefreshState.Failed(LiveContentFailure.NETWORK))
    }

    @Test
    fun `refresh failure without cache remains explicit and retryable`() = runTest {
        val repository = FakeLiveContentRepository().apply {
            caibaoState.value = LiveContentRefreshState.Failed(LiveContentFailure.INVALID_IMAGE)
        }

        val viewModel = CaibaoViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.document).isNull()
        assertThat(viewModel.uiState.value.hasCachedContent).isFalse()
        assertThat(viewModel.uiState.value.refreshState)
            .isEqualTo(LiveContentRefreshState.Failed(LiveContentFailure.INVALID_IMAGE))
    }

    @Test
    fun `validated local image bytes make cached document readable`() = runTest {
        val cached = caibao()
        val repository = FakeLiveContentRepository().apply {
            caibao.value = cached
            imageReadResult = CaibaoImageReadResult.Loaded(IMAGE_BYTES)
        }

        val viewModel = CaibaoViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.document).isEqualTo(cached)
        assertThat(viewModel.uiState.value.imageBytes).isEqualTo(IMAGE_BYTES)
        assertThat(viewModel.uiState.value.localImageAvailable).isTrue()
        assertThat(repository.readDocuments).containsExactly(cached)
    }

    @Test
    fun `unreadable local image is never exposed as cached content`() = runTest {
        val cached = caibao()
        val repository = FakeLiveContentRepository().apply {
            caibao.value = cached
            imageReadResult =
                CaibaoImageReadResult.Unavailable(LiveContentFailure.INVALID_IMAGE)
        }

        val viewModel = CaibaoViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.document).isNull()
        assertThat(viewModel.uiState.value.imageBytes).isNull()
        assertThat(viewModel.uiState.value.hasCachedContent).isFalse()
        assertThat(repository.readDocuments).containsExactly(cached)
    }

    @Test
    fun `decode failure delegates matching document cleanup`() = runTest {
        val cached = caibao()
        val repository = FakeLiveContentRepository().apply {
            caibao.value = cached
        }
        val viewModel = CaibaoViewModel(repository)
        advanceUntilIdle()

        viewModel.onImageDecodeFailed(cached)
        advanceUntilIdle()

        assertThat(repository.invalidatedDocuments).containsExactly(cached)
    }

    private class FakeLiveContentRepository : LiveContentRepository {
        val caibao = MutableStateFlow<CaibaoDocument?>(null)
        val caibaoState =
            MutableStateFlow<LiveContentRefreshState>(LiveContentRefreshState.Idle)
        val caibaoRefreshTriggers = mutableListOf<LiveRefreshTrigger>()
        val readDocuments = mutableListOf<CaibaoDocument>()
        val invalidatedDocuments = mutableListOf<CaibaoDocument>()
        var imageReadResult: CaibaoImageReadResult =
            CaibaoImageReadResult.Loaded(IMAGE_BYTES)

        override val trialNumber = MutableStateFlow(null)
        override val trialRefreshState =
            MutableStateFlow<LiveContentRefreshState>(LiveContentRefreshState.Idle)
        override val caibaoDocument: Flow<CaibaoDocument?> = caibao
        override val caibaoRefreshState: Flow<LiveContentRefreshState> = caibaoState

        override suspend fun refreshTrial(
            trigger: LiveRefreshTrigger,
        ): LiveContentRefreshResult = LiveContentRefreshResult.Success

        override suspend fun refreshCaibao(
            trigger: LiveRefreshTrigger,
        ): LiveContentRefreshResult {
            caibaoRefreshTriggers += trigger
            return LiveContentRefreshResult.Success
        }

        override suspend fun readCaibaoImage(
            document: CaibaoDocument,
        ): CaibaoImageReadResult {
            readDocuments += document
            return imageReadResult
        }

        override suspend fun invalidateCaibaoImage(document: CaibaoDocument) {
            invalidatedDocuments += document
        }

        override suspend fun cleanCaibaoCache() = Unit
    }

    private class FakeDrawRepository(
        draws: List<DrawRecord>,
    ) : DrawRepository {
        override val latestDraw = flowOf(draws.maxByOrNull(DrawRecord::issue))
        override val allDrawsAscending = flowOf(draws.sortedBy(DrawRecord::issue))
        override val syncMetadata = flowOf<DrawSyncMetadata?>(null)

        override fun observeRecent(limit: Int) = flowOf(emptyList<DrawRecord>())

        override fun observe(query: DrawQuery) = flowOf(emptyList<DrawRecord>())

        override suspend fun refresh(): SyncResult = SyncResult.Throttled

        override suspend fun syncOnForeground(): SyncResult = SyncResult.Throttled
    }

    private fun adjacentIssueDrawRepository(): DrawRepository {
        val today = LocalDate.now(ZoneId.of("Asia/Shanghai"))
        return FakeDrawRepository(
            listOf(
                draw("2026202", today.minusDays(2).toString()),
                draw("2026203", today.minusDays(1).toString()),
                draw("2026204", today.toString()),
            ),
        )
    }

    private fun caibao() = CaibaoDocument(
        issue = "2026204",
        edition = "A11",
        title = "彩吧彩报第三版",
        sourcePageUrl = "https://www.cz89.com/",
        imageUrl = "https://www.cz89.com/a11.png",
        localFileName = "2026204-A11-test.png",
        sha256 = "A".repeat(64),
        mimeType = "image/png",
        width = 720,
        height = 1280,
        cachedLocalDate = LocalDate.parse("2026-07-31"),
        fetchedAtEpochMillis = 1L,
    )

    private fun draw(issue: String, date: String) = DrawRecord(
        issue = issue,
        drawDate = date,
        number = com.lucky3d.app.domain.attributes.DrawNumber.parse("123"),
        officialDetailUrl = "https://example.com/$issue",
        officialFingerprint = issue,
    )

    private companion object {
        val IMAGE_BYTES = byteArrayOf(1, 2, 3, 4)
    }
}
