package com.lucky3d.app.domain.filter

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.domain.attributes.DrawNumber
import org.junit.Test

class PlayConversionTest {
    @Test
    fun `play universes have official bet counts`() {
        val straight = PlayConverter.universe(PlayType.STRAIGHT)
        val group3 = PlayConverter.universe(PlayType.GROUP3)
        val group6 = PlayConverter.universe(PlayType.GROUP6)

        assertThat(straight).hasSize(1000)
        assertThat(group3).hasSize(90)
        assertThat(group6).hasSize(120)
        assertThat(group3.sumOf { PlayConverter.toStraightPermutations(it, PlayType.GROUP3).size })
            .isEqualTo(270)
        assertThat(group6.sumOf { PlayConverter.toStraightPermutations(it, PlayType.GROUP6).size })
            .isEqualTo(720)
    }

    @Test
    fun `golden group permutations are deduplicated and ordered`() {
        assertThat(
            PlayConverter.toStraightPermutations(DrawNumber.parse("112"), PlayType.GROUP3)
                .map { it.value },
        ).containsExactly("112", "121", "211").inOrder()

        assertThat(
            PlayConverter.toStraightPermutations(DrawNumber.parse("123"), PlayType.GROUP6)
                .map { it.value },
        ).containsExactly("123", "132", "213", "231", "312", "321").inOrder()

        assertThat(
            PlayConverter.toStraightPermutations(DrawNumber.parse("001"), PlayType.GROUP3)
                .map { it.value },
        ).containsExactly("001", "010", "100").inOrder()
    }

    @Test
    fun `amount uses two yuan and validates multiplier`() {
        assertThat(PlayConverter.amountYuan(betCount = 54, multiplier = 1)).isEqualTo(108)
        assertThat(PlayConverter.amountYuan(betCount = 1, multiplier = 99)).isEqualTo(198)
        assertThat(runCatching { PlayConverter.amountYuan(1, 0) }.isFailure).isTrue()
    }
}
