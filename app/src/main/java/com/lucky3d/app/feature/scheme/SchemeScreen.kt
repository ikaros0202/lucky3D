package com.lucky3d.app.feature.scheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lucky3d.app.R
import com.lucky3d.app.core.ui.EmptyState
import com.lucky3d.app.core.ui.InlineMessage
import com.lucky3d.app.core.ui.InlineStatusBanner
import com.lucky3d.app.core.ui.MessageKind
import com.lucky3d.app.data.repository.SavedScheme
import com.lucky3d.app.data.repository.SavedTemplate
import com.lucky3d.app.data.repository.SchemeWithReplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeScreen(
    state: SchemeUiState,
    onShowSection: (SchemeSection) -> Unit,
    onSelectScheme: (String?) -> Unit,
    onSelectTemplate: (String?) -> Unit,
    onSetBacktestRange: (String, String) -> Unit,
    onRunBacktest: () -> Unit,
    onCopyScheme: (String, String) -> Unit,
    onUpdateNote: (String, String) -> Unit,
    onDismissStatus: () -> Unit,
    onStartPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
    ) {
        item {
            Text(
                stringResource(R.string.scheme_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SchemeSection.entries.forEach { section ->
                    FilterChip(
                        selected = state.section == section,
                        onClick = { onShowSection(section) },
                        label = {
                            Text(
                                stringResource(
                                    if (section == SchemeSection.SCHEMES) {
                                        R.string.scheme_section_schemes
                                    } else {
                                        R.string.scheme_section_templates
                                    },
                                ),
                            )
                        },
                    )
                }
            }
        }
        if (state.operationSucceeded) {
            item {
                InlineStatusBanner(
                    message = InlineMessage(
                        title = stringResource(R.string.operation_success),
                        kind = MessageKind.SUCCESS,
                        actionLabel = stringResource(R.string.dismiss),
                    ),
                    onAction = onDismissStatus,
                )
            }
        }
        if (state.operationFailed) {
            item {
                InlineStatusBanner(
                    message = InlineMessage(
                        title = stringResource(R.string.operation_failed),
                        kind = MessageKind.ERROR,
                        actionLabel = stringResource(R.string.dismiss),
                    ),
                    onAction = onDismissStatus,
                )
            }
        }
        when (state.section) {
            SchemeSection.SCHEMES -> {
                if (state.schemes.isEmpty()) {
                    item {
                        EmptyState(
                            title = stringResource(R.string.scheme_empty_title),
                            detail = stringResource(R.string.scheme_empty_detail),
                            actionLabel = stringResource(R.string.scheme_start_pick),
                            onAction = onStartPick,
                        )
                    }
                } else {
                    items(state.schemes, key = { it.scheme.id }) { item ->
                        SchemeCard(item = item, onClick = { onSelectScheme(item.scheme.id) })
                    }
                }
            }
            SchemeSection.TEMPLATES -> {
                if (state.templates.isEmpty()) {
                    item {
                        EmptyState(
                            title = stringResource(R.string.scheme_template_empty_title),
                            detail = stringResource(R.string.scheme_template_empty_detail),
                            actionLabel = stringResource(R.string.scheme_start_pick),
                            onAction = onStartPick,
                        )
                    }
                } else {
                    items(state.templates, key = SavedTemplate::id) { template ->
                        TemplateCard(
                            template = template,
                            selected = state.selectedTemplateId == template.id,
                            onClick = { onSelectTemplate(template.id) },
                        )
                    }
                    item {
                        BacktestSection(
                            startIssue = state.backtestStartIssue,
                            endIssue = state.backtestEndIssue,
                            isRunning = state.isBacktesting,
                            report = state.backtest,
                            onRangeChange = onSetBacktestRange,
                            onRun = onRunBacktest,
                        )
                    }
                    items(
                        items = state.backtest?.results.orEmpty(),
                        key = { it.targetIssue },
                    ) { result ->
                        BacktestResultCard(result)
                    }
                }
            }
        }
    }

    state.schemes
        .firstOrNull { it.scheme.id == state.selectedSchemeId }
        ?.let { item ->
            SchemeDetailSheet(
                item = item,
                onDismiss = { onSelectScheme(null) },
                onCopy = { issue -> onCopyScheme(item.scheme.id, issue) },
                onUpdateNote = { note -> onUpdateNote(item.scheme.id, note) },
            )
        }
}

@Composable
private fun SchemeCard(
    item: SchemeWithReplay,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(R.string.scheme_issue, item.scheme.issue),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(item.scheme.title, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.scheme_summary,
                    item.scheme.betCount,
                    item.scheme.multiplier,
                    item.scheme.amountYuan,
                ),
            )
            Text(
                replayStatus(item),
                style = MaterialTheme.typography.bodySmall,
                color = if (item.replay?.covered == true) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: SavedTemplate,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(template.name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.scheme_template_window,
                    template.observationWindow,
                    template.ruleVersion,
                ),
            )
            Text(template.conditions.joinToString(" · ") { it.title })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchemeDetailSheet(
    item: SchemeWithReplay,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
    onUpdateNote: (String) -> Unit,
) {
    var note by rememberSaveable(item.scheme.id) { mutableStateOf(item.scheme.note) }
    var copyIssue by rememberSaveable(item.scheme.id) { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.scheme_detail), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.scheme_issue, item.scheme.issue),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(item.scheme.title, style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(
                    R.string.scheme_summary,
                    item.scheme.betCount,
                    item.scheme.multiplier,
                    item.scheme.amountYuan,
                ),
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(replayStatus(item), modifier = Modifier.padding(12.dp))
            }
            Text(stringResource(R.string.scheme_conditions), style = MaterialTheme.typography.titleMedium)
            Text(item.scheme.conditions.joinToString("\n") { "• ${it.title}" })
            Text(
                stringResource(R.string.scheme_candidates, item.scheme.candidates.size),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                item.scheme.candidates.joinToString(" ") { it.value },
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
            HorizontalDivider()
            if (item.scheme.isDrawn) {
                Text(
                    stringResource(R.string.scheme_drawn_locked),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.scheme_note)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onUpdateNote(note) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.scheme_save_note))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = copyIssue,
                    onValueChange = { copyIssue = it.filter(Char::isDigit).take(7) },
                    label = { Text(stringResource(R.string.scheme_copy_issue)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = { onCopy(copyIssue) },
                    enabled = copyIssue.length == 7,
                ) {
                    Text(stringResource(R.string.scheme_copy))
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.close))
            }
        }
    }
}

@Composable
private fun replayStatus(item: SchemeWithReplay): String {
    val replay = item.replay ?: return stringResource(R.string.scheme_waiting)
    return stringResource(
        if (replay.covered) R.string.scheme_covered else R.string.scheme_not_covered,
        replay.winningNumber.value,
    )
}
