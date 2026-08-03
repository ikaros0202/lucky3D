package com.lucky3d.app.feature.home

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.lucky3d.app.R
import com.lucky3d.app.core.ui.Lucky3dDesign
import com.lucky3d.app.domain.attributes.GroupShape
import com.lucky3d.app.domain.omission.DigitPosition
import com.lucky3d.app.domain.omission.HeatLevel

private const val DesignWidth = 1080f
private const val DesignContentHeight = 1598f
private const val TallDesignContentHeight = 2078f
private val ApprovedCrystalRed = Color(0xFFC90B2F)
private val ApprovedCrystalInk = Color(0xFF241F23)
private val ApprovedCrystalBlue = Color(0xFF155CAA)
private val ApprovedCrystalMuted = Color(0xFF6B6268)
private val LocalDesignCanvasHeight = staticCompositionLocalOf<Dp> {
    error("Approved crystal Home requires a measured canvas height")
}
private val LocalUseTallDesign = staticCompositionLocalOf { false }

@Composable
internal fun ApprovedCrystalHome(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onRefreshTrial: () -> Unit,
    onOpenIssue: (String) -> Unit,
    onOpenDate: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latest = state.latest ?: return
    val attributes = state.insights.attributes ?: return
    val colors = Lucky3dDesign.colors
    val context = LocalContext.current
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousLightStatusBars = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = true
        onDispose {
            if (previousLightStatusBars != null) {
                controller.isAppearanceLightStatusBars = previousLightStatusBars
            }
        }
    }
    val number = latest.number.value
    val issueText = stringResource(R.string.home_issue, latest.issue)
    val issueDescription = stringResource(R.string.home_query_issue, latest.issue)
    val dateDescription = stringResource(R.string.home_query_date, latest.drawDate)
    val stageDescription = stringResource(R.string.home_crystal_stage_a11y)
    val drawDescription = stringResource(
        R.string.draw_number_digits_a11y,
        number[0],
        number[1],
        number[2],
    )
    val groupText = approvedGroupShapeLabel(attributes.groupShape)
    val consecutiveText = consecutiveDigitsLabel(number)
    val coldHitCount = state.insights.coldHits.size.toString()
    val syncText = when (state.syncState) {
        HomeSyncState.LOCAL -> stringResource(R.string.sync_local)
        HomeSyncState.UP_TO_DATE -> stringResource(R.string.sync_updated)
        HomeSyncState.UPDATING -> stringResource(R.string.sync_updating)
        HomeSyncState.ERROR -> stringResource(R.string.sync_failed)
        HomeSyncState.CORRECTED -> stringResource(R.string.sync_corrected)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_live_data_layer"),
    ) {
        val canvasWidth = maxWidth
        val useTallArtwork = maxHeight.value / maxWidth.value > 1.7f
        CompositionLocalProvider(
            LocalDesignCanvasHeight provides maxHeight,
            LocalUseTallDesign provides useTallArtwork,
        ) {
            Image(
                painter = painterResource(
                    if (useTallArtwork) {
                        R.drawable.home_crystal_content_shell_tall
                    } else {
                        R.drawable.home_crystal_content_shell
                    },
                ),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_crystal_artwork"),
                contentScale = ContentScale.FillBounds,
            )

        HiddenDesignText(
            text = stringResource(R.string.home_title),
            canvasWidth = canvasWidth,
            x = 56f,
            y = 86f,
            width = 270f,
            height = 70f,
        )
        HiddenDesignText(
            text = stringResource(R.string.home_crystal_subtitle),
            canvasWidth = canvasWidth,
            x = 56f,
            y = 150f,
            width = 240f,
            height = 48f,
        )

        DesignAction(
            canvasWidth = canvasWidth,
            x = 455f,
            y = 72f,
            width = 320f,
            height = 112f,
            description = issueDescription,
            onClick = { onOpenIssue(latest.issue) },
        ) {}
        DesignValue(
            text = issueText,
            canvasWidth = canvasWidth,
            centerX = 640f,
            centerY = 126f,
            width = 190f,
            height = 54f,
            fontSize = 12,
            color = colors.primaryDeep,
        )
        DesignAction(
            canvasWidth = canvasWidth,
            x = 790f,
            y = 72f,
            width = 270f,
            height = 112f,
            description = dateDescription,
            onClick = { onOpenDate(latest.drawDate) },
        ) {}
        DesignValue(
            text = latest.drawDate,
            canvasWidth = canvasWidth,
            centerX = 950f,
            centerY = 126f,
            width = 180f,
            height = 54f,
            fontSize = 10,
            color = colors.primaryDeep,
        )

        Box(
            modifier = Modifier
                .designFrame(canvasWidth, 105f, 218f, 850f, 286f)
                .clearAndSetSemantics { contentDescription = drawDescription },
        )
        Box(
            modifier = Modifier
                .designFrame(canvasWidth, 145f, 430f, 790f, 175f)
                .clearAndSetSemantics {
                    contentDescription = stageDescription
                },
        )
        listOf(260f, 540f, 820f).forEachIndexed { index, centerX ->
            DesignValue(
                text = number[index].toString(),
                canvasWidth = canvasWidth,
                centerX = centerX,
                centerY = 358f,
                width = 180f,
                height = 210f,
                fontSize = 48,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }

        DesignSemanticValue(
            label = stringResource(R.string.home_sum_label),
            value = attributes.sum.toString(),
            canvasWidth = canvasWidth,
            centerX = 180f,
            centerY = 710f,
            width = 190f,
            height = 90f,
            fontSize = 21,
            color = ApprovedCrystalRed,
        )
        DesignSemanticValue(
            label = stringResource(R.string.home_sum_tail_label),
            value = attributes.sumTail.toString(),
            canvasWidth = canvasWidth,
            centerX = 410f,
            centerY = 710f,
            width = 190f,
            height = 90f,
            fontSize = 21,
            color = ApprovedCrystalRed,
        )
        DesignSemanticValue(
            label = stringResource(R.string.home_span_label),
            value = attributes.span.toString(),
            canvasWidth = canvasWidth,
            centerX = 650f,
            centerY = 710f,
            width = 190f,
            height = 90f,
            fontSize = 21,
            color = ApprovedCrystalRed,
        )
        DesignSemanticValue(
            label = stringResource(R.string.home_group_label),
            value = groupText,
            canvasWidth = canvasWidth,
            centerX = 900f,
            centerY = 710f,
            width = 220f,
            height = 90f,
            fontSize = 20,
            color = ApprovedCrystalRed,
        )

        DesignSemanticValue(
            label = stringResource(R.string.home_parity_label),
            value = "${attributes.oddCount}:${attributes.evenCount}",
            canvasWidth = canvasWidth,
            centerX = 165f,
            centerY = 885f,
            width = 200f,
            height = 78f,
            fontSize = 17,
        )
        DesignSemanticValue(
            label = stringResource(R.string.home_size_label),
            value = "${attributes.bigCount}:${attributes.smallCount}",
            canvasWidth = canvasWidth,
            centerX = 410f,
            centerY = 885f,
            width = 200f,
            height = 78f,
            fontSize = 17,
        )
        DesignSemanticValue(
            label = stringResource(R.string.home_quality_label),
            value = "${attributes.primeLikeCount}:${attributes.compositeLikeCount}",
            canvasWidth = canvasWidth,
            centerX = 655f,
            centerY = 885f,
            width = 200f,
            height = 78f,
            fontSize = 17,
        )
        DesignSemanticValue(
            label = stringResource(R.string.home_route_label),
            value = attributes.routeCounts.joinToString(":"),
            canvasWidth = canvasWidth,
            centerX = 905f,
            centerY = 885f,
            width = 220f,
            height = 78f,
            fontSize = 17,
        )

        DesignSemanticValue(
            label = stringResource(R.string.home_consecutive_label),
            value = consecutiveText,
            canvasWidth = canvasWidth,
            centerX = 550f,
            centerY = 1000f,
            width = 250f,
            height = 70f,
            fontSize = 18,
            color = ApprovedCrystalRed,
        )
        DesignSemanticValue(
            label = stringResource(R.string.home_cold_hits),
            value = coldHitCount,
            canvasWidth = canvasWidth,
            centerX = 635f,
            centerY = 1090f,
            width = 90f,
            height = 68f,
            fontSize = 18,
            color = ApprovedCrystalBlue,
        )
        DesignValue(
            text = "❄︎",
            canvasWidth = canvasWidth,
            centerX = 418f,
            centerY = 1090f,
            width = 56f,
            height = 68f,
            fontSize = 18,
            color = ApprovedCrystalBlue,
            description = stringResource(R.string.home_cold_hits),
        )
        if (useTallArtwork) {
            DesignValue(
                text = stringResource(R.string.home_cold_hits),
                canvasWidth = canvasWidth,
                centerX = 515f,
                centerY = 1090f,
                width = 180f,
                height = 68f,
                fontSize = 15,
                fontWeight = FontWeight.Medium,
            )
        }

        val positions = state.insights.positions.associateBy(HomePositionInsight::position)
        listOf(
            DigitPosition.HUNDREDS to 225f,
            DigitPosition.TENS to 542f,
            DigitPosition.ONES to 860f,
        ).forEach { (position, centerX) ->
            val insight = positions[position] ?: return@forEach
            val positionText = approvedPositionLabel(position)
            val heatText = approvedHeatLabel(insight.heatLevel)
            val description = stringResource(
                R.string.home_position_dial_a11y,
                positionText,
                insight.digit,
                insight.previousOmission,
                heatText,
                insight.windowSize,
            )
            Box(
                modifier = Modifier
                    .designFrame(canvasWidth, centerX - 125f, 1145f, 250f, 250f)
                    .clearAndSetSemantics { contentDescription = description },
            )
            DesignValue(
                text = insight.digit.toString(),
                canvasWidth = canvasWidth,
                centerX = centerX,
                centerY = 1268f,
                width = 120f,
                height = 100f,
                fontSize = 23,
                color = ApprovedCrystalRed,
                fontWeight = FontWeight.Bold,
            )
            DesignValue(
                text = stringResource(R.string.home_omission_short, insight.previousOmission),
                canvasWidth = canvasWidth,
                centerX = centerX,
                centerY = 1330f,
                width = 180f,
                height = 54f,
                fontSize = 11,
                fontWeight = FontWeight.Medium,
            )
        }

        ApprovedHeatMarker(
            positions = state.insights.positions,
            canvasWidth = canvasWidth,
        )

        HiddenDesignText(
            text = stringResource(R.string.home_trial_title),
            canvasWidth = canvasWidth,
            x = 90f,
            y = 1480f,
            width = 145f,
            height = 90f,
        )
        DesignAction(
            canvasWidth = canvasWidth,
            x = 35f,
            y = 1460f,
            width = 390f,
            height = 125f,
            description = stringResource(R.string.home_trial_refresh),
            onClick = onRefreshTrial,
        ) {}
        val trialText = state.trialNumber?.number ?: "---"
        DesignValue(
            text = trialText,
            canvasWidth = canvasWidth,
            centerX = 300f,
            centerY = 1523f,
            width = 170f,
            height = 70f,
            fontSize = if (state.trialNumber != null) 21 else 12,
            color = if (state.trialNumber != null) {
                ApprovedCrystalRed
            } else {
                ApprovedCrystalInk
            },
            fontWeight = if (state.trialNumber != null) FontWeight.Bold else FontWeight.Medium,
            description = state.trialNumber?.number ?: trialText,
        )
        DesignAction(
            canvasWidth = canvasWidth,
            x = 425f,
            y = 1460f,
            width = 300f,
            height = 125f,
            description = stringResource(R.string.home_refresh),
            enabled = state.syncState != HomeSyncState.UPDATING,
            onClick = onRefresh,
        ) {}
        DesignValue(
            text = syncText,
            canvasWidth = canvasWidth,
            centerX = 535f,
            centerY = 1523f,
            width = 170f,
            height = 56f,
            fontSize = 12,
            color = if (state.syncState == HomeSyncState.ERROR) {
                ApprovedCrystalRed
            } else {
                ApprovedCrystalMuted
            },
        )
        DesignAction(
            canvasWidth = canvasWidth,
            x = 725f,
            y = 1460f,
            width = 330f,
            height = 125f,
            description = stringResource(R.string.home_settings),
            onClick = onOpenSettings,
        ) {}
        }
    }
}

