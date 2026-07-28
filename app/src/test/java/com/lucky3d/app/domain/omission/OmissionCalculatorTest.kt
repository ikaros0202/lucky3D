package com.lucky3d.app.domain.omission

import com.google.common.truth.Truth.assertThat
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
}
