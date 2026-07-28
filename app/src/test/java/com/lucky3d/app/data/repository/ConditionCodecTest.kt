package com.lucky3d.app.data.repository

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.domain.attributes.GroupShape
import com.lucky3d.app.domain.filter.BigCountAllowed
import com.lucky3d.app.domain.filter.ConsecutiveCondition
import com.lucky3d.app.domain.filter.DanTuoCondition
import com.lucky3d.app.domain.filter.GlobalExcludedDigits
import com.lucky3d.app.domain.filter.GlobalRequiredDigits
import com.lucky3d.app.domain.filter.GroupShapeCondition
import com.lucky3d.app.domain.filter.OddCountAllowed
import com.lucky3d.app.domain.filter.PairMetric
import com.lucky3d.app.domain.filter.PairPosition
import com.lucky3d.app.domain.filter.PairRelationRange
import com.lucky3d.app.domain.filter.Position
import com.lucky3d.app.domain.filter.PositionAllowed
import com.lucky3d.app.domain.filter.PrimeLikeCountAllowed
import com.lucky3d.app.domain.filter.PlayType
import com.lucky3d.app.domain.filter.RouteAllowed
import com.lucky3d.app.domain.filter.SpanRange
import com.lucky3d.app.domain.filter.SumRange
import com.lucky3d.app.domain.filter.SumTailAllowed
import org.junit.Test

class ConditionCodecTest {
    @Test
    fun `all V1 condition types round trip without losing parameters`() {
        val conditions = listOf(
            GlobalRequiredDigits(setOf(0, 7)),
            GlobalExcludedDigits(setOf(9)),
            PositionAllowed(Position.HUNDREDS, setOf(0, 1, 2)),
            SumRange(6, 15),
            SumTailAllowed(setOf(0, 5)),
            SpanRange(2, 8),
            OddCountAllowed(setOf(1, 3)),
            BigCountAllowed(setOf(0, 2)),
            PrimeLikeCountAllowed(setOf(1, 2)),
            RouteAllowed(Position.TENS, setOf(0, 2)),
            GroupShapeCondition(setOf(GroupShape.GROUP3, GroupShape.GROUP6)),
            ConsecutiveCondition(requirePair = true, requireTriple = false),
            PairRelationRange(
                position = PairPosition.HUNDREDS_ONES,
                metric = PairMetric.ABSOLUTE_DIFFERENCE,
                minimum = 2,
                maximum = 7,
            ),
            DanTuoCondition(setOf(0), setOf(1, 2, 3), PlayType.GROUP6),
        )

        val encoded = ConditionCodec.encode(conditions)

        assertThat(ConditionCodec.decode(encoded)).containsExactlyElementsIn(conditions).inOrder()
        assertThat(encoded).contains("\"schemaVersion\":1")
        assertThat(encoded).contains("\"ruleVersion\":1")
    }
}
