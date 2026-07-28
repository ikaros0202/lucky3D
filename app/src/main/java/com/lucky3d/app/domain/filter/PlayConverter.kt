package com.lucky3d.app.domain.filter

import com.lucky3d.app.domain.attributes.DrawAttributes
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.attributes.GroupShape

enum class PlayType { STRAIGHT, GROUP3, GROUP6 }

object PlayConverter {
    fun universe(playType: PlayType): List<DrawNumber> = when (playType) {
        PlayType.STRAIGHT -> (0..999).map(DrawNumber::fromInt)
        PlayType.GROUP3 -> buildList {
            for (repeated in 0..9) {
                for (single in 0..9) {
                    if (single != repeated) {
                        add(canonical(DrawNumber.of(repeated, repeated, single), PlayType.GROUP3))
                    }
                }
            }
        }.distinct().sortedBy(DrawNumber::value)
        PlayType.GROUP6 -> buildList {
            for (first in 0..7) {
                for (second in first + 1..8) {
                    for (third in second + 1..9) {
                        add(DrawNumber.of(first, second, third))
                    }
                }
            }
        }
    }

    fun canonical(number: DrawNumber, playType: PlayType): DrawNumber {
        val shape = DrawAttributes.calculate(number).groupShape
        when (playType) {
            PlayType.STRAIGHT -> return number
            PlayType.GROUP3 -> require(shape == GroupShape.GROUP3) { "组选3 requires exactly two equal digits" }
            PlayType.GROUP6 -> require(shape == GroupShape.GROUP6) { "组选6 requires three distinct digits" }
        }
        return DrawNumber.parse(number.digits.sorted().joinToString(""))
    }

    fun toStraightPermutations(number: DrawNumber, playType: PlayType): List<DrawNumber> {
        if (playType == PlayType.STRAIGHT) return listOf(number)
        val canonical = canonical(number, playType)
        val digits = canonical.digits
        return buildSet {
            for (first in digits.indices) {
                for (second in digits.indices) {
                    for (third in digits.indices) {
                        if (setOf(first, second, third).size == 3) {
                            add(DrawNumber.of(digits[first], digits[second], digits[third]))
                        }
                    }
                }
            }
        }.sortedBy(DrawNumber::value)
    }

    fun amountYuan(betCount: Int, multiplier: Int): Int {
        require(betCount >= 0) { "Bet count cannot be negative" }
        require(multiplier in 1..99) { "Multiplier must be between 1 and 99" }
        return Math.multiplyExact(Math.multiplyExact(betCount, 2), multiplier)
    }
}
