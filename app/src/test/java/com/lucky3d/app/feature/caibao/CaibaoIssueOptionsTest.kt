package com.lucky3d.app.feature.caibao

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CaibaoIssueOptionsTest {
    @Test
    fun `issue menu keeps every valid period in descending order`() {
        val result = compactCaibaoIssueOptions(
            listOf(
                "2026198",
                "2026204",
                "2026201",
                "2026203",
                "2026199",
                "2026202",
                "2026200",
                "2026204",
            ),
        )

        assertThat(result)
            .containsExactly(
                "2026204",
                "2026203",
                "2026202",
                "2026201",
                "2026200",
                "2026199",
                "2026198",
            )
            .inOrder()
    }
}
