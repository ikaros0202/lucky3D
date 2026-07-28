package com.lucky3d.app.domain.attributes

import kotlin.math.abs

enum class DigitParity { ODD, EVEN }

enum class DigitSize { BIG, SMALL }

enum class DigitQuality { PRIME_LIKE, COMPOSITE_LIKE }

enum class SumSize { BIG, SMALL }

enum class SumZone { LOW, MIDDLE, HIGH }

enum class GroupShape { LEOPARD, GROUP3, GROUP6 }

data class PairValues(
    val hundredsTens: Int,
    val tensOnes: Int,
    val hundredsOnes: Int,
) {
    fun asList(): List<Int> = listOf(hundredsTens, tensOnes, hundredsOnes)
}

data class DrawAttributes(
    val sum: Int,
    val sumTail: Int,
    val span: Int,
    val parityByPosition: List<DigitParity>,
    val oddCount: Int,
    val evenCount: Int,
    val parityPattern: String,
    val sizeByPosition: List<DigitSize>,
    val bigCount: Int,
    val smallCount: Int,
    val sizePattern: String,
    val sumSize: SumSize,
    val sumZone: SumZone,
    val qualityByPosition: List<DigitQuality>,
    val primeLikeCount: Int,
    val compositeLikeCount: Int,
    val qualityPattern: String,
    val routesByPosition: List<Int>,
    val routeCounts: List<Int>,
    val routePositionPattern: String,
    val routeCountPattern: String,
    val groupShape: GroupShape,
    val hasPairConsecutive: Boolean,
    val hasTripleConsecutive: Boolean,
    val pairSums: PairValues,
    val pairDifferences: PairValues,
) {
    companion object {
        private val primeLikeDigits = setOf(1, 2, 3, 5, 7)

        fun calculate(number: DrawNumber): DrawAttributes {
            val digits = number.digits
            val sum = digits.sum()
            val parity = digits.map { if (it % 2 == 0) DigitParity.EVEN else DigitParity.ODD }
            val sizes = digits.map { if (it >= 5) DigitSize.BIG else DigitSize.SMALL }
            val qualities = digits.map {
                if (it in primeLikeDigits) DigitQuality.PRIME_LIKE else DigitQuality.COMPOSITE_LIKE
            }
            val routes = digits.map { it % 3 }
            val distinctSorted = digits.distinct().sorted()
            val adjacentPairs = distinctSorted.zipWithNext().count { (left, right) -> right - left == 1 }

            return DrawAttributes(
                sum = sum,
                sumTail = sum % 10,
                span = digits.max() - digits.min(),
                parityByPosition = parity,
                oddCount = parity.count { it == DigitParity.ODD },
                evenCount = parity.count { it == DigitParity.EVEN },
                parityPattern = parity.joinToString("") { if (it == DigitParity.ODD) "奇" else "偶" },
                sizeByPosition = sizes,
                bigCount = sizes.count { it == DigitSize.BIG },
                smallCount = sizes.count { it == DigitSize.SMALL },
                sizePattern = sizes.joinToString("") { if (it == DigitSize.BIG) "大" else "小" },
                sumSize = if (sum >= 14) SumSize.BIG else SumSize.SMALL,
                sumZone = when (sum) {
                    in 0..8 -> SumZone.LOW
                    in 9..18 -> SumZone.MIDDLE
                    else -> SumZone.HIGH
                },
                qualityByPosition = qualities,
                primeLikeCount = qualities.count { it == DigitQuality.PRIME_LIKE },
                compositeLikeCount = qualities.count { it == DigitQuality.COMPOSITE_LIKE },
                qualityPattern = qualities.joinToString("") {
                    if (it == DigitQuality.PRIME_LIKE) "质" else "合"
                },
                routesByPosition = routes,
                routeCounts = (0..2).map { route -> routes.count { it == route } },
                routePositionPattern = routes.joinToString(""),
                routeCountPattern = (0..2).joinToString("") { route -> routes.count { it == route }.toString() },
                groupShape = when (distinctSorted.size) {
                    1 -> GroupShape.LEOPARD
                    2 -> GroupShape.GROUP3
                    else -> GroupShape.GROUP6
                },
                hasPairConsecutive = adjacentPairs > 0,
                hasTripleConsecutive = distinctSorted.size == 3 && adjacentPairs == 2,
                pairSums = PairValues(
                    hundredsTens = number.hundreds + number.tens,
                    tensOnes = number.tens + number.ones,
                    hundredsOnes = number.hundreds + number.ones,
                ),
                pairDifferences = PairValues(
                    hundredsTens = abs(number.hundreds - number.tens),
                    tensOnes = abs(number.tens - number.ones),
                    hundredsOnes = abs(number.hundreds - number.ones),
                ),
            )
        }
    }
}
