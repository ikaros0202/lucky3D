package com.lucky3d.app.domain.attributes

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CrossDrawAttributesTest {
    @Test
    fun `golden repeat and neighbor digits are calculated from distinct digits`() {
        val attributes = CrossDrawAttributes.calculate(
            previous = DrawNumber.parse("275"),
            current = DrawNumber.parse("685"),
        )

        assertThat(attributes.repeatDigits).containsExactly(5)
        assertThat(attributes.neighborDigits).containsExactly(6, 8)
    }

    @Test
    fun `neighbors stop at zero and nine without wrapping`() {
        val attributes = CrossDrawAttributes.calculate(
            previous = DrawNumber.parse("009"),
            current = DrawNumber.parse("180"),
        )

        assertThat(attributes.neighborDigits).containsExactly(1, 8)
        assertThat(attributes.neighborDigits).doesNotContain(9)
    }
}
