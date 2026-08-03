package com.lucky3d.app.domain.omission

import kotlin.math.round

data class OmissionStatistics(
    val omissionByDraw: List<Int>,
    val completedOmissions: List<Int>,
    val currentOmission: Int,
    val averageOmission: Double?,
    val maxOmission: Int?,
)

object OmissionCalculator {
    fun calculate(values: List<Int>, target: Int): OmissionStatistics =
        calculate(values = values, target = target, allowedRange = 0..9)

    fun calculate(
        values: List<Int>,
        target: Int,
        allowedRange: IntRange,
    ): OmissionStatistics {
        require(!allowedRange.isEmpty()) { "Allowed range must not be empty" }
        require(target in allowedRange) { "Target must be within allowed range" }

        var currentOmission = 0
        var previousHitIndex: Int? = null
        val omissionByDraw = ArrayList<Int>(values.size)
        val completed = mutableListOf<Int>()

        values.forEachIndexed { index, value ->
            require(value in allowedRange) { "History values must be within allowed range" }
            if (value == target) {
                previousHitIndex?.let { completed += index - it - 1 }
                previousHitIndex = index
                currentOmission = 0
            } else {
                currentOmission += 1
            }
            omissionByDraw += currentOmission
        }

        val average = completed
            .takeIf(List<Int>::isNotEmpty)
            ?.average()
            ?.let { round(it * 100.0) / 100.0 }

        return OmissionStatistics(
            omissionByDraw = omissionByDraw,
            completedOmissions = completed,
            currentOmission = currentOmission,
            averageOmission = average,
            maxOmission = completed.maxOrNull(),
        )
    }

    fun calculateVisibleWindow(
        values: List<Int>,
        target: Int,
        visibleWindowSize: Int,
    ): OmissionStatistics = calculateVisibleWindow(
        values = values,
        target = target,
        visibleWindowSize = visibleWindowSize,
        allowedRange = 0..9,
    )

    fun calculateVisibleWindow(
        values: List<Int>,
        target: Int,
        visibleWindowSize: Int,
        allowedRange: IntRange,
    ): OmissionStatistics {
        require(visibleWindowSize > 0) { "Visible window must be positive" }
        val full = calculate(values, target, allowedRange)
        return full.copy(omissionByDraw = full.omissionByDraw.takeLast(visibleWindowSize))
    }
}
