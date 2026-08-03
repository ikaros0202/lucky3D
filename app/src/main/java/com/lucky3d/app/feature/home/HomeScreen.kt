package com.lucky3d.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky3d.app.R
import com.lucky3d.app.core.model.TrialNumber
import com.lucky3d.app.core.ui.CrystalNumberBall
import com.lucky3d.app.core.ui.EmptyState
import com.lucky3d.app.core.ui.InlineLoading
import com.lucky3d.app.core.ui.InlineMessage
import com.lucky3d.app.core.ui.InlineStatusBanner
import com.lucky3d.app.core.ui.Lucky3dDesign
import com.lucky3d.app.core.ui.MessageKind
import com.lucky3d.app.domain.attributes.DrawAttributes
import com.lucky3d.app.domain.attributes.GroupShape
import com.lucky3d.app.domain.livecontent.LiveContentRefreshState
import com.lucky3d.app.domain.omission.DigitPosition
import com.lucky3d.app.domain.omission.HeatLevel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onRefreshTrial: () -> Unit,
    onOpenIssue: (String) -> Unit,
    onOpenDate: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visualColors = Lucky3dDesign.colors
    val latest = state.latest
    if (latest != null && state.insights.attributes != null) {
        ApprovedCrystalHome(
            state = state,
            onRefresh = onRefresh,
            onRefreshTrial = onRefreshTrial,
            onOpenIssue = onOpenIssue,
            onOpenDate = onOpenDate,
            onOpenSettings = onOpenSettings,
            modifier = modifier,
        )
        return
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(visualColors.crystalBackground),
    ) {
        CrystalFacetBackdrop(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("home_list")
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            CompactHomeTopBar(
                issue = latest?.issue,
                date = latest?.drawDate,
                onOpenIssue = onOpenIssue,
                onOpenDate = onOpenDate,
            )
            if (latest == null) {
                EmptyState(
                    title = stringResource(R.string.home_no_data_title),
                    detail = stringResource(R.string.home_no_data_detail),
                    actionLabel = stringResource(R.string.retry),
                    onAction = onRefresh,
                    modifier = Modifier.weight(1f),
                )
            } else {
                OfficialDrawHero(
                    number = latest.number.value,
                    modifier = Modifier.weight(0.84f),
                )
                DrawAttributesDashboard(
                    number = latest.number.value,
                    attributes = state.insights.attributes,
                    insights = state.insights,
                    modifier = Modifier.weight(1.48f),
                )
                PositionInsightsSection(
                    insights = state.insights,
                    modifier = Modifier.weight(1.03f),
                )
            }
            CompactTrialAndStatusPanel(
                state = state,
                onRefresh = onRefresh,
                onRefreshTrial = onRefreshTrial,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

@Composable
private fun CrystalFacetBackdrop(modifier: Modifier = Modifier) {
    val colors = Lucky3dDesign.colors
    Canvas(modifier = modifier) {
        fun facet(
            first: Offset,
            second: Offset,
            third: Offset,
            useRose: Boolean,
        ) {
            drawPath(
                path = Path().apply {
                    moveTo(first.x, first.y)
                    lineTo(second.x, second.y)
                    lineTo(third.x, third.y)
                    close()
                },
                color = if (useRose) {
                    colors.crystalRose.copy(alpha = 0.54f)
                } else {
                    colors.crystalLavender.copy(alpha = 0.46f)
                },
            )
        }
        facet(
            first = Offset.Zero,
            second = Offset(size.width * 0.38f, 0f),
            third = Offset(0f, size.height * 0.18f),
            useRose = false,
        )
        facet(
            first = Offset(size.width, size.height * 0.06f),
            second = Offset(size.width * 0.70f, size.height * 0.20f),
            third = Offset(size.width, size.height * 0.31f),
            useRose = true,
        )
        facet(
            first = Offset(0f, size.height * 0.55f),
            second = Offset(size.width * 0.22f, size.height * 0.72f),
            third = Offset(0f, size.height * 0.84f),
            useRose = true,
        )
        facet(
            first = Offset(size.width, size.height * 0.72f),
            second = Offset(size.width * 0.78f, size.height * 0.90f),
            third = Offset(size.width, size.height),
            useRose = false,
        )
    }
}

@Composable
private fun CompactHomeTopBar(
    issue: String?,
    date: String?,
    onOpenIssue: (String) -> Unit,
    onOpenDate: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1.18f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_title),
                    fontSize = 17.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.home_crystal_subtitle),
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    color = Lucky3dDesign.colors.primaryDeep,
                    maxLines = 1,
                )
            }
            HeaderCrystalMark()
        }
        if (issue != null && date != null) {
            OutlinedButton(
                onClick = { onOpenIssue(issue) },
                modifier = Modifier
                    .weight(1.02f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(horizontal = 3.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_issue, issue),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
            FilledTonalButton(
                onClick = { onOpenDate(date) },
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(horizontal = 3.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = date,
                    modifier = Modifier.padding(start = 2.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HeaderCrystalMark() {
    val colors = Lucky3dDesign.colors
    Canvas(modifier = Modifier.size(width = 19.dp, height = 27.dp)) {
        val outline = Path().apply {
            moveTo(size.width * 0.50f, 0f)
            lineTo(size.width, size.height * 0.32f)
            lineTo(size.width * 0.73f, size.height)
            lineTo(size.width * 0.20f, size.height * 0.83f)
            lineTo(0f, size.height * 0.28f)
            close()
        }
        drawPath(outline, colors.crystalRose)
        drawPath(
            Path().apply {
                moveTo(size.width * 0.50f, 0f)
                lineTo(size.width * 0.73f, size.height)
                lineTo(0f, size.height * 0.28f)
                close()
            },
            colors.crystalHighlight.copy(alpha = 0.62f),
        )
        drawPath(
            Path().apply {
                moveTo(size.width * 0.50f, 0f)
                lineTo(size.width, size.height * 0.32f)
                lineTo(size.width * 0.28f, size.height * 0.48f)
                close()
            },
            colors.primaryDeep.copy(alpha = 0.42f),
        )
    }
}

@Composable
private fun HomeHeader(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRefreshTrial: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = stringResource(R.string.home_title),
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.home_crystal_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = Lucky3dDesign.colors.primaryDeep,
            )
        }
        IconButton(
            onClick = {
                onRefresh()
                onRefreshTrial()
            },
            enabled = !isRefreshing,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Icon(
                Icons.Outlined.Refresh,
                contentDescription = stringResource(R.string.home_refresh),
            )
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.home_settings),
            )
        }
    }
}

@Composable
private fun TargetedQueryRow(
    issue: String,
    date: String,
    onOpenIssue: (String) -> Unit,
    onOpenDate: (String) -> Unit,
) {
    val issueDescription = stringResource(R.string.home_query_issue, issue)
    val dateDescription = stringResource(R.string.home_query_date, date)
    val issueLabel = stringResource(R.string.home_issue, issue)
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 400.dp && fontScale > 1.15f) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IssueQueryButton(
                    label = issueLabel,
                    description = issueDescription,
                    onClick = { onOpenIssue(issue) },
                    showIcon = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                DateQueryButton(
                    label = date,
                    description = dateDescription,
                    onClick = { onOpenDate(date) },
                    showIcon = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            val showIcons = maxWidth >= 340.dp
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IssueQueryButton(
                    label = issueLabel,
                    description = issueDescription,
                    onClick = { onOpenIssue(issue) },
                    showIcon = showIcons,
                    modifier = Modifier.weight(1f),
                )
                DateQueryButton(
                    label = date,
                    description = dateDescription,
                    onClick = { onOpenDate(date) },
                    showIcon = showIcons,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun IssueQueryButton(
    label: String,
    description: String,
    onClick: () -> Unit,
    showIcon: Boolean,
    modifier: Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Text(
            text = label,
            maxLines = 1,
            fontFamily = FontFamily.Monospace,
        )
        if (showIcon) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(18.dp),
            )
        }
    }
}

@Composable
private fun DateQueryButton(
    label: String,
    description: String,
    onClick: () -> Unit,
    showIcon: Boolean,
    modifier: Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        if (showIcon) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = label,
            modifier = if (showIcon) Modifier.padding(start = 4.dp) else Modifier,
            maxLines = 1,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun OfficialDrawHero(
    number: String,
    modifier: Modifier = Modifier,
) {
    val numberDescription = officialNumberDescription(number)
    val stageDescription = stringResource(R.string.home_crystal_stage_a11y)
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = stageDescription },
        ) {
            CrystalDrawStage(modifier = Modifier.fillMaxSize())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .clearAndSetSemantics {
                        contentDescription = numberDescription
                    },
                horizontalArrangement = Arrangement.spacedBy(
                    8.dp,
                    Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                number.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CrystalNumberBall(
                            digit = digit,
                            contentDescription = digit.toString(),
                            modifier = Modifier.size(96.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CrystalDrawStage(modifier: Modifier = Modifier) {
    val colors = Lucky3dDesign.colors
    Canvas(modifier = modifier) {
        val upperFacet = Path().apply {
            moveTo(size.width * 0.02f, size.height * 0.10f)
            lineTo(size.width * 0.28f, size.height * 0.03f)
            lineTo(size.width * 0.14f, size.height * 0.72f)
            close()
        }
        val rightFacet = Path().apply {
            moveTo(size.width * 0.82f, size.height * 0.08f)
            lineTo(size.width * 0.98f, size.height * 0.24f)
            lineTo(size.width * 0.91f, size.height * 0.82f)
            lineTo(size.width * 0.72f, size.height * 0.50f)
            close()
        }
        drawPath(upperFacet, colors.crystalLavender.copy(alpha = 0.62f))
        drawPath(rightFacet, colors.crystalRose.copy(alpha = 0.62f))
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors.crystalHighlight,
                    colors.crystalRose.copy(alpha = 0.86f),
                    colors.crystalLavender.copy(alpha = 0.72f),
                ),
                center = Offset(size.width / 2f, size.height * 0.77f),
                radius = size.width * 0.48f,
            )
            ,
            topLeft = Offset(size.width * 0.08f, size.height * 0.62f),
            size = Size(size.width * 0.84f, size.height * 0.30f),
        )
        drawOval(
            color = colors.crystalHighlight.copy(alpha = 0.72f),
            topLeft = Offset(size.width * 0.20f, size.height * 0.68f),
            size = Size(size.width * 0.60f, size.height * 0.18f),
            style = Stroke(width = 2.dp.toPx()),
        )
        drawOval(
            color = colors.crystalBorder,
            topLeft = Offset(size.width * 0.08f, size.height * 0.62f),
            size = Size(size.width * 0.84f, size.height * 0.30f),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

@Composable
private fun DrawAttributesDashboard(
    number: String,
    attributes: DrawAttributes?,
    insights: HomeInsights,
    modifier: Modifier = Modifier,
) {
    if (attributes == null) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        CrystalGlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.34f),
            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 2.dp),
            shape = RoundedCornerShape(9.dp),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                CompactDashboardMetric(
                    stringResource(R.string.home_sum_label),
                    attributes.sum.toString(),
                    Modifier.weight(1f),
                )
                CompactDashboardMetric(
                    stringResource(R.string.home_sum_tail_label),
                    attributes.sumTail.toString(),
                    Modifier.weight(1f),
                )
                CompactDashboardMetric(
                    stringResource(R.string.home_span_label),
                    attributes.span.toString(),
                    Modifier.weight(1f),
                )
                CompactDashboardMetric(
                    stringResource(R.string.home_group_label),
                    groupShapeLabel(attributes.groupShape),
                    Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.08f),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            CrystalAttributeTile(
                label = stringResource(R.string.home_parity_label),
                value = "${attributes.oddCount}:${attributes.evenCount}",
                modifier = Modifier.weight(1f),
            )
            CrystalAttributeTile(
                label = stringResource(R.string.home_size_label),
                value = "${attributes.bigCount}:${attributes.smallCount}",
                modifier = Modifier.weight(1f),
            )
            CrystalAttributeTile(
                label = stringResource(R.string.home_quality_label),
                value = "${attributes.primeLikeCount}:${attributes.compositeLikeCount}",
                modifier = Modifier.weight(1f),
            )
            CrystalAttributeTile(
                label = stringResource(R.string.home_route_label),
                value = attributes.routeCounts.joinToString(":"),
                modifier = Modifier.weight(1f),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.68f),
            contentAlignment = Alignment.Center,
        ) {
            CrystalGlassPanel(
                modifier = Modifier
                    .fillMaxWidth(0.53f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                shape = RoundedCornerShape(50),
            ) {
                CompactInsightLabel(
                    label = stringResource(R.string.home_consecutive_label),
                    value = consecutiveDigitsLabel(number),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.65f),
            contentAlignment = Alignment.Center,
        ) {
            CrystalGlassPanel(
                modifier = Modifier
                    .fillMaxWidth(0.43f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                shape = RoundedCornerShape(50),
            ) {
                CompactInsightLabel(
                    label = stringResource(R.string.home_cold_hits),
                    value = insights.coldHits.size.toString(),
                    cold = true,
                )
            }
        }
    }
}

@Composable
private fun CrystalAttributeTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    CrystalGlassPanel(
        modifier = modifier.fillMaxHeight(),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
        shape = RoundedCornerShape(9.dp),
    ) {
        CompactDashboardMetric(
            label = label,
            value = value,
            modifier = Modifier.fillMaxSize(),
            primary = false,
        )
    }
}

@Composable
private fun CompactDashboardMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clearAndSetSemantics {
                contentDescription = "$label $value"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (primary) 16.sp else 12.sp,
            lineHeight = if (primary) 17.sp else 13.sp,
            color = if (primary) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun CompactInsightLabel(
    label: String,
    value: String,
    cold: Boolean = false,
) {
    Row(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "$label $value"
        },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cold) {
            Icon(
                imageVector = Icons.Outlined.AcUnit,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            color = if (cold) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 13.sp,
            color = if (cold) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}

@Composable
private fun PrimaryAttributes(attributes: DrawAttributes) {
    CrystalGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            CrystalMetric(
                label = stringResource(R.string.home_sum_label),
                value = attributes.sum.toString(),
                modifier = Modifier.weight(1f),
            )
            CrystalMetric(
                label = stringResource(R.string.home_sum_tail_label),
                value = attributes.sumTail.toString(),
                modifier = Modifier.weight(1f),
            )
            CrystalMetric(
                label = stringResource(R.string.home_span_label),
                value = attributes.span.toString(),
                modifier = Modifier.weight(1f),
            )
            CrystalMetric(
                label = stringResource(R.string.home_group_label),
                value = groupShapeLabel(attributes.groupShape),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SecondaryAttributes(attributes: DrawAttributes) {
    CrystalGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            CrystalSecondaryMetric(
                label = stringResource(R.string.home_parity_label),
                value = "${attributes.oddCount}:${attributes.evenCount}",
                Modifier.weight(1f),
            )
            CrystalSecondaryMetric(
                label = stringResource(R.string.home_size_label),
                value = "${attributes.bigCount}:${attributes.smallCount}",
                Modifier.weight(1f),
            )
            CrystalSecondaryMetric(
                label = stringResource(R.string.home_quality_label),
                value = "${attributes.primeLikeCount}:${attributes.compositeLikeCount}",
                Modifier.weight(1f),
            )
            CrystalSecondaryMetric(
                label = stringResource(R.string.home_route_label),
                value = attributes.routeCounts.joinToString(":"),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ConsecutivePlate(number: String) {
    val label = stringResource(R.string.home_consecutive_label)
    val value = consecutiveDigitsLabel(number)
    CrystalGlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 52.dp)
            .clearAndSetSemantics {
                contentDescription = "$label $value"
            },
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                modifier = Modifier.padding(start = 10.dp),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CrystalMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "$label $value"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CrystalSecondaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = Lucky3dDesign.colors
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "$label $value"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            drawCircle(
                color = primary.copy(alpha = 0.78f),
            )
            drawCircle(
                color = colors.crystalHighlight.copy(alpha = 0.75f),
                radius = size.minDimension * 0.18f,
                center = Offset(size.width * 0.35f, size.height * 0.30f),
            )
            drawCircle(
                color = colors.crystalBorder,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CrystalGlassPanel(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(18.dp),
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable () -> Unit,
) {
    val colors = Lucky3dDesign.colors
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .shadow(3.dp, shape)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        colors.crystalHighlight.copy(alpha = if (isDark) 0.10f else 0.92f),
                        colors.crystalRose.copy(alpha = if (isDark) 0.90f else 0.82f),
                        colors.crystalLavender.copy(alpha = if (isDark) 0.82f else 0.60f),
                    ),
                ),
            )
            .border(1.dp, colors.crystalBorder, shape)
            .padding(contentPadding),
    ) {
        content()
    }
}

@Composable
private fun CompactTrialAndStatusPanel(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onRefreshTrial: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val syncLabel = when (state.syncState) {
        HomeSyncState.LOCAL -> stringResource(R.string.sync_local)
        HomeSyncState.UP_TO_DATE -> stringResource(R.string.sync_updated)
        HomeSyncState.UPDATING -> stringResource(R.string.sync_updating)
        HomeSyncState.ERROR -> stringResource(R.string.sync_failed)
        HomeSyncState.CORRECTED -> stringResource(R.string.sync_corrected)
    }
    val refreshDescription = stringResource(R.string.home_refresh)
    CrystalGlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 2.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1.4f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_trial_title),
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                when {
                    state.trialNumber != null -> {
                        CompactTrialDigits(state.trialNumber.number)
                    }
                    state.trialManualRefreshFailed -> {
                        Text(
                            text = stringResource(R.string.home_trial_failed),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    else -> {
                        Text(
                            text = "---",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(0.65f)
                    .background(Lucky3dDesign.colors.crystalBorder),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        enabled = state.syncState != HomeSyncState.UPDATING,
                        onClick = {
                            onRefresh()
                            onRefreshTrial()
                        },
                    )
                    .semantics { contentDescription = refreshDescription },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = syncLabel,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    color = if (state.syncState == HomeSyncState.ERROR) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 3.dp)
                        .size(20.dp),
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(0.65f)
                    .background(Lucky3dDesign.colors.crystalBorder),
            )
            Row(
                modifier = Modifier
                    .weight(0.82f)
                    .fillMaxHeight()
                    .clickable(onClick = onOpenSettings),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.home_settings),
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.home_settings),
                    modifier = Modifier.padding(start = 2.dp),
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun CompactTrialDigits(number: String) {
    Text(
        text = number,
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = number
        },
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 17.sp,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun TrialNumberSection(
    state: HomeUiState,
    onRefreshTrial: () -> Unit,
) {
    CrystalGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_trial_title),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(
                        1.dp,
                        Lucky3dDesign.colors.crystalBorder,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.home_trial_badge),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            state.trialNumber?.let { trial ->
                TrialContent(trial)
            }
            TrialStatus(
                state = state,
                onRefreshTrial = onRefreshTrial,
            )
            Text(
                text = stringResource(R.string.home_trial_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrialContent(trial: TrialNumber) {
    val numberDescription = trialNumberDescription(trial.number)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = numberDescription
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        trial.number.forEach { digit ->
            TrialDigitBubble(digit)
        }
    }
    Text(
        text = stringResource(
            R.string.home_trial_issue,
            trial.issue,
            trial.sourceLocalDate.toString(),
        ),
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
    )
    Text(
        text = stringResource(
            R.string.home_trial_fetched,
            formatEpoch(trial.fetchedAtEpochMillis),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TrialDigitBubble(digit: Char) {
    val colors = Lucky3dDesign.colors
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(colors.crystalHighlight.copy(alpha = 0.72f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = digit.toString(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun TrialStatus(
    state: HomeUiState,
    onRefreshTrial: () -> Unit,
) {
    when {
        state.trialState == LiveContentRefreshState.Refreshing -> {
            InlineLoading(stringResource(R.string.home_trial_loading))
        }
        state.trialState is LiveContentRefreshState.Failed && state.trialNumber != null -> {
            TrialStatusRow(
                message = stringResource(R.string.home_trial_cached_failure),
                onRefreshTrial = onRefreshTrial,
            )
        }
        state.trialState is LiveContentRefreshState.Failed -> {
            TrialStatusRow(
                message = stringResource(R.string.home_trial_unavailable),
                onRefreshTrial = onRefreshTrial,
            )
        }
        state.isBeforeTrialReleaseWindow && state.trialNumber == null -> {
            Text(
                text = stringResource(R.string.home_trial_before_release),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.trialNumber == null -> {
            TrialStatusRow(
                message = stringResource(R.string.home_trial_empty),
                onRefreshTrial = onRefreshTrial,
            )
        }
    }
}

@Composable
private fun TrialStatusRow(
    message: String,
    onRefreshTrial: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onRefreshTrial,
            modifier = Modifier.heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Text(stringResource(R.string.home_trial_retry))
        }
    }
}

@Composable
private fun ColdHitsSection(insights: HomeInsights) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CrystalGlassPanel(
            modifier = Modifier.padding(horizontal = 52.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AcUnit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = stringResource(R.string.home_cold_hits),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        if (insights.coldHits.isEmpty()) {
            Text(
                text = stringResource(R.string.home_no_cold_hit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            insights.coldHits.forEach { hit ->
                Text(
                    text = stringResource(
                        R.string.home_cold_hit_row,
                        positionLabel(hit.position),
                        hit.digit,
                        hit.previousOmission,
                        hit.windowSize,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PositionInsightsSection(
    insights: HomeInsights,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            insights.positions.forEach { position ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    PositionCrystalDial(
                        position = position,
                        modifier = Modifier
                            .sizeIn(maxWidth = 104.dp, maxHeight = 104.dp)
                            .aspectRatio(1f),
                    )
                }
            }
        }
        HeatScale(insights.positions)
    }
}

@Composable
private fun PositionCrystalDial(
    position: HomePositionInsight,
    modifier: Modifier = Modifier,
) {
    val colors = Lucky3dDesign.colors
    val isDark = isSystemInDarkTheme()
    val positionText = positionLabel(position.position)
    val heatText = heatLabel(position.heatLevel)
    val description = stringResource(
        R.string.home_position_dial_a11y,
        positionText,
        position.digit,
        position.previousOmission,
        heatText,
        position.windowSize,
    )
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        colors.crystalHighlight.copy(alpha = if (isDark) 0.10f else 0.94f),
                        colors.crystalRose.copy(alpha = 0.76f),
                        colors.crystalLavender.copy(alpha = 0.62f),
                    ),
                ),
            )
            .border(1.dp, colors.crystalBorder, CircleShape)
            .clearAndSetSemantics {
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = positionText,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = position.digit.toString(),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(
                    R.string.home_omission_short,
                    position.previousOmission,
                ),
                fontSize = 9.sp,
                lineHeight = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun HeatScale(positions: List<HomePositionInsight>) {
    val coldColor = MaterialTheme.colorScheme.tertiary
    val hotColor = MaterialTheme.colorScheme.primary
    val trackColor = Lucky3dDesign.colors.crystalHighlight
    val outlineColor = Lucky3dDesign.colors.crystalBorder
    val description = stringResource(R.string.home_heat_scale_a11y)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_heat_cold),
            fontSize = 9.sp,
            lineHeight = 10.sp,
            color = coldColor,
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(18.dp),
        ) {
            val verticalInset = 7.dp.toPx()
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(coldColor, trackColor, hotColor)),
                topLeft = Offset(0f, verticalInset),
                size = Size(size.width, size.height - verticalInset * 2f),
                cornerRadius = CornerRadius(size.height),
            )
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(0f, verticalInset),
                size = Size(size.width, size.height - verticalInset * 2f),
                cornerRadius = CornerRadius(size.height),
                style = Stroke(width = 1.dp.toPx()),
            )
            repeat(7) { index ->
                val x = size.width * index / 6f
                drawCircle(
                    color = trackColor,
                    radius = 2.dp.toPx(),
                    center = Offset(x, size.height / 2f),
                )
            }
            drawCircle(
                color = trackColor,
                radius = 7.dp.toPx(),
                center = Offset(size.width * score, size.height / 2f),
            )
            drawCircle(
                color = hotColor,
                radius = 6.dp.toPx(),
                center = Offset(size.width * score, size.height / 2f),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        Text(
            text = stringResource(R.string.home_heat_hot),
            fontSize = 9.sp,
            lineHeight = 10.sp,
            color = hotColor,
        )
    }
}

@Composable
private fun HomeSyncStatus(
    state: HomeUiState,
    onRefresh: () -> Unit,
) {
    if (state.syncState == HomeSyncState.UPDATING) {
        InlineLoading(stringResource(R.string.sync_updating))
        return
    }
    val lastSuccess = state.lastSuccessEpochMillis?.let(::formatEpoch)
        ?: stringResource(R.string.sync_never)
    val detail = if (state.lastSuccessEpochMillis == null) {
        lastSuccess
    } else {
        stringResource(R.string.sync_last_success, lastSuccess)
    }
    val message = when (state.syncState) {
        HomeSyncState.LOCAL -> InlineMessage(
            kind = MessageKind.INFO,
            title = stringResource(R.string.sync_local),
            detail = detail,
            actionLabel = stringResource(R.string.retry),
        )
        HomeSyncState.UP_TO_DATE -> InlineMessage(
            kind = MessageKind.SUCCESS,
            title = stringResource(R.string.sync_updated),
            detail = detail,
        )
        HomeSyncState.ERROR -> InlineMessage(
            kind = MessageKind.ERROR,
            title = stringResource(R.string.sync_failed),
            detail = failureMessage(state.failureType),
            actionLabel = stringResource(R.string.retry),
        )
        HomeSyncState.CORRECTED -> InlineMessage(
            kind = MessageKind.WARNING,
            title = stringResource(R.string.sync_corrected),
            detail = stringResource(R.string.sync_corrected_detail),
        )
        HomeSyncState.UPDATING -> return
    }
    InlineStatusBanner(
        message = message,
        onAction = if (message.actionLabel == null) null else onRefresh,
    )
}

@Composable
private fun groupShapeLabel(shape: GroupShape): String = when (shape) {
    GroupShape.LEOPARD -> stringResource(R.string.group_leopard)
    GroupShape.GROUP3 -> stringResource(R.string.group_3)
    GroupShape.GROUP6 -> stringResource(R.string.group_6)
}

@Composable
private fun positionLabel(position: DigitPosition): String = when (position) {
    DigitPosition.HUNDREDS -> stringResource(R.string.home_position_hundreds)
    DigitPosition.TENS -> stringResource(R.string.home_position_tens)
    DigitPosition.ONES -> stringResource(R.string.home_position_ones)
    DigitPosition.ALL -> error("Home requires a concrete digit position")
}

@Composable
private fun heatLabel(level: HeatLevel?): String = when (level) {
    HeatLevel.COLD -> stringResource(R.string.home_heat_cold)
    HeatLevel.WARM -> stringResource(R.string.home_heat_warm)
    HeatLevel.HOT -> stringResource(R.string.home_heat_hot)
    null -> stringResource(R.string.home_heat_unknown)
}

@Composable
private fun failureMessage(type: String?): String = when {
    type == "NETWORK" -> stringResource(R.string.sync_error_network)
    type == "INVALID_PAYLOAD" -> stringResource(R.string.sync_error_payload)
    type == "EMPTY_RESPONSE" -> stringResource(R.string.sync_error_empty)
    type?.startsWith("HTTP_") == true -> stringResource(R.string.sync_error_http)
    else -> stringResource(R.string.sync_error_generic)
}

@Composable
private fun officialNumberDescription(number: String): String = stringResource(
    R.string.draw_number_digits_a11y,
    number[0],
    number[1],
    number[2],
)

@Composable
private fun trialNumberDescription(number: String): String = stringResource(
    R.string.trial_number_digits_a11y,
    number[0],
    number[1],
    number[2],
)

private fun formatEpoch(epochMillis: Long): String = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm")
    .withZone(BEIJING)
    .format(Instant.ofEpochMilli(epochMillis))

private val BEIJING: ZoneId = ZoneId.of("Asia/Shanghai")
