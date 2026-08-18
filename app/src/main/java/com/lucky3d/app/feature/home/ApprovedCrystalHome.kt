package com.lucky3d.app.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky3d.app.R
import com.lucky3d.app.core.model.YunnanAnnouncement
import com.lucky3d.app.core.model.YunnanPlayAnnouncement
import com.lucky3d.app.core.model.YunnanPlayType
import com.lucky3d.app.domain.attributes.GroupShape
import com.lucky3d.app.domain.omission.DigitPosition
import com.lucky3d.app.domain.omission.HeatLevel
import java.util.Locale

private const val DesignWidth = 1080f
private const val CrystalTypographyScale = 1.65f
private const val DesignContentHeight = 2078f
private val CrystalRed = Color(0xFFC90B2F)
private val CrystalDeepRed = Color(0xFF8F102B)
private val CrystalInk = Color(0xFF271F23)
private val CrystalMuted = Color(0xFF6C6066)
private val CrystalBlue = Color(0xFF125AB6)
private val CrystalHot = Color(0xFFE51B2C)
private val LocalDesignCanvasHeight = staticCompositionLocalOf<Dp> {
    error("Approved crystal home requires a measured canvas height")
}

/**
 * Runtime overlay for the approved, text-free crystal shell. Business values
 * stay live and are measured together with their background slots.
 */
