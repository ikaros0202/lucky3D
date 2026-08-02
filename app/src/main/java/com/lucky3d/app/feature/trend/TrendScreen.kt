package com.lucky3d.app.feature.trend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky3d.app.R
import com.lucky3d.app.core.ui.EmptyState
import com.lucky3d.app.core.ui.Lucky3dDesign

@Composable
fun TrendScreen(
    state: TrendUiState,
    onSetWindow: (Int) -> Unit,
    onSetScale: (Float) -> Unit = {},
    onSelectPoint: (TrendPoint?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayedPoint = state.selectedPoint
        ?: state.points
            .filter { it.position == TrendPosition.HUNDREDS }
            .maxByOrNull(TrendPoint::rowIndex)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        item(key = "trend-header") {
            FlowingCinnabarTrendHeader()
        }
        item(key = "trend-hint") {
            TrendGestureHint()
        }
        if (state.visibleDraws.isEmpty()) {
            item(key = "trend-empty") {
                EmptyState(
                    title = stringResource(R.string.trend_no_data_title),
                    detail = stringResource(R.string.trend_no_data_detail),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                )
            }
        } else {
            item(key = "trend-column-order") {
                Text(
                    text = "试机号　开奖号　百位　十位　个位　和值　和尾　跨度　奇偶比　大小比　012路个数比",
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .clearAndSetSemantics {
                            contentDescription = "走势图表头顺序：试机号、开奖号、百位、十位、个位、和值、和尾、跨度、奇偶比、大小比、012路个数比"
                        },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
            item(key = "trend-table") {
                TrendChart(
                    state = state,
                    accessibilitySummary = stringResource(R.string.trend_long_table_a11y),
                    onSetWindow = onSetWindow,
                    onSetScale = onSetScale,
                    onSelectPoint = onSelectPoint,
                )
            }
            item(key = "trend-detail") {
                TrendPointSummary(
                    point = displayedPoint,
                    drawNumber = displayedPoint?.issue?.let { issue ->
                        state.visibleDraws.firstOrNull { it.issue == issue }?.number?.value
                    },
                )
            }
        }
        item(key = "trend-bottom-space") {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FlowingCinnabarTrendHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
    ) {
        CinnabarFlowBackdrop(modifier = Modifier.fillMaxSize())
        Text(
            text = stringResource(R.string.home_title),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Text(
            text = stringResource(R.string.trend_title),
            modifier = Modifier.align(Alignment.Center),
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.trend_local_data),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
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
            val wave = Path().apply {
                moveTo(size.width * 0.22f, size.height * (0.72f + index * 0.02f))
                cubicTo(
                    size.width * 0.45f,
                    size.height * (0.48f + index * 0.018f),
                    size.width * 0.73f,
                    size.height * (0.92f - index * 0.025f),
                    size.width * 1.10f,
                    size.height * (0.57f + index * 0.015f),
                )
            }
            drawPath(
                path = wave,
                color = androidx.compose.ui.graphics.Color.White.copy(
                    alpha = 0.20f - index * 0.022f,
                ),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        drawLine(
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f),
            start = Offset(size.width * 0.22f, size.height - 3.dp.toPx()),
            end = Offset(size.width * 1.05f, size.height * 0.64f),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

@Composable
private fun TrendGestureHint() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.SwapHoriz,
                contentDescription = null,
                modifier = Modifier.width(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.trend_long_scroll_hint),
                modifier = Modifier.padding(start = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TrendPointSummary(
    point: TrendPoint?,
    drawNumber: String?,
) {
    val description = point?.let {
        stringResource(
            R.string.trend_point_summary_a11y,
            it.issue,
            positionLabel(it.position),
            it.digit,
            it.omission,
        )
    } ?: stringResource(R.string.trend_point_summary_empty)
    val visibleText = point?.let {
        stringResource(
            R.string.trend_selected_detail,
            it.issue,
            drawNumber ?: "---",
            positionLabel(it.position),
            it.digit,
            it.omission,
        )
    } ?: stringResource(R.string.trend_point_summary_empty)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clearAndSetSemantics { contentDescription = description },
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = visibleText,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun positionLabel(position: TrendPosition): String = when (position) {
    TrendPosition.HUNDREDS -> stringResource(R.string.trend_position_hundreds)
    TrendPosition.TENS -> stringResource(R.string.trend_position_tens)
    TrendPosition.ONES -> stringResource(R.string.trend_position_ones)
}
