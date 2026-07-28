package com.lucky3d.app.domain.attributes

data class CrossDrawAttributes(
    val repeatDigits: Set<Int>,
    val neighborDigits: Set<Int>,
) {
    val repeatCount: Int get() = repeatDigits.size
    val neighborCount: Int get() = neighborDigits.size

    companion object {
        fun calculate(previous: DrawNumber, current: DrawNumber): CrossDrawAttributes {
            val previousDigits = previous.digits.toSet()
            val currentDigits = current.digits.toSet()
            val neighborCandidates = buildSet {
                previousDigits.forEach { digit ->
                    if (digit > 0) add(digit - 1)
                    if (digit < 9) add(digit + 1)
                }
            }

            return CrossDrawAttributes(
                repeatDigits = (currentDigits intersect previousDigits).toSortedSet(),
                neighborDigits = (currentDigits intersect neighborCandidates).toSortedSet(),
            )
        }
    }
}
