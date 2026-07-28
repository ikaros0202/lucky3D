package com.lucky3d.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lucky3d.app.R
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.core.ui.EmptyState
import com.lucky3d.app.core.ui.InlineLoading
import com.lucky3d.app.core.ui.InlineMessage
import com.lucky3d.app.core.ui.InlineStatusBanner
import com.lucky3d.app.core.ui.MessageKind
import com.lucky3d.app.domain.attributes.DrawAttributes
import com.lucky3d.app.domain.attributes.GroupShape
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTrend: () -> Unit,
    onOpenPick: () -> Unit,
    onOpenSchemes: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
    ) {
        item {
            HomeHeader(
                isRefreshing = state.syncState == HomeSyncState.UPDATING,
                onRefresh = onRefresh,
                onOpenSettings = onOpenSettings,
            )
        }
        item {
            HomeSyncStatus(state = state, onRefresh = onRefresh)
        }
        val latest = state.latest
        if (latest == null) {
            item {
                EmptyState(
                    title = stringResource(R.string.home_no_data_title),
                    detail = stringResource(R.string.home_no_data_detail),
                    actionLabel = stringResource(R.string.retry),
                    onAction = onRefresh,
                )
            }
        } else {
            item { LatestDrawCard(latest) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onOpenTrend,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.home_view_trend))
                    }
                    FilledTonalButton(
                        onClick = onOpenPick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.home_start_pick))
                    }
                }
            }
        }
        item {
            SectionHeader(
                title = stringResource(R.string.home_recent),
                actionLabel = stringResource(R.string.home_all_history),
                onAction = onOpenHistory,
            )
        }
        if (state.recent.isEmpty()) {
            item {
                OutlinedButton(
                    onClick = onOpenHistory,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.History, contentDescription = null)
                    Text(
                        text = stringResource(R.string.home_history),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        } else {
            items(state.recent, key = DrawRecord::issue) { draw ->
                RecentDrawRow(draw)
            }
        }
        item {
            OutlinedButton(
                onClick = onOpenSchemes,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.home_schemes))
            }
        }
    }
}

@Composable
private fun HomeHeader(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        IconButton(
            onClick = onRefresh,
            enabled = !isRefreshing,
        ) {
            Icon(
                Icons.Outlined.Refresh,
                contentDescription = stringResource(R.string.home_refresh),
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.home_settings),
            )
        }
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
private fun LatestDrawCard(draw: DrawRecord) {
    val attributes = DrawAttributes.calculate(draw.number)
    val numberDescription = stringResource(R.string.draw_number_a11y, draw.number.value)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = numberDescription
            },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.home_latest),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.home_issue, draw.issue),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = draw.drawDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DrawDigits(draw.number.value)
            }
            Text(
                text = stringResource(
                    R.string.home_attributes,
                    attributes.sum,
                    attributes.span,
                    groupShapeLabel(attributes.groupShape),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun DrawDigits(number: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        number.forEach { digit ->
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = digit.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentDrawRow(draw: DrawRecord) {
    val attributes = DrawAttributes.calculate(draw.number)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.draw_row_summary, draw.issue, draw.drawDate),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    stringResource(
                        R.string.draw_row_attributes,
                        attributes.sum,
                        attributes.span,
                        groupShapeLabel(attributes.groupShape),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = draw.number.value,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        OutlinedButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun groupShapeLabel(shape: GroupShape): String = when (shape) {
    GroupShape.LEOPARD -> stringResource(R.string.group_leopard)
    GroupShape.GROUP3 -> stringResource(R.string.group_3)
    GroupShape.GROUP6 -> stringResource(R.string.group_6)
}

@Composable
private fun failureMessage(type: String?): String = when {
    type == "NETWORK" -> stringResource(R.string.sync_error_network)
    type == "INVALID_PAYLOAD" -> stringResource(R.string.sync_error_payload)
    type == "EMPTY_RESPONSE" -> stringResource(R.string.sync_error_empty)
    type?.startsWith("HTTP_") == true -> stringResource(R.string.sync_error_http)
    else -> stringResource(R.string.sync_error_generic)
}

private fun formatEpoch(epochMillis: Long): String = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(epochMillis))
