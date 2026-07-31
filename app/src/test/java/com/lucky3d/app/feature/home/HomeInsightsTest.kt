package com.lucky3d.app.feature.home

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.omission.DigitPosition
import com.lucky3d.app.domain.omission.HeatLevel
import org.junit.Test

class HomeInsightsTest {
    @Test
    fun `latest draw attributes and position activity use only the preceding window`() {
        val previous = (1..30).map { index ->
            val number = when {
                index <= 3 -> "181"
                index <= 9 -> "115"
                else -> "111"
            }
            draw(issue = "2026${index.toString().padStart(3, '0')}", number = number)
        }
        val latest = draw(issue = "2026198", number = "685")

        val insights = buildHomeInsights(
            drawsDescending = listOf(latest) + previous.asReversed(),
            window = 30,
        )

        assertThat(insights.attributes?.sum).isEqualTo(19)
        assertThat(insights.attributes?.sumTail).isEqualTo(9)
        assertThat(insights.attributes?.span).isEqualTo(3)
        assertThat(insights.attributes?.oddCount).isEqualTo(1)
        assertThat(insights.attributes?.evenCount).isEqualTo(2)
        assertThat(insights.attributes?.bigCount).isEqualTo(3)
        assertThat(insights.attributes?.smallCount).isEqualTo(0)
        assertThat(insights.attributes?.routeCounts).containsExactly(1, 0, 2).inOrder()

        assertThat(insights.positions.map(HomePositionInsight::position))
            .containsExactly(
                DigitPosition.HUNDREDS,
                DigitPosition.TENS,
                DigitPosition.ONES,
            )
            .inOrder()
        assertThat(insights.positions.map(HomePositionInsight::digit))
            .containsExactly(6, 8, 5)
            .inOrder()
        assertThat(insights.positions.map(HomePositionInsight::heatLevel))
            .containsExactly(HeatLevel.COLD, HeatLevel.WARM, HeatLevel.HOT)
            .inOrder()

        assertThat(insights.coldHits).containsExactly(
            HomeColdHit(
                position = DigitPosition.HUNDREDS,
                digit = 6,
                previousOmission = 30,
                windowSize = 30,
            ),
        )
    }

    @Test
    fun `leading zero remains three digits while insights are calculated`() {
        val latest = draw(issue = "2026198", number = "007")

        val insights = buildHomeInsights(listOf(latest))

        assertThat(latest.number.value).isEqualTo("007")
        assertThat(insights.attributes?.sum).isEqualTo(7)
        assertThat(insights.positions.map(HomePositionInsight::digit))
            .containsExactly(0, 0, 7)
            .inOrder()
    }

    @Test
    fun `consecutive label lists the actual adjacent digits`() {
        assertThat(consecutiveDigitsLabel("685")).isEqualTo("5-6")
        assertThat(consecutiveDigitsLabel("123")).isEqualTo("1-2-3")
        assertThat(consecutiveDigitsLabel("007")).isEqualTo("无")
    }

    private fun draw(issue: String, number: String) = DrawRecord(
        issue = issue,
        drawDate = "2026-07-27",
        number = DrawNumber.parse(number),
        officialDetailUrl = "https://www.cwl.gov.cn/c/$issue.shtml",
        officialFingerprint = "fingerprint-$issue",
    )
}
