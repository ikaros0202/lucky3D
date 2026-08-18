package com.lucky3d.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lucky3d.app.R
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.core.model.YunnanAnnouncement
import com.lucky3d.app.core.model.YunnanPlayAnnouncement
import com.lucky3d.app.core.model.YunnanPlayType
import com.lucky3d.app.core.ui.EmptyState
import com.lucky3d.app.core.ui.Lucky3dDesign
import com.lucky3d.app.core.ui.MatteNumberBall
import com.lucky3d.app.data.repository.DrawQuery
import com.lucky3d.app.domain.attributes.DrawAttributes
import com.lucky3d.app.domain.attributes.GroupShape
import java.text.NumberFormat
import java.math.BigDecimal
import java.util.Locale

/** Issue is the only query control. Dates are displayed only as result metadata. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit,
    onSearchIssue: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    // Source-compatible callbacks for older callers; none is rendered.
    onShowRecent: (Int) -> Unit = {},
    onSearchYear: (String) -> Unit = {},
    onSearchDate: (String) -> Unit = {},
    onSearchDateRange: (String, String) -> Unit = { _, _ -> },
    onSelectDraw: (DrawRecord?) -> Unit = {},
) {
    var issueSheetVisible by rememberSaveable { mutableStateOf(false) }
    val selectedDraw = state.records.firstOrNull()
    val selectedIssue = when (val query = state.query) {
        is DrawQuery.Issue -> query.issue
        else -> selectedDraw?.issue.orEmpty()
    }
    val issueChoices = state.availableIssues.ifEmpty { state.records }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            HistoryCinnabarHeader(onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                IssueSelector(
                    issue = selectedIssue,
                    date = selectedDraw?.drawDate,
                    onClick = { issueSheetVisible = true },
                )
                when {
                    state.inputError == HistoryInputError.INVALID_ISSUE -> EmptyState(
                        title = stringResource(R.string.history_issue_query_invalid),
                        detail = stringResource(R.string.history_issue_query_empty_detail),
                    )
                    state.records.isEmpty() -> EmptyState(
                        title = stringResource(R.string.history_issue_query_empty_title),
                        detail = stringResource(R.string.history_issue_query_empty_detail),
                    )
                    else -> state.records.forEach { draw ->
                        DrawResultSection(draw)
                        if (state.yunnanAnnouncement?.issue == draw.issue) {
                            YunnanAnnouncementSection(state.yunnanAnnouncement)
                        } else if (state.announcementState == HistoryAnnouncementState.IDLE) {
                            Text(
                                stringResource(R.string.history_yunnan_loading),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else if (state.announcementState == HistoryAnnouncementState.UNAVAILABLE) {
                            Text(
                                stringResource(R.string.history_yunnan_unavailable),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        DrawAnalysisSection(draw)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (issueSheetVisible) {
        ModalBottomSheet(onDismissRequest = { issueSheetVisible = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.history_issue_sheet_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.history_issue_sheet_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { issueSheetVisible = false }) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.close))
                    }
                }
                if (issueChoices.isEmpty()) {
                    Text(
                        stringResource(R.string.history_issue_sheet_empty),
                        modifier = Modifier.padding(vertical = 32.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 590.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(issueChoices, key = DrawRecord::issue) { draw ->
                            IssueChoiceRow(
                                draw = draw,
                                selected = draw.issue == selectedIssue,
                                onClick = {
                                    issueSheetVisible = false
                                    onSearchIssue(draw.issue)
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun HistoryCinnabarHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(Lucky3dDesign.colors.primaryDeep, MaterialTheme.colorScheme.primary),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                stringResource(R.string.back),
                tint = Color.White,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.history_date_query_title),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.history_header_subtitle),
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun IssueSelector(issue: String, date: String?, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (issue.isBlank()) "—" else stringResource(R.string.home_issue, issue),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                date?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                stringResource(R.string.history_issue_sheet_action),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
            Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun IssueChoiceRow(draw: DrawRecord, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_issue, draw.issue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    draw.drawDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                draw.number.value,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.size(20.dp))
            if (selected) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            } else {
                Box(
                    Modifier
                        .size(24.dp)
                        .background(Color.Transparent, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = 4.dp, height = 24.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
        )
        Text(
            text,
            modifier = Modifier.padding(start = 10.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DrawResultSection(draw: DrawRecord) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.history_date_query_result_title))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            draw.number.value.forEachIndexed { index, digit ->
                MatteNumberBall(
                    text = digit.toString(),
                    selected = true,
                    contentDescription = digit.toString(),
                )
                if (index != 2) Spacer(Modifier.size(16.dp))
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun YunnanAnnouncementSection(announcement: YunnanAnnouncement) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.history_yunnan_title))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnnouncementMetric(
                stringResource(R.string.history_yunnan_sales_label),
                formatYuan(announcement.salesAmountYuan),
                Modifier.weight(1f),
            )
            AnnouncementMetric(
                stringResource(R.string.history_yunnan_winning_total_label),
                formatYuan(announcement.winningTotalYuan),
                Modifier.weight(1f),
            )
        }
        if (announcement.prizePoolBalanceFen != null || announcement.redemptionDeadline != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                announcement.prizePoolBalanceFen?.let { balanceFen ->
                    AnnouncementMetric(
                        stringResource(R.string.history_yunnan_prize_pool_balance_label),
                        formatFenAsYuan(balanceFen),
                        Modifier.weight(1f),
                    )
                }
                announcement.redemptionDeadline?.let { deadline ->
                    AnnouncementMetric(
                        stringResource(R.string.history_yunnan_redemption_deadline_label),
                        deadline,
                        Modifier.weight(1f),
                    )
                }
            }
        }
        HorizontalDivider()
        val plays = orderedPlays(announcement.plays)
        Column {
            plays.forEachIndexed { index, play ->
                PlayAnnouncementRow(play)
                if (index != plays.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                }
            }
        }
        payoutSummary(announcement)?.let { summary ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    summary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun AnnouncementMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun PlayAnnouncementRow(play: YunnanPlayAnnouncement) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(0.72f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                playLabel(play.playType),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.history_yunnan_prize_per_bet, formatInteger(play.prizePerBetYuan)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                stringResource(R.string.history_yunnan_winning_count, formatInteger(play.winningCount)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (play.hasPayout) {
            Column(
                modifier = Modifier.weight(1.2f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    stringResource(R.string.history_yunnan_payout_count, formatInteger(play.payoutCount!!)),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(
                        R.string.history_yunnan_prize_per_bet,
                        formatInteger(play.payoutPerBetYuan!!),
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            Spacer(Modifier.weight(1.2f))
        }
    }
}

@Composable
private fun DrawAnalysisSection(draw: DrawRecord) {
    val attributes = DrawAttributes.calculate(draw.number)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.history_date_query_analysis_title))
        val chips = listOf(
            "和值${attributes.sum}", "和尾${attributes.sumTail}", "跨度${attributes.span}",
            historyGroupShapeLabel(attributes.groupShape),
            "奇偶${attributes.oddCount}:${attributes.evenCount}",
            "大小${attributes.bigCount}:${attributes.smallCount}",
            "质合${attributes.primeLikeCount}:${attributes.compositeLikeCount}",
            "012路${attributes.routeCounts.joinToString(":")}",
        )
        chips.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { value ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            value,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Text(
            stringResource(
                R.string.history_pair_sums,
                attributes.pairSums.hundredsTens,
                attributes.pairSums.tensOnes,
                attributes.pairSums.hundredsOnes,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            stringResource(
                R.string.history_pair_differences,
                attributes.pairDifferences.hundredsTens,
                attributes.pairDifferences.tensOnes,
                attributes.pairDifferences.hundredsOnes,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        HorizontalDivider()
    }
}

private fun orderedPlays(plays: List<YunnanPlayAnnouncement>): List<YunnanPlayAnnouncement> =
    YunnanPlayType.entries.mapNotNull { type -> plays.firstOrNull { it.playType == type } }

@Composable
private fun playLabel(type: YunnanPlayType): String = when (type) {
    YunnanPlayType.SINGLE -> stringResource(R.string.home_yunnan_single)
    YunnanPlayType.GROUP3 -> stringResource(R.string.home_yunnan_group3)
    YunnanPlayType.GROUP6 -> stringResource(R.string.home_yunnan_group6)
}

private fun payoutSummary(announcement: YunnanAnnouncement): String? = orderedPlays(announcement.plays)
    .filter(YunnanPlayAnnouncement::hasPayout)
    .joinToString(separator = " ") { play ->
        val label = when (play.playType) {
            YunnanPlayType.SINGLE -> "单选"
            YunnanPlayType.GROUP3 -> "组三"
            YunnanPlayType.GROUP6 -> "组六"
        }
        "$label${formatInteger(play.payoutCount!!)}注"
    }
    .ifBlank { null }
    ?.let { "【派奖】$it" }

private fun formatInteger(value: Long): String = NumberFormat.getIntegerInstance(Locale.CHINA).format(value)
private fun formatYuan(value: Long): String = "${formatInteger(value)}元"
private fun formatFenAsYuan(value: Long): String =
    "${NumberFormat.getNumberInstance(Locale.CHINA).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(BigDecimal.valueOf(value, 2))}元"

@Composable
private fun historyGroupShapeLabel(shape: GroupShape): String = when (shape) {
    GroupShape.LEOPARD -> stringResource(R.string.group_leopard)
    GroupShape.GROUP3 -> stringResource(R.string.group_3)
    GroupShape.GROUP6 -> stringResource(R.string.group_6)
}
