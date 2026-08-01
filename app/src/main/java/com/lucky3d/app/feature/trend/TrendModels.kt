package com.lucky3d.app.feature.trend

import androidx.compose.runtime.Immutable
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.domain.omission.HeatLevel

enum class TrendPosition {
    HUNDREDS,
    TENS,
    ONES,
}

@Immutable
data class TrendPoint(
    val issue: String,
    val rowIndex: Int,
    val position: TrendPosition,
    val digit: Int,
    val omission: Int,
)

@Immutable
data class TrendTableRow(
    val issue: String,
    val drawNumber: String,
    val omissions: List<Int>,
)

@Immutable
data class TrendDigitStatistics(
    val digit: Int,
    val currentOmission: Int,
    val averageOmission: Double?,
    val maxOmission: Int?,
    val occurrences: Int,
    val heatLevel: HeatLevel,
)

@Immutable
data class TrendPositionStatistics(
    val position: TrendPosition,
    val requestedWindowSize: Int,
    val actualWindowSize: Int,
    val sampleComplete: Boolean,
    val digits: List<TrendDigitStatistics>,
)

@Immutable
data class TrendUiState(
    val window: Int = 30,
    val visibleDraws: List<DrawRecord> = emptyList(),
    val visiblePositions: Set<TrendPosition> = TrendPosition.entries.toSet(),
    val tableRows: List<TrendTableRow> = emptyList(),
    val points: List<TrendPoint> = emptyList(),
    val statistics: List<TrendPositionStatistics> = emptyList(),
    val selectedPoint: TrendPoint? = null,
    val statisticsPosition: TrendPosition = TrendPosition.HUNDREDS,
    val scale: Float = 1f,
)
