package com.lucky3d.app.feature.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lucky3d.app.R
import com.lucky3d.app.core.ui.InlineMessage
import com.lucky3d.app.core.ui.InlineStatusBanner
import com.lucky3d.app.core.ui.MessageKind
import com.lucky3d.app.domain.filter.Position
import com.lucky3d.app.domain.replay.ReminderType

@Composable
fun SettingsScreen(
    state: UserPreferences,
    onBack: () -> Unit,
    onSetDefaultObservationWindow: (Int) -> Unit,
    onSetReminderEnabled: (ReminderType, Boolean) -> Unit,
    onSetOmissionRule: (Position, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
                Text(
                    stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        item {
            SectionTitle(stringResource(R.string.settings_analysis))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(10, 30, 60, 100).forEach { window ->
                    FilterChip(
                        selected = state.defaultObservationWindow == window,
                        onClick = { onSetDefaultObservationWindow(window) },
                        label = { Text(stringResource(R.string.trend_window, window)) },
                    )
                }
            }
        }
        item { HorizontalDivider() }
        item { SectionTitle(stringResource(R.string.settings_reminders)) }
        item {
            ReminderSettingRow(
                title = stringResource(R.string.settings_draw_reminder),
                detail = stringResource(R.string.settings_draw_reminder_detail),
                enabled = state.drawReminderEnabled,
                onEnabledChange = {
                    onSetReminderEnabled(ReminderType.DRAW, it)
                },
            )
        }
        item {
            ReminderSettingRow(
                title = stringResource(R.string.settings_replay_reminder),
                detail = stringResource(R.string.settings_replay_reminder_detail),
                enabled = state.replayReminderEnabled,
                onEnabledChange = {
                    onSetReminderEnabled(ReminderType.REPLAY, it)
                },
            )
        }
        item {
            ReminderSettingRow(
                title = stringResource(R.string.settings_omission_reminder),
                detail = stringResource(R.string.settings_omission_reminder_detail),
                enabled = state.omissionReminderEnabled,
                onEnabledChange = {
                    onSetReminderEnabled(ReminderType.OMISSION, it)
                },
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.settings_omission_rule),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Position.entries.forEach { position ->
                        FilterChip(
                            selected = state.omissionPosition == position,
                            onClick = {
                                onSetOmissionRule(
                                    position,
                                    state.omissionDigit,
                                    state.omissionThreshold,
                                )
                            },
                            label = { Text(positionLabel(position)) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.omissionDigit.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { digit ->
                                onSetOmissionRule(
                                    state.omissionPosition,
                                    digit,
                                    state.omissionThreshold,
                                )
                            }
                        },
                        label = { Text(stringResource(R.string.settings_omission_digit)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.omissionThreshold.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { threshold ->
                                onSetOmissionRule(
                                    state.omissionPosition,
                                    state.omissionDigit,
                                    threshold,
                                )
                            }
                        },
                        label = { Text(stringResource(R.string.settings_omission_threshold)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            InlineStatusBanner(
                message = InlineMessage(
                    kind = MessageKind.SUCCESS,
                    title = stringResource(R.string.settings_saved),
                ),
            )
        }
        item { HorizontalDivider() }
        item {
            SectionTitle(stringResource(R.string.settings_privacy))
            Text(
                stringResource(R.string.settings_privacy_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun ReminderSettingRow(
    title: String,
    detail: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
private fun positionLabel(position: Position): String = stringResource(
    when (position) {
        Position.HUNDREDS -> R.string.trend_position_hundreds
        Position.TENS -> R.string.trend_position_tens
        Position.ONES -> R.string.trend_position_ones
    },
)
