package com.lucky3d.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lucky3d.app.R
import com.lucky3d.app.core.model.YunnanAnnouncement
import com.lucky3d.app.core.model.YunnanPlayType
import com.lucky3d.app.core.ui.CrystalNumberBall
import com.lucky3d.app.domain.attributes.GroupShape
import com.lucky3d.app.domain.omission.DigitPosition
import com.lucky3d.app.domain.omission.HeatLevel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Content-led home; all surfaces use the current light or dark theme. */
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
    val colors = MaterialTheme.colorScheme
    Surface(modifier.fillMaxSize().testTag("home_live_data_layer"), color = colors.background) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Lucky3D", style = MaterialTheme.typography.headlineSmall,
                    color = colors.primary, modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = { onOpenIssue(latest.issue) },
                    modifier = Modifier.heightIn(min = 48.dp).semantics {
                        contentDescription = "按期号查看第${latest.issue}期，开奖日期${latest.drawDate}"
                    },
                ) {
                    Text("${latest.issue}期", fontFamily = FontFamily.Monospace)
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("开奖结果", style = MaterialTheme.typography.titleMedium)
                Text(latest.drawDate, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Row(
                Modifier.fillMaxWidth().clearAndSetSemantics {
                    contentDescription = "开奖号 ${latest.number.value.toList().joinToString("，")}"
                },
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            ) {
                latest.number.value.forEach { digit ->
                    CrystalNumberBall(digit, digit.toString(), Modifier.size(76.dp))
                }
            }
            Surface(color = colors.surface, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val group = when (attributes.groupShape) {
                        GroupShape.LEOPARD -> "豹子"
                        GroupShape.GROUP3 -> "组三"
                        GroupShape.GROUP6 -> "组六"
                    }
                    HomeMetricRow(listOf("和值" to "${attributes.sum}", "和尾" to "${attributes.sumTail}",
                        "跨度" to "${attributes.span}", "组选" to group), prominent = true)
                    HorizontalDivider(color = colors.outlineVariant)
                    HomeMetricRow(listOf("奇偶" to "${attributes.oddCount}:${attributes.evenCount}",
                        "大小" to "${attributes.bigCount}:${attributes.smallCount}",
                        "质合" to "${attributes.primeLikeCount}:${attributes.compositeLikeCount}",
                        "012路" to attributes.routeCounts.joinToString(":")), prominent = false)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("本期号码回顾", style = MaterialTheme.typography.titleMedium)
                Text("开奖前${state.insights.positions.firstOrNull()?.windowSize ?: 0}期",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.insights.positions.forEach { position ->
                    val label = when (position.position) {
                        DigitPosition.HUNDREDS -> "百位"
                        DigitPosition.TENS -> "十位"
                        DigitPosition.ONES -> "个位"
                        DigitPosition.ALL -> "全部"
                    }
                    val heat = when (position.heatLevel) {
                        HeatLevel.COLD -> "冷态"
                        HeatLevel.WARM -> "温态"
                        HeatLevel.HOT -> "热态"
                        null -> "样本不足"
                    }
                    Surface(Modifier.weight(1f).semantics {
                        contentDescription = "${label}数字${position.digit}，此前遗漏${position.previousOmission}，${heat}，窗口${position.windowSize}期"
                    }, color = colors.surface, shape = MaterialTheme.shapes.small) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("$label ${position.digit}", style = MaterialTheme.typography.titleMedium)
                            Text("开奖前遗漏", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            val omission = when {
                                position.windowSize == 0 -> "--"
                                position.previousOmission >= position.windowSize -> "至少${position.previousOmission}期"
                                else -> "${position.previousOmission}期"
                            }
                            Text(omission, style = MaterialTheme.typography.titleMedium, color = colors.primary)
                            Text(heat, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            CompactAnnouncement(state.yunnanAnnouncement?.takeIf { it.issue == latest.issue }, latest.issue)
            HomeActions(state, onRefresh = { onRefresh(); onRefreshTrial() }, onOpenSettings = onOpenSettings)
        }
    }
}

@Composable
private fun HomeMetricRow(values: List<Pair<String, String>>, prominent: Boolean) {
    Row(Modifier.fillMaxWidth()) {
        values.forEach { (label, value) ->
            Column(Modifier.weight(1f).semantics { contentDescription = "$label $value" },
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = if (prominent) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    color = if (prominent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CompactAnnouncement(announcement: YunnanAnnouncement?, issue: String) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface, shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, colors.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("云南省开奖公告", style = MaterialTheme.typography.titleMedium)
                Text("第${issue}期", style = MaterialTheme.typography.labelMedium,
                    color = colors.primary, modifier = Modifier.testTag("home_announcement_issue"))
            }
            if (announcement == null) {
                Text("开奖公告更新中", style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant, modifier = Modifier.testTag("home_announcement_pending"))
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("销售额" to announcement.salesAmountYuan, "中奖总额" to announcement.winningTotalYuan).forEach { (label, amount) ->
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            Text("${formatHomeAmount(amount)}元", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                HorizontalDivider(color = colors.outlineVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    YunnanPlayType.entries.forEach { type ->
                        val play = announcement.plays.firstOrNull { it.playType == type } ?: return@forEach
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(homePlayLabel(type), style = MaterialTheme.typography.bodySmall)
                            Text("${formatHomeAmount(play.winningCount)}注", color = colors.primary,
                                style = MaterialTheme.typography.titleMedium)
                            Text("${formatHomeAmount(play.prizePerBetYuan)}元/注",
                                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
                val payout = YunnanPlayType.entries.mapNotNull { type ->
                    announcement.plays.firstOrNull { it.playType == type && it.hasPayout }?.let {
                        "${homePlayLabel(type)}${formatHomeAmount(it.payoutCount!!)}注"
                    }
                }.joinToString("  ")
                if (payout.isNotEmpty()) Text("【派奖】$payout", style = MaterialTheme.typography.bodySmall, color = colors.primary)
            }
        }
    }
}

@Composable
private fun HomeActions(state: HomeUiState, onRefresh: () -> Unit, onOpenSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val failed = state.syncState == HomeSyncState.ERROR || state.announcementRefreshFailed
    val status = stringResource(when {
        state.syncState == HomeSyncState.UPDATING -> R.string.sync_updating
        failed -> R.string.sync_failed
        state.syncState == HomeSyncState.CORRECTED -> R.string.sync_corrected
        state.syncState == HomeSyncState.LOCAL -> R.string.sync_local
        else -> R.string.sync_updated
    })
    val trial = state.trialNumber?.number ?: if (state.trialManualRefreshFailed) "失败" else "---"
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("试机号", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(trial, Modifier.testTag("home_trial_number").semantics { contentDescription = trial },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (trial == "---") colors.onSurfaceVariant else colors.primary,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onRefresh, enabled = state.syncState != HomeSyncState.UPDATING,
                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "刷新开奖号和试机号" }) {
                Icon(Icons.Outlined.Refresh, null, Modifier.size(20.dp).testTag("home_refresh_icon"))
                Text("刷新", Modifier.padding(start = 6.dp))
            }
            TextButton(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 48.dp)
                .semantics { contentDescription = "设置" }) {
                Icon(Icons.Outlined.Settings, null, Modifier.size(20.dp).testTag("home_settings_icon"))
                Text("设置", Modifier.padding(start = 6.dp))
            }
        }
        Text(status, color = if (failed) colors.error else colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        state.lastSuccessEpochMillis?.let { timestamp ->
            val time = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai"))
                .format(Instant.ofEpochMilli(timestamp))
            Text("上次更新 $time", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

private fun formatHomeAmount(value: Long): String = String.format(Locale.CHINA, "%,d", value)
private fun homePlayLabel(type: YunnanPlayType): String = when (type) {
    YunnanPlayType.SINGLE -> "单选"
    YunnanPlayType.GROUP3 -> "组三"
    YunnanPlayType.GROUP6 -> "组六"
}
