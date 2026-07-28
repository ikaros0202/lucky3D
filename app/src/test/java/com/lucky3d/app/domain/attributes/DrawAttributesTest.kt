package com.lucky3d.app.domain.attributes

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DrawAttributesTest {
    @Test
    fun `golden attribute cases match rule version one`() {
        val cases = listOf(
            GoldenCase("000", 0, 0, 0, 0, 0, 0, listOf(0, 0, 0), listOf(3, 0, 0), GroupShape.LEOPARD, false, false, listOf(0, 0, 0), listOf(0, 0, 0)),
            GoldenCase("007", 7, 7, 7, 1, 1, 1, listOf(0, 0, 1), listOf(2, 1, 0), GroupShape.GROUP3, false, false, listOf(0, 7, 7), listOf(0, 7, 7)),
            GoldenCase("112", 4, 4, 1, 2, 0, 3, listOf(1, 1, 2), listOf(0, 2, 1), GroupShape.GROUP3, true, false, listOf(2, 3, 3), listOf(0, 1, 1)),
            GoldenCase("123", 6, 6, 2, 2, 0, 3, listOf(1, 2, 0), listOf(1, 1, 1), GroupShape.GROUP6, true, true, listOf(3, 5, 4), listOf(1, 1, 2)),
            GoldenCase("685", 19, 9, 3, 1, 3, 1, listOf(0, 2, 2), listOf(1, 0, 2), GroupShape.GROUP6, true, false, listOf(14, 13, 11), listOf(2, 3, 1)),
            GoldenCase("999", 27, 7, 0, 3, 3, 0, listOf(0, 0, 0), listOf(3, 0, 0), GroupShape.LEOPARD, false, false, listOf(18, 18, 18), listOf(0, 0, 0)),
        )

        cases.forEach { expected ->
            val actual = DrawAttributes.calculate(DrawNumber.parse(expected.number))

            assertThat(actual.sum).isEqualTo(expected.sum)
            assertThat(actual.sumTail).isEqualTo(expected.sumTail)
            assertThat(actual.span).isEqualTo(expected.span)
            assertThat(actual.oddCount).isEqualTo(expected.oddCount)
            assertThat(actual.bigCount).isEqualTo(expected.bigCount)
            assertThat(actual.primeLikeCount).isEqualTo(expected.primeLikeCount)
            assertThat(actual.routesByPosition).containsExactlyElementsIn(expected.routesByPosition).inOrder()
            assertThat(actual.routeCounts).containsExactlyElementsIn(expected.routeCounts).inOrder()
            assertThat(actual.groupShape).isEqualTo(expected.groupShape)
            assertThat(actual.hasPairConsecutive).isEqualTo(expected.hasPairConsecutive)
            assertThat(actual.hasTripleConsecutive).isEqualTo(expected.hasTripleConsecutive)
            assertThat(actual.pairSums.asList()).containsExactlyElementsIn(expected.pairSums).inOrder()
            assertThat(actual.pairDifferences.asList()).containsExactlyElementsIn(expected.pairDifferences).inOrder()
        }
    }

    @Test
    fun `group shapes are mutually exclusive and cover all numbers`() {
        val counts = (0..999)
            .map { DrawNumber.fromInt(it) }
            .map { DrawAttributes.calculate(it).groupShape }
            .groupingBy { it }
            .eachCount()

        assertThat(counts.values.sum()).isEqualTo(1000)
        assertThat(counts[GroupShape.LEOPARD]).isEqualTo(10)
        assertThat(counts[GroupShape.GROUP3]).isEqualTo(270)
        assertThat(counts[GroupShape.GROUP6]).isEqualTo(720)
    }

    @Test
    fun `draw number preserves leading zero and rejects invalid values`() {
        assertThat(DrawNumber.parse("007").value).isEqualTo("007")
        assertThat(DrawNumber.fromInt(7).value).isEqualTo("007")
        assertThat(runCatching { DrawNumber.parse("07") }.isFailure).isTrue()
        assertThat(runCatching { DrawNumber.parse("10a") }.isFailure).isTrue()
        assertThat(runCatching { DrawNumber.fromInt(1000) }.isFailure).isTrue()
    }

    private data class GoldenCase(
        val number: String,
        val sum: Int,
        val sumTail: Int,
        val span: Int,
        val oddCount: Int,
        val bigCount: Int,
        val primeLikeCount: Int,
        val routesByPosition: List<Int>,
        val routeCounts: List<Int>,
        val groupShape: GroupShape,
        val hasPairConsecutive: Boolean,
        val hasTripleConsecutive: Boolean,
        val pairSums: List<Int>,
        val pairDifferences: List<Int>,
    )
}
