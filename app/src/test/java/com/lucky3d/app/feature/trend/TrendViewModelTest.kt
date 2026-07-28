package com.lucky3d.app.feature.trend

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.MainDispatcherRule
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.data.repository.DrawQuery
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.DrawSyncMetadata
import com.lucky3d.app.data.repository.SyncResult
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.omission.OmissionCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrendViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `default thirty-period matrix maps issue position and digit exactly`() = runTest {
        val records = records(120)
        val viewModel = TrendViewModel(FakeTrendRepository(records))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.visibleDraws).hasSize(30)
        assertThat(state.points).hasSize(90)
        assertThat(state.points.first()).isEqualTo(
            TrendPoint(
                issue = records[90].issue,
                rowIndex = 0,
                position = TrendPosition.HUNDREDS,
                digit = records[90].number.hundreds,
                omission = OmissionCalculator.calculate(
                    records.map { it.number.hundreds },
                    records[90].number.hundreds,
                ).omissionByDraw[90],
            ),
        )
    }

    @Test
    fun `hundred-period selection drives statistics from the same window`() = runTest {
        val viewModel = TrendViewModel(FakeTrendRepository(records(120)))
        advanceUntilIdle()

        viewModel.setWindow(100)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.visibleDraws).hasSize(100)
        assertThat(state.statistics.first().actualWindowSize).isEqualTo(100)
        assertThat(state.points).hasSize(300)
    }

    @Test
    fun `hiding a position removes its points without changing selected window`() = runTest {
        val viewModel = TrendViewModel(FakeTrendRepository(records(120)))
        advanceUntilIdle()

        viewModel.togglePosition(TrendPosition.HUNDREDS)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.points).hasSize(60)
        assertThat(viewModel.uiState.value.visiblePositions)
            .containsExactly(TrendPosition.TENS, TrendPosition.ONES)
    }

    private class FakeTrendRepository(records: List<DrawRecord>) : DrawRepository {
        private val values = MutableStateFlow(records)
        override val latestDraw: Flow<DrawRecord?> = MutableStateFlow(records.lastOrNull())
        override val allDrawsAscending: Flow<List<DrawRecord>> = values
        override val syncMetadata: Flow<DrawSyncMetadata?> = MutableStateFlow(null)
        override fun observeRecent(limit: Int): Flow<List<DrawRecord>> = values
        override fun observe(query: DrawQuery): Flow<List<DrawRecord>> = values
        override suspend fun refresh(): SyncResult = SyncResult.Throttled
        override suspend fun syncOnForeground(): SyncResult = SyncResult.Throttled
    }

    private fun records(count: Int): List<DrawRecord> = (1..count).map { index ->
        val issue = (2026000 + index).toString()
        DrawRecord(
            issue = issue,
            drawDate = "2026-01-${((index - 1) % 28 + 1).toString().padStart(2, '0')}",
            number = DrawNumber.of(index % 10, (index + 1) % 10, (index + 2) % 10),
            officialDetailUrl = "https://www.cwl.gov.cn/c/$issue.shtml",
            officialFingerprint = "fingerprint-$issue",
        )
    }
}
