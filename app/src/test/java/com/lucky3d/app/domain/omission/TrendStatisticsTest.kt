package com.lucky3d.app.domain.omission

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.domain.attributes.DrawNumber
import org.junit.Test

class TrendStatisticsTest {
    @Test
    fun `window changes occurrences but not full-history omission`() {
        val history = (0 until 120).map { DrawNumber.of(it % 10, (it + 1) % 10, (it + 2) % 10) }
        val fullOmission = OmissionCalculator.calculate(history.map { it.hundreds }, target = 1)
        val visibleThirty = OmissionCalculator.calculateVisibleWindow(
            history.map { it.hundreds },
            target = 1,
            visibleWindowSize = 30,
        )

        val thirty = TrendStatistics.calculate(history, DigitPosition.HUNDREDS, windowSize = 30)
        val hundred = TrendStatistics.calculate(history, DigitPosition.HUNDREDS, windowSize = 100)

        assertThat(thirty.activities.single { it.digit == 1 }.occurrences).isEqualTo(3)
        assertThat(hundred.activities.single { it.digit == 1 }.occurrences).isEqualTo(10)
        assertThat(visibleThirty.omissionByDraw)
            .containsExactlyElementsIn(fullOmission.omissionByDraw.takeLast(30))
            .inOrder()
    }

    @Test
    fun `combined positions use three times the single-position expectation`() {
        val history = List(10) { DrawNumber.parse("111") }
        val stats = TrendStatistics.calculate(history, DigitPosition.ALL, windowSize = 10)
        val one = stats.activities.single { it.digit == 1 }

        assertThat(one.occurrences).isEqualTo(30)
        assertThat(one.expectedOccurrences).isEqualTo(3.0)
        assertThat(one.level).isEqualTo(HeatLevel.HOT)
        assertThat(stats.sampleComplete).isTrue()
    }

    @Test
    fun `short history is marked as an incomplete window`() {
        val stats = TrendStatistics.calculate(
            history = listOf(DrawNumber.parse("123")),
            position = DigitPosition.HUNDREDS,
            windowSize = 30,
        )

        assertThat(stats.sampleComplete).isFalse()
        assertThat(stats.actualWindowSize).isEqualTo(1)
    }
}
