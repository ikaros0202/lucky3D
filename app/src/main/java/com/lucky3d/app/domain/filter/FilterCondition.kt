package com.lucky3d.app.domain.filter

import com.lucky3d.app.domain.attributes.DigitQuality
import com.lucky3d.app.domain.attributes.DrawAttributes
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.attributes.GroupShape

sealed interface FilterCondition {
    val typeId: String
    val title: String
    fun matches(number: DrawNumber): Boolean
}

data class GlobalRequiredDigits(val digits: Set<Int>) : FilterCondition {
    init {
        validateDigits(digits)
        require(digits.size <= 3) { "At most three distinct required digits are allowed" }
    }

    override val typeId = "GLOBAL_REQUIRED_DIGITS"
    override val title = "全局胆码"
    override fun matches(number: DrawNumber): Boolean = digits.all(number.digits::contains)
}

data class GlobalExcludedDigits(val digits: Set<Int>) : FilterCondition {
    init {
        validateDigits(digits)
    }

    override val typeId = "GLOBAL_EXCLUDED_DIGITS"
    override val title = "全局杀码"
    override fun matches(number: DrawNumber): Boolean = number.digits.none(digits::contains)
}

enum class Position { HUNDREDS, TENS, ONES }

data class PositionAllowed(
    val position: Position,
    val allowedDigits: Set<Int>,
) : FilterCondition {
    init {
        validateDigits(allowedDigits)
        require(allowedDigits.isNotEmpty()) { "A position must allow at least one digit" }
    }

    override val typeId = "POSITION_ALLOWED"
    override val title = when (position) {
        Position.HUNDREDS -> "百位允许"
        Position.TENS -> "十位允许"
        Position.ONES -> "个位允许"
    }

    override fun matches(number: DrawNumber): Boolean = when (position) {
        Position.HUNDREDS -> number.hundreds
        Position.TENS -> number.tens
        Position.ONES -> number.ones
    } in allowedDigits
}

data class SumRange(val minimum: Int, val maximum: Int) : FilterCondition {
    init {
        require(minimum in 0..27 && maximum in minimum..27)
    }

    override val typeId = "SUM_RANGE"
    override val title = "和值"
    override fun matches(number: DrawNumber): Boolean = number.digits.sum() in minimum..maximum
}

data class SumTailAllowed(val values: Set<Int>) : FilterCondition {
    init {
        validateDigits(values)
    }

    override val typeId = "SUM_TAIL"
    override val title = "和尾"
    override fun matches(number: DrawNumber): Boolean = number.digits.sum() % 10 in values
}

data class SpanRange(val minimum: Int, val maximum: Int) : FilterCondition {
    init {
        require(minimum in 0..9 && maximum in minimum..9)
    }

    override val typeId = "SPAN_RANGE"
    override val title = "跨度"
    override fun matches(number: DrawNumber): Boolean {
        val digits = number.digits
        return digits.max() - digits.min() in minimum..maximum
    }
}

data class OddCountAllowed(val counts: Set<Int>) : FilterCondition {
    init {
        require(counts.all { it in 0..3 })
    }

    override val typeId = "ODD_COUNT"
    override val title = "奇数个数"
    override fun matches(number: DrawNumber): Boolean =
        number.digits.count { it % 2 != 0 } in counts
}

data class BigCountAllowed(val counts: Set<Int>) : FilterCondition {
    init {
        require(counts.all { it in 0..3 })
    }

    override val typeId = "BIG_COUNT"
    override val title = "大数个数"
    override fun matches(number: DrawNumber): Boolean =
        number.digits.count { it >= 5 } in counts
}

data class PrimeLikeCountAllowed(val counts: Set<Int>) : FilterCondition {
    init {
        require(counts.all { it in 0..3 })
    }

    override val typeId = "PRIME_LIKE_COUNT"
    override val title = "质数个数"
    override fun matches(number: DrawNumber): Boolean =
        DrawAttributes.calculate(number).qualityByPosition.count { it == DigitQuality.PRIME_LIKE } in counts
}

data class RouteAllowed(
    val position: Position,
    val routes: Set<Int>,
) : FilterCondition {
    init {
        require(routes.isNotEmpty() && routes.all { it in 0..2 })
    }

    override val typeId = "ROUTE_ALLOWED"
    override val title = "012路"
    override fun matches(number: DrawNumber): Boolean {
        val digit = when (position) {
            Position.HUNDREDS -> number.hundreds
            Position.TENS -> number.tens
            Position.ONES -> number.ones
        }
        return digit % 3 in routes
    }
}

data class GroupShapeCondition(val values: Set<GroupShape>) : FilterCondition {
    init {
        require(values.isNotEmpty())
    }

    override val typeId = "GROUP_SHAPE"
    override val title = "组选形态"
    override fun matches(number: DrawNumber): Boolean =
        DrawAttributes.calculate(number).groupShape in values
}

data class ConsecutiveCondition(
    val requirePair: Boolean,
    val requireTriple: Boolean? = null,
) : FilterCondition {
    override val typeId = "CONSECUTIVE"
    override val title = "连号"
    override fun matches(number: DrawNumber): Boolean {
        val attributes = DrawAttributes.calculate(number)
        return attributes.hasPairConsecutive == requirePair &&
            (requireTriple == null || attributes.hasTripleConsecutive == requireTriple)
    }
}

enum class PairPosition { HUNDREDS_TENS, TENS_ONES, HUNDREDS_ONES }

enum class PairMetric { SUM, ABSOLUTE_DIFFERENCE }

data class PairRelationRange(
    val position: PairPosition,
    val metric: PairMetric,
    val minimum: Int,
    val maximum: Int,
) : FilterCondition {
    init {
        val validMaximum = if (metric == PairMetric.SUM) 18 else 9
        require(minimum in 0..validMaximum && maximum in minimum..validMaximum)
    }

    override val typeId = "PAIR_RELATION_RANGE"
    override val title = "两码关系"
    override fun matches(number: DrawNumber): Boolean {
        val attributes = DrawAttributes.calculate(number)
        val values = if (metric == PairMetric.SUM) attributes.pairSums else attributes.pairDifferences
        val value = when (position) {
            PairPosition.HUNDREDS_TENS -> values.hundredsTens
            PairPosition.TENS_ONES -> values.tensOnes
            PairPosition.HUNDREDS_ONES -> values.hundredsOnes
        }
        return value in minimum..maximum
    }
}

data class DanTuoCondition(
    val danDigits: Set<Int>,
    val tuoDigits: Set<Int>,
    val playType: PlayType,
) : FilterCondition {
    init {
        validateDigits(danDigits)
        validateDigits(tuoDigits)
        require((danDigits intersect tuoDigits).isEmpty()) {
            "Dan and tuo digits cannot overlap"
        }
    }

    override val typeId = "DAN_TUO"
    override val title = "胆码拖码"

    override fun matches(number: DrawNumber): Boolean {
        val allowed = danDigits + tuoDigits
        return number.digits.all(allowed::contains) &&
            danDigits.all(number.digits::contains)
    }
}

private fun validateDigits(digits: Set<Int>) {
    require(digits.all { it in 0..9 }) { "Digits must be between 0 and 9" }
}
