package com.lucky3d.app.feature.trend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky3d.app.R
import com.lucky3d.app.core.model.TrialNumber
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlinx.coroutines.launch

private val TrendCellWidth = 34.dp
private val TrendPrefixWidth = 48.dp
private val TrendRowHeight = 32.dp
private val TrendGroupHeight = 44.dp
private val TrendScaleToolbarHeight = 48.dp
private const val TrendDigitCount = 30
private val TrendAttributeWidth = 62.dp
private val TrendAttributeLabels = listOf("和值", "和尾", "跨度", "奇偶比", "大小比", "012路个数比")
private const val FutureRowCount = 2
private const val SummaryRowCount = 4

internal fun calculateTrendFitScale(
    availableWidthPx: Float,
    logicalTableWidthPx: Float,
): Float {
    require(availableWidthPx > 0f) { "Available trend width must be positive" }
    require(logicalTableWidthPx > 0f) { "Logical trend width must be positive" }
    return minOf(1f, availableWidthPx / logicalTableWidthPx)
}

@Composable
fun TrendChart(
    state: TrendUiState,
    accessibilitySummary: String,
    onSetWindow: (Int) -> Unit,
    onSetScale: (Float) -> Unit = {},
    onSelectPoint: (TrendPoint?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val horizontalScrollState = rememberScrollState()
    val transformScope = rememberCoroutineScope()
    val bodyRowCount = state.tableRows.size + FutureRowCount + SummaryRowCount
    val bodyHeight = TrendRowHeight * bodyRowCount
    val tableHeight = TrendGroupHeight + TrendRowHeight + bodyHeight + TrendScaleToolbarHeight
    val positionLabels = listOf(
        stringResource(R.string.trend_position_hundreds),
        stringResource(R.string.trend_position_tens),
        stringResource(R.string.trend_position_ones),
    )
    val summaryLabels = listOf(
        stringResource(R.string.trend_occurrence_count),
        stringResource(R.string.trend_current_omission),
        stringResource(R.string.trend_average_omission),
        stringResource(R.string.trend_max_omission),
    )
    val futureIssues = remember(state.tableRows) {
        nextIssues(state.tableRows.lastOrNull()?.issue)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(tableHeight)
            .semantics { contentDescription = accessibilitySummary },
    ) {
        val lockedWidth = if (maxWidth < 390.dp) 72.dp else 80.dp
        val availableWidth = (maxWidth - lockedWidth).coerceAtLeast(1.dp)
        val logicalWidth = TrendPrefixWidth * 2 +
            TrendCellWidth * TrendDigitCount +
            TrendAttributeWidth * TrendAttributeLabels.size
        val fitScale = calculateTrendFitScale(
            availableWidthPx = with(density) { availableWidth.toPx() },
            logicalTableWidthPx = with(density) { logicalWidth.toPx() },
        )
        val appliedScale = state.scale.coerceIn(fitScale, 2.5f)
        val cellWidth = TrendCellWidth * appliedScale
        val prefixWidth = TrendPrefixWidth * appliedScale
        val attributeWidth = TrendAttributeWidth * appliedScale
        val totalContentWidth = prefixWidth * 2 +
            cellWidth * TrendDigitCount +
            attributeWidth * TrendAttributeLabels.size
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            onSetScale(state.scale * zoomChange)
            if (panChange.x != 0f) {
                horizontalScrollState.dispatchRawDelta(-panChange.x)
            }
        }
        Row(modifier = Modifier.fillMaxSize()) {
            LockedTrendColumn(
                state = state,
                futureIssues = futureIssues,
                summaryLabels = summaryLabels,
                onSetWindow = onSetWindow,
                lockedWidth = lockedWidth,
                modifier = Modifier
                    .width(lockedWidth)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScrollState)
                    .transformable(transformState),
            ) {
                TrendGroupHeader(
                    labels = positionLabels,
                    attributeLabels = TrendAttributeLabels,
                    cellWidth = cellWidth,
                    prefixWidth = prefixWidth,
                    attributeWidth = attributeWidth,
                    modifier = Modifier.width(totalContentWidth),
                )
                TrendDigitHeader(
                    cellWidth = cellWidth,
                    attributeLabels = TrendAttributeLabels,
                    prefixWidth = prefixWidth,
                    attributeWidth = attributeWidth,
                    modifier = Modifier.width(totalContentWidth),
                )
                Canvas(
                    modifier = Modifier
                        .width(totalContentWidth)
                        .height(bodyHeight)
                        .semantics {
                            contentDescription = buildTrendAccessibilitySummary(
                                accessibilitySummary,
                                state,
                            )
                        }
                        .pointerInput(state.tableRows) {
                            detectTapGestures { tap ->
                                val rowHeightPx = with(density) { TrendRowHeight.toPx() }
                                val cellWidthPx = with(density) { cellWidth.toPx() }
                                val rowIndex = floor(tap.y / rowHeightPx).toInt()
                                if (rowIndex !in state.tableRows.indices) {
                                    onSelectPoint(null)
                                    return@detectTapGestures
                                }
                                val prefixWidthPx = with(density) { (prefixWidth * 2).toPx() }
                                val groupIndex = floor((tap.x - prefixWidthPx) / (cellWidthPx * 10f)).toInt()
                                if (groupIndex !in TrendPosition.entries.indices) {
                                    onSelectPoint(null)
                                    return@detectTapGestures
                                }
                                val row = state.tableRows[rowIndex]
                                val hitDigit = row.drawNumber[groupIndex].digitToInt()
                                val hitCenterX = prefixWidthPx +
                                    (groupIndex * 10 + hitDigit + 0.5f) * cellWidthPx
                                val hitCenterY = (rowIndex + 0.5f) * rowHeightPx
                                val targetRadius = with(density) { 24.dp.toPx() }
                                if (
                                    abs(tap.x - hitCenterX) <= targetRadius &&
                                    abs(tap.y - hitCenterY) <= targetRadius
                                ) {
                                    onSelectPoint(
                                        TrendPoint(
                                            issue = row.issue,
                                            rowIndex = rowIndex,
                                            position = TrendPosition.entries[groupIndex],
                                            digit = hitDigit,
                                            omission = row.omissions[groupIndex * 10 + hitDigit],
                                        ),
                                    )
                                } else {
                                    onSelectPoint(null)
                                }
                            }
                        },
                ) {
                    drawTrendBody(
                        state = state,
                        cellWidth = cellWidth.toPx(),
                        prefixWidth = prefixWidth.toPx(),
                        attributeWidth = attributeWidth.toPx(),
                        rowHeight = TrendRowHeight.toPx(),
                        gridColor = colors.outlineVariant,
                        textColor = colors.onSurfaceVariant,
                        primary = colors.primary,
                        background = colors.background,
                        surfaceVariant = colors.surfaceVariant,
                    )
                }
                Box(modifier = Modifier.height(TrendScaleToolbarHeight))
            }
        }
        TrendScaleToolbar(
            state = state,
            onSetScale = onSetScale,
            onShowAll = {
                onSetScale(fitScale)
                transformScope.launch { horizontalScrollState.scrollTo(0) }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = lockedWidth)
                .height(TrendScaleToolbarHeight),
        )
        Box(
            modifier = Modifier
                .offset(x = lockedWidth)
                .width(8.dp)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colors.onSurface.copy(alpha = 0.07f),
                            Color.Transparent,
                        ),
                    ),
                )
                .clearAndSetSemantics { },
        )
    }
}

