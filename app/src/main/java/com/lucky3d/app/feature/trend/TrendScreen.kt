package com.lucky3d.app.feature.trend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky3d.app.R
import com.lucky3d.app.core.ui.EmptyState
import com.lucky3d.app.core.ui.Lucky3dDesign
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
    var showCustomWindow by rememberSaveable { mutableStateOf(false) }
    var customWindow by rememberSaveable { mutableStateOf("") }
    val latestIssue = state.visibleDraws.lastOrNull()?.issue
    val chartDescription = stringResource(R.string.trend_chart_viewport_a11y)
    val selectedStatistics = state.statistics
        .firstOrNull { it.position == state.statisticsPosition }
    val displayedPoint = state.selectedPoint
        ?: state.points
            .filter { it.position == state.statisticsPosition }
            .maxByOrNull(TrendPoint::rowIndex)

    if (showCustomWindow) {
        CustomWindowDialog(
            value = customWindow,
            onValueChange = { customWindow = it.filter(Char::isDigit).take(4) },
            onDismiss = { showCustomWindow = false },
            onConfirm = {
                customWindow.toIntOrNull()
                    ?.takeIf { it in 1..3334 }
                    ?.let(onSetWindow)
                showCustomWindow = false
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        FlowingCinnabarTrendHeader(issue = latestIssue)
        TrendWindowBar(
            selectedWindow = state.window,
            onSetWindow = onSetWindow,
            onCustom = { showCustomWindow = true },
        )
        TrendPositionBar(
            visiblePositions = state.visiblePositions,
            onTogglePosition = onTogglePosition,
            onReturnLatest = onReturnLatest,
        )
        TrendGestureHint()

        if (state.visibleDraws.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.trend_no_data_title),
                detail = stringResource(R.string.trend_no_data_detail),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
            TrendChart(
                state = state,
                accessibilitySummary = chartDescription,
                issueHeader = stringResource(R.string.trend_issue_header),
                onSelectPoint = onSelectPoint,
                onScaleChange = onScaleChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.45f),
            )
            TrendPointSummary(displayedPoint)
            TrendStatisticsHeader(
                position = state.statisticsPosition,
                window = state.window,
            )
            if (selectedStatistics == null) {
                Spacer(modifier = Modifier.weight(0.82f))
            } else {
                TrendStatisticsList(
                    statistics = selectedStatistics,
                    onShowStatistics = onShowStatistics,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.82f),
                )
            }
        }
    }
}

@Composable
private fun FlowingCinnabarTrendHeader(issue: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
    ) {
        CinnabarFlowBackdrop(modifier = Modifier.fillMaxSize())
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_title),
                modifier = Modifier.weight(1f),
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.trend_title),
                modifier = Modifier.weight(0.72f),
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.weight(1.38f),
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.94f),
                    contentColor = Lucky3dDesign.colors.primaryDeep,
                ) {
                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = issue?.let { stringResource(R.string.home_issue, it) } ?: "---",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.94f),
                    contentColor = Lucky3dDesign.colors.primaryDeep,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = stringResource(R.string.trend_calendar),
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CinnabarFlowBackdrop(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val deep = Lucky3dDesign.colors.primaryDeep
    Canvas(
        modifier = modifier.background(
            Brush.horizontalGradient(listOf(primary, deep, primary)),
        ),
    ) {
        repeat(7) { index ->
            val inset = index * 5.dp.toPx()
            val wave = Path().apply {
                moveTo(-10.dp.toPx(), size.height * (0.30f + index * 0.045f))
                cubicTo(
                    size.width * 0.22f,
                    size.height * (0.95f - index * 0.035f),
                    size.width * 0.70f,
                    size.height * (0.05f + index * 0.04f),
                    size.width + inset,
                    size.height * (0.55f + index * 0.02f),
                )
            }
            drawPath(
                path = wave,
                color = androidx.compose.ui.graphics.Color.White.copy(
                    alpha = 0.12f - index * 0.012f,
                ),
                style = Stroke(width = (2.4f - index * 0.2f).dp.toPx()),
            )
        }
        drawPath(
            path = Path().apply {
                moveTo(size.width * 0.72f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(size.width * 0.55f, size.height)
                close()
            },
            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.10f),
        )
    }
}

