package com.lucky3d.app.feature.pick

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lucky3d.app.R
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

internal enum class ConditionKind(@StringRes val labelRes: Int) {
    REQUIRED(R.string.pick_kind_required),
    EXCLUDED(R.string.pick_kind_excluded),
    POSITION(R.string.pick_kind_position),
    SUM(R.string.pick_kind_sum),
    SUM_TAIL(R.string.pick_kind_sum_tail),
    SPAN(R.string.pick_kind_span),
    ODD(R.string.pick_kind_odd),
    BIG(R.string.pick_kind_big),
    PRIME(R.string.pick_kind_prime),
    ROUTE(R.string.pick_kind_route),
    SHAPE(R.string.pick_kind_shape),
    CONSECUTIVE(R.string.pick_kind_consecutive),
    PAIR(R.string.pick_kind_pair),
}

private data class EditorSeed(
    val kind: ConditionKind,
    val digits: String = "",
    val minimum: String = "",
    val maximum: String = "",
    val position: Position = Position.HUNDREDS,
    val pairPosition: PairPosition = PairPosition.HUNDREDS_TENS,
    val pairMetric: PairMetric = PairMetric.SUM,
    val shapes: Set<GroupShape> = setOf(GroupShape.GROUP3, GroupShape.GROUP6),
    val requirePair: Boolean = true,
)

