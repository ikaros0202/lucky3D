package com.lucky3d.app.domain.scheme

import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.filter.FilterCondition
import com.lucky3d.app.domain.filter.PlayType

data class HistoricalDraw(
    val issue: String,
    val number: DrawNumber,
    val officialFingerprint: String,
) {
    init {
        require(issue.matches(Regex("""\d{7}"""))) { "Issue must contain seven digits" }
        require(officialFingerprint.isNotBlank()) { "Official fingerprint cannot be blank" }
    }
}

data class FilterTemplate(
    val id: String,
    val name: String,
    val playType: PlayType,
    val conditions: List<FilterCondition>,
    val observationWindow: Int,
    val ruleVersion: Int,
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
        require(observationWindow > 0)
        require(ruleVersion > 0)
    }
}

data class Scheme(
    val id: String,
    val issue: String,
    val playType: PlayType,
    val candidateNumbers: List<DrawNumber>,
    val ruleVersion: Int,
    val note: String,
) {
    init {
        require(id.isNotBlank())
        require(issue.matches(Regex("""\d{7}""")))
        require(candidateNumbers.distinct().size == candidateNumbers.size) {
            "Scheme candidates must be deduplicated"
        }
        require(ruleVersion > 0)
    }

    val contentFingerprint: String
        get() = buildString {
            append(playType.name)
            append(':')
            append(ruleVersion)
            append(':')
            append(candidateNumbers.joinToString(",") { it.value })
        }
}
