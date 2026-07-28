package com.lucky3d.app.feature.trend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.lucky3d.app.R
import com.lucky3d.app.core.ui.EmptyState
import com.lucky3d.app.domain.omission.HeatLevel

@Composable
fun TrendScreen(
    state: TrendUiState,
    onSetWindow: (Int) -> Unit,
    onTogglePosition: (TrendPosition) -> Unit,
    onSelectPoint: (TrendPoint?) -> Unit,
    onShowStatistics: (TrendPosition) -> Unit,
    onScaleChange: (Float) -> Unit,
    onReturnLatest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var customWindow by rememberSaveable { mutableStateOf("") }
    val positionLabels = mapOf(
        TrendPosition.HUNDREDS to stringResource(R.string.trend_position_hundreds),
        TrendPosition.TENS to stringResource(R.string.trend_position_tens),
        TrendPosition.ONES to stringResource(R.string.trend_position_ones),
    )
    val visibleLabels = TrendPosition.entries
        .filter(state.visiblePositions::contains)
        .joinToString("、") { positionLabels.getValue(it) }
    val chartSummary = stringResource(
        R.string.trend_chart_a11y,
        state.visibleDraws.size,
        visibleLabels,
        state.points.size,
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.trend_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        item {
            TrendControls(
                state = state,
                customWindow = customWindow,
                onCustomWindowChange = { customWindow = it.filter(Char::isDigit).take(4) },
                onSetWindow = onSetWindow,
                onApplyCustom = {
                    customWindow.toIntOrNull()?.takeIf { it in 1..3334 }?.let(onSetWindow)
                },
                onTogglePosition = onTogglePosition,
            )
        }
        if (state.visibleDraws.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.trend_no_data_title),
                    detail = stringResource(R.string.trend_no_data_detail),
                )
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.trend_chart_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.trend_chart_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(onClick = onReturnLatest) {
                            Icon(Icons.Outlined.MyLocation, contentDescription = null)
                            Text(
                                stringResource(R.string.trend_return_latest),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.trend_scale, state.scale),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Card(
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        TrendChart(
                            state = state,
                            accessibilitySummary = chartSummary,
                            issueHeader = stringResource(R.string.trend_issue_header),
                            onSelectPoint = onSelectPoint,
                            onScaleChange = onScaleChange,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
            state.selectedPoint?.let { point ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.trend_point_detail,
                                point.issue,
                                positionLabel(point.position),
                                point.digit,
                                point.omission,
                            ),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.trend_statistics),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrendPosition.entries.filter(state.visiblePositions::contains).forEach { position ->
                        FilterChip(
                            selected = state.statisticsPosition == position,
                            onClick = { onShowStatistics(position) },
                            label = { Text(positionLabel(position)) },
                        )
                    }
                }
            }
            val selectedStatistics = state.statistics
                .firstOrNull { it.position == state.statisticsPosition }
            selectedStatistics?.let { statistics ->
                if (!statistics.sampleComplete) {
                    item {
                        Text(
                            stringResource(
                                R.string.trend_sample_incomplete,
                                statistics.actualWindowSize,
                                statistics.requestedWindowSize,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                items(
                    statistics.digits.sortedWith(
                        compareByDescending<TrendDigitStatistics> { it.currentOmission }
                            .thenBy { it.digit },
                    ),
                    key = TrendDigitStatistics::digit,
                ) { row ->
                    TrendStatisticsRow(
                        row = row,
                        maximumOccurrences = statistics.digits.maxOfOrNull { it.occurrences }
                            ?.coerceAtLeast(1)
                            ?: 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendControls(
    state: TrendUiState,
    customWindow: String,
    onCustomWindowChange: (String) -> Unit,
    onSetWindow: (Int) -> Unit,
    onApplyCustom: () -> Unit,
    onTogglePosition: (TrendPosition) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(10, 30, 50, 100).forEach { count ->
                FilterChip(
                    selected = state.window == count,
                    onClick = { onSetWindow(count) },
                    label = { Text(stringResource(R.string.trend_window, count)) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = customWindow,
                onValueChange = onCustomWindowChange,
                label = { Text(stringResource(R.string.trend_custom_window)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onApplyCustom) {
                Text(stringResource(R.string.trend_apply))
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TrendPosition.entries.forEach { position ->
                FilterChip(
                    selected = position in state.visiblePositions,
                    onClick = { onTogglePosition(position) },
                    label = { Text(positionLabel(position)) },
                )
            }
        }
    }
}

@Composable
private fun TrendStatisticsRow(
    row: TrendDigitStatistics,
    maximumOccurrences: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.trend_digit_row,
                    row.digit,
                    row.currentOmission,
                    row.averageOmission?.toString() ?: "--",
                    row.maxOmission?.toString() ?: "--",
                    row.occurrences,
                    heatLabel(row.heatLevel),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = { row.occurrences.toFloat() / maximumOccurrences },
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun positionLabel(position: TrendPosition): String = when (position) {
    TrendPosition.HUNDREDS -> stringResource(R.string.trend_position_hundreds)
    TrendPosition.TENS -> stringResource(R.string.trend_position_tens)
    TrendPosition.ONES -> stringResource(R.string.trend_position_ones)
}

@Composable
private fun heatLabel(level: HeatLevel): String = when (level) {
    HeatLevel.COLD -> stringResource(R.string.trend_heat_cold)
    HeatLevel.WARM -> stringResource(R.string.trend_heat_warm)
    HeatLevel.HOT -> stringResource(R.string.trend_heat_hot)
}