@Composable
internal fun ConditionEditor(
    initialCondition: FilterCondition?,
    onDismiss: () -> Unit,
    onConfirm: (FilterCondition) -> Unit,
) {
    val seed = remember(initialCondition) { initialCondition.toEditorSeed() }
    var kind by remember(seed) { mutableStateOf(seed.kind) }
    var digits by remember(seed) { mutableStateOf(seed.digits) }
    var minimum by remember(seed) { mutableStateOf(seed.minimum) }
    var maximum by remember(seed) { mutableStateOf(seed.maximum) }
    var position by remember(seed) { mutableStateOf(seed.position) }
    var pairPosition by remember(seed) { mutableStateOf(seed.pairPosition) }
    var pairMetric by remember(seed) { mutableStateOf(seed.pairMetric) }
    var shapes by remember(seed) { mutableStateOf(seed.shapes) }
    var requirePair by remember(seed) { mutableStateOf(seed.requirePair) }
    var showError by remember(seed) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_condition_editor_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.pick_condition_type))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ConditionKind.entries.forEach { option ->
                        FilterChip(
                            selected = kind == option,
                            onClick = { kind = option },
                            label = { Text(stringResource(option.labelRes)) },
                        )
                    }
                }
                when (kind) {
                    ConditionKind.REQUIRED,
                    ConditionKind.EXCLUDED,
                    ConditionKind.SUM_TAIL,
                    ConditionKind.ODD,
                    ConditionKind.BIG,
                    ConditionKind.PRIME,
                    -> {
                        OutlinedTextField(
                            value = digits,
                            onValueChange = { digits = it },
                            label = {
                                Text(
                                    stringResource(
                                        if (kind in setOf(
                                                ConditionKind.ODD,
                                                ConditionKind.BIG,
                                                ConditionKind.PRIME,
                                            )
                                        ) {
                                            R.string.pick_counts_label
                                        } else {
                                            R.string.pick_digits_label
                                        },
                                    ),
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    ConditionKind.POSITION,
                    ConditionKind.ROUTE,
                    -> {
                        PositionSelector(position = position, onSelect = { position = it })
                        OutlinedTextField(
                            value = digits,
                            onValueChange = { digits = it },
                            label = { Text(stringResource(R.string.pick_digits_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    ConditionKind.SUM,
                    ConditionKind.SPAN,
                    -> RangeFields(
                        minimum = minimum,
                        maximum = maximum,
                        onMinimumChange = { minimum = it.filter(Char::isDigit) },
                        onMaximumChange = { maximum = it.filter(Char::isDigit) },
                    )
                    ConditionKind.SHAPE -> ShapeSelector(
                        selected = shapes,
                        onToggle = { shape ->
                            shapes = if (shape in shapes) shapes - shape else shapes + shape
                        },
                    )
                    ConditionKind.CONSECUTIVE -> {
                        OutlinedButton(onClick = { requirePair = !requirePair }) {
                            Text(
                                stringResource(
                                    if (requirePair) {
                                        R.string.pick_require_pair
                                    } else {
                                        R.string.pick_exclude_pair
                                    },
                                ),
                            )
                        }
                    }
                    ConditionKind.PAIR -> {
                        PairPositionSelector(pairPosition, onSelect = { pairPosition = it })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PairMetric.entries.forEach { metric ->
                                FilterChip(
                                    selected = pairMetric == metric,
                                    onClick = { pairMetric = metric },
                                    label = {
                                        Text(
                                            stringResource(
                                                if (metric == PairMetric.SUM) {
                                                    R.string.pick_metric_sum
                                                } else {
                                                    R.string.pick_metric_difference
                                                },
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                        RangeFields(
                            minimum = minimum,
                            maximum = maximum,
                            onMinimumChange = { minimum = it.filter(Char::isDigit) },
                            onMaximumChange = { maximum = it.filter(Char::isDigit) },
                        )
                    }
                }
                if (showError) {
                    Text(stringResource(R.string.pick_invalid_condition))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val condition = buildCondition(
                        kind = kind,
                        digitsText = digits,
                        minimumText = minimum,
                        maximumText = maximum,
                        position = position,
                        pairPosition = pairPosition,
                        pairMetric = pairMetric,
                        shapes = shapes,
                        requirePair = requirePair,
                    )
                    if (condition == null) {
                        showError = true
                    } else {
                        onConfirm(condition)
                    }
                },
            ) {
                Text(stringResource(R.string.pick_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun RangeFields(
    minimum: String,
    maximum: String,
    onMinimumChange: (String) -> Unit,
    onMaximumChange: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = minimum,
            onValueChange = onMinimumChange,
            label = { Text(stringResource(R.string.pick_minimum)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = maximum,
            onValueChange = onMaximumChange,
            label = { Text(stringResource(R.string.pick_maximum)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PositionSelector(
    position: Position,
    onSelect: (Position) -> Unit,
) {
    Text(stringResource(R.string.pick_position))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Position.entries.forEach { option ->
            FilterChip(
                selected = position == option,
                onClick = { onSelect(option) },
                label = { Text(positionLabel(option)) },
            )
        }
    }
}

@Composable
private fun PairPositionSelector(
    position: PairPosition,
    onSelect: (PairPosition) -> Unit,
) {
    Text(stringResource(R.string.pick_pair_position))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PairPosition.entries.forEach { option ->
            FilterChip(
                selected = position == option,
                onClick = { onSelect(option) },
                label = { Text(pairPositionLabel(option)) },
            )
        }
    }
}

@Composable
private fun ShapeSelector(
    selected: Set<GroupShape>,
    onToggle: (GroupShape) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupShape.entries.forEach { shape ->
            FilterChip(
                selected = shape in selected,
                onClick = { onToggle(shape) },
                label = {
                    Text(
                        stringResource(
                            when (shape) {
                                GroupShape.LEOPARD -> R.string.pick_shape_leopard
                                GroupShape.GROUP3 -> R.string.pick_shape_group3
                                GroupShape.GROUP6 -> R.string.pick_shape_group6
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun positionLabel(position: Position): String = stringResource(
    when (position) {
        Position.HUNDREDS -> R.string.trend_position_hundreds
        Position.TENS -> R.string.trend_position_tens
        Position.ONES -> R.string.trend_position_ones
    },
)

@Composable
private fun pairPositionLabel(position: PairPosition): String = when (position) {
    PairPosition.HUNDREDS_TENS -> stringResource(R.string.pick_pair_hundreds_tens)
    PairPosition.TENS_ONES -> stringResource(R.string.pick_pair_tens_ones)
    PairPosition.HUNDREDS_ONES -> stringResource(R.string.pick_pair_hundreds_ones)
}

private fun buildCondition(
    kind: ConditionKind,
    digitsText: String,
    minimumText: String,
    maximumText: String,
    position: Position,
    pairPosition: PairPosition,
    pairMetric: PairMetric,
    shapes: Set<GroupShape>,
    requirePair: Boolean,
): FilterCondition? = runCatching {
    val values = parseIntegerSet(digitsText)
    val minimum = minimumText.toIntOrNull()
    val maximum = maximumText.toIntOrNull()
    when (kind) {
        ConditionKind.REQUIRED -> GlobalRequiredDigits(values.requireNotEmpty())
        ConditionKind.EXCLUDED -> GlobalExcludedDigits(values.requireNotEmpty())
        ConditionKind.POSITION -> PositionAllowed(position, values.requireNotEmpty())
        ConditionKind.SUM -> SumRange(requireNotNull(minimum), requireNotNull(maximum))
        ConditionKind.SUM_TAIL -> SumTailAllowed(values.requireNotEmpty())
        ConditionKind.SPAN -> SpanRange(requireNotNull(minimum), requireNotNull(maximum))
        ConditionKind.ODD -> OddCountAllowed(values.requireNotEmpty())
        ConditionKind.BIG -> BigCountAllowed(values.requireNotEmpty())
        ConditionKind.PRIME -> PrimeLikeCountAllowed(values.requireNotEmpty())
        ConditionKind.ROUTE -> RouteAllowed(position, values.requireNotEmpty())
        ConditionKind.SHAPE -> GroupShapeCondition(shapes.requireNotEmpty())
        ConditionKind.CONSECUTIVE -> ConsecutiveCondition(requirePair = requirePair)
        ConditionKind.PAIR -> PairRelationRange(
            position = pairPosition,
            metric = pairMetric,
            minimum = requireNotNull(minimum),
            maximum = requireNotNull(maximum),
        )
    }
}.getOrNull()

private fun parseIntegerSet(value: String): Set<Int> = value
    .split(',', '，', ' ', ';', '；')
    .filter(String::isNotBlank)
    .map(String::toInt)
    .toSet()

private fun <T> Set<T>.requireNotEmpty(): Set<T> = apply { require(isNotEmpty()) }

private fun FilterCondition?.toEditorSeed(): EditorSeed = when (this) {
    null -> EditorSeed(kind = ConditionKind.REQUIRED, digits = "1")
    is GlobalRequiredDigits -> EditorSeed(ConditionKind.REQUIRED, digits.joinToString())
    is GlobalExcludedDigits -> EditorSeed(ConditionKind.EXCLUDED, digits.joinToString())
    is PositionAllowed -> EditorSeed(
        ConditionKind.POSITION,
        allowedDigits.joinToString(),
        position = position,
    )
    is SumRange -> EditorSeed(ConditionKind.SUM, minimum = minimum.toString(), maximum = maximum.toString())
    is SumTailAllowed -> EditorSeed(ConditionKind.SUM_TAIL, values.joinToString())
    is SpanRange -> EditorSeed(ConditionKind.SPAN, minimum = minimum.toString(), maximum = maximum.toString())
    is OddCountAllowed -> EditorSeed(ConditionKind.ODD, counts.joinToString())
    is BigCountAllowed -> EditorSeed(ConditionKind.BIG, counts.joinToString())
    is PrimeLikeCountAllowed -> EditorSeed(ConditionKind.PRIME, counts.joinToString())
    is RouteAllowed -> EditorSeed(ConditionKind.ROUTE, routes.joinToString(), position = position)
    is GroupShapeCondition -> EditorSeed(ConditionKind.SHAPE, shapes = values)
    is ConsecutiveCondition -> EditorSeed(ConditionKind.CONSECUTIVE, requirePair = requirePair)
    is PairRelationRange -> EditorSeed(
        kind = ConditionKind.PAIR,
        minimum = minimum.toString(),
        maximum = maximum.toString(),
        pairPosition = position,
        pairMetric = metric,
    )
    is DanTuoCondition -> EditorSeed(
        kind = ConditionKind.REQUIRED,
        digits = danDigits.joinToString(),
    )
}
