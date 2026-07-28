package com.lucky3d.app.domain.replay

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.filter.PlayType
import com.lucky3d.app.domain.scheme.HistoricalDraw
import com.lucky3d.app.domain.scheme.Scheme
import org.junit.Test

class ReplayEngineTest {
    @Test
    fun `unchanged official fingerprint reuses replay and changed fingerprint recalculates`() {
        val scheme = Scheme(
            id = "scheme-1",
            issue = "2026001",
            playType = PlayType.STRAIGHT,
            candidateNumbers = listOf(DrawNumber.parse("123")),
            ruleVersion = 1,
            note = "",
        )
        val firstDraw = HistoricalDraw("2026001", DrawNumber.parse("123"), "fingerprint-a")
        val correctedDraw = HistoricalDraw("2026001", DrawNumber.parse("124"), "fingerprint-b")

        val first = ReplayEngine.replay(scheme, firstDraw)
        val unchanged = ReplayEngine.replay(scheme, firstDraw, existing = first)
        val corrected = ReplayEngine.replay(scheme, correctedDraw, existing = first)

        assertThat(first.covered).isTrue()
        assertThat(unchanged).isEqualTo(first)
        assertThat(corrected.covered).isFalse()
        assertThat(corrected.officialFingerprint).isEqualTo("fingerprint-b")
        assertThat(corrected.revision).isEqualTo(2)
    }

    @Test
    fun `group replay compares canonical number`() {
        val scheme = Scheme(
            id = "scheme-2",
            issue = "2026002",
            playType = PlayType.GROUP6,
            candidateNumbers = listOf(DrawNumber.parse("123")),
            ruleVersion = 1,
            note = "",
        )

        val replay = ReplayEngine.replay(
            scheme,
            HistoricalDraw("2026002", DrawNumber.parse("321"), "group-fingerprint"),
        )

        assertThat(replay.covered).isTrue()
        assertThat(replay.matchedCandidate?.value).isEqualTo("123")
    }

    @Test
    fun `group replay is a miss when official shape belongs to another play`() {
        val scheme = Scheme(
            id = "scheme-3",
            issue = "2026003",
            playType = PlayType.GROUP6,
            candidateNumbers = listOf(DrawNumber.parse("123")),
            ruleVersion = 1,
            note = "",
        )

        val replay = ReplayEngine.replay(
            scheme,
            HistoricalDraw("2026003", DrawNumber.parse("112"), "wrong-shape"),
        )

        assertThat(replay.covered).isFalse()
        assertThat(replay.matchedCandidate).isNull()
    }
}