@Composable
internal fun ApprovedCrystalHome(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onRefreshTrial: () -> Unit,
    onOpenIssue: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latest = state.latest ?: return
    val attributes = state.insights.attributes ?: return
    val number = latest.number.value
    val announcement = state.yunnanAnnouncement?.takeIf { it.issue == latest.issue }
    val positions = state.insights.positions.associateBy(HomePositionInsight::position)
    val hasSyncError = state.syncState == HomeSyncState.ERROR || state.announcementRefreshFailed
    val syncText = when {
        state.syncState == HomeSyncState.UPDATING -> stringResource(R.string.sync_updating)
        hasSyncError -> stringResource(R.string.sync_failed)
        state.syncState == HomeSyncState.LOCAL -> stringResource(R.string.sync_local)
        state.syncState == HomeSyncState.UP_TO_DATE -> stringResource(R.string.sync_updated)
        state.syncState == HomeSyncState.CORRECTED -> stringResource(R.string.sync_corrected)
        else -> stringResource(R.string.sync_updated)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().testTag("home_live_data_layer"),
    ) {
        val canvasWidth = maxWidth
        androidx.compose.runtime.CompositionLocalProvider(LocalDesignCanvasHeight provides maxHeight) {
            Image(
                painter = painterResource(R.drawable.home_crystal_content_shell_redesign),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().testTag("home_crystal_artwork"),
                contentScale = ContentScale.FillBounds,
            )

            CrystalText(
                text = stringResource(R.string.home_title),
                canvasWidth = canvasWidth,
                centerX = 270f,
                centerY = 110f,
                width = 300f,
                height = 60f,
                fontSize = 32,
                color = Color(0xFFE31522),
                fontWeight = FontWeight.ExtraBold,
            )
            CrystalText(
                text = stringResource(R.string.home_crystal_subtitle),
                canvasWidth = canvasWidth,
                centerX = 260f,
                centerY = 160f,
                width = 290f,
                height = 42f,
                fontSize = 16,
                color = Color(0xFFE31522),
                fontWeight = FontWeight.SemiBold,
            )

            DesignAction(
                canvasWidth = canvasWidth,
                x = 465f,
                y = 76f,
                width = 560f,
                height = 122f,
                description = stringResource(
                    R.string.home_issue_entry_a11y,
                    latest.issue,
                    latest.drawDate,
                ),
                onClick = { onOpenIssue(latest.issue) },
            )
            CrystalText(
                text = stringResource(R.string.home_issue, latest.issue),
                canvasWidth = canvasWidth,
                centerX = 685f,
                centerY = 116f,
                width = 390f,
                height = 46f,
                fontSize = 21,
                color = CrystalDeepRed,
                fontWeight = FontWeight.Bold,
                align = TextAlign.Start,
            )
            CrystalText(
                text = latest.drawDate,
                canvasWidth = canvasWidth,
                centerX = 685f,
                centerY = 163f,
                width = 390f,
                height = 34f,
                fontSize = 13,
                color = CrystalMuted,
                align = TextAlign.Start,
            )
            DropdownChevron(canvasWidth)

    val drawDescription = stringResource(
        R.string.draw_number_digits_a11y,
        number[0],
        number[1],
        number[2],
    )
    Box(
                modifier = Modifier
                    .designFrame(canvasWidth, 205f, 270f, 670f, 205f)
                    .clearAndSetSemantics {
                        contentDescription = drawDescription
                    },
            )
            listOf(317f, 540f, 763f).forEachIndexed { index, x ->
                CrystalText(
                    text = number[index].toString(),
                    canvasWidth = canvasWidth,
                    centerX = x,
                    centerY = 373f,
                    width = 150f,
                    height = 150f,
                    fontSize = 54,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }

            val metricXs = listOf(215f, 431f, 647f, 863f)
            val metricLabels = listOf(
                stringResource(R.string.home_sum_label),
                stringResource(R.string.home_sum_tail_label),
                stringResource(R.string.home_span_label),
                stringResource(R.string.home_group_label),
            )
            val metricValues = listOf(
                attributes.sum.toString(),
                attributes.sumTail.toString(),
                attributes.span.toString(),
                approvedGroupShapeLabel(attributes.groupShape),
            )
            metricXs.forEachIndexed { index, x ->
                MetricPair(
                    label = metricLabels[index],
                    value = metricValues[index],
                    canvasWidth = canvasWidth,
                    centerX = x,
                    labelY = 596f,
                    valueY = 659f,
                    prominent = true,
                )
            }
            val ratioLabels = listOf(
                stringResource(R.string.home_parity_label),
                stringResource(R.string.home_size_label),
                stringResource(R.string.home_quality_label),
                stringResource(R.string.home_route_label),
            )
            val ratioValues = listOf(
                "${attributes.oddCount}:${attributes.evenCount}",
                "${attributes.bigCount}:${attributes.smallCount}",
                "${attributes.primeLikeCount}:${attributes.compositeLikeCount}",
                attributes.routeCounts.joinToString(":"),
            )
            metricXs.forEachIndexed { index, x ->
                MetricPair(
                    label = ratioLabels[index],
                    value = ratioValues[index],
                    canvasWidth = canvasWidth,
                    centerX = x,
                    labelY = 773f,
                    valueY = 827f,
                    prominent = false,
                )
            }

            YunnanAnnouncementOverlay(
                canvasWidth = canvasWidth,
                issue = latest.issue,
                announcement = announcement,
            )

            listOf(
                DigitPosition.HUNDREDS to 233f,
                DigitPosition.TENS to 540f,
                DigitPosition.ONES to 847f,
            ).forEach { (position, x) ->
                val insight = positions[position] ?: return@forEach
                val label = approvedPositionLabel(position)
                val positionDescription = stringResource(
                    R.string.home_position_dial_a11y,
                    label,
                    insight.digit,
                    insight.previousOmission,
                    approvedHeatLabel(insight.heatLevel),
                    insight.windowSize,
                )
                Box(
                    modifier = Modifier
                        .designFrame(canvasWidth, x - 90f, 1540f, 180f, 174f)
                        .clearAndSetSemantics {
                            contentDescription = positionDescription
                        },
                )
                CrystalText(label, canvasWidth, x, 1588f, 170f, 36f, 13)
                CrystalText(
                    insight.digit.toString(), canvasWidth, x, 1640f, 150f, 54f, 23,
                    color = CrystalRed, fontWeight = FontWeight.Bold,
                )
                CrystalText(
                    stringResource(R.string.home_omission_short, insight.previousOmission),
                    canvasWidth, x, 1687f, 170f, 34f, 11, color = CrystalMuted,
                )
            }
            HeatOverlay(state.insights.positions, canvasWidth)

            CrystalText(
                stringResource(R.string.home_trial_title), canvasWidth,
                215f, 1889f, 220f, 42f, 15,
                color = CrystalMuted, fontWeight = FontWeight.SemiBold,
            )
            val trialText = state.trialNumber?.number
                ?: if (state.trialManualRefreshFailed) stringResource(R.string.home_trial_failed) else "---"
            CrystalText(
                trialText, canvasWidth, 215f, 1942f, 220f, 58f,
                if (state.trialNumber != null) 31 else 24,
                color = if (state.trialNumber != null || state.trialManualRefreshFailed) {
                    CrystalRed
                } else {
                    CrystalMuted
                },
                fontWeight = if (state.trialNumber != null) FontWeight.ExtraBold else FontWeight.SemiBold,
                description = trialText,
                tag = "home_trial_number",
            )
            DesignAction(
                canvasWidth, 376f, 1847f, 324f, 115f,
                stringResource(R.string.home_refresh),
                onClick = { onRefresh(); onRefreshTrial() },
                enabled = state.syncState != HomeSyncState.UPDATING,
            )
            CrystalText(
                syncText, canvasWidth, 538f, 1944f, 250f, 42f, 17,
                color = if (hasSyncError) CrystalRed else CrystalInk,
                fontWeight = FontWeight.SemiBold,
            )
            CrystalActionIcon(
                imageVector = Icons.Outlined.Refresh,
                canvasWidth = canvasWidth,
                centerX = 538f,
                centerY = 1891f,
                tag = "home_refresh_icon",
                tint = if (hasSyncError) CrystalRed else CrystalDeepRed,
            )
            DesignAction(
                canvasWidth, 700f, 1847f, 325f, 115f,
                stringResource(R.string.home_settings),
                onClick = onOpenSettings,
            )
            CrystalText(
                stringResource(R.string.home_settings), canvasWidth,
                862f, 1944f, 220f, 42f, 17,
                color = CrystalInk, fontWeight = FontWeight.SemiBold,
            )
            CrystalActionIcon(
                imageVector = Icons.Outlined.Settings,
                canvasWidth = canvasWidth,
                centerX = 862f,
                centerY = 1891f,
                tag = "home_settings_icon",
                tint = CrystalDeepRed,
            )
        }
    }
}

@Composable
private fun MetricPair(
    label: String,
    value: String,
    canvasWidth: Dp,
    centerX: Float,
    labelY: Float,
    valueY: Float,
    prominent: Boolean,
) {
    CrystalText(label, canvasWidth, centerX, labelY, 190f, 38f, if (prominent) 14 else 12)
    CrystalText(
        value, canvasWidth, centerX, valueY, 190f, 58f,
        if (prominent) 25 else 17,
        color = if (prominent) CrystalRed else CrystalInk,
        fontWeight = FontWeight.Bold,
        description = "$label $value",
    )
}

@Composable
private fun YunnanAnnouncementOverlay(
    canvasWidth: Dp,
    issue: String,
    announcement: YunnanAnnouncement?,
) {
    CrystalText(
        stringResource(R.string.home_yunnan_announcement), canvasWidth,
        285f, 995f, 360f, 58f, 24,
        color = CrystalDeepRed, fontWeight = FontWeight.ExtraBold, align = TextAlign.Start,
    )
    CrystalText(
        stringResource(R.string.home_yunnan_issue, issue), canvasWidth,
        780f, 998f, 320f, 54f, 23,
        color = CrystalDeepRed,
        fontWeight = FontWeight.Bold,
        align = TextAlign.End,
        tag = "home_announcement_issue",
    )
    if (announcement == null) {
        CrystalText(
            stringResource(R.string.home_yunnan_announcement_pending), canvasWidth,
            540f, 1210f, 480f, 60f, 17, color = CrystalMuted,
            tag = "home_announcement_pending",
        )
        return
    }

    AnnouncementSummaryField(
        stringResource(R.string.home_yunnan_sales_label), announcement.salesAmountYuan,
        canvasWidth, 310f,
    )
    AnnouncementSummaryField(
        stringResource(R.string.home_yunnan_winning_total_label), announcement.winningTotalYuan,
        canvasWidth, 770f,
    )
    val playCenters = mapOf(
        YunnanPlayType.SINGLE to 270f,
        YunnanPlayType.GROUP3 to 540f,
        YunnanPlayType.GROUP6 to 810f,
    )
    YunnanPlayType.entries.forEach { type ->
        val play = announcement.plays.firstOrNull { it.playType == type } ?: return@forEach
        PlayResult(play, canvasWidth, playCenters.getValue(type))
    }
    val payoutText = announcement.plays
        .filter(YunnanPlayAnnouncement::hasPayout)
        .joinToString(separator = "  ") { play ->
            val label = when (play.playType) {
                YunnanPlayType.SINGLE -> "单选"
                YunnanPlayType.GROUP3 -> "组三"
                YunnanPlayType.GROUP6 -> "组六"
            }
            "$label${formatNumber(play.payoutCount!!)}注"
        }
    if (payoutText.isNotEmpty()) {
        CrystalText(
            stringResource(R.string.home_yunnan_payout_prefix) + payoutText,
            canvasWidth, 540f, 1423f, 790f, 60f, 17,
            color = CrystalDeepRed, fontWeight = FontWeight.Bold,
            description = stringResource(R.string.home_yunnan_payout_prefix) + payoutText,
        )
    }
}

@Composable
private fun AnnouncementSummaryField(label: String, amount: Long, canvasWidth: Dp, centerX: Float) {
    CrystalText(label, canvasWidth, centerX, 1105f, 360f, 36f, 14, color = CrystalMuted, align = TextAlign.Start)
    CrystalText(
        stringResource(R.string.home_yunnan_amount_yuan, amount), canvasWidth,
        centerX, 1153f, 360f, 52f, 21,
        fontWeight = FontWeight.Bold, align = TextAlign.Start, description = "$label${amount}元",
    )
}

@Composable
private fun PlayResult(play: YunnanPlayAnnouncement, canvasWidth: Dp, centerX: Float) {
    val label = when (play.playType) {
        YunnanPlayType.SINGLE -> stringResource(R.string.home_yunnan_single)
        YunnanPlayType.GROUP3 -> stringResource(R.string.home_yunnan_group3)
        YunnanPlayType.GROUP6 -> stringResource(R.string.home_yunnan_group6)
    }
    CrystalText(label, canvasWidth, centerX, 1257f, 220f, 38f, 15, fontWeight = FontWeight.SemiBold)
    CrystalText(
        stringResource(R.string.home_yunnan_winning_count, play.winningCount), canvasWidth,
        centerX, 1305f, 220f, 46f, 19, color = CrystalRed, fontWeight = FontWeight.Bold,
        description = "$label${play.winningCount}注",
    )
    CrystalText(
        stringResource(R.string.home_yunnan_prize_per_bet, play.prizePerBetYuan), canvasWidth,
        centerX, 1344f, 220f, 34f, 11, color = CrystalMuted,
    )
}

@Composable
private fun DropdownChevron(canvasWidth: Dp) {
    Canvas(modifier = Modifier.designFrame(canvasWidth, 972f, 120f, 28f, 28f)) {
        val stroke = 3.dp.toPx()
        drawLine(CrystalRed, start = androidx.compose.ui.geometry.Offset(2f, 6f), end = center, strokeWidth = stroke)
        drawLine(CrystalRed, start = center, end = androidx.compose.ui.geometry.Offset(size.width - 2f, 6f), strokeWidth = stroke)
    }
}

@Composable
private fun HeatOverlay(positions: List<HomePositionInsight>, canvasWidth: Dp) {
    CrystalText(stringResource(R.string.home_heat_cold), canvasWidth, 76f, 1798f, 95f, 38f, 12, color = CrystalBlue, fontWeight = FontWeight.Bold)
    CrystalText(stringResource(R.string.home_heat_hot), canvasWidth, 1005f, 1798f, 95f, 38f, 12, color = CrystalHot, fontWeight = FontWeight.Bold)
    val score = positions.mapNotNull {
        when (it.heatLevel) {
            HeatLevel.COLD -> 0.15f
            HeatLevel.WARM -> 0.50f
            HeatLevel.HOT -> 0.85f
            null -> null
        }
    }.average().takeUnless(Double::isNaN)?.toFloat() ?: 0.5f
    val centerX = 116f + 850f * score
    val heatDescription = stringResource(R.string.home_heat_scale_a11y)
    Canvas(
        modifier = Modifier
            .designFrame(canvasWidth, centerX - 22f, 1776f, 44f, 44f)
            .semantics { contentDescription = heatDescription },
    ) {
        drawCircle(Color.White.copy(alpha = 0.92f), radius = size.minDimension * 0.42f)
        drawCircle(CrystalRed, radius = size.minDimension * 0.36f, style = Stroke(3.dp.toPx()))
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
    content: @Composable () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .sizeIn(minHeight = 48.dp)
            .designFrame(canvasWidth, x, y, width, height)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun CrystalText(
    text: String,
    canvasWidth: Dp,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    fontSize: Int,
    color: Color = CrystalInk,
    fontWeight: FontWeight = FontWeight.Medium,
    align: TextAlign = TextAlign.Center,
    description: String? = null,
    tag: String? = null,
) {
    val density = LocalDensity.current
    val designScale = canvasWidth.value / DesignWidth
    val opticalSize = fontSize * designScale * CrystalTypographyScale / density.fontScale
    Box(
        modifier = Modifier
            .designFrame(canvasWidth, centerX - width / 2f, centerY - height / 2f, width, height)
            .then(if (description == null) Modifier else Modifier.semantics { contentDescription = description })
            .then(if (tag == null) Modifier else Modifier.testTag(tag)),
        contentAlignment = when (align) {
            TextAlign.Start -> Alignment.CenterStart
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.Center
        },
    ) {
        Text(
            text = text,
            color = color,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = fontWeight,
                fontSize = opticalSize.sp,
                lineHeight = ((fontSize + 4) * designScale * CrystalTypographyScale / density.fontScale).sp,
                fontFeatureSettings = "tnum",
            ),
            textAlign = align,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CrystalActionIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    canvasWidth: Dp,
    centerX: Float,
    centerY: Float,
    tag: String,
    tint: Color,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .designFrame(canvasWidth, centerX - 24f, centerY - 24f, 48f, 48f)
            .testTag(tag),
    )
}

@Composable
private fun Modifier.designFrame(
    canvasWidth: Dp,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
): Modifier = this
    .offset(
        x = canvasWidth * (x / DesignWidth),
        y = LocalDesignCanvasHeight.current * (y / DesignContentHeight),
    )
    .size(
        width = canvasWidth * (width / DesignWidth),
        height = LocalDesignCanvasHeight.current * (height / DesignContentHeight),
    )

private fun formatNumber(value: Long): String = String.format(Locale.CHINA, "%,d", value)

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
