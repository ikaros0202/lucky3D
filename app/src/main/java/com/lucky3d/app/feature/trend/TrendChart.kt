package com.lucky3d.app.feature.trend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky3d.app.R
import com.lucky3d.app.core.model.TrialNumber
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

private val TrendCellWidth = 34.dp
private val TrendPrefixWidth = 48.dp
private val TrendRowHeight = 32.dp
private val TrendGroupHeight = 44.dp
private const val TrendDigitCount = 30
private const val TrendSumCount = 28
private val TrendAttributeWidth = 62.dp
private val TrendAttributeLabels = listOf("和尾", "跨度", "奇偶比", "大小比", "012路个数比")
private const val FutureRowCount = 2
private const val SummaryRowCount = 4

internal fun calculateTrendLogicalRightWidth(
    prefixWidth: Float,
    cellWidth: Float,
    attributeWidth: Float,
): Float = prefixWidth * 2f +
    cellWidth * (TrendDigitCount + TrendSumCount) +
    attributeWidth * TrendAttributeLabels.size

internal fun calculateTrendLogicalTableWidth(
    issueWidth: Float,
    prefixWidth: Float,
    cellWidth: Float,
    attributeWidth: Float,
): Float = issueWidth + calculateTrendLogicalRightWidth(
    prefixWidth = prefixWidth,
    cellWidth = cellWidth,
    attributeWidth = attributeWidth,
)

internal fun calculateTrendValueCellCenterX(
    sum: Int,
    prefixWidth: Float,
    cellWidth: Float,
): Float {
    require(sum in 0..27) { "Sum must be between 0 and 27" }
    return prefixWidth * 2f + (TrendDigitCount + sum + 0.5f) * cellWidth
}

internal fun calculateTrendFitScale(
    availableWidthPx: Float,
    logicalTableWidthPx: Float,
): Float {
    require(availableWidthPx > 0f) { "Available trend width must be positive" }
    require(logicalTableWidthPx > 0f) { "Logical trend width must be positive" }
    return minOf(1f, availableWidthPx / logicalTableWidthPx)
}

internal data class TrendViewportMetrics(
    val issueWidth: Float,
    val cellWidth: Float,
    val prefixWidth: Float,
    val attributeWidth: Float,
    val rowHeight: Float,
    val groupHeight: Float,
    val textScale: Float,
)

internal fun calculateTrendViewportMetrics(
    scale: Float,
    baseIssueWidth: Float,
): TrendViewportMetrics = TrendViewportMetrics(
    issueWidth = baseIssueWidth * scale,
    cellWidth = TrendCellWidth.value * scale,
    prefixWidth = TrendPrefixWidth.value * scale,
    attributeWidth = TrendAttributeWidth.value * scale,
    rowHeight = TrendRowHeight.value * scale,
    groupHeight = TrendGroupHeight.value * scale,
    textScale = scale,
)

internal fun calculateTrendScaleAfterZoom(
    currentScale: Float,
    zoomChange: Float,
    fitScale: Float,
): Float = (currentScale * zoomChange).coerceIn(fitScale, 2.5f)