@Composable
private fun DesignAction(
    canvasWidth: Dp,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .designFrame(canvasWidth, x, y, width, height)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun DesignSemanticValue(
    label: String,
    value: String,
    canvasWidth: Dp,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    fontSize: Int,
    color: Color = ApprovedCrystalInk,
) {
    DesignValue(
        text = value,
        canvasWidth = canvasWidth,
        centerX = centerX,
        centerY = centerY,
        width = width,
        height = height,
        fontSize = fontSize,
        color = color,
        fontWeight = FontWeight.Bold,
        description = "$label $value",
    )
}

@Composable
private fun DesignValue(
    text: String,
    canvasWidth: Dp,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    fontSize: Int,
    color: Color = ApprovedCrystalInk,
    fontWeight: FontWeight = FontWeight.Medium,
    description: String? = null,
) {
    val fontScale = LocalDensity.current.fontScale
    val opticalFontSize = fontSize / fontScale
    Box(
        modifier = Modifier
            .designFrame(
                canvasWidth = canvasWidth,
                x = centerX - width / 2f,
                y = centerY - height / 2f,
                width = width,
                height = height,
            )
            .then(
                if (description == null) {
                    Modifier
                } else {
                    Modifier.semantics { contentDescription = description }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = fontWeight,
            fontSize = opticalFontSize.sp,
            lineHeight = ((fontSize + 3) / fontScale).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun ApprovedHeatMarker(
    positions: List<HomePositionInsight>,
    canvasWidth: Dp,
) {
    val score = positions
        .mapNotNull { position ->
            when (position.heatLevel) {
                HeatLevel.COLD -> 0.15f
                HeatLevel.WARM -> 0.50f
                HeatLevel.HOT -> 0.85f
                null -> null
            }
        }
        .average()
        .takeUnless(Double::isNaN)
        ?.toFloat()
        ?: 0.5f
    val centerX = 150f + 785f * score
    val description = stringResource(R.string.home_heat_scale_a11y)
    Canvas(
        modifier = Modifier
            .designFrame(canvasWidth, centerX - 24f, 1394f, 48f, 52f)
            .semantics { contentDescription = description },
    ) {
        drawCircle(
            color = Color.White,
            radius = size.minDimension * 0.38f,
            center = center,
        )
        drawCircle(
            color = ApprovedCrystalRed,
            radius = size.minDimension * 0.33f,
            center = center,
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

@Composable
private fun HiddenDesignText(
    text: String,
    canvasWidth: Dp,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
) {
    Text(
        text = text,
        modifier = Modifier
            .designFrame(canvasWidth, x, y, width, height)
            .alpha(0f),
        fontSize = 1.sp,
        maxLines = 1,
    )
}

@Composable
private fun Modifier.designFrame(
    canvasWidth: Dp,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
): Modifier {
    val canvasHeight = LocalDesignCanvasHeight.current
    val useTallDesign = LocalUseTallDesign.current
    val designHeight = if (useTallDesign) TallDesignContentHeight else DesignContentHeight
    val mappedTop = if (useTallDesign) mapTallDesignY(y) else y
    val mappedBottom = if (useTallDesign) mapTallDesignY(y + height) else y + height
    return this
        .offset(
            x = canvasWidth * (x / DesignWidth),
            y = canvasHeight * (mappedTop / designHeight),
        )
        .size(
            width = canvasWidth * (width / DesignWidth),
            height = canvasHeight * ((mappedBottom - mappedTop) / designHeight),
        )
}

private val TallDesignYAnchors = listOf(
    0f to 0f,
    126f to 139f,
    358f to 386f,
    600f to 660f,
    710f to 812f,
    790f to 910f,
    885f to 1021f,
    950f to 1100f,
    1000f to 1182f,
    1090f to 1284f,
    1145f to 1373f,
    1268f to 1517f,
    1330f to 1588f,
    1394f to 1685f,
    1446f to 1745f,
    1460f to 1810f,
    1523f to 1910f,
    1585f to 2006f,
    1598f to 2078f,
)

private fun mapTallDesignY(sourceY: Float): Float {
    val clamped = sourceY.coerceIn(0f, DesignContentHeight)
    val upperIndex = TallDesignYAnchors.indexOfFirst { (source, _) -> source >= clamped }
        .takeIf { it >= 0 }
        ?: TallDesignYAnchors.lastIndex
    if (upperIndex == 0) return TallDesignYAnchors.first().second
    val (lowerSource, lowerTarget) = TallDesignYAnchors[upperIndex - 1]
    val (upperSource, upperTarget) = TallDesignYAnchors[upperIndex]
    val fraction = (clamped - lowerSource) / (upperSource - lowerSource)
    return lowerTarget + (upperTarget - lowerTarget) * fraction
}

@Composable
private fun approvedGroupShapeLabel(shape: GroupShape): String = when (shape) {
    GroupShape.LEOPARD -> stringResource(R.string.group_leopard)
    GroupShape.GROUP3 -> stringResource(R.string.group_3)
    GroupShape.GROUP6 -> stringResource(R.string.group_6)
}

@Composable
private fun approvedPositionLabel(position: DigitPosition): String = when (position) {
    DigitPosition.HUNDREDS -> stringResource(R.string.home_position_hundreds)
    DigitPosition.TENS -> stringResource(R.string.home_position_tens)
    DigitPosition.ONES -> stringResource(R.string.home_position_ones)
    DigitPosition.ALL -> error("Home requires a concrete digit position")
}

@Composable
private fun approvedHeatLabel(level: HeatLevel?): String = when (level) {
    HeatLevel.COLD -> stringResource(R.string.home_heat_cold)
    HeatLevel.WARM -> stringResource(R.string.home_heat_warm)
    HeatLevel.HOT -> stringResource(R.string.home_heat_hot)
    null -> stringResource(R.string.home_heat_unknown)
}
