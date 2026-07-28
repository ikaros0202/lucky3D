package com.lucky3d.app.domain.filter

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.domain.attributes.GroupShape
import org.junit.Test

class NumberPoolTest {
    @Test
    fun `play-specific universes keep canonical group bets`() {
        assertThat(NumberPool.filter(emptyList(), PlayType.STRAIGHT).candidates).hasSize(1000)
        assertThat(NumberPool.filter(emptyList(), PlayType.GROUP3).candidates).hasSize(90)
        assertThat(NumberPool.filter(emptyList(), PlayType.GROUP6).candidates).hasSize(120)
        assertThat(NumberPool.filter(emptyList(), PlayType.GROUP6).candidates.first().value)
            .isEqualTo("012")
    }

    @Test
    fun `golden universe counts are exact`() {
        val cases = listOf(
            emptyList<FilterCondition>() to 1000,
            listOf(GlobalExcludedDigits(setOf(0))) to 729,
            listOf(GlobalRequiredDigits(setOf(1))) to 271,
            listOf(GlobalRequiredDigits(setOf(1, 2))) to 54,
            listOf(SumRange(minimum = 6, maximum = 6)) to 28,
            listOf(GroupShapeCondition(setOf(GroupShape.LEOPARD))) to 10,
        )

        cases.forEach { (conditions, expectedCount) ->
            val result = NumberPool.filter(conditions)

            assertThat(result.conflict).isNull()
            assertThat(result.candidates).hasSize(expectedCount)
        }
    }

    @Test
    fun `conditions are intersected and report individual exclusions`() {
        val result = NumberPool.filter(
            listOf(
                GlobalExcludedDigits(setOf(0)),
                SumRange(6, 6),
            ),
        )

        assertThat(result.conflict).isNull()
        assertThat(result.candidates).containsNoDuplicates()
        assertThat(result.candidates.all { '0' !in it.value && it.digits.sum() == 6 }).isTrue()
        assertThat(result.impacts).hasSize(2)
        assertThat(result.impacts.all { it.excludedCount >= 0 }).isTrue()
    }

    @Test
    fun `required and excluded same digit returns explicit conflict`() {
        val result = NumberPool.filter(
            listOf(
                GlobalRequiredDigits(setOf(1)),
                GlobalExcludedDigits(setOf(1, 2)),
            ),
        )

        assertThat(result.candidates).isEmpty()
        assertThat(result.conflict?.code).isEqualTo("REQUIRED_EXCLUDED_OVERLAP")
        assertThat(result.conflict?.message).contains("1")
    }

    @Test
    fun `position rules preserve leading zero`() {
        val result = NumberPool.filter(
            listOf(
                PositionAllowed(Position.HUNDREDS, setOf(0)),
                PositionAllowed(Position.TENS, setOf(0)),
                PositionAllowed(Position.ONES, setOf(7)),
            ),
        )

        assertThat(result.candidates.map { it.value }).containsExactly("007")
    }

    @Test
    fun `dan and tuo must be disjoint and generate the requested shape`() {
        val conflict = NumberPool.generateDanTuo(
            danDigits = setOf(1),
            tuoDigits = setOf(1, 2, 3),
            playType = PlayType.GROUP6,
        )
        assertThat(conflict.conflict?.code).isEqualTo("DAN_TUO_OVERLAP")

        val generated = NumberPool.generateDanTuo(
            danDigits = setOf(1, 2),
            tuoDigits = setOf(3),
            playType = PlayType.GROUP6,
        )
        assertThat(generated.candidates.map { it.value }).containsExactly("123")
    }
}
