package com.lucky3d.app.data.repository

import com.lucky3d.app.domain.attributes.GroupShape
import com.lucky3d.app.domain.filter.BigCountAllowed
import com.lucky3d.app.domain.filter.ConsecutiveCondition
import com.lucky3d.app.domain.filter.DanTuoCondition
import com.lucky3d.app.domain.filter.FilterCondition
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
import com.lucky3d.app.domain.filter.RouteAllowed
import com.lucky3d.app.domain.filter.SpanRange
import com.lucky3d.app.domain.filter.SumRange
import com.lucky3d.app.domain.filter.SumTailAllowed
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ConditionCodec {
    const val SCHEMA_VERSION = 1
    const val RULE_VERSION = 1

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    fun encode(
        conditions: List<FilterCondition>,
        ruleVersion: Int = RULE_VERSION,
    ): String = json.encodeToString(
        ConditionEnvelope(
            schemaVersion = SCHEMA_VERSION,
            ruleVersion = ruleVersion,
            conditions = conditions.map(FilterCondition::toDto),
        ),
    )

    fun decode(value: String): List<FilterCondition> {
        val envelope = json.decodeFromString<ConditionEnvelope>(value)
        require(envelope.schemaVersion == SCHEMA_VERSION) {
            "Unsupported condition schema ${envelope.schemaVersion}"
        }
        require(envelope.ruleVersion > 0) { "Rule version must be positive" }
        return envelope.conditions.map(ConditionDto::toCondition)
    }
}

@Serializable
private data class ConditionEnvelope(
    val schemaVersion: Int,
    val ruleVersion: Int,
    val conditions: List<ConditionDto>,
)

@Serializable
private data class ConditionDto(
    val type: String,
    val digits: List<Int> = emptyList(),
    val minimum: Int? = null,
    val maximum: Int? = null,
    val position: String? = null,
    val shapes: List<String> = emptyList(),
    val requirePair: Boolean? = null,
    val requireTriple: Boolean? = null,
    val hasRequireTriple: Boolean = false,
    val pairPosition: String? = null,
    val pairMetric: String? = null,
)

private fun FilterCondition.toDto(): ConditionDto = when (this) {
    is GlobalRequiredDigits -> ConditionDto(typeId, digits = digits.sorted())
    is GlobalExcludedDigits -> ConditionDto(typeId, digits = digits.sorted())
    is PositionAllowed -> ConditionDto(
        typeId,
        digits = allowedDigits.sorted(),
        position = position.name,
    )
    is SumRange -> ConditionDto(typeId, minimum = minimum, maximum = maximum)
    is SumTailAllowed -> ConditionDto(typeId, digits = values.sorted())
    is SpanRange -> ConditionDto(typeId, minimum = minimum, maximum = maximum)
    is OddCountAllowed -> ConditionDto(typeId, digits = counts.sorted())
    is BigCountAllowed -> ConditionDto(typeId, digits = counts.sorted())
    is PrimeLikeCountAllowed -> ConditionDto(typeId, digits = counts.sorted())
    is RouteAllowed -> ConditionDto(
        typeId,
        digits = routes.sorted(),
        position = position.name,
    )
    is GroupShapeCondition -> ConditionDto(
        typeId,
        shapes = values.map(GroupShape::name).sorted(),
    )
    is ConsecutiveCondition -> ConditionDto(
        typeId,
        requirePair = requirePair,
        requireTriple = requireTriple,
        hasRequireTriple = requireTriple != null,
    )
    is PairRelationRange -> ConditionDto(
        typeId,
        minimum = minimum,
        maximum = maximum,
        pairPosition = position.name,
        pairMetric = metric.name,
    )
    is DanTuoCondition -> ConditionDto(
        typeId,
        digits = danDigits.sorted(),
        shapes = tuoDigits.map(Int::toString).sorted(),
        pairMetric = playType.name,
    )
}

private fun ConditionDto.toCondition(): FilterCondition = when (type) {
    "GLOBAL_REQUIRED_DIGITS" -> GlobalRequiredDigits(digits.toSet())
    "GLOBAL_EXCLUDED_DIGITS" -> GlobalExcludedDigits(digits.toSet())
    "POSITION_ALLOWED" -> PositionAllowed(
        position = Position.valueOf(requireNotNull(position)),
        allowedDigits = digits.toSet(),
    )
    "SUM_RANGE" -> SumRange(requireNotNull(minimum), requireNotNull(maximum))
    "SUM_TAIL" -> SumTailAllowed(digits.toSet())
    "SPAN_RANGE" -> SpanRange(requireNotNull(minimum), requireNotNull(maximum))
    "ODD_COUNT" -> OddCountAllowed(digits.toSet())
    "BIG_COUNT" -> BigCountAllowed(digits.toSet())
    "PRIME_LIKE_COUNT" -> PrimeLikeCountAllowed(digits.toSet())
    "ROUTE_ALLOWED" -> RouteAllowed(
        position = Position.valueOf(requireNotNull(position)),
        routes = digits.toSet(),
    )
    "GROUP_SHAPE" -> GroupShapeCondition(shapes.map(GroupShape::valueOf).toSet())
    "CONSECUTIVE" -> ConsecutiveCondition(
        requirePair = requireNotNull(requirePair),
        requireTriple = if (hasRequireTriple) requireTriple else null,
    )
    "PAIR_RELATION_RANGE" -> PairRelationRange(
        position = PairPosition.valueOf(requireNotNull(pairPosition)),
        metric = PairMetric.valueOf(requireNotNull(pairMetric)),
        minimum = requireNotNull(minimum),
        maximum = requireNotNull(maximum),
    )
    "DAN_TUO" -> DanTuoCondition(
        danDigits = digits.toSet(),
        tuoDigits = shapes.map(String::toInt).toSet(),
        playType = com.lucky3d.app.domain.filter.PlayType.valueOf(requireNotNull(pairMetric)),
    )
    else -> error("Unsupported condition type $type")
}