internal data class TrendViewport(
    val scale: Float,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

internal data class TrendViewportBounds(
    val viewportWidth: Float,
    val viewportHeight: Float,
    val baseHeaderHeight: Float,
    val contentWidth: Float,
    val bodyContentHeight: Float,
    val fitScale: Float,
)

internal fun constrainTrendViewport(
    viewport: TrendViewport,
    bounds: TrendViewportBounds,
): TrendViewport {
    val scale = viewport.scale.coerceIn(bounds.fitScale, 2.5f)
    val renderedHeaderHeight = bounds.baseHeaderHeight * scale
    val availableBodyHeight = (bounds.viewportHeight - renderedHeaderHeight).coerceAtLeast(0f)
    val renderedContentWidth = bounds.contentWidth * scale
    val renderedBodyHeight = bounds.bodyContentHeight * scale
    val maxOffsetX = (renderedContentWidth - bounds.viewportWidth).coerceAtLeast(0f)
    val maxOffsetY = (renderedBodyHeight - availableBodyHeight).coerceAtLeast(0f)
    return TrendViewport(
        scale = scale,
        offsetX = viewport.offsetX.coerceIn(0f, maxOffsetX),
        offsetY = viewport.offsetY.coerceIn(0f, maxOffsetY),
    )
}

internal fun transformTrendViewport(
    viewport: TrendViewport,
    zoomChange: Float,
    centroidX: Float,
    centroidY: Float,
    panX: Float,
    panY: Float,
    bounds: TrendViewportBounds,
): TrendViewport {
    val oldScale = viewport.scale.coerceIn(bounds.fitScale, 2.5f)
    val newScale = calculateTrendScaleAfterZoom(
        currentScale = oldScale,
        zoomChange = zoomChange,
        fitScale = bounds.fitScale,
    )
    val currentCentroidX = centroidX + panX
    val currentCentroidY = centroidY + panY
    val oldHeaderHeight = bounds.baseHeaderHeight * oldScale
    val newHeaderHeight = bounds.baseHeaderHeight * newScale
    val logicalX = (viewport.offsetX + centroidX) / oldScale
    val nextOffsetX = logicalX * newScale - currentCentroidX
    val nextOffsetY = if (
        centroidY > oldHeaderHeight && currentCentroidY > newHeaderHeight
    ) {
        val logicalY = (viewport.offsetY + centroidY - oldHeaderHeight) / oldScale
        logicalY * newScale - (currentCentroidY - newHeaderHeight)
    } else {
        viewport.offsetY
    }
    return constrainTrendViewport(
        viewport = TrendViewport(
            scale = newScale,
            offsetX = nextOffsetX,
            offsetY = nextOffsetY,
        ),
        bounds = bounds,
    )
}

internal fun panTrendViewport(
    viewport: TrendViewport,
    panX: Float,
    panY: Float,
    bounds: TrendViewportBounds,
): TrendViewport = constrainTrendViewport(
    viewport = viewport.copy(
        offsetX = viewport.offsetX - panX,
        offsetY = viewport.offsetY - panY,
    ),
    bounds = bounds,
)

@Composable
fun TrendChart(
    state: TrendUiState,
    accessibilitySummary: String,
    onSetScale: (Float) -> Unit = {},
    onSelectPoint: (TrendPoint?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val bodyRowCount = state.tableRows.size + FutureRowCount + SummaryRowCount
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
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val baseIssueWidth = if (maxWidth < 390.dp) 72.dp else 80.dp
            val logicalRightWidth = calculateTrendLogicalRightWidth(
                prefixWidth = TrendPrefixWidth.value,
                cellWidth = TrendCellWidth.value,
                attributeWidth = TrendAttributeWidth.value,
            ).dp
            val logicalTableWidth = calculateTrendLogicalTableWidth(
                issueWidth = baseIssueWidth.value,
                prefixWidth = TrendPrefixWidth.value,
                cellWidth = TrendCellWidth.value,
                attributeWidth = TrendAttributeWidth.value,
            ).dp
            val baseHeaderHeight = TrendGroupHeight + TrendRowHeight
            val baseBodyHeight = TrendRowHeight * bodyRowCount
            val viewportWidthPx = with(density) { maxWidth.toPx() }
            val viewportHeightPx = with(density) { maxHeight.toPx() }
            val contentWidthPx = with(density) { logicalTableWidth.toPx() }
            val baseHeaderHeightPx = with(density) { baseHeaderHeight.toPx() }
            val bodyContentHeightPx = with(density) { baseBodyHeight.toPx() }
            val fitScale = calculateTrendFitScale(
                availableWidthPx = viewportWidthPx,
                logicalTableWidthPx = contentWidthPx,
            )
            val bounds = TrendViewportBounds(
                viewportWidth = viewportWidthPx,
                viewportHeight = viewportHeightPx,
                baseHeaderHeight = baseHeaderHeightPx,
                contentWidth = contentWidthPx,
                bodyContentHeight = bodyContentHeightPx,
                fitScale = fitScale,
            )
            var viewport by remember(
                state.window,
                fitScale,
                viewportWidthPx,
                viewportHeightPx,
                bodyContentHeightPx,
            ) {
                mutableStateOf(
                    constrainTrendViewport(
                        TrendViewport(scale = state.scale),
                        bounds,
                    ),
                )
            }
            val currentOnSetScale by rememberUpdatedState(onSetScale)
            val currentOnSelectPoint by rememberUpdatedState(onSelectPoint)
            val renderedViewport = constrainTrendViewport(viewport, bounds)
            val touchRadiusPx = with(density) { 24.dp.toPx() }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = buildTrendAccessibilitySummary(
                            accessibilitySummary,
                            state,
                            futureIssues,
                            summaryLabels,
                        )
                    }
                    .pointerInput(bounds, state.tableRows) {
                        awaitEachGesture {
                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            var multiTouch = false
                            var moved = false
                            var dragAxis: TrendDragAxis? = null
                            var accumulatedPan = Offset.Zero
                            var lastSinglePosition = firstDown.position
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pressedChanges = event.changes.filter { it.pressed }
                                if (pressedChanges.size >= 2) {
                                    multiTouch = true
                                    moved = true
                                    val centroid = event.calculateCentroid(useCurrent = false)
                                    val pan = event.calculatePan()
                                    viewport = transformTrendViewport(
                                        viewport = viewport,
                                        zoomChange = event.calculateZoom(),
                                        centroidX = centroid.x,
                                        centroidY = centroid.y,
                                        panX = pan.x,
                                        panY = pan.y,
                                        bounds = bounds,
                                    )
                                    event.changes.forEach { change ->
                                        if (change.positionChanged()) change.consume()
                                    }
                                } else if (pressedChanges.size == 1 && !multiTouch) {
                                    val change = pressedChanges.first()
                                    val pan = change.position - lastSinglePosition
                                    lastSinglePosition = change.position
                                    accumulatedPan += pan
                                    if (
                                        dragAxis == null &&
                                        accumulatedPan.getDistance() >= viewConfiguration.touchSlop
                                    ) {
                                        dragAxis = if (
                                            abs(accumulatedPan.y) >= abs(accumulatedPan.x)
                                        ) {
                                            TrendDragAxis.Vertical
                                        } else {
                                            TrendDragAxis.Horizontal
                                        }
                                    }
                                    when (dragAxis) {
                                        TrendDragAxis.Vertical -> {
                                            viewport = panTrendViewport(
                                                viewport = viewport,
                                                panX = 0f,
                                                panY = pan.y,
                                                bounds = bounds,
                                            )
                                            moved = true
                                            change.consume()
                                        }

                                        TrendDragAxis.Horizontal -> {
                                            viewport = panTrendViewport(
                                                viewport = viewport,
                                                panX = pan.x,
                                                panY = 0f,
                                                bounds = bounds,
                                            )
                                            moved = true
                                            change.consume()
                                        }

                                        null -> Unit
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                            if (multiTouch) {
                                currentOnSetScale(viewport.scale)
                            } else if (!moved) {
                                currentOnSelectPoint(
                                    hitTestTrendPoint(
                                        state = state,
                                        tap = firstDown.position,
                                        viewport = viewport,
                                        bounds = bounds,
                                        issueWidthPx = with(density) { baseIssueWidth.toPx() },
                                        cellWidthPx = with(density) { TrendCellWidth.toPx() },
                                        prefixWidthPx = with(density) { TrendPrefixWidth.toPx() },
                                        rowHeightPx = with(density) { TrendRowHeight.toPx() },
                                        touchRadiusPx = touchRadiusPx,
                                    ),
                                )
                            }
                        }
                    },
            ) {
                drawTrendViewport(
                    state = state,
                    viewport = renderedViewport,
                    futureIssues = futureIssues,
                    summaryLabels = summaryLabels,
                    positionLabels = positionLabels,
                    issueWidth = baseIssueWidth.toPx(),
                    cellWidth = TrendCellWidth.toPx(),
                    prefixWidth = TrendPrefixWidth.toPx(),
                    attributeWidth = TrendAttributeWidth.toPx(),
                    rowHeight = TrendRowHeight.toPx(),
                    groupHeight = TrendGroupHeight.toPx(),
                    rightContentWidth = logicalRightWidth.toPx(),
                    bodyContentHeight = baseBodyHeight.toPx(),
                    gridColor = colors.outlineVariant,
                    textColor = colors.onSurfaceVariant,
                    strongTextColor = colors.onSurface,
                    primary = colors.primary,
                    background = colors.background,
                    surfaceVariant = colors.surfaceVariant,
                )
            }
    }
}

private enum class TrendDragAxis { Horizontal, Vertical }

@Composable
internal fun TrendPeriodSelector(
    selectedWindow: Int,
    onSetWindow: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val fixedWindows = listOf(10, 30, 60, 100)
    val selectedText = stringResource(R.string.trend_window, selectedWindow)
    val selectorDescription = stringResource(
        R.string.trend_period_selector_a11y,
        selectedWindow,
    )
    Box(
        modifier = modifier
            .clickable { expanded = true }
            .semantics {
                contentDescription = selectorDescription
                selected = true
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(18.dp),
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.14f),
                contentColor = androidx.compose.ui.graphics.Color.White,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.46f),
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .width(16.dp),
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

private fun hitTestTrendPoint(
    state: TrendUiState,
    tap: Offset,
    viewport: TrendViewport,
    bounds: TrendViewportBounds,
    issueWidthPx: Float,
    cellWidthPx: Float,
    prefixWidthPx: Float,
    rowHeightPx: Float,
    touchRadiusPx: Float,
): TrendPoint? {
    val headerHeight = bounds.baseHeaderHeight * viewport.scale
    if (tap.y <= headerHeight) return null
    val logicalX = (tap.x + viewport.offsetX) / viewport.scale
    val logicalY = (tap.y - headerHeight + viewport.offsetY) / viewport.scale
    val rowIndex = floor(logicalY / rowHeightPx).toInt()
    if (rowIndex !in state.tableRows.indices) return null
    val digitStart = issueWidthPx + prefixWidthPx * 2f
    val groupIndex = floor((logicalX - digitStart) / (cellWidthPx * 10f)).toInt()
    if (groupIndex !in TrendPosition.entries.indices) return null
    val row = state.tableRows[rowIndex]
    val hitDigit = row.drawNumber[groupIndex].digitToInt()
    val hitCenterX = digitStart + (groupIndex * 10 + hitDigit + 0.5f) * cellWidthPx
    val hitCenterY = (rowIndex + 0.5f) * rowHeightPx
    val logicalTouchRadius = touchRadiusPx / viewport.scale
    return if (
        abs(logicalX - hitCenterX) <= logicalTouchRadius &&
        abs(logicalY - hitCenterY) <= logicalTouchRadius
    ) {
        TrendPoint(
            issue = row.issue,
            rowIndex = rowIndex,
            position = TrendPosition.entries[groupIndex],
            digit = hitDigit,
            omission = row.omissions[groupIndex * 10 + hitDigit],
        )
    } else {
        null
    }
}

private fun DrawScope.drawTrendViewport(
    state: TrendUiState,
    viewport: TrendViewport,
    futureIssues: List<String>,
    summaryLabels: List<String>,
    positionLabels: List<String>,
    issueWidth: Float,
    cellWidth: Float,
    prefixWidth: Float,
    attributeWidth: Float,
    rowHeight: Float,
    groupHeight: Float,
    rightContentWidth: Float,
    bodyContentHeight: Float,
    gridColor: Color,
    textColor: Color,
    strongTextColor: Color,
    primary: Color,
    background: Color,
    surfaceVariant: Color,
) {
    val logicalHeaderHeight = groupHeight + rowHeight
    val renderedHeaderHeight = logicalHeaderHeight * viewport.scale
    drawRect(background)

    clipRect(
        left = 0f,
        top = 0f,
        right = size.width,
        bottom = renderedHeaderHeight.coerceAtMost(size.height),
    ) {
        translate(left = -viewport.offsetX) {
            scale(viewport.scale, pivot = Offset.Zero) {
                drawIssueTrendHeader(
                    issueWidth = issueWidth,
                    groupHeight = groupHeight,
                    rowHeight = rowHeight,
                    gridColor = gridColor,
                    textColor = strongTextColor,
                    surfaceVariant = surfaceVariant,
                )
                translate(left = issueWidth) {
                    drawRightTrendHeader(
                        positionLabels = positionLabels,
                        cellWidth = cellWidth,
                        prefixWidth = prefixWidth,
                        attributeWidth = attributeWidth,
                        groupHeight = groupHeight,
                        rowHeight = rowHeight,
                        rightContentWidth = rightContentWidth,
                        gridColor = gridColor,
                        textColor = strongTextColor,
                        primary = primary,
                        surfaceVariant = surfaceVariant,
                    )
                }
            }
        }
    }
    clipRect(
        left = 0f,
        top = renderedHeaderHeight,
        right = size.width,
        bottom = size.height,
    ) {
        translate(
            left = -viewport.offsetX,
            top = renderedHeaderHeight - viewport.offsetY,
        ) {
            scale(viewport.scale, pivot = Offset.Zero) {
                drawIssueTrendBody(
                    rows = state.tableRows,
                    futureIssues = futureIssues,
                    summaryLabels = summaryLabels,
                    issueWidth = issueWidth,
                    bodyHeight = bodyContentHeight,
                    rowHeight = rowHeight,
                    gridColor = gridColor,
                    textColor = strongTextColor,
                    mutedTextColor = textColor,
                    primary = primary,
                    background = background,
                    surfaceVariant = surfaceVariant,
                )
                translate(left = issueWidth) {
                    drawTrendBody(
                        state = state,
                        logicalWidth = rightContentWidth,
                        logicalHeight = bodyContentHeight,
                        cellWidth = cellWidth,
                        prefixWidth = prefixWidth,
                        attributeWidth = attributeWidth,
                        rowHeight = rowHeight,
                        scale = 1f,
                        gridColor = gridColor,
                        textColor = textColor,
                        primary = primary,
                        background = background,
                        surfaceVariant = surfaceVariant,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawIssueTrendHeader(
    issueWidth: Float,
    groupHeight: Float,
    rowHeight: Float,
    gridColor: Color,
    textColor: Color,
    surfaceVariant: Color,
) {
    val headerHeight = groupHeight + rowHeight
    drawRect(surfaceVariant.copy(alpha = 0.78f), size = androidx.compose.ui.geometry.Size(issueWidth, headerHeight))
    drawLine(gridColor, Offset(0f, groupHeight), Offset(issueWidth, groupHeight), 0.8.dp.toPx())
    drawLine(gridColor, Offset(0f, headerHeight), Offset(issueWidth, headerHeight), 0.8.dp.toPx())
    drawLine(gridColor, Offset(issueWidth, 0f), Offset(issueWidth, headerHeight), 1.dp.toPx())
    drawCenteredText(
        "期号",
        issueWidth / 2f,
        groupHeight + rowHeight / 2f,
        tablePaint(textColor, 10.dp.toPx(), monospace = false, bold = true),
    )
}

private fun DrawScope.drawRightTrendHeader(
    positionLabels: List<String>,
    cellWidth: Float,
    prefixWidth: Float,
    attributeWidth: Float,
    groupHeight: Float,
    rowHeight: Float,
    rightContentWidth: Float,
    gridColor: Color,
    textColor: Color,
    primary: Color,
    surfaceVariant: Color,
) {
    val headerHeight = groupHeight + rowHeight
    val digitStart = prefixWidth * 2f
    val sumStart = digitStart + TrendDigitCount * cellWidth
    val attributeStart = sumStart + TrendSumCount * cellWidth
    drawRect(surfaceVariant.copy(alpha = 0.72f), size = androidx.compose.ui.geometry.Size(rightContentWidth, headerHeight))
    repeat(3) { group ->
        drawRect(
            primary.copy(alpha = 0.04f + group * 0.03f),
            topLeft = Offset(digitStart + group * 10 * cellWidth, 0f),
            size = androidx.compose.ui.geometry.Size(10 * cellWidth, headerHeight),
        )
    }
    val groupPaint = tablePaint(textColor, 11.dp.toPx(), monospace = false, bold = true)
    val smallPaint = tablePaint(textColor, 9.dp.toPx(), monospace = false, bold = false)
    drawCenteredText("试机号", prefixWidth / 2f, groupHeight / 2f, smallPaint)
    drawCenteredText("开奖号", prefixWidth * 1.5f, groupHeight / 2f, smallPaint)
    positionLabels.forEachIndexed { index, label ->
        drawCenteredText(
            label,
            digitStart + (index * 10 + 5f) * cellWidth,
            groupHeight / 2f,
            groupPaint,
        )
    }
    repeat(TrendDigitCount) { column ->
        drawCenteredText(
            (column % 10).toString(),
            digitStart + (column + 0.5f) * cellWidth,
            groupHeight + rowHeight / 2f,
            groupPaint,
        )
    }
    drawRect(
        primary.copy(alpha = 0.10f),
        topLeft = Offset(sumStart, 0f),
        size = androidx.compose.ui.geometry.Size(TrendSumCount * cellWidth, groupHeight),
    )
    drawCenteredText(
        "和值",
        sumStart + TrendSumCount * cellWidth / 2f,
        groupHeight / 2f,
        groupPaint,
    )
    repeat(TrendSumCount) { sum ->
        drawCenteredText(
            sum.toString(),
            sumStart + (sum + 0.5f) * cellWidth,
            groupHeight + rowHeight / 2f,
            groupPaint,
        )
    }
    TrendAttributeLabels.forEachIndexed { index, label ->
        drawCenteredText(
            label,
            attributeStart + (index + 0.5f) * attributeWidth,
            groupHeight + rowHeight / 2f,
            smallPaint,
        )
    }
    listOf(0f, prefixWidth, digitStart).forEach { x ->
        drawLine(gridColor, Offset(x, 0f), Offset(x, headerHeight), 0.8.dp.toPx())
    }
    repeat(TrendDigitCount + 1) { column ->
        val x = digitStart + column * cellWidth
        drawLine(gridColor, Offset(x, groupHeight), Offset(x, headerHeight), 0.7.dp.toPx())
    }
    repeat(TrendSumCount + 1) { column ->
        val x = sumStart + column * cellWidth
        drawLine(
            gridColor,
            Offset(x, if (column == 0 || column == TrendSumCount) 0f else groupHeight),
            Offset(x, headerHeight),
            if (column == 0 || column == TrendSumCount) 1.5.dp.toPx() else 0.7.dp.toPx(),
        )
    }
    repeat(TrendAttributeLabels.size + 1) { column ->
        val x = attributeStart + column * attributeWidth
        drawLine(gridColor, Offset(x, groupHeight), Offset(x, headerHeight), 0.8.dp.toPx())
    }
    drawLine(gridColor, Offset(0f, groupHeight), Offset(rightContentWidth, groupHeight), 0.8.dp.toPx())
    drawLine(gridColor, Offset(0f, headerHeight), Offset(rightContentWidth, headerHeight), 1.dp.toPx())
}

private fun DrawScope.drawIssueTrendBody(
    rows: List<TrendTableRow>,
    futureIssues: List<String>,
    summaryLabels: List<String>,
    issueWidth: Float,
    bodyHeight: Float,
    rowHeight: Float,
    gridColor: Color,
    textColor: Color,
    mutedTextColor: Color,
    primary: Color,
    background: Color,
    surfaceVariant: Color,
) {
    drawRect(background, size = androidx.compose.ui.geometry.Size(issueWidth, bodyHeight))
    if (rows.isNotEmpty()) {
        drawRect(
            primary.copy(alpha = 0.10f),
            topLeft = Offset(0f, rows.lastIndex * rowHeight),
            size = androidx.compose.ui.geometry.Size(issueWidth, rowHeight),
        )
    }
    futureIssues.indices.forEach { index ->
        val y = (rows.size + index) * rowHeight
        drawRect(
            primary.copy(alpha = 0.04f),
            topLeft = Offset(0f, y),
            size = androidx.compose.ui.geometry.Size(issueWidth, rowHeight),
        )
        drawHatch(y, issueWidth, rowHeight, primary.copy(alpha = 0.10f), 1f)
    }
    summaryLabels.indices.forEach { index ->
        val y = (rows.size + FutureRowCount + index) * rowHeight
        drawRect(
            surfaceVariant.copy(alpha = 0.72f),
            topLeft = Offset(0f, y),
            size = androidx.compose.ui.geometry.Size(issueWidth, rowHeight),
        )
    }
    repeat(rows.size + FutureRowCount + SummaryRowCount + 1) { index ->
        val y = index * rowHeight
        drawLine(
            gridColor,
            Offset(0f, y),
            Offset(issueWidth, y),
            if (index in 1..rows.size && index % 5 == 0) 1.5.dp.toPx() else 0.7.dp.toPx(),
        )
    }
    drawLine(gridColor, Offset(issueWidth, 0f), Offset(issueWidth, bodyHeight), 1.dp.toPx())
    val issuePaint = tablePaint(textColor, 10.dp.toPx(), monospace = true)
    val mutedPaint = tablePaint(mutedTextColor, 10.dp.toPx(), monospace = true)
    rows.forEachIndexed { index, row ->
        drawCenteredText(row.issue, issueWidth / 2f, (index + 0.5f) * rowHeight, issuePaint)
    }
    futureIssues.forEachIndexed { index, issue ->
        drawCenteredText(
            issue,
            issueWidth / 2f,
            (rows.size + index + 0.5f) * rowHeight,
            mutedPaint,
        )
    }
    summaryLabels.forEachIndexed { index, label ->
        drawCenteredText(
            label,
            issueWidth / 2f,
            (rows.size + FutureRowCount + index + 0.5f) * rowHeight,
            issuePaint,
        )
    }
}

private fun buildTrendAccessibilitySummary(
    base: String,
    state: TrendUiState,
    futureIssues: List<String>,
    summaryLabels: List<String>,
): String = buildString {
    append(base.replace("百位十位个位遗漏走势图", "百位十位个位及和值0至27遗漏走势图"))
    append("；表头：期号、试机号、开奖号、百位、十位、个位、和值0至27连线走势、和尾、跨度、奇偶比、大小比、012路个数比")
    state.tableRows.forEach { row ->
        append("；期号 ${row.issue}，试机号 ${row.trialNumber ?: "—"}，开奖号 ${row.drawNumber}")
        append("，百位 ${row.drawNumber[0]}，十位 ${row.drawNumber[1]}，个位 ${row.drawNumber[2]}")
        append("，和值 ${row.sum}，和尾 ${row.sumTail}，跨度 ${row.span}")
        append("，奇偶比 ${row.oddEvenRatio}，大小比 ${row.bigSmallRatio}，012路个数比 ${row.routeRatio}")
    }
    futureIssues.forEach { issue -> append("；${issue}期待开奖") }
    append("；表尾统计：${summaryLabels.joinToString("、")}")
}

private fun DrawScope.drawTrendBody(
    state: TrendUiState,
    logicalWidth: Float,
    logicalHeight: Float,
    cellWidth: Float,
    prefixWidth: Float,
    attributeWidth: Float,
    rowHeight: Float,
    scale: Float,
    gridColor: Color,
    textColor: Color,
    primary: Color,
    background: Color,
    surfaceVariant: Color,
) {
    val rows = state.tableRows
    val bodyRows = rows.size + FutureRowCount + SummaryRowCount
    val digitStart = prefixWidth * 2f
    val sumStart = digitStart + TrendDigitCount * cellWidth
    val attributeStart = sumStart + TrendSumCount * cellWidth
    drawRect(background, size = androidx.compose.ui.geometry.Size(logicalWidth, logicalHeight))
    repeat(3) { group ->
        drawRect(
            color = primary.copy(alpha = 0.04f + group * 0.03f),
            topLeft = Offset(digitStart + group * 10 * cellWidth, 0f),
            size = androidx.compose.ui.geometry.Size(10 * cellWidth, logicalHeight),
        )
    }
    if (rows.isNotEmpty()) {
        drawRect(
            color = primary.copy(alpha = 0.10f),
            topLeft = Offset(0f, rows.lastIndex * rowHeight),
            size = androidx.compose.ui.geometry.Size(logicalWidth, rowHeight),
        )
    }
    repeat(FutureRowCount) { index ->
        val y = (rows.size + index) * rowHeight
        drawRect(
            color = primary.copy(alpha = 0.04f),
            topLeft = Offset(0f, y),
            size = androidx.compose.ui.geometry.Size(logicalWidth, rowHeight),
        )
        drawHatch(y, logicalWidth, rowHeight, primary.copy(alpha = 0.10f), scale)
    }
    repeat(SummaryRowCount) { index ->
        val y = (rows.size + FutureRowCount + index) * rowHeight
        drawRect(
            color = surfaceVariant.copy(alpha = 0.30f),
            topLeft = Offset(0f, y),
            size = androidx.compose.ui.geometry.Size(logicalWidth, rowHeight),
        )
    }
    repeat(TrendDigitCount + 1) { column ->
        val x = digitStart + column * cellWidth
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, logicalHeight),
            strokeWidth = if (column % 10 == 0) {
                1.5.dp.toPx() * scale
            } else {
                0.7.dp.toPx() * scale
            },
        )
    }
    repeat(TrendSumCount + 1) { column ->
        val x = sumStart + column * cellWidth
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, logicalHeight),
            strokeWidth = if (column == 0 || column == TrendSumCount) {
                1.5.dp.toPx() * scale
            } else {
                0.7.dp.toPx() * scale
            },
        )
    }
    repeat(TrendAttributeLabels.size + 1) { column ->
        val x = attributeStart + column * attributeWidth
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, logicalHeight),
            strokeWidth = 0.8.dp.toPx() * scale,
        )
    }
    repeat(3) { column ->
        val x = column * prefixWidth
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, logicalHeight),
            strokeWidth = 0.8.dp.toPx() * scale,
        )
    }
    repeat(bodyRows + 1) { row ->
        val y = row * rowHeight
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(logicalWidth, y),
            strokeWidth = if (row in 1..rows.size && row % 5 == 0) {
                1.5.dp.toPx() * scale
            } else {
                0.7.dp.toPx() * scale
            },
        )
    }
    val omissionPaint = tablePaint(
        textColor.copy(alpha = 0.76f),
        9.dp.toPx() * scale,
        monospace = true,
    )
    val statPaint = tablePaint(textColor, 9.dp.toPx() * scale, monospace = true, bold = true)
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
        repeat(TrendSumCount) { sum ->
            if (row.sum != sum) {
                row.sumOmissions.getOrNull(sum)?.let { omission ->
                    drawCenteredText(
                        omission.toString(),
                        sumStart + (sum + 0.5f) * cellWidth,
                        (rowIndex + 0.5f) * rowHeight,
                        omissionPaint,
                    )
                }
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
        listOf(row.sumTail, row.span, row.oddEvenRatio, row.bigSmallRatio, row.routeRatio)
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
    drawTrendLinesAndHits(
        rows,
        state.selectedPoint,
        digitStart,
        cellWidth,
        rowHeight,
        primary,
        scale,
    )
    drawSumTrendLineAndHits(
        rows = rows,
        prefixWidth = prefixWidth,
        cellWidth = cellWidth,
        rowHeight = rowHeight,
        primary = primary,
        scale = scale,
    )
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
    state.sumStatistics.forEach { stat ->
        val values = listOf(
            stat.occurrences.toString(),
            stat.currentOmission.toString(),
            stat.averageOmission?.let { String.format(Locale.US, "%.2f", it) } ?: "--",
            stat.maxOmission?.toString() ?: "--",
        )
        values.forEachIndexed { summaryIndex, value ->
            val rowIndex = rows.size + FutureRowCount + summaryIndex
            drawCenteredText(
                value,
                sumStart + (stat.sum + 0.5f) * cellWidth,
                (rowIndex + 0.5f) * rowHeight,
                statPaint,
            )
        }
    }
}

private fun DrawScope.drawSumTrendLineAndHits(
    rows: List<TrendTableRow>,
    prefixWidth: Float,
    cellWidth: Float,
    rowHeight: Float,
    primary: Color,
    scale: Float,
) {
    val centers = rows.mapIndexed { rowIndex, row ->
        Offset(
            x = calculateTrendValueCellCenterX(row.sum, prefixWidth, cellWidth),
            y = (rowIndex + 0.5f) * rowHeight,
        )
    }
    centers.zipWithNext().forEach { (start, end) ->
        drawLine(
            color = primary.copy(alpha = 0.82f),
            start = start,
            end = end,
            strokeWidth = 1.6.dp.toPx() * scale,
        )
    }
    val hitPaint = tablePaint(
        Color.White,
        10.dp.toPx() * scale,
        monospace = true,
        bold = true,
    )
    centers.forEachIndexed { rowIndex, center ->
        drawCircle(primary, 12.dp.toPx() * scale, center)
        drawCenteredText(rows[rowIndex].sum.toString(), center.x, center.y, hitPaint)
    }
}

private fun DrawScope.drawTrendLinesAndHits(
    rows: List<TrendTableRow>,
    selectedPoint: TrendPoint?,
    digitStart: Float,
    cellWidth: Float,
    rowHeight: Float,
    primary: Color,
    scale: Float,
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
                strokeWidth = 1.6.dp.toPx() * scale,
            )
        }
        centers.forEachIndexed { rowIndex, center ->
            val row = rows[rowIndex]
            val digit = row.drawNumber[group].digitToInt()
            val selected = selectedPoint?.issue == row.issue &&
                selectedPoint.position == TrendPosition.entries[group] &&
                selectedPoint.digit == digit
            val radius = 12.dp.toPx() * scale
            drawCircle(primary, radius, center)
            if (selected) {
                drawCircle(
                    color = primary,
                    radius = radius + 3.dp.toPx() * scale,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx() * scale),
                )
            }
            val hitPaint = tablePaint(
                Color.White,
                10.dp.toPx() * scale,
                monospace = true,
                bold = true,
            )
            drawCenteredText(digit.toString(), center.x, center.y, hitPaint)
        }
    }
}

private fun DrawScope.drawHatch(
    top: Float,
    width: Float,
    height: Float,
    color: Color,
    scale: Float,
) {
    val step = 12.dp.toPx() * scale
    var x = -height
    while (x < width) {
        drawLine(
            color = color,
            start = Offset(x, top + height),
            end = Offset(x + height, top),
            strokeWidth = 2.dp.toPx() * scale,
            pathEffect = PathEffect.cornerPathEffect(1.dp.toPx() * scale),
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
