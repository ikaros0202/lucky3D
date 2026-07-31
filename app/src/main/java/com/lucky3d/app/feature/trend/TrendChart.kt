package com.lucky3d.app.feature.trend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.floor

@Composable
fun TrendChart(
    state: TrendUiState,
    accessibilitySummary: String,
    issueHeader: String,
    onSelectPoint: (TrendPoint?) -> Unit,
    onScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val horizontalState = rememberScrollState()
    val verticalState = rememberScrollState()
    val colors = MaterialTheme.colorScheme
    val labelWidthDp = 52.dp
    val cellWidthDp = 30.dp * state.scale
    val rowHeightDp = 20.dp
    val headerHeightDp = 22.dp
    val chartWidthDp = labelWidthDp + cellWidthDp * 10
    val chartHeightDp = headerHeightDp + rowHeightDp * state.visibleDraws.size
    val positions = remember(state.visiblePositions) {
        TrendPosition.entries.filter(state.visiblePositions::contains)
    }
    val pointLookup = remember(state.points) {
        state.points.groupBy { it.rowIndex to it.digit }
    }
    val displayedRowCount = state.visibleDraws.size
    LaunchedEffect(state.selectedPoint) {
        if (state.selectedPoint?.rowIndex == state.visibleDraws.lastIndex) {
            verticalState.animateScrollTo(0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .horizontalScroll(horizontalState)
            .verticalScroll(verticalState)
            .semantics { contentDescription = accessibilitySummary },
    ) {
        Canvas(
            modifier = Modifier
                .width(chartWidthDp)
                .height(chartHeightDp)
                .pointerInput(state.scale) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) onScaleChange(state.scale * zoom)
                    }
                }
                .pointerInput(state.points, state.scale) {
                    detectTapGestures { tap ->
                        val labelWidthPx = with(density) { labelWidthDp.toPx() }
                        val headerHeightPx = with(density) { headerHeightDp.toPx() }
                        val cellWidthPx = with(density) { cellWidthDp.toPx() }
                        val rowHeightPx = with(density) { rowHeightDp.toPx() }
                        val digit = floor((tap.x - labelWidthPx) / cellWidthPx).toInt()
                        val displayRow = floor((tap.y - headerHeightPx) / rowHeightPx).toInt()
                        val chronologicalRow = displayedRowCount - 1 - displayRow
                        val point = if (
                            digit in 0..9 &&
                            chronologicalRow in 0 until displayedRowCount
                        ) {
                            pointLookup[chronologicalRow to digit]
                                ?.firstOrNull { it.position in positions }
                        } else {
                            null
                        }
                        onSelectPoint(point)
                    }
                },
        ) {
            val labelWidth = labelWidthDp.toPx()
            val cellWidth = cellWidthDp.toPx()
            val rowHeight = rowHeightDp.toPx()
            val headerHeight = headerHeightDp.toPx()
            drawTrendGrid(
                state = state,
                issueHeader = issueHeader,
                labelWidth = labelWidth,
                cellWidth = cellWidth,
                rowHeight = rowHeight,
                headerHeight = headerHeight,
                gridColor = colors.outlineVariant,
                textColor = colors.onSurfaceVariant,
                primary = colors.primary,
            )
        }
    }
}

private fun DrawScope.drawTrendGrid(
    state: TrendUiState,
    issueHeader: String,
    labelWidth: Float,
    cellWidth: Float,
    rowHeight: Float,
    headerHeight: Float,
    gridColor: Color,
    textColor: Color,
    primary: Color,
) {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = 10.dp.toPx()
        color = textColor.toArgbValue()
    }
    val issuePaint = android.graphics.Paint(paint).apply {
        textAlign = android.graphics.Paint.Align.LEFT
        textSize = 9.dp.toPx()
    }
    for (column in 0..10) {
        val x = labelWidth + column * cellWidth
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.dp.toPx(),
        )
    }
    for (row in 0..state.visibleDraws.size) {
        val y = headerHeight + row * rowHeight
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
        )
    }
    drawContext.canvas.nativeCanvas.apply {
        drawText(issueHeader, 8.dp.toPx(), headerHeight * 0.68f, issuePaint)
        (0..9).forEach { digit ->
            drawText(
                digit.toString(),
                labelWidth + (digit + 0.5f) * cellWidth,
                headerHeight * 0.68f,
                paint,
            )
        }
        state.visibleDraws.asReversed().forEachIndexed { displayRow, draw ->
            drawText(
                draw.issue,
                8.dp.toPx(),
                headerHeight + (displayRow + 0.62f) * rowHeight,
                issuePaint,
            )
        }
    }

    val visiblePoints = state.points.groupBy(TrendPoint::position)
    TrendPosition.entries.filter(state.visiblePositions::contains).forEachIndexed { positionIndex, position ->
        val points = visiblePoints[position].orEmpty().sortedBy(TrendPoint::rowIndex)
        val color = primary
        val centers = points.map { point ->
            val displayRow = state.visibleDraws.lastIndex - point.rowIndex
            val offset = (positionIndex - 1) * 4.dp.toPx()
            Offset(
                x = labelWidth + (point.digit + 0.5f) * cellWidth + offset,
                y = headerHeight + (displayRow + 0.5f) * rowHeight,
            )
        }
        centers.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = color.copy(alpha = 0.7f),
                start = start,
                end = end,
                strokeWidth = when (position) {
                    TrendPosition.HUNDREDS -> 1.6.dp.toPx()
                    TrendPosition.TENS -> 1.2.dp.toPx()
                    TrendPosition.ONES -> 1.dp.toPx()
                },
                pathEffect = when (position) {
                    TrendPosition.HUNDREDS -> null
                    TrendPosition.TENS -> PathEffect.dashPathEffect(
                        floatArrayOf(5.dp.toPx(), 3.dp.toPx()),
                    )
                    TrendPosition.ONES -> PathEffect.dashPathEffect(
                        floatArrayOf(2.dp.toPx(), 3.dp.toPx()),
                    )
                },
            )
        }
        centers.forEachIndexed { index, center ->
            drawMarker(
                center = center,
                position = position,
                digit = points[index].digit,
                color = color,
                selected = points[index] == state.selectedPoint,
            )
        }
    }
}

private fun DrawScope.drawMarker(
    center: Offset,
    position: TrendPosition,
    digit: Int,
    color: Color,
    selected: Boolean,
) {
    val radius = if (selected) 7.dp.toPx() else 5.dp.toPx()
    when (position) {
        TrendPosition.HUNDREDS -> drawCircle(
            color = color,
            radius = radius,
            center = center,
        )
        TrendPosition.TENS -> drawRect(
            color = color,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
        )
        TrendPosition.ONES -> rotate(45f, center) {
            drawRect(
                color = color,
                topLeft = Offset(center.x - radius * 0.8f, center.y - radius * 0.8f),
                size = Size(radius * 1.6f, radius * 1.6f),
            )
        }
    }
    if (selected) {
        drawCircle(
            color = color,
            radius = radius + 3.dp.toPx(),
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
    drawContext.canvas.nativeCanvas.drawText(
        digit.toString(),
        center.x,
        center.y + 3.2.dp.toPx(),
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 9.dp.toPx()
            this.color = android.graphics.Color.WHITE
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        },
    )
}

private fun Color.toArgbValue(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
    )