@Composable
private fun LockedTrendColumn(
    state: TrendUiState,
    futureIssues: List<String>,
    summaryLabels: List<String>,
    onSetWindow: (Int) -> Unit,
    lockedWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val bodyRows = state.tableRows.size + FutureRowCount + SummaryRowCount
    val bodyHeight = TrendRowHeight * bodyRows
    val lockedColumnDescription = stringResource(
        R.string.trend_locked_column_a11y,
        state.tableRows.size,
    )
    Box(
        modifier = modifier
            .background(colors.background)
            .border(1.dp, colors.outlineVariant),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLockedColumn(
                rows = state.tableRows,
                futureIssues = futureIssues,
                lockedWidth = lockedWidth.toPx(),
                groupHeight = TrendGroupHeight.toPx(),
                rowHeight = TrendRowHeight.toPx(),
                gridColor = colors.outlineVariant,
                textColor = colors.onSurface,
                mutedTextColor = colors.onSurfaceVariant,
                primary = colors.primary,
                background = colors.background,
                surfaceVariant = colors.surfaceVariant,
            )
        }
        TrendPeriodSelector(
            selectedWindow = state.window,
            onSetWindow = onSetWindow,
            showLabel = lockedWidth > 108.dp,
            modifier = Modifier
                .offset(y = (-2).dp)
                .fillMaxWidth()
                .height(48.dp),
        )
        Row(
            modifier = Modifier
                .offset(y = TrendGroupHeight)
                .fillMaxWidth()
                .height(TrendRowHeight)
                .background(colors.surfaceVariant.copy(alpha = 0.78f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TableHeaderText(
                text = "期号",
                modifier = Modifier.weight(1f),
            )
        }
        futureIssues.forEachIndexed { index, issue ->
            Box(
                modifier = Modifier
                    .offset(
                        y = TrendGroupHeight +
                            TrendRowHeight * (state.tableRows.size + 1 + index),
                    )
                    .fillMaxWidth()
                    .height(TrendRowHeight)
                    .clearAndSetSemantics {
                        contentDescription = "${issue}期待开奖"
                    },
            )
        }
        summaryLabels.forEachIndexed { index, label ->
            Text(
                text = label,
                modifier = Modifier
                    .offset(
                        y = TrendGroupHeight +
                            TrendRowHeight * (
                                state.tableRows.size + FutureRowCount + 1 + index
                                ),
                    )
                    .fillMaxWidth()
                    .height(TrendRowHeight)
                    .background(colors.surfaceVariant.copy(alpha = 0.72f))
                    .padding(horizontal = 6.dp),
                color = colors.onSurface,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start,
            )
        }
        Box(
            modifier = Modifier
                .offset(y = TrendGroupHeight + TrendRowHeight)
                .fillMaxWidth()
                .height(bodyHeight)
                .clearAndSetSemantics {
                    contentDescription = lockedColumnDescription
                },
        )
    }
}

@Composable
private fun TrendScaleToolbar(
    state: TrendUiState,
    onSetScale: (Float) -> Unit,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onShowAll,
            modifier = Modifier.weight(1f).height(TrendScaleToolbarHeight),
        ) {
            Text("全览", modifier = Modifier.semantics { contentDescription = "走势图全览" })
        }
        TextButton(
            onClick = { onSetScale(1f) },
            modifier = Modifier.weight(1f).height(TrendScaleToolbarHeight),
        ) {
            Text("100%", modifier = Modifier.semantics { contentDescription = "走势图百分之百" }, fontFamily = FontFamily.Monospace)
        }
        TextButton(
            onClick = { onSetScale(state.scale - 0.25f) },
            modifier = Modifier.weight(1f).height(TrendScaleToolbarHeight),
        ) {
            Text("缩小", modifier = Modifier.semantics { contentDescription = "走势图缩小" })
        }
        TextButton(
            onClick = { onSetScale(state.scale + 0.25f) },
            modifier = Modifier.weight(1f).height(TrendScaleToolbarHeight),
        ) {
            Text("放大", modifier = Modifier.semantics { contentDescription = "走势图放大" })
        }
    }
}

