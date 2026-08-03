package com.lucky3d.app.feature.trend

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        FlowingCinnabarTrendHeader(
            selectedWindow = state.window,
            onSetWindow = onSetWindow,
        )
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
                accessibilitySummary = stringResource(R.string.trend_long_table_a11y),
                onSetScale = onSetScale,
                onSelectPoint = onSelectPoint,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
@Composable
private fun FlowingCinnabarTrendHeader(
    selectedWindow: Int,
    onSetWindow: (Int) -> Unit,
) {
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
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.trend_title),
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            TrendPeriodSelector(
                selectedWindow = selectedWindow,
                onSetWindow = onSetWindow,
                modifier = Modifier
                    .width(72.dp)
                    .height(48.dp),
            )
        }
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
