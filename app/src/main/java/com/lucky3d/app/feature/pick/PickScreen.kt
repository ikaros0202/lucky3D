package com.lucky3d.app.feature.pick

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lucky3d.app.R
import com.lucky3d.app.core.ui.EmptyState
import com.lucky3d.app.core.ui.InlineMessage
import com.lucky3d.app.core.ui.InlineStatusBanner
import com.lucky3d.app.core.ui.MessageKind
import com.lucky3d.app.core.ui.FlowingCinnabarHeader
import com.lucky3d.app.core.ui.MatteNumberBall
import com.lucky3d.app.domain.attributes.DrawNumber
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
import com.lucky3d.app.domain.filter.PlayType
import com.lucky3d.app.domain.filter.Position
import com.lucky3d.app.domain.filter.PositionAllowed
import com.lucky3d.app.domain.filter.PrimeLikeCountAllowed
import com.lucky3d.app.domain.filter.RouteAllowed
import com.lucky3d.app.domain.filter.SpanRange
import com.lucky3d.app.domain.filter.SumRange
import com.lucky3d.app.domain.filter.SumTailAllowed

@Composable
fun PickScreen(
    state: PickUiState,
    onSetTargetIssue: (String) -> Unit,
    onSetObservationWindow: (Int) -> Unit,
    onSetPlayType: (PlayType) -> Unit,
    onSetMode: (PickMode) -> Unit,
    onSelectManualPosition: (Int) -> Unit,
    onSelectManualDigit: (Int) -> Unit,
    onRemoveManualBet: (DrawNumber) -> Unit,
    onClearManual: () -> Unit,
    onAddCondition: (FilterCondition) -> Unit,
    onEditCondition: (String, FilterCondition) -> Unit,
    onSetConditionEnabled: (String, Boolean) -> Unit,
    onRemoveCondition: (String) -> Unit,
    onSetDanDigits: (Set<Int>) -> Unit,
    onSetTuoDigits: (Set<Int>) -> Unit,
    onSetMultiplier: (Int) -> Unit,
    onUndo: () -> Unit,
    saveStatus: PickSaveStatus = PickSaveStatus.IDLE,
    onDismissSaveStatus: () -> Unit = {},
    onSaveTemplate: (String) -> Unit,
    onSaveScheme: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingConditionId by rememberSaveable { mutableStateOf<String?>(null) }
    var saveTemplateDialogOpen by rememberSaveable { mutableStateOf(false) }
    var saveSchemeDialogOpen by rememberSaveable { mutableStateOf(false) }
    var moreMethodsExpanded by rememberSaveable { mutableStateOf(false) }
    val editingCondition = state.conditions
        .firstOrNull { it.id == editingConditionId }
        ?.condition

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("pick_list")
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 10.dp),
    ) {
        item {
            FlowingCinnabarHeader(
                title = stringResource(R.string.pick_title),
                subtitle = state.targetIssue.takeIf(String::isNotBlank)?.let { "第 $it 期" },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
                actions = {
                    if (state.mode == PickMode.MANUAL) {
                        TextButton(onClick = onClearManual) {
                            Text(
                                stringResource(R.string.pick_manual_clear),
                                color = Color.White,
                            )
                        }
                    }
                },
            )
        }
        item {
            PickContextControls(
                state = state,
                onSetTargetIssue = onSetTargetIssue,
                onSetObservationWindow = onSetObservationWindow,
                onSetPlayType = onSetPlayType,
                onSetMode = onSetMode,
                moreMethodsExpanded = moreMethodsExpanded,
                onToggleMoreMethods = { moreMethodsExpanded = !moreMethodsExpanded },
            )
        }
        if (saveStatus == PickSaveStatus.SAVED || saveStatus == PickSaveStatus.FAILED) {
            item {
                InlineStatusBanner(
                    message = InlineMessage(
                        kind = if (saveStatus == PickSaveStatus.SAVED) {
                            MessageKind.SUCCESS
                        } else {
                            MessageKind.ERROR
                        },
                        title = stringResource(
                            if (saveStatus == PickSaveStatus.SAVED) {
                                R.string.operation_success
                            } else {
                                R.string.operation_failed
                            },
                        ),
                        actionLabel = stringResource(R.string.dismiss),
                    ),
                    onAction = onDismissSaveStatus,
                )
            }
        }
        if (state.mode == PickMode.MANUAL) {
            item {
                ManualPickSection(
                    state = state,
                    onSelectPosition = onSelectManualPosition,
                    onSelectDigit = onSelectManualDigit,
                )
            }
        } else if (state.mode == PickMode.FILTER) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(
                            R.string.pick_enabled_conditions,
                            state.enabledConditionCount,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Button(
                        onClick = {
                            editingConditionId = null
                            editorOpen = true
                        },
                        modifier = Modifier.testTag("condition_add_button"),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text(
                            stringResource(R.string.pick_add_condition),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
            if (state.conditions.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.pick_no_conditions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.conditions, key = PickConditionItem::id) { item ->
                    ConditionCard(
                        item = item,
                        onEdit = {
                            editingConditionId = item.id
                            editorOpen = true
                        },
                        onSetEnabled = { onSetConditionEnabled(item.id, it) },
                        onDelete = { onRemoveCondition(item.id) },
                    )
                }
            }
        } else if (state.mode == PickMode.DAN_TUO) {
            item {
                DanTuoEditor(
                    danDigits = state.danDigits,
                    tuoDigits = state.tuoDigits,
                    onSetDanDigits = onSetDanDigits,
                    onSetTuoDigits = onSetTuoDigits,
                )
            }
        }
        state.conflict?.let { conflict ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.pick_conflict_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(conflict.message)
                        if (state.canUndo) {
                            OutlinedButton(onClick = onUndo) {
                                Text(stringResource(R.string.pick_undo))
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(
                stringResource(
                    if (state.mode == PickMode.MANUAL) {
                        R.string.pick_manual_numbers
                    } else {
                        R.string.pick_candidates
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (state.candidates.isEmpty()) {
            item {
                if (state.mode == PickMode.MANUAL) {
                    Text(
                        stringResource(R.string.pick_manual_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    EmptyState(
                        title = stringResource(R.string.pick_conflict_title),
                        detail = state.conflict?.message.orEmpty(),
                    )
                }
            }
        } else {
            itemsIndexed(
                items = state.candidates,
                key = { _, candidate -> candidate.value },
            ) { index, candidate ->
                CandidateRow(
                    candidate = candidate,
                    permutations = state.candidatePermutations[index],
                    showPermutations = state.playType != PlayType.STRAIGHT,
                    onRemove = if (state.mode == PickMode.MANUAL) {
                        { onRemoveManualBet(candidate) }
                    } else {
                        null
                    },
                )
            }
        }
        item {
            PickSummary(
                state = state,
                onSetMultiplier = onSetMultiplier,
                onSaveTemplate = { saveTemplateDialogOpen = true },
                onSaveScheme = { saveSchemeDialogOpen = true },
            )
        }
    }

    if (editorOpen) {
        ConditionEditor(
            initialCondition = editingCondition,
            onDismiss = {
                editorOpen = false
                editingConditionId = null
            },
            onConfirm = { condition ->
                val id = editingConditionId
                if (id == null) {
                    onAddCondition(condition)
                } else {
                    onEditCondition(id, condition)
                }
                editorOpen = false
                editingConditionId = null
            },
        )
    }
    if (saveTemplateDialogOpen) {
        SaveTemplateDialog(
            onDismiss = { saveTemplateDialogOpen = false },
            onSave = {
                onSaveTemplate(it)
                saveTemplateDialogOpen = false
            },
        )
    }
    if (saveSchemeDialogOpen) {
        SaveSchemeDialog(
            onDismiss = { saveSchemeDialogOpen = false },
            onSave = { title, note ->
                onSaveScheme(title, note)
                saveSchemeDialogOpen = false
            },
        )
    }
}

@Composable
private fun PickContextControls(
    state: PickUiState,
    onSetTargetIssue: (String) -> Unit,
    onSetObservationWindow: (Int) -> Unit,
    onSetPlayType: (PlayType) -> Unit,
    onSetMode: (PickMode) -> Unit,
    moreMethodsExpanded: Boolean,
    onToggleMoreMethods: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.targetIssue,
                onValueChange = onSetTargetIssue,
                label = { Text(stringResource(R.string.pick_target_issue)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToggleMoreMethods) {
                Text(stringResource(R.string.pick_more_methods))
            }
        }
        if (moreMethodsExpanded) {
            OutlinedTextField(
                value = state.observationWindow.toString(),
                onValueChange = { it.toIntOrNull()?.let(onSetObservationWindow) },
                label = { Text(stringResource(R.string.pick_observation_window)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = state.mode == PickMode.MANUAL,
                    onClick = { onSetMode(PickMode.MANUAL) },
                    label = { Text(stringResource(R.string.pick_manual_mode)) },
                )
                FilterChip(
                    selected = state.mode == PickMode.FILTER &&
                        state.playType == PlayType.GROUP3,
                    onClick = {
                        onSetMode(PickMode.FILTER)
                        onSetPlayType(PlayType.GROUP3)
                    },
                    label = { Text(stringResource(R.string.pick_play_group3)) },
                )
                FilterChip(
                    selected = state.mode == PickMode.FILTER &&
                        state.playType == PlayType.GROUP6,
                    onClick = {
                        onSetMode(PickMode.FILTER)
                        onSetPlayType(PlayType.GROUP6)
                    },
                    label = { Text(stringResource(R.string.pick_play_group6)) },
                )
                FilterChip(
                    selected = state.mode == PickMode.FILTER &&
                        state.playType == PlayType.STRAIGHT,
                    onClick = {
                        onSetMode(PickMode.FILTER)
                        onSetPlayType(PlayType.STRAIGHT)
                    },
                    label = { Text(stringResource(R.string.pick_mode_filter)) },
                )
                FilterChip(
                    selected = state.mode == PickMode.DAN_TUO,
                    onClick = { onSetMode(PickMode.DAN_TUO) },
                    label = { Text(stringResource(R.string.pick_mode_dan_tuo)) },
                )
            }
        }
    }
}

@Composable
private fun ManualPickSection(
    state: PickUiState,
    onSelectPosition: (Int) -> Unit,
    onSelectDigit: (Int) -> Unit,
) {
    val positionLabels = listOf(
        stringResource(R.string.pick_manual_hundreds),
        stringResource(R.string.pick_manual_tens),
        stringResource(R.string.pick_manual_ones),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("manual_pick"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.pick_manual_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            state.manualDigits.forEachIndexed { index, digit ->
                val displayed = digit?.toString() ?: "-"
                Column(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onSelectPosition(index) }
                        .testTag("manual_ball_$index")
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(positionLabels[index], style = MaterialTheme.typography.labelMedium)
                    MatteNumberBall(
                        text = displayed,
                        selected = state.activeManualPosition == index,
                        contentDescription = stringResource(
                            R.string.pick_manual_ball_description,
                            positionLabels[index],
                            displayed,
                        ),
                        modifier = Modifier.size(64.dp),
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..9).chunked(5).forEach { digits ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    digits.forEach { digit ->
                        OutlinedButton(
                            onClick = { onSelectDigit(digit) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("manual_digit_$digit"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) {
                            Text(
                                digit.toString(),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConditionCard(
    item: PickConditionItem,
    onEdit: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.condition.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        conditionSummary(item.condition),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.pick_condition_excluded, item.excludedCount),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Switch(
                    checked = item.enabled,
                    onCheckedChange = onSetEnabled,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.pick_edit))
                }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.pick_delete))
                }
            }
        }
    }
}

@Composable
private fun DanTuoEditor(
    danDigits: Set<Int>,
    tuoDigits: Set<Int>,
    onSetDanDigits: (Set<Int>) -> Unit,
    onSetTuoDigits: (Set<Int>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.pick_digit_hint))
        DigitSelector(
            label = stringResource(R.string.pick_dan_digits),
            selected = danDigits,
            onToggle = { digit ->
                onSetDanDigits(if (digit in danDigits) danDigits - digit else danDigits + digit)
            },
        )
        DigitSelector(
            label = stringResource(R.string.pick_tuo_digits),
            selected = tuoDigits,
            onToggle = { digit ->
                onSetTuoDigits(if (digit in tuoDigits) tuoDigits - digit else tuoDigits + digit)
            },
        )
    }
}

@Composable
private fun DigitSelector(
    label: String,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (0..9).forEach { digit ->
                FilterChip(
                    selected = digit in selected,
                    onClick = { onToggle(digit) },
                    label = { Text(digit.toString()) },
                )
            }
        }
    }
}

@Composable
private fun PickSummary(
    state: PickUiState,
    onSetMultiplier: (Int) -> Unit,
    onSaveTemplate: () -> Unit,
    onSaveScheme: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pick_summary"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(
                        R.string.pick_summary,
                        state.betCount,
                        state.multiplier,
                        state.amountYuan,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { onSetMultiplier(state.multiplier - 1) },
                        modifier = Modifier.size(48.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text("−")
                    }
                    Text("${state.multiplier} 倍")
                    OutlinedButton(
                        onClick = { onSetMultiplier(state.multiplier + 1) },
                        modifier = Modifier.size(48.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text("+")
                    }
                }
            }
            Text(
                stringResource(R.string.pick_reconcile_only),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.mode == PickMode.FILTER) {
                    OutlinedButton(
                        onClick = onSaveTemplate,
                        enabled = state.conditions.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.pick_save_template))
                    }
                }
                Button(
                    onClick = onSaveScheme,
                    enabled = state.candidates.isNotEmpty() && state.targetIssue.length == 7,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_scheme_button"),
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null)
                    Text(
                        stringResource(R.string.pick_save_scheme),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: DrawNumber,
    permutations: List<DrawNumber>,
    showPermutations: Boolean,
    onRemove: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(candidate.value, style = MaterialTheme.typography.titleMedium)
            if (showPermutations) {
                Text(
                    stringResource(
                        R.string.pick_candidate_permutations,
                        candidate.value,
                        permutations.joinToString(" ") { it.value },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        onRemove?.let {
            TextButton(onClick = it) {
                Icon(Icons.Outlined.Close, contentDescription = null)
                Text(stringResource(R.string.pick_delete))
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun conditionSummary(condition: FilterCondition): String = when (condition) {
    is GlobalRequiredDigits -> digitsSummary(condition.digits)
    is GlobalExcludedDigits -> digitsSummary(condition.digits)
    is PositionAllowed -> stringResource(
        R.string.pick_condition_position_value,
        pickPositionLabel(condition.position),
        condition.allowedDigits.sorted().joinToString(),
    )
    is SumRange -> rangeSummary(condition.minimum, condition.maximum)
    is SumTailAllowed -> digitsSummary(condition.values)
    is SpanRange -> rangeSummary(condition.minimum, condition.maximum)
    is OddCountAllowed -> digitsSummary(condition.counts)
    is BigCountAllowed -> digitsSummary(condition.counts)
    is PrimeLikeCountAllowed -> digitsSummary(condition.counts)
    is RouteAllowed -> stringResource(
        R.string.pick_condition_position_value,
        pickPositionLabel(condition.position),
        condition.routes.sorted().joinToString(),
    )
    is GroupShapeCondition -> stringResource(
        R.string.pick_condition_shapes_value,
        pickShapeLabels(condition.values),
    )
    is ConsecutiveCondition -> stringResource(
        R.string.pick_condition_consecutive_value,
        stringResource(if (condition.requirePair) R.string.history_yes else R.string.history_no),
    )
    is PairRelationRange -> stringResource(
        R.string.pick_condition_pair_value,
        pickPairPositionLabel(condition.position),
        stringResource(
            if (condition.metric == PairMetric.SUM) {
                R.string.pick_metric_sum
            } else {
                R.string.pick_metric_difference
            },
        ),
        condition.minimum,
        condition.maximum,
    )
    is DanTuoCondition -> stringResource(
        R.string.pick_condition_dan_tuo_value,
        condition.danDigits.sorted().joinToString(),
        condition.tuoDigits.sorted().joinToString(),
    )
}

@Composable
private fun digitsSummary(values: Set<Int>): String = stringResource(
    R.string.pick_condition_digits_value,
    values.sorted().joinToString(),
)

@Composable
private fun rangeSummary(minimum: Int, maximum: Int): String = stringResource(
    R.string.pick_condition_range_value,
    minimum,
    maximum,
)

@Composable
private fun pickPositionLabel(position: Position): String = stringResource(
    when (position) {
        Position.HUNDREDS -> R.string.trend_position_hundreds
        Position.TENS -> R.string.trend_position_tens
        Position.ONES -> R.string.trend_position_ones
    },
)

@Composable
private fun pickPairPositionLabel(position: PairPosition): String = stringResource(
    when (position) {
        PairPosition.HUNDREDS_TENS -> R.string.pick_pair_hundreds_tens
        PairPosition.TENS_ONES -> R.string.pick_pair_tens_ones
        PairPosition.HUNDREDS_ONES -> R.string.pick_pair_hundreds_ones
    },
)

@Composable
private fun pickShapeLabels(shapes: Set<GroupShape>): String {
    val leopard = if (GroupShape.LEOPARD in shapes) {
        stringResource(R.string.pick_shape_leopard)
    } else {
        null
    }
    val group3 = if (GroupShape.GROUP3 in shapes) {
        stringResource(R.string.pick_shape_group3)
    } else {
        null
    }
    val group6 = if (GroupShape.GROUP6 in shapes) {
        stringResource(R.string.pick_shape_group6)
    } else {
        null
    }
    return listOfNotNull(leopard, group3, group6).joinToString()
}

@Composable
private fun SaveTemplateDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_dialog_template_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.save_dialog_template_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save))
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
private fun SaveSchemeDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_dialog_scheme_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.save_dialog_scheme_name)) },
                    singleLine = true,
                    modifier = Modifier.testTag("save_scheme_title"),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.save_dialog_scheme_note)) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, note) }, enabled = title.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