@Composable
private fun TrendPeriodSelector(
    selectedWindow: Int,
    onSetWindow: (Int) -> Unit,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val fixedWindows = listOf(10, 30, 60, 100)
    val selectedText = stringResource(R.string.trend_window, selectedWindow)
    val selectorDescription = stringResource(
        R.string.trend_period_selector_a11y,
        selectedWindow,
    )
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .clickable { expanded = true }
            .semantics {
                contentDescription = selectorDescription
                selected = true
            }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showLabel) {
            Text(
                text = stringResource(R.string.trend_title),
                modifier = Modifier.padding(horizontal = 4.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedText,
                        modifier = Modifier.weight(1f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.width(16.dp),
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                fixedWindows.forEach { window ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.trend_window, window)) },
                        onClick = {
                            expanded = false
                            onSetWindow(window)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TableHeaderText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun TrendGroupHeader(
    labels: List<String>,
    attributeLabels: List<String>,
    cellWidth: Dp,
    prefixWidth: Dp,
    attributeWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(TrendGroupHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
    ) {
        PrefixHeaderCell("试机号", prefixWidth)
        PrefixHeaderCell("开奖号", prefixWidth)
        labels.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .width(cellWidth * 10)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f + index * 0.03f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Box(
            modifier = Modifier
                .width(attributeWidth * attributeLabels.size)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

private fun buildTrendAccessibilitySummary(
    base: String,
    state: TrendUiState,
): String = buildString {
    append(base)
    state.tableRows.forEach { row ->
        append("；期号 ${row.issue}，试机号 ${row.trialNumber ?: "—"}，开奖号 ${row.drawNumber}")
        append("，百位 ${row.drawNumber[0]}，十位 ${row.drawNumber[1]}，个位 ${row.drawNumber[2]}")
        append("，和值 ${row.sum}，和尾 ${row.sumTail}，跨度 ${row.span}")
        append("，奇偶比 ${row.oddEvenRatio}，大小比 ${row.bigSmallRatio}，012路个数比 ${row.routeRatio}")
    }
}

@Composable
private fun TrendDigitHeader(
    cellWidth: Dp,
    attributeLabels: List<String>,
    prefixWidth: Dp,
    attributeWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(TrendRowHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
    ) {
        PrefixHeaderCell("", prefixWidth)
        PrefixHeaderCell("", prefixWidth)
        repeat(TrendDigitCount) { column ->
            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f + column / 10 * 0.03f))
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (column % 10).toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        attributeLabels.forEach { label ->
            Box(
                modifier = Modifier
                    .width(attributeWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PrefixHeaderCell(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

private fun DrawScope.drawLockedColumn(
    rows: List<TrendTableRow>,
    futureIssues: List<String>,
    lockedWidth: Float,
    groupHeight: Float,
    rowHeight: Float,
    gridColor: Color,
    textColor: Color,
    mutedTextColor: Color,
    primary: Color,
    background: Color,
    surfaceVariant: Color,
) {
    drawRect(background)
    drawRect(surfaceVariant.copy(alpha = 0.72f), size = androidx.compose.ui.geometry.Size(lockedWidth, groupHeight + rowHeight))
    val bodyStart = groupHeight + rowHeight
    if (rows.isNotEmpty()) {
        drawRect(
            color = primary.copy(alpha = 0.10f),
            topLeft = Offset(0f, bodyStart + (rows.lastIndex * rowHeight)),
            size = androidx.compose.ui.geometry.Size(lockedWidth, rowHeight),
        )
    }
    futureIssues.indices.forEach { index ->
        val y = bodyStart + (rows.size + index) * rowHeight
        drawRect(
            color = primary.copy(alpha = 0.04f),
            topLeft = Offset(0f, y),
            size = androidx.compose.ui.geometry.Size(lockedWidth, rowHeight),
        )
        drawHatch(y, lockedWidth, rowHeight, primary.copy(alpha = 0.10f))
    }
    repeat(SummaryRowCount) { index ->
        val y = bodyStart + (rows.size + FutureRowCount + index) * rowHeight
        drawRect(
            color = surfaceVariant.copy(alpha = 0.72f),
            topLeft = Offset(0f, y),
            size = androidx.compose.ui.geometry.Size(lockedWidth, rowHeight),
        )
    }
    val totalRows = 1 + rows.size + FutureRowCount + SummaryRowCount
    repeat(totalRows + 1) { index ->
        val y = groupHeight + index * rowHeight
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(lockedWidth, y),
            strokeWidth = if (index > 1 && (index - 1) % 5 == 0) 1.5.dp.toPx() else 0.7.dp.toPx(),
        )
    }
    val issuePaint = tablePaint(textColor, 10.dp.toPx(), monospace = true)
    val mutedPaint = tablePaint(mutedTextColor, 10.dp.toPx(), monospace = true)
    rows.forEachIndexed { index, row ->
        val centerY = bodyStart + (index + 0.5f) * rowHeight
        drawCenteredText(row.issue, lockedWidth / 2f, centerY, issuePaint)
    }
    futureIssues.forEachIndexed { index, issue ->
        val centerY = bodyStart + (rows.size + index + 0.5f) * rowHeight
        drawCenteredText(issue, lockedWidth / 2f, centerY, mutedPaint)
    }
}

private fun DrawScope.drawTrendBody(
    state: TrendUiState,
    cellWidth: Float,
    prefixWidth: Float,
    attributeWidth: Float,
    rowHeight: Float,
    gridColor: Color,
    textColor: Color,
    primary: Color,
    background: Color,
    surfaceVariant: Color,
) {
    val rows = state.tableRows
    val bodyRows = rows.size + FutureRowCount + SummaryRowCount
    val digitStart = prefixWidth * 2f
    val attributeStart = digitStart + TrendDigitCount * cellWidth
    drawRect(background)
    repeat(3) { group ->
        drawRect(
            color = primary.copy(alpha = 0.04f + group * 0.03f),
            topLeft = Offset(digitStart + group * 10 * cellWidth, 0f),
            size = androidx.compose.ui.geometry.Size(10 * cellWidth, size.height),
        )
    }
    if (rows.isNotEmpty()) {
        drawRect(
            color = primary.copy(alpha = 0.10f),
            topLeft = Offset(0f, rows.lastIndex * rowHeight),
            size = androidx.compose.ui.geometry.Size(size.width, rowHeight),
        )
    }
    repeat(FutureRowCount) { index ->
        val y = (rows.size + index) * rowHeight
        drawRect(
            color = primary.copy(alpha = 0.04f),
            topLeft = Offset(0f, y),
            size = androidx.compose.ui.geometry.Size(size.width, rowHeight),
        )
        drawHatch(y, size.width, rowHeight, primary.copy(alpha = 0.10f))
    }
    repeat(SummaryRowCount) { index ->
        val y = (rows.size + FutureRowCount + index) * rowHeight
        drawRect(
            color = surfaceVariant.copy(alpha = 0.30f),
            topLeft = Offset(0f, y),
            size = androidx.compose.ui.geometry.Size(size.width, rowHeight),
        )
    }
    repeat(TrendDigitCount + 1) { column ->
        val x = digitStart + column * cellWidth
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = if (column % 10 == 0) 1.5.dp.toPx() else 0.7.dp.toPx(),
        )
    }
    repeat(TrendAttributeLabels.size + 1) { column ->
        val x = attributeStart + column * attributeWidth
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 0.8.dp.toPx(),
        )
    }
    repeat(3) { column ->
        val x = column * prefixWidth
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 0.8.dp.toPx(),
        )
    }
    repeat(bodyRows + 1) { row ->
        val y = row * rowHeight
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = if (row in 1..rows.size && row % 5 == 0) 1.5.dp.toPx() else 0.7.dp.toPx(),
        )
    }
    val omissionPaint = tablePaint(textColor.copy(alpha = 0.76f), 9.dp.toPx(), monospace = true)
    val statPaint = tablePaint(textColor, 9.dp.toPx(), monospace = true, bold = true)
    rows.forEachIndexed { rowIndex, row ->
        repeat(TrendDigitCount) { column ->
            val group = column / 10
            val digit = column % 10
            if (row.drawNumber[group].digitToInt() != digit) {
                drawCenteredText(
                    row.omissions[column].toString(),
                    digitStart + (column + 0.5f) * cellWidth,
                    (rowIndex + 0.5f) * rowHeight,
                    omissionPaint,
                )
            }
        }
        drawCenteredText(
            row.trialNumber ?: "—",
            prefixWidth / 2f,
            (rowIndex + 0.5f) * rowHeight,
            statPaint,
        )
        drawCenteredText(
            row.drawNumber,
            prefixWidth + prefixWidth / 2f,
            (rowIndex + 0.5f) * rowHeight,
            statPaint,
        )
        listOf(row.sum, row.sumTail, row.span, row.oddEvenRatio, row.bigSmallRatio, row.routeRatio)
            .forEachIndexed { attributeIndex, value ->
                drawCenteredText(
                    value,
                    attributeStart + (attributeIndex + 0.5f) * attributeWidth,
                    (rowIndex + 0.5f) * rowHeight,
                    statPaint,
                )
            }
    }
    val futureTrials = state.trialNumbers.associateBy(TrialNumber::issue)
    nextIssues(rows.lastOrNull()?.issue).forEachIndexed { index, issue ->
        drawCenteredText(
            futureTrials[issue]?.number ?: "—",
            prefixWidth / 2f,
            (rows.size + index + 0.5f) * rowHeight,
            statPaint,
        )
    }
    drawTrendLinesAndHits(rows, state.selectedPoint, digitStart, cellWidth, rowHeight, primary)
    val statisticsByPosition = state.statistics.associateBy(TrendPositionStatistics::position)
    repeat(TrendDigitCount) { column ->
        val position = TrendPosition.entries[column / 10]
        val digit = column % 10
        val stat = statisticsByPosition[position]?.digits?.firstOrNull { it.digit == digit }
        val values = listOf(
            stat?.occurrences?.toString() ?: "--",
            stat?.currentOmission?.toString() ?: "--",
            stat?.averageOmission?.let { String.format(Locale.US, "%.2f", it) } ?: "--",
            stat?.maxOmission?.toString() ?: "--",
        )
        values.forEachIndexed { summaryIndex, value ->
            val rowIndex = rows.size + FutureRowCount + summaryIndex
            drawCenteredText(
                value,
                digitStart + (column + 0.5f) * cellWidth,
                (rowIndex + 0.5f) * rowHeight,
                statPaint,
            )
        }
    }
}

private fun DrawScope.drawTrendLinesAndHits(
    rows: List<TrendTableRow>,
    selectedPoint: TrendPoint?,
    digitStart: Float,
    cellWidth: Float,
    rowHeight: Float,
    primary: Color,
) {
    repeat(3) { group ->
        val centers = rows.mapIndexed { rowIndex, row ->
            val digit = row.drawNumber[group].digitToInt()
            Offset(
                x = digitStart + (group * 10 + digit + 0.5f) * cellWidth,
                y = (rowIndex + 0.5f) * rowHeight,
            )
        }
        centers.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = primary.copy(alpha = 0.82f),
                start = start,
                end = end,
                strokeWidth = 1.6.dp.toPx(),
            )
        }
        centers.forEachIndexed { rowIndex, center ->
            val row = rows[rowIndex]
            val digit = row.drawNumber[group].digitToInt()
            val selected = selectedPoint?.issue == row.issue &&
                selectedPoint.position == TrendPosition.entries[group] &&
                selectedPoint.digit == digit
            val radius = 12.dp.toPx()
            drawCircle(primary, radius, center)
            if (selected) {
                drawCircle(
                    color = primary,
                    radius = radius + 3.dp.toPx(),
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()),
                )
            }
            val hitPaint = tablePaint(Color.White, 10.dp.toPx(), monospace = true, bold = true)
            drawCenteredText(digit.toString(), center.x, center.y, hitPaint)
        }
    }
}

private fun DrawScope.drawHatch(
    top: Float,
    width: Float,
    height: Float,
    color: Color,
) {
    val step = 12.dp.toPx()
    var x = -height
    while (x < width) {
        drawLine(
            color = color,
            start = Offset(x, top + height),
            end = Offset(x + height, top),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.cornerPathEffect(1.dp.toPx()),
        )
        x += step
    }
}

private fun DrawScope.drawCenteredText(
    text: String,
    centerX: Float,
    centerY: Float,
    paint: android.graphics.Paint,
) {
    val baseline = centerY - (paint.ascent() + paint.descent()) / 2f
    drawContext.canvas.nativeCanvas.drawText(text, centerX, baseline, paint)
}

private fun tablePaint(
    color: Color,
    textSize: Float,
    monospace: Boolean,
    bold: Boolean = false,
): android.graphics.Paint = android.graphics.Paint().apply {
    isAntiAlias = true
    textAlign = android.graphics.Paint.Align.CENTER
    this.textSize = textSize
    this.color = color.toArgb()
    typeface = when {
        monospace && bold -> android.graphics.Typeface.create(
            android.graphics.Typeface.MONOSPACE,
            android.graphics.Typeface.BOLD,
        )
        monospace -> android.graphics.Typeface.MONOSPACE
        bold -> android.graphics.Typeface.DEFAULT_BOLD
        else -> android.graphics.Typeface.DEFAULT
    }
}

private fun nextIssues(latestIssue: String?): List<String> {
    val latest = latestIssue?.toLongOrNull() ?: return List(FutureRowCount) { "--" }
    return (1..FutureRowCount).map { offset -> (latest + offset).toString() }
}