@Composable
private fun TrendWindowBar(
    selectedWindow: Int,
    onSetWindow: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ),
    ) {
        listOf(10, 30, 50, 100).forEach { window ->
            TrendWindowItem(
                label = stringResource(R.string.trend_window, window),
                selected = selectedWindow == window,
                onClick = { onSetWindow(window) },
                modifier = Modifier.weight(1f),
            )
        }
        TrendWindowItem(
            label = stringResource(R.string.trend_custom_window_short),
            selected = selectedWindow !in setOf(10, 30, 50, 100),
            onClick = onCustom,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TrendWindowItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = label,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(3.dp)
                .background(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
private fun TrendPositionBar(
    visiblePositions: Set<TrendPosition>,
    onTogglePosition: (TrendPosition) -> Unit,
    onReturnLatest: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrendPosition.entries.forEach { position ->
            val selected = position in visiblePositions
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTogglePosition(position) }
                    .semantics { this.selected = selected },
                shape = RoundedCornerShape(50),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.44f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    1.dp,
                    if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.56f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TrendPositionGlyph(position = position, selected = selected)
                    Text(
                        text = positionLabel(position),
                        modifier = Modifier.padding(start = 5.dp),
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
        Surface(
            modifier = Modifier
                .weight(1.22f)
                .fillMaxHeight()
                .clickable(onClick = onReturnLatest),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)),
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.trend_return_latest),
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun TrendPositionGlyph(
    position: TrendPosition,
    selected: Boolean,
) {
    val color = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Canvas(modifier = Modifier.size(14.dp)) {
        when (position) {
            TrendPosition.HUNDREDS -> drawCircle(
                color = color,
                radius = size.minDimension * 0.38f,
                style = Stroke(width = 2.dp.toPx()),
            )
            TrendPosition.TENS -> drawRect(
                color = color,
                topLeft = Offset(size.width * 0.18f, size.height * 0.18f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.64f),
                style = Stroke(width = 1.5.dp.toPx()),
            )
            TrendPosition.ONES -> {
                val diamond = Path().apply {
                    moveTo(size.width / 2f, size.height * 0.08f)
                    lineTo(size.width * 0.92f, size.height / 2f)
                    lineTo(size.width / 2f, size.height * 0.92f)
                    lineTo(size.width * 0.08f, size.height / 2f)
                    close()
                }
                drawPath(diamond, color)
            }
        }
    }
}

@Composable
private fun TrendGestureHint() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.trend_chart_hint_short),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TrendPointSummary(point: TrendPoint?) {
    val description = point?.let {
        stringResource(
            R.string.trend_point_summary_a11y,
            it.issue,
            positionLabel(it.position),
            it.digit,
            it.omission,
        )
    } ?: stringResource(R.string.trend_point_summary_empty)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
    ) {
        if (point == null) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.trend_point_summary_empty),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_issue, point.issue),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
                Text(
                    text = positionLabel(point.position),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                )
                Text(
                    text = point.digit.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.trend_omission_value, point.omission),
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun TrendStatisticsHeader(
    position: TrendPosition,
    window: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                R.string.trend_statistics_summary,
                positionLabel(position),
                window,
            ),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TrendStatisticsList(
    statistics: TrendPositionStatistics,
    onShowStatistics: (TrendPosition) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ordered = remember(statistics.digits) {
        statistics.digits.sortedWith(
            compareByDescending<TrendDigitStatistics> { it.currentOmission }
                .thenBy { it.digit },
        )
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 2.dp),
    ) {
        if (!statistics.sampleComplete) {
            item {
                Text(
                    text = stringResource(
                        R.string.trend_sample_incomplete,
                        statistics.actualWindowSize,
                        statistics.requestedWindowSize,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 9.sp,
                )
            }
        }
        items(ordered, key = TrendDigitStatistics::digit) { row ->
            TrendStatisticsRow(
                row = row,
                onClick = { onShowStatistics(statistics.position) },
            )
        }
    }
}

@Composable
private fun TrendStatisticsRow(
    row: TrendDigitStatistics,
    onClick: () -> Unit,
) {
    val average = row.averageOmission?.let { "%.2f".format(it) } ?: "--"
    val maximum = row.maxOmission?.toString() ?: "--"
    val heat = heatLabel(row.heatLevel)
    val description = stringResource(
        R.string.trend_statistics_row_a11y,
        row.digit,
        row.currentOmission,
        average,
        maximum,
        row.occurrences,
        heat,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            )
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = row.digit.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
        CompactStatistic(stringResource(R.string.trend_current_short), row.currentOmission.toString())
        CompactStatistic(stringResource(R.string.trend_average_short), average)
        CompactStatistic(stringResource(R.string.trend_max_short), maximum)
        CompactStatistic(stringResource(R.string.trend_occurrences_short), row.occurrences.toString())
        Text(
            text = heat,
            modifier = Modifier
                .background(
                    color = when (row.heatLevel) {
                        HeatLevel.COLD -> MaterialTheme.colorScheme.tertiaryContainer
                        HeatLevel.WARM -> MaterialTheme.colorScheme.secondaryContainer
                        HeatLevel.HOT -> MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = RoundedCornerShape(6.dp),
                )
                .padding(horizontal = 6.dp, vertical = 3.dp),
            color = when (row.heatLevel) {
                HeatLevel.COLD -> MaterialTheme.colorScheme.onTertiaryContainer
                HeatLevel.WARM -> MaterialTheme.colorScheme.onSecondaryContainer
                HeatLevel.HOT -> MaterialTheme.colorScheme.onPrimaryContainer
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RowScope.CompactStatistic(label: String, value: String) {
    Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 8.sp,
            maxLines = 1,
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun CustomWindowDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trend_custom_window_short)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(stringResource(R.string.trend_custom_window)) },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = value.toIntOrNull()?.let { it in 1..3334 } == true,
            ) {
                Text(stringResource(R.string.trend_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
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
