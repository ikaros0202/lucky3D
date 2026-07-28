package com.lucky3d.app.feature.trend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.domain.omission.DigitPosition
import com.lucky3d.app.domain.omission.OmissionCalculator
import com.lucky3d.app.domain.omission.TrendStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class TrendViewModel @Inject constructor(
    repository: DrawRepository,
) : ViewModel() {
    private val window = MutableStateFlow(30)
    private val visiblePositions = MutableStateFlow(TrendPosition.entries.toSet())
    private val selectedPoint = MutableStateFlow<TrendPoint?>(null)
    private val statisticsPosition = MutableStateFlow(TrendPosition.HUNDREDS)
    private val scale = MutableStateFlow(1f)

    val uiState: StateFlow<TrendUiState> = combine(
        repository.allDrawsAscending,
        window,
        visiblePositions,
        selectedPoint,
        combine(statisticsPosition, scale, ::Pair),
    ) { draws, activeWindow, positions, selection, display ->
        buildTrendState(
            allDraws = draws,
            window = activeWindow,
            visiblePositions = positions,
            selectedPoint = selection,
            statisticsPosition = display.first,
            scale = display.second,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TrendUiState(),
    )

    fun setWindow(value: Int) {
        require(value in 1..3334) { "Trend window must be between 1 and 3334" }
        window.value = value
        selectedPoint.value = null
    }

    fun togglePosition(position: TrendPosition) {
        val current = visiblePositions.value
        if (position in current && current.size == 1) return
        visiblePositions.value = if (position in current) current - position else current + position
        if (statisticsPosition.value !in visiblePositions.value) {
            statisticsPosition.value = visiblePositions.value.minBy(TrendPosition::ordinal)
        }
    }

    fun selectPoint(point: TrendPoint?) {
        selectedPoint.value = point
    }

    fun selectLatest() {
        selectedPoint.value = uiState.value.points
            .filter { it.rowIndex == uiState.value.visibleDraws.lastIndex }
            .minByOrNull { it.position.ordinal }
    }

    fun showStatistics(position: TrendPosition) {
        statisticsPosition.value = position
    }

    fun setScale(value: Float) {
        scale.value = value.coerceIn(1f, 2.5f)
    }
}

internal fun buildTrendState(
    allDraws: List<DrawRecord>,
    window: Int,
    visiblePositions: Set<TrendPosition>,
    selectedPoint: TrendPoint? = null,
    statisticsPosition: TrendPosition = TrendPosition.HUNDREDS,
    scale: Float = 1f,
): TrendUiState {
    val visible = allDraws.takeLast(window)
    val positionOrder = TrendPosition.entries.filter(visiblePositions::contains)
    val points = positionOrder.flatMap { position ->
        val fullValues = allDraws.map { it.digitAt(position) }
        val omissionsByDigit = (0..9).associateWith { digit ->
            OmissionCalculator.calculateVisibleWindow(fullValues, digit, visible.size).omissionByDraw
        }
        visible.mapIndexed { rowIndex, draw ->
            val digit = draw.digitAt(position)
            TrendPoint(
                issue = draw.issue,
                rowIndex = rowIndex,
                position = position,
                digit = digit,
                omission = omissionsByDigit.getValue(digit)[rowIndex],
            )
        }
    }
    val numbers = allDraws.map(DrawRecord::number)
    val statistics = positionOrder.map { position ->
        val activity = TrendStatistics.calculate(numbers, position.toDigitPosition(), window)
        val fullValues = allDraws.map { it.digitAt(position) }
        TrendPositionStatistics(
            position = position,
            requestedWindowSize = window,
            actualWindowSize = activity.actualWindowSize,
            sampleComplete = activity.sampleComplete,
            digits = activity.activities.map { digitActivity ->
                val omission = OmissionCalculator.calculate(fullValues, digitActivity.digit)
                TrendDigitStatistics(
                    digit = digitActivity.digit,
                    currentOmission = omission.currentOmission,
                    averageOmission = omission.averageOmission,
                    maxOmission = omission.maxOmission,
                    occurrences = digitActivity.occurrences,
                    heatLevel = digitActivity.level,
                )
            },
        )
    }
    return TrendUiState(
        window = window,
        visibleDraws = visible,
        visiblePositions = visiblePositions,
        points = points,
        statistics = statistics,
        selectedPoint = selectedPoint,
        statisticsPosition = statisticsPosition,
        scale = scale,
    )
}

private fun DrawRecord.digitAt(position: TrendPosition): Int = when (position) {
    TrendPosition.HUNDREDS -> number.hundreds
    TrendPosition.TENS -> number.tens
    TrendPosition.ONES -> number.ones
}

private fun TrendPosition.toDigitPosition(): DigitPosition = when (this) {
    TrendPosition.HUNDREDS -> DigitPosition.HUNDREDS
    TrendPosition.TENS -> DigitPosition.TENS
    TrendPosition.ONES -> DigitPosition.ONES
}
