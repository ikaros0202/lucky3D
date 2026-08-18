package com.lucky3d.app.feature.home

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.MainDispatcherRule
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.core.model.YunnanAnnouncement
import com.lucky3d.app.core.model.YunnanPlayAnnouncement
import com.lucky3d.app.core.model.YunnanPlayType
import com.lucky3d.app.data.remote.YunnanAnnouncementDataResult
import com.lucky3d.app.data.repository.DrawQuery
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.DrawSyncMetadata
import com.lucky3d.app.data.repository.SyncResult
import com.lucky3d.app.data.repository.YunnanAnnouncementRepository
import com.lucky3d.app.domain.attributes.DrawNumber
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `defaults to an empty issue-first query`() = runTest {
        val repository = FakeHistoryRepository()
        val viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.query)
            .isEqualTo(DrawQuery.Recent(1))
    }

    @Test
    fun `invalid issue keeps prior results and exposes field error`() = runTest {
        val repository = FakeHistoryRepository()
        val viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        viewModel.searchIssue("123")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.query)
            .isEqualTo(DrawQuery.Recent(1))
        assertThat(viewModel.uiState.value.inputError).isEqualTo(HistoryInputError.INVALID_ISSUE)
    }

    @Test
    fun `valid issue switches repository query`() = runTest {
        val repository = FakeHistoryRepository()
        val viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        viewModel.searchIssue("2017001")
        advanceUntilIdle()

        assertThat(repository.lastQuery.value).isEqualTo(DrawQuery.Issue("2017001"))
        assertThat(viewModel.uiState.value.inputError).isNull()
    }

    @Test
    fun `failed refresh hides cached announcement instead of reporting it available`() = runTest {
        val repository = FakeHistoryRepository()
        val yunnan = FakeHistoryYunnanRepository().apply {
            cached.value = announcement("2017001")
            refreshResult = YunnanAnnouncementDataResult.InvalidPayload("mismatch")
        }
        val viewModel = HistoryViewModel(repository, yunnan)
        advanceUntilIdle()

        viewModel.searchIssue("2017001")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.yunnanAnnouncement).isNull()
        assertThat(viewModel.uiState.value.announcementState)
            .isEqualTo(HistoryAnnouncementState.UNAVAILABLE)
    }

    @Test
    fun `date query switches repository to a single-day range`() = runTest {
        val repository = FakeHistoryRepository()
        val viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        viewModel.searchDate("2017-01-01")
        advanceUntilIdle()

        assertThat(repository.lastQuery.value)
            .isEqualTo(DrawQuery.DateRange("2017-01-01", "2017-01-01"))
        assertThat(viewModel.uiState.value.inputError).isNull()
    }

    @Test
    fun `date before bundled baseline is rejected`() = runTest {
        val repository = FakeHistoryRepository()
        val viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        viewModel.searchDate("2016-12-31")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.inputError)
            .isEqualTo(HistoryInputError.INVALID_DATE_RANGE)
        assertThat(repository.lastQuery.value).isEqualTo(viewModel.uiState.value.query)
    }

    @Test
    fun `date after Beijing today is rejected`() = runTest {
        val repository = FakeHistoryRepository()
        val viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        val tomorrow = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1).toString()
        viewModel.searchDate(tomorrow)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.inputError)
            .isEqualTo(HistoryInputError.INVALID_DATE_RANGE)
    }

    private class FakeHistoryRepository : DrawRepository {
        private val records = MutableStateFlow(
            listOf(
                DrawRecord(
                    issue = "2017001",
                    drawDate = "2017-01-01",
                    number = DrawNumber.parse("007"),
                    officialDetailUrl = "https://www.cwl.gov.cn/c/2017001.shtml",
                    officialFingerprint = "fingerprint",
                ),
            ),
        )
        val lastQuery = MutableStateFlow<DrawQuery>(DrawQuery.Recent(30))

        override val latestDraw: Flow<DrawRecord?> = MutableStateFlow(records.value.first())
        override val allDrawsAscending: Flow<List<DrawRecord>> = records
        override val syncMetadata: Flow<DrawSyncMetadata?> = MutableStateFlow(null)

        override fun observeRecent(limit: Int): Flow<List<DrawRecord>> = records

        override fun observe(query: DrawQuery): Flow<List<DrawRecord>> {
            lastQuery.value = query
            return records
        }

        override suspend fun refresh(): SyncResult = SyncResult.Throttled

        override suspend fun syncOnForeground(): SyncResult = SyncResult.Throttled
    }

    private class FakeHistoryYunnanRepository : YunnanAnnouncementRepository {
        val cached = MutableStateFlow<YunnanAnnouncement?>(null)
        var refreshResult: YunnanAnnouncementDataResult = YunnanAnnouncementDataResult.EmptyResponse

        override val latestAnnouncement = cached

        override fun observeByIssue(issue: String): Flow<YunnanAnnouncement?> = cached

        override suspend fun refreshRecent(limit: Int): YunnanAnnouncementDataResult = refreshResult

        override suspend fun refreshIssue(issue: String): YunnanAnnouncementDataResult = refreshResult
    }

    private fun announcement(issue: String) = YunnanAnnouncement(
        issue = issue,
        drawDate = "2017-01-01",
        number = DrawNumber.parse("007"),
        salesAmountYuan = 100L,
        winningTotalYuan = 50L,
        plays = listOf(
            YunnanPlayAnnouncement(YunnanPlayType.SINGLE, 1L, 1_040L),
            YunnanPlayAnnouncement(YunnanPlayType.GROUP3, 0L, 346L),
            YunnanPlayAnnouncement(YunnanPlayType.GROUP6, 0L, 173L),
        ),
        redemptionDeadline = null,
        sourceUpdatedAt = "2017-01-01 21:48:00",
        fetchedAtEpochMillis = 1L,
        fingerprint = "announcement-$issue",
    )
}
