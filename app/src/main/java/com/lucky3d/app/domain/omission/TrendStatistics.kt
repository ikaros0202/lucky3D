package com.lucky3d.app.domain.omission

import com.lucky3d.app.domain.attributes.DrawNumber

enum class DigitPosition { HUNDREDS, TENS, ONES, ALL }

enum class HeatLevel { COLD, WARM, HOT }

data class DigitActivity(
    val digit: Int,
    val occurrences: Int,
    val expectedOccurrences: Double,
    val activityRatio: Double,
    val level: HeatLevel,
)

data class TrendStatistics(
    val requestedWindowSize: Int,
    val actualWindowSize: Int,
    val sampleComplete: Boolean,
    val activities: List<DigitActivity>,
) {
    companion object {
        fun calculate(
            history: List<DrawNumber>,
            position: DigitPosition,
            windowSize: Int,
        ): TrendStatistics {
            require(windowSize > 0) { "Window size must be positive" }
            val window = history.takeLast(windowSize)
            val observedDigits = when (position) {
                DigitPosition.HUNDREDS -> window.map(DrawNumber::hundreds)
                DigitPosition.TENS -> window.map(DrawNumber::tens)
                DigitPosition.ONES -> window.map(DrawNumber::ones)
                DigitPosition.ALL -> window.flatMap(DrawNumber::digits)
            }
            val expected = when (position) {
                DigitPosition.ALL -> 3.0 * window.size / 10.0
                else -> window.size / 10.0
            }

            val activities = (0..9).map { digit ->
                val occurrences = observedDigits.count { it == digit }
                val ratio = if (expected == 0.0) 0.0 else occurrences / expected
                DigitActivity(
                    digit = digit,
                    occurrences = occurrences,
                    expectedOccurrences = expected,
                    activityRatio = ratio,
                    level = when {
                        ratio < 0.8 -> HeatLevel.COLD
                        ratio < 1.2 -> HeatLevel.WARM
                        else -> HeatLevel.HOT
                    },
                )
            }

            return TrendStatistics(
                requestedWindowSize = windowSize,
                actualWindowSize = window.size,
                sampleComplete = window.size == windowSize,
                activities = activities,
            )
        }
    }
}
