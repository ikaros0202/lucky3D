package com.lucky3d.app.domain.omission

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test

class OmissionCalculatorTest {
    @Test
    fun `golden omission sequence uses completed segments only`() {
        val first = OmissionCalculator.calculate(listOf(1, 2, 1, 3), target = 1)
        assertThat(first.omissionByDraw).containsExactly(0, 1, 0, 1).inOrder()
        assertThat(first.completedOmissions).containsExactly(1)
        assertThat(first.currentOmission).isEqualTo(1)
        assertThat(first.averageOmission).isEqualTo(1.0)
        assertThat(first.maxOmission).isEqualTo(1)

        val second = OmissionCalculator.calculate(listOf(1, 2, 3, 4, 1), target = 1)
        assertThat(second.omissionByDraw).containsExactly(0, 1, 2, 3, 0).inOrder()
        assertThat(second.completedOmissions).containsExactly(3)
        assertThat(second.currentOmission).isEqualTo(0)
        assertThat(second.averageOmission).isEqualTo(3.0)
        assertThat(second.maxOmission).isEqualTo(3)
    }

    @Test
    fun `no completed segment returns explainable null history values`() {
        val result = OmissionCalculator.calculate(listOf(2, 3, 4), target = 1)

        assertThat(result.omissionByDraw).containsExactly(1, 2, 3).inOrder()
        assertThat(result.completedOmissions).isEmpty()
        assertThat(result.currentOmission).isEqualTo(3)
        assertThat(result.averageOmission).isNull()
        assertThat(result.maxOmission).isNull()
    }

    @Test
    fun `explicit sum range supports zero and twenty seven`() {
        val zero = OmissionCalculator.calculate(
            values = listOf(0, 9, 27, 0),
            target = 0,
            allowedRange = 0..27,
        )
        val twentySeven = OmissionCalculator.calculateVisibleWindow(
            values = listOf(0, 27, 9, 27),
            target = 27,
            visibleWindowSize = 2,
            allowedRange = 0..27,
        )

        assertThat(zero.omissionByDraw).containsExactly(0, 1, 2, 0).inOrder()
        assertThat(zero.completedOmissions).containsExactly(2)
        assertThat(twentySeven.omissionByDraw).containsExactly(1, 0).inOrder()
    }

    @Test
    fun `explicit range rejects targets and history outside its bounds`() {
        assertFailsWith<IllegalArgumentException> {
            OmissionCalculator.calculate(listOf(0, 27), target = 28, allowedRange = 0..27)
        }
        assertFailsWith<IllegalArgumentException> {
            OmissionCalculator.calculate(listOf(0, -1), target = 0, allowedRange = 0..27)
        }
    }

    @Test
    fun `legacy digit overload remains fixed to zero through nine`() {
        assertFailsWith<IllegalArgumentException> {
            OmissionCalculator.calculate(listOf(0, 10), target = 0)
        }
    }
}
