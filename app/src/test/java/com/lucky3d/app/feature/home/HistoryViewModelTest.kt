package com.lucky3d.app.feature.home

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.MainDispatcherRule
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.data.repository.DrawQuery
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.DrawSyncMetadata
import com.lucky3d.app.data.repository.SyncResult
import com.lucky3d.app.domain.attributes.DrawNumber
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `defaults to latest thirty draws`() = runTest {
        val repository = FakeHistoryRepository()
        val viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.query).isEqualTo(DrawQuery.Recent(30))
    }

    @Test
    fun `invalid issue keeps prior results and exposes field error`() = runTest {
        val repository = FakeHistoryRepository()
        val viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        viewModel.searchIssue("123")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.query).isEqualTo(DrawQuery.Recent(30))
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
}
