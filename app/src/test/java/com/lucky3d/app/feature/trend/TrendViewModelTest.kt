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
    fun `fit scale is bounded by one and uses complete logical width`() {
        assertThat(calculateTrendFitScale(360f, 1_488f)).isWithin(0.0001f).of(360f / 1_488f)
        assertThat(calculateTrendFitScale(2_000f, 1_488f)).isEqualTo(1f)
    }

    @Test
    fun `viewport scale changes every table dimension by the same ratio`() {
        val metrics = calculateTrendViewportMetrics(
            scale = 0.5f,
            baseLockedWidth = 80f,
        )

        assertThat(metrics.lockedWidth).isEqualTo(40f)
        assertThat(metrics.cellWidth).isEqualTo(17f)
        assertThat(metrics.prefixWidth).isEqualTo(24f)
        assertThat(metrics.attributeWidth).isEqualTo(31f)
        assertThat(metrics.rowHeight).isEqualTo(16f)
        assertThat(metrics.groupHeight).isEqualTo(22f)
        assertThat(metrics.textScale).isEqualTo(0.5f)
    }

    @Test
    fun `pinch zoom respects fit and maximum scale limits`() {
        assertThat(
            calculateTrendScaleAfterZoom(
                currentScale = 1f,
                zoomChange = 0.1f,
                fitScale = 0.32f,
            ),
        ).isEqualTo(0.32f)
        assertThat(
            calculateTrendScaleAfterZoom(
                currentScale = 2f,
                zoomChange = 2f,
                fitScale = 0.32f,
            ),
        ).isEqualTo(2.5f)
    }

    @Test
    fun `unified viewport keeps one logical cell under a moving two finger centroid`() {
        val bounds = TrendViewportBounds(
            viewportWidth = 1080f,
            viewportHeight = 1800f,
            baseLockedWidth = 240f,
            baseHeaderHeight = 228f,
            rightContentWidth = 4464f,
            bodyContentHeight = 10176f,
            fitScale = 0.23f,
        )
        var centroidX = 690f
        var centroidY = 930f
        val logicalX = (900f + centroidX - bounds.baseLockedWidth) / 1f
        val logicalY = (1600f + centroidY - bounds.baseHeaderHeight) / 1f
        var viewport = TrendViewport(
            scale = 1f,
            offsetX = 900f,
            offsetY = 1600f,
        )

        repeat(5) {
            val panX = 7f
            val panY = -9f
            viewport = transformTrendViewport(
                viewport = viewport,
                zoomChange = 0.88f,
                centroidX = centroidX,
                centroidY = centroidY,
                panX = panX,
                panY = panY,
                bounds = bounds,
            )
            centroidX += panX
            centroidY += panY

            val renderedX = bounds.baseLockedWidth * viewport.scale +
                logicalX * viewport.scale - viewport.offsetX
            val renderedY = bounds.baseHeaderHeight * viewport.scale +
                logicalY * viewport.scale - viewport.offsetY
            assertThat(renderedX).isWithin(0.001f).of(centroidX)
            assertThat(renderedY).isWithin(0.001f).of(centroidY)
        }
    }

    @Test
    fun `shrinking then enlarging at the top left never exposes empty canvas`() {
        val bounds = TrendViewportBounds(
            viewportWidth = 1080f,
            viewportHeight = 1800f,
            baseLockedWidth = 240f,
            baseHeaderHeight = 228f,
            rightContentWidth = 4464f,
            bodyContentHeight = 10176f,
            fitScale = 0.23f,
        )
        val centroidX = 690f
        val centroidY = 930f
        val shrunken = transformTrendViewport(
            viewport = TrendViewport(scale = 1f),
            zoomChange = 0.4f,
            centroidX = centroidX,
            centroidY = centroidY,
            panX = 0f,
            panY = 0f,
            bounds = bounds,
        )
        val enlarged = transformTrendViewport(
            viewport = shrunken,
            zoomChange = 2f,
            centroidX = centroidX,
            centroidY = centroidY,
            panX = 0f,
            panY = 0f,
            bounds = bounds,
        )

        assertThat(shrunken.offsetX).isEqualTo(0f)
        assertThat(shrunken.offsetY).isEqualTo(0f)
        assertThat(enlarged.offsetX).isAtLeast(0f)
        assertThat(enlarged.offsetY).isAtLeast(0f)
    }

    @Test
    fun `unified viewport clamps both axes against the same rendered bounds`() {
        val bounds = TrendViewportBounds(
            viewportWidth = 1080f,
            viewportHeight = 1800f,
            baseLockedWidth = 240f,
            baseHeaderHeight = 228f,
            rightContentWidth = 4464f,
            bodyContentHeight = 10176f,
            fitScale = 0.23f,
        )

        val viewport = constrainTrendViewport(
            viewport = TrendViewport(
                scale = 0.5f,
                offsetX = Float.MAX_VALUE,
                offsetY = Float.MAX_VALUE,
            ),
            bounds = bounds,
        )

        assertThat(viewport.offsetX).isEqualTo(1272f)
        assertThat(viewport.offsetY).isEqualTo(3402f)
    }

    @Test
    fun `single finger vertical pan updates only the unified vertical offset`() {
        val bounds = TrendViewportBounds(
            viewportWidth = 1080f,
            viewportHeight = 1800f,
            baseLockedWidth = 240f,
            baseHeaderHeight = 228f,
            rightContentWidth = 4464f,
            bodyContentHeight = 10176f,
            fitScale = 0.23f,
        )

        val viewport = panTrendViewport(
            viewport = TrendViewport(scale = 1f, offsetX = 360f, offsetY = 900f),
            panX = 0f,
            panY = -180f,
            allowHorizontal = false,
            bounds = bounds,
        )

        assertThat(viewport.offsetX).isEqualTo(360f)
        assertThat(viewport.offsetY).isEqualTo(1080f)
    }

    @Test
    fun `repeated vertical swipes cannot drag the table beyond either content edge`() {
        val bounds = TrendViewportBounds(
            viewportWidth = 1080f,
            viewportHeight = 1800f,
            baseLockedWidth = 240f,
            baseHeaderHeight = 228f,
            rightContentWidth = 4464f,
            bodyContentHeight = 10176f,
            fitScale = 0.23f,
        )

        val draggedPastTop = panTrendViewport(
            viewport = TrendViewport(scale = 1f),
            panX = 0f,
            panY = 900f,
            allowHorizontal = false,
            bounds = bounds,
        )
        val draggedPastBottom = panTrendViewport(
            viewport = TrendViewport(scale = 1f, offsetY = 8_000f),
            panX = 0f,
            panY = -5_000f,
            allowHorizontal = false,
            bounds = bounds,
        )

        assertThat(draggedPastTop.offsetY).isEqualTo(0f)
        assertThat(draggedPastBottom.offsetY).isEqualTo(8_604f)
    }

    @Test
    fun `down then right swipes at the top left cannot expose empty canvas`() {
        val bounds = TrendViewportBounds(
            viewportWidth = 1080f,
            viewportHeight = 1800f,
            baseLockedWidth = 240f,
            baseHeaderHeight = 228f,
            rightContentWidth = 4464f,
            bodyContentHeight = 10176f,
            fitScale = 0.23f,
        )

        val afterDown = panTrendViewport(
            viewport = TrendViewport(scale = 1f),
            panX = 0f,
            panY = 700f,
            allowHorizontal = false,
            bounds = bounds,
        )
        val afterRight = panTrendViewport(
            viewport = afterDown,
            panX = 700f,
            panY = 0f,
            allowHorizontal = true,
            bounds = bounds,
        )

        assertThat(afterRight.offsetX).isEqualTo(0f)
        assertThat(afterRight.offsetY).isEqualTo(0f)
    }

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
    fun `trend table exposes thirty omission cells per draw in issue order`() = runTest {
        val records = records(120)
        val viewModel = TrendViewModel(FakeTrendRepository(records))
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertThat(state.tableRows.map(TrendTableRow::issue))
            .containsExactlyElementsIn(records.takeLast(30).map(DrawRecord::issue))
            .inOrder()
        assertThat(state.tableRows.first().omissions).hasSize(30)
        assertThat(state.tableRows.first().omissions.take(10))
            .containsExactlyElementsIn(
                (0..9).map { digit ->
                    OmissionCalculator.calculateVisibleWindow(
                        records.map { it.number.hundreds },
                        digit,
                        30,
                    ).omissionByDraw.first()
                },
            )
            .inOrder()
    }

    @Test
    fun `trend table always retains all three position groups`() = runTest {
        val viewModel = TrendViewModel(FakeTrendRepository(records(120)))
        advanceUntilIdle()

        viewModel.togglePosition(TrendPosition.HUNDREDS)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.tableRows).isNotEmpty()
        assertThat(viewModel.uiState.value.tableRows.first().omissions).hasSize(30)
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
