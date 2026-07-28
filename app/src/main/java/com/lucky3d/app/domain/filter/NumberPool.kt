package com.lucky3d.app.domain.filter

import com.lucky3d.app.domain.attributes.DrawAttributes
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.attributes.GroupShape

data class FilterConflict(
    val code: String,
    val message: String,
)

data class ConditionImpact(
    val typeId: String,
    val title: String,
    val excludedCount: Int,
)

data class FilterResult(
    val candidates: List<DrawNumber>,
    val impacts: List<ConditionImpact> = emptyList(),
    val conflict: FilterConflict? = null,
)

object NumberPool {
    private val straightUniverse: List<DrawNumber> = (0..999).map(DrawNumber::fromInt)

    fun filter(
        conditions: List<FilterCondition>,
        playType: PlayType = PlayType.STRAIGHT,
    ): FilterResult {
        findConflict(conditions)?.let { return FilterResult(emptyList(), conflict = it) }
        val universe = if (playType == PlayType.STRAIGHT) {
            straightUniverse
        } else {
            PlayConverter.universe(playType)
        }

        val impacts = conditions.map { condition ->
            ConditionImpact(
                typeId = condition.typeId,
                title = condition.title,
                excludedCount = universe.count { !condition.matches(it) },
            )
        }
        val candidates = universe.filter { number -> conditions.all { it.matches(number) } }
        if (conditions.isNotEmpty() && candidates.isEmpty()) {
            return FilterResult(
                candidates = emptyList(),
                impacts = impacts,
                conflict = FilterConflict(
                    code = "NO_CANDIDATES",
                    message = "当前条件组合没有剩余号码，请放宽至少一个条件",
                ),
            )
        }
        return FilterResult(candidates = candidates, impacts = impacts)
    }

    fun generateDanTuo(
        danDigits: Set<Int>,
        tuoDigits: Set<Int>,
        playType: PlayType,
    ): FilterResult {
        if ((danDigits intersect tuoDigits).isNotEmpty()) {
            return FilterResult(
                emptyList(),
                conflict = FilterConflict(
                    "DAN_TUO_OVERLAP",
                    "胆码与拖码不能包含相同数字：${(danDigits intersect tuoDigits).sorted().joinToString()}",
                ),
            )
        }
        if (danDigits.any { it !in 0..9 } || tuoDigits.any { it !in 0..9 }) {
            return FilterResult(
                emptyList(),
                conflict = FilterConflict("INVALID_DIGIT", "胆码和拖码只能使用0—9"),
            )
        }
        val allowed = danDigits + tuoDigits
        val straight = straightUniverse.filter { number ->
            number.digits.all(allowed::contains) &&
                danDigits.all(number.digits::contains) &&
                when (playType) {
                    PlayType.STRAIGHT -> true
                    PlayType.GROUP3 -> DrawAttributes.calculate(number).groupShape == GroupShape.GROUP3
                    PlayType.GROUP6 -> DrawAttributes.calculate(number).groupShape == GroupShape.GROUP6
                }
        }
        val candidates = if (playType == PlayType.STRAIGHT) {
            straight
        } else {
            straight.map { PlayConverter.canonical(it, playType) }.distinct().sortedBy(DrawNumber::value)
        }
        return if (candidates.isEmpty()) {
            FilterResult(
                emptyList(),
                conflict = FilterConflict("NO_CANDIDATES", "当前胆码、拖码和玩法无法组成号码"),
            )
        } else {
            FilterResult(candidates)
        }
    }

    private fun findConflict(conditions: List<FilterCondition>): FilterConflict? {
        val required = conditions.filterIsInstance<GlobalRequiredDigits>().flatMap { it.digits }.toSet()
        val excluded = conditions.filterIsInstance<GlobalExcludedDigits>().flatMap { it.digits }.toSet()
        val overlap = required intersect excluded
        if (overlap.isNotEmpty()) {
            return FilterConflict(
                code = "REQUIRED_EXCLUDED_OVERLAP",
                message = "同一数字不能同时作为胆码和杀码：${overlap.sorted().joinToString()}",
            )
        }
        if (required.size > 3) {
            return FilterConflict(
                code = "TOO_MANY_REQUIRED_DIGITS",
                message = "全局胆码最多选择3个不同数字",
            )
        }
        return null
    }
}
