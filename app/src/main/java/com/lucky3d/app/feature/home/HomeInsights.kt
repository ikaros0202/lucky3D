package com.lucky3d.app.feature.home

import androidx.compose.runtime.Immutable
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.domain.attributes.DrawAttributes
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.omission.DigitPosition
import com.lucky3d.app.domain.omission.HeatLevel
import com.lucky3d.app.domain.omission.OmissionCalculator
import com.lucky3d.app.domain.omission.TrendStatistics

@Immutable
data class HomeInsights(
    val attributes: DrawAttributes?,
    val positions: List<HomePositionInsight>,
    val coldHits: List<HomeColdHit>,
) {
    companion object {
        val Empty = HomeInsights(
            attributes = null,
            positions = emptyList(),
            coldHits = emptyList(),
        )
    }
}

@Immutable
data class HomePositionInsight(
    val position: DigitPosition,
    val digit: Int,
    val previousOmission: Int,
    val occurrences: Int,
    val heatLevel: HeatLevel?,
    val windowSize: Int,
)

@Immutable
data class HomeColdHit(
    val position: DigitPosition,
    val digit: Int,
    val previousOmission: Int,
    val windowSize: Int,
)

internal fun buildHomeInsights(
    drawsDescending: List<DrawRecord>,
    window: Int = 30,
): HomeInsights {
    require(window > 0) { "Window must be positive" }
    val latest = drawsDescending.firstOrNull() ?: return HomeInsights.Empty
    val previousAscending = drawsDescending
        .drop(1)
        .take(window)
        .asReversed()
    val previousNumbers = previousAscending.map(DrawRecord::number)

    val positions = listOf(
        DigitPosition.HUNDREDS,
        DigitPosition.TENS,
        DigitPosition.ONES,
    ).map { position ->
        val digit = position.digit(latest.number)
        val previousDigits = previousNumbers.map(position::digit)
        val activity = TrendStatistics
            .calculate(previousNumbers, position, window)
            .activities
            .first { it.digit == digit }
        HomePositionInsight(
            position = position,
            digit = digit,
            previousOmission = OmissionCalculator
                .calculate(previousDigits, digit)
                .currentOmission,
            occurrences = activity.occurrences,
            heatLevel = activity.level.takeIf { previousNumbers.isNotEmpty() },
            windowSize = previousNumbers.size,
        )
    }

    return HomeInsights(
        attributes = DrawAttributes.calculate(latest.number),
        positions = positions,
        coldHits = positions
            .filter { it.windowSize > 0 && it.heatLevel == HeatLevel.COLD }
            .map {
                HomeColdHit(
                    position = it.position,
                    digit = it.digit,
                    previousOmission = it.previousOmission,
                    windowSize = it.windowSize,
                )
            },
    )
}

internal fun consecutiveDigitsLabel(number: String): String {
    val digits = DrawNumber.parse(number).digits.distinct().sorted()
    val runs = buildList {
        var current = mutableListOf<Int>()
        digits.forEach { digit ->
            if (current.isEmpty() || digit == current.last() + 1) {
                current += digit
            } else {
                if (current.size >= 2) add(current.toList())
                current = mutableListOf(digit)
            }
        }
        if (current.size >= 2) add(current.toList())
    }
    return runs.maxByOrNull(List<Int>::size)?.joinToString("-") ?: "无"
}

private fun DigitPosition.digit(number: DrawNumber): Int = when (this) {
    DigitPosition.HUNDREDS -> number.hundreds
    DigitPosition.TENS -> number.tens
    DigitPosition.ONES -> number.ones
    DigitPosition.ALL -> error("Home insights require a concrete digit position")
}
