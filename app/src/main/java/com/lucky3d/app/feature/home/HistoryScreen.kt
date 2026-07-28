package com.lucky3d.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.lucky3d.app.R
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.core.ui.EmptyState
import com.lucky3d.app.data.repository.DrawQuery
import com.lucky3d.app.domain.attributes.DrawAttributes
import com.lucky3d.app.domain.attributes.GroupShape

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit,
    onShowRecent: (Int) -> Unit,
    onSearchIssue: (String) -> Unit,
    onSearchYear: (String) -> Unit,
    onSearchDateRange: (String, String) -> Unit,
    onSelectDraw: (DrawRecord?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var issueInput by rememberSaveable { mutableStateOf("") }
    var yearInput by rememberSaveable { mutableStateOf("") }
    var startDateInput by rememberSaveable { mutableStateOf("") }
    var endDateInput by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.history_data_start),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HistoryRangeChips(
            query = state.query,
            onShowRecent = onShowRecent,
        )
        HistoryQueryFields(
            issue = issueInput,
            year = yearInput,
            startDate = startDateInput,
            endDate = endDateInput,
            error = state.inputError,
            onIssueChange = { issueInput = it },
            onYearChange = { yearInput = it },
            onStartDateChange = { startDateInput = it },
            onEndDateChange = { endDateInput = it },
            onSearchIssue = { onSearchIssue(issueInput) },
            onSearchYear = { onSearchYear(yearInput) },
            onSearchDate = { onSearchDateRange(startDateInput, endDateInput) },
        )
        Text(
            text = stringResource(R.string.history_range_summary, state.records.size),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        if (state.records.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.history_empty_title),
                detail = stringResource(R.string.history_empty_detail),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                items(state.records, key = DrawRecord::issue) { draw ->
                    HistoryDrawRow(draw = draw, onClick = { onSelectDraw(draw) })
                }
            }
        }
    }

    state.selectedDraw?.let { draw ->
        DrawDetailSheet(
            draw = draw,
            onDismiss = { onSelectDraw(null) },
        )
    }
}

@Composable
private fun HistoryRangeChips(
    query: DrawQuery,
    onShowRecent: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(10, 30, 50, 100).forEach { count ->
            FilterChip(
                selected = query == DrawQuery.Recent(count),
                onClick = { onShowRecent(count) },
                label = { Text(stringResource(R.string.history_recent_count, count)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryQueryFields(
    issue: String,
    year: String,
    startDate: String,
    endDate: String,
    error: HistoryInputError?,
    onIssueChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onSearchIssue: () -> Unit,
    onSearchYear: () -> Unit,
    onSearchDate: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = issue,
                    onValueChange = onIssueChange,
                    label = { Text(stringResource(R.string.history_issue_label)) },
                    singleLine = true,
                    isError = error == HistoryInputError.INVALID_ISSUE,
                )
                Button(onClick = onSearchIssue) {
                    Text(stringResource(R.string.history_search_issue))
                }
                OutlinedTextField(
                    value = year,
                    onValueChange = onYearChange,
                    label = { Text(stringResource(R.string.history_year_label)) },
                    singleLine = true,
                    isError = error == HistoryInputError.INVALID_YEAR,
                )
                Button(onClick = onSearchYear) {
                    Text(stringResource(R.string.history_search_year))
                }
                OutlinedTextField(
                    value = startDate,
                    onValueChange = onStartDateChange,
                    label = { Text(stringResource(R.string.history_start_date_label)) },
                    singleLine = true,
                    isError = error == HistoryInputError.INVALID_DATE_RANGE,
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = onEndDateChange,
                    label = { Text(stringResource(R.string.history_end_date_label)) },
                    singleLine = true,
                    isError = error == HistoryInputError.INVALID_DATE_RANGE,
                )
                Button(onClick = onSearchDate) {
                    Text(stringResource(R.string.history_search_date))
                }
            }
            historyErrorText(error)?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun HistoryDrawRow(
    draw: DrawRecord,
    onClick: () -> Unit,
) {
    val attributes = DrawAttributes.calculate(draw.number)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(draw.issue, style = MaterialTheme.typography.labelLarge)
                Text(
                    draw.drawDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    draw.number.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(
                        R.string.draw_row_attributes,
                        attributes.sum,
                        attributes.span,
                        historyGroupShapeLabel(attributes.groupShape),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawDetailSheet(
    draw: DrawRecord,
    onDismiss: () -> Unit,
) {
    val attributes = DrawAttributes.calculate(draw.number)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.history_draw_detail),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.draw_row_summary, draw.issue, draw.drawDate),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                draw.number.value,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
            AttributeSection(
                title = stringResource(R.string.history_basic_attributes),
                lines = listOf(
                    stringResource(
                        R.string.history_sum_tail,
                        attributes.sum,
                        attributes.sumTail,
                        attributes.span,
                    ),
                    stringResource(
                        R.string.history_patterns,
                        attributes.parityPattern,
                        attributes.sizePattern,
                        attributes.qualityPattern,
                    ),
                    stringResource(
                        R.string.history_routes,
                        attributes.routePositionPattern,
                        historyGroupShapeLabel(attributes.groupShape),
                    ),
                ),
            )
            AttributeSection(
                title = stringResource(R.string.history_shape_attributes),
                lines = listOf(
                    stringResource(
                        R.string.history_consecutive,
                        yesNo(attributes.hasPairConsecutive),
                        yesNo(attributes.hasTripleConsecutive),
                    ),
                ),
            )
            AttributeSection(
                title = stringResource(R.string.history_pair_attributes),
                lines = listOf(
                    stringResource(
                        R.string.history_pair_sums,
                        attributes.pairSums.hundredsTens,
                        attributes.pairSums.tensOnes,
                        attributes.pairSums.hundredsOnes,
                    ),
                    stringResource(
                        R.string.history_pair_differences,
                        attributes.pairDifferences.hundredsTens,
                        attributes.pairDifferences.tensOnes,
                        attributes.pairDifferences.hundredsOnes,
                    ),
                ),
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text(stringResource(R.string.close))
            }
        }
    }
}

@Composable
private fun AttributeSection(title: String, lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        HorizontalDivider()
        lines.forEach { line ->
            Text(line, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun historyErrorText(error: HistoryInputError?): String? = when (error) {
    HistoryInputError.INVALID_ISSUE -> stringResource(R.string.history_invalid_issue)
    HistoryInputError.INVALID_YEAR -> stringResource(R.string.history_invalid_year)
    HistoryInputError.INVALID_DATE_RANGE -> stringResource(R.string.history_invalid_date)
    null -> null
}

@Composable
private fun historyGroupShapeLabel(shape: GroupShape): String = when (shape) {
    GroupShape.LEOPARD -> stringResource(R.string.group_leopard)
    GroupShape.GROUP3 -> stringResource(R.string.group_3)
    GroupShape.GROUP6 -> stringResource(R.string.group_6)
}

@Composable
private fun yesNo(value: Boolean): String =
    stringResource(if (value) R.string.history_yes else R.string.history_no)
