package com.lucky3d.app.domain.attributes

@JvmInline
value class DrawNumber private constructor(val value: String) {
    val hundreds: Int get() = value[0].digitToInt()
    val tens: Int get() = value[1].digitToInt()
    val ones: Int get() = value[2].digitToInt()
    val digits: List<Int> get() = listOf(hundreds, tens, ones)

    override fun toString(): String = value

    companion object {
        fun parse(value: String): DrawNumber {
            require(value.length == 3 && value.all(Char::isDigit)) {
                "Draw number must contain exactly three decimal digits"
            }
            return DrawNumber(value)
        }

        fun fromInt(value: Int): DrawNumber {
            require(value in 0..999) { "Draw number must be between 0 and 999" }
            return DrawNumber(value.toString().padStart(3, '0'))
        }

        fun of(hundreds: Int, tens: Int, ones: Int): DrawNumber {
            require(hundreds in 0..9 && tens in 0..9 && ones in 0..9) {
                "Every draw digit must be between 0 and 9"
            }
            return DrawNumber("$hundreds$tens$ones")
        }
    }
}
