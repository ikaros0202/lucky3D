package com.lucky3d.app.feature.scheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucky3d.app.R
import com.lucky3d.app.core.ui.EmptyState
import com.lucky3d.app.core.ui.InlineMessage
import com.lucky3d.app.core.ui.InlineStatusBanner
import com.lucky3d.app.core.ui.MessageKind
import com.lucky3d.app.data.repository.SavedScheme
import com.lucky3d.app.data.repository.SavedTemplate
import com.lucky3d.app.data.repository.SchemeWithReplay
import com.lucky3d.app.domain.filter.PlayType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeScreen(
    state: SchemeUiState,
    onShowSection: (SchemeSection) -> Unit,
    onSelectScheme: (String?) -> Unit,
    onSelectTemplate: (String?) -> Unit,
    onSetBacktestRange: (String, String) -> Unit,
    onRunBacktest: () -> Unit,
    onCopyScheme: (String, String) -> Unit,
    onUpdateNote: (String, String) -> Unit,
    onDismissStatus: () -> Unit,
    onStartPick: () -> Unit,
    modifier: Modifier = Modifier,
    onSetQuery: (String) -> Unit = {},
    onSetFilter: (SchemeFilter) -> Unit = {},
) {
    val selectedItem = state.schemes.firstOrNull {
        it.scheme.id == state.selectedSchemeId
    }
    if (selectedItem != null) {
        FlowingSchemeDetailScreen(
            item = selectedItem,
            onBack = { onSelectScheme(null) },
            onCopy = { issue -> onCopyScheme(selectedItem.scheme.id, issue) },
            onUpdateNote = { note -> onUpdateNote(selectedItem.scheme.id, note) },
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        FlowingSchemeHeader()
        SchemeSectionSwitch(
            state = state,
            onShowSection = onShowSection,
        )
        if (state.operationSucceeded || state.operationFailed) {
            CompactOperationStatus(
                succeeded = state.operationSucceeded,
                onDismiss = onDismissStatus,
            )
        }
        when (state.section) {
            SchemeSection.SCHEMES -> SchemeListContent(
                state = state,
                onSetQuery = onSetQuery,
                onSetFilter = onSetFilter,
                onSelectScheme = onSelectScheme,
                onStartPick = onStartPick,
                modifier = Modifier.weight(1f),
            )
            SchemeSection.TEMPLATES -> TemplateListContent(
                state = state,
                onSelectTemplate = onSelectTemplate,
                onSetBacktestRange = onSetBacktestRange,
                onRunBacktest = onRunBacktest,
                onStartPick = onStartPick,
                modifier = Modifier.weight(1f),
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacySchemeScreen(
    state: SchemeUiState,
    onShowSection: (SchemeSection) -> Unit,
    onSelectScheme: (String?) -> Unit,
    onSelectTemplate: (String?) -> Unit,
    onSetBacktestRange: (String, String) -> Unit,
    onRunBacktest: () -> Unit,
    onCopyScheme: (String, String) -> Unit,
    onUpdateNote: (String, String) -> Unit,
    onDismissStatus: () -> Unit,
    onStartPick: () -> Unit,
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
            Text(
                stringResource(R.string.scheme_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SchemeSection.entries.forEach { section ->
                    FilterChip(
                        selected = state.section == section,
                        onClick = { onShowSection(section) },
                        label = {
                            Text(
                                stringResource(
                                    if (section == SchemeSection.SCHEMES) {
                                        R.string.scheme_section_schemes
                                    } else {
                                        R.string.scheme_section_templates
                                    },
                                ),
                            )
                        },
                    )
                }
            }
        }
        if (state.operationSucceeded) {
            item {
                InlineStatusBanner(
                    message = InlineMessage(
                        title = stringResource(R.string.operation_success),
                        kind = MessageKind.SUCCESS,
                        actionLabel = stringResource(R.string.dismiss),
                    ),
                    onAction = onDismissStatus,
                )
            }
        }
        if (state.operationFailed) {
            item {
                InlineStatusBanner(
                    message = InlineMessage(
                        title = stringResource(R.string.operation_failed),
                        kind = MessageKind.ERROR,
                        actionLabel = stringResource(R.string.dismiss),
                    ),
                    onAction = onDismissStatus,
                )
            }
        }
        when (state.section) {
            SchemeSection.SCHEMES -> {
                if (state.schemes.isEmpty()) {
                    item {
                        EmptyState(
                            title = stringResource(R.string.scheme_empty_title),
                            detail = stringResource(R.string.scheme_empty_detail),
                            actionLabel = stringResource(R.string.scheme_start_pick),
                            onAction = onStartPick,
                        )
                    }
                } else {
                    items(state.schemes, key = { it.scheme.id }) { item ->
                        SchemeCard(item = item, onClick = { onSelectScheme(item.scheme.id) })
                    }
                }
            }
            SchemeSection.TEMPLATES -> {
                if (state.templates.isEmpty()) {
                    item {
                        EmptyState(
                            title = stringResource(R.string.scheme_template_empty_title),
                            detail = stringResource(R.string.scheme_template_empty_detail),
                            actionLabel = stringResource(R.string.scheme_start_pick),
                            onAction = onStartPick,
                        )
                    }
                } else {
                    items(state.templates, key = SavedTemplate::id) { template ->
                        TemplateCard(
                            template = template,
                            selected = state.selectedTemplateId == template.id,
                            onClick = { onSelectTemplate(template.id) },
                        )
                    }
                    item {
                        BacktestSection(
                            startIssue = state.backtestStartIssue,
                            endIssue = state.backtestEndIssue,
                            isRunning = state.isBacktesting,
                            report = state.backtest,
                            onRangeChange = onSetBacktestRange,
                            onRun = onRunBacktest,
                        )
                    }
                    items(
                        items = state.backtest?.results.orEmpty(),
                        key = { it.targetIssue },
                    ) { result ->
                        BacktestResultCard(result)
                    }
                }
            }
        }
    }

    state.schemes
        .firstOrNull { it.scheme.id == state.selectedSchemeId }
        ?.let { item ->
            SchemeDetailSheet(
                item = item,
                onDismiss = { onSelectScheme(null) },
                onCopy = { issue -> onCopyScheme(item.scheme.id, issue) },
                onUpdateNote = { note -> onUpdateNote(item.scheme.id, note) },
            )
        }
}

@Composable
private fun FlowingSchemeHeader() {
    val primary = MaterialTheme.colorScheme.primary
    val primaryDeep = com.lucky3d.app.core.ui.Lucky3dDesign.colors.primaryDeep
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            val redArc = Path().apply {
                moveTo(size.width * 0.58f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height * 0.42f)
                cubicTo(
                    size.width * 0.86f,
                    size.height * 0.76f,
                    size.width * 0.67f,
                    size.height * 0.62f,
                    size.width * 0.58f,
                    0f,
                )
                close()
            }
            drawPath(
                redArc,
                brush = Brush.horizontalGradient(
                    listOf(
                        primary,
                        primaryDeep,
                    ),
                ),
            )
            repeat(3) { index ->
                drawArc(
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f),
                    startAngle = 160f,
                    sweepAngle = 88f,
                    useCenter = false,
                    topLeft = Offset(
                        size.width * (0.55f + index * 0.04f),
                        -size.height * (0.52f - index * 0.08f),
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        size.width * 0.55f,
                        size.height * 1.25f,
                    ),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Text(
            text = stringResource(R.string.scheme_title),
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SchemeSectionSwitch(
    state: SchemeUiState,
    onShowSection: (SchemeSection) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SchemeSection.entries.forEach { section ->
                val selected = state.section == section
                val count = if (section == SchemeSection.SCHEMES) {
                    state.schemes.size
                } else {
                    state.templates.size
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onShowSection(section) }
                        .semantics { this.selected = selected },
                    color = if (selected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = if (selected) 2.dp else 0.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(
                                if (section == SchemeSection.SCHEMES) {
                                    R.string.scheme_section_count
                                } else {
                                    R.string.scheme_template_section_count
                                },
                                count,
                            ),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactOperationStatus(
    succeeded: Boolean,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clickable(onClick = onDismiss),
        color = if (succeeded) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(
                    if (succeeded) R.string.operation_success else R.string.operation_failed,
                ),
                color = if (succeeded) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun SchemeListContent(
    state: SchemeUiState,
    onSetQuery: (String) -> Unit,
    onSetFilter: (SchemeFilter) -> Unit,
    onSelectScheme: (String?) -> Unit,
    onStartPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onSetQuery,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 12.dp, vertical = 2.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.scheme_search_hint),
                    fontSize = 11.sp,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ),
        )
        SchemeFilterBar(
            selected = state.filter,
            onSetFilter = onSetFilter,
        )
        val replayedCount = state.schemes.count { it.replay != null }
        if (replayedCount > 0) {
            ReplaySummaryBanner(count = replayedCount)
        }
        if (state.schemes.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.scheme_empty_title),
                detail = stringResource(R.string.scheme_empty_detail),
                actionLabel = stringResource(R.string.scheme_start_pick),
                onAction = onStartPick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
            val pending = state.visibleSchemes.filter { it.replay == null }
            val replayed = state.visibleSchemes.filter { it.replay != null }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 2.dp),
            ) {
                if (pending.isNotEmpty()) {
                    item {
                        SchemeGroupHeader(
                            title = stringResource(R.string.scheme_group_pending),
                            count = pending.size,
                        )
                    }
                    items(pending, key = { it.scheme.id }) { item ->
                        CompactSchemeRow(
                            item = item,
                            onClick = { onSelectScheme(item.scheme.id) },
                        )
                    }
                }
                if (replayed.isNotEmpty()) {
                    item {
                        SchemeGroupHeader(
                            title = stringResource(R.string.scheme_group_replayed),
                            count = replayed.size,
                        )
                    }
                    items(replayed, key = { it.scheme.id }) { item ->
                        CompactSchemeRow(
                            item = item,
                            onClick = { onSelectScheme(item.scheme.id) },
                        )
                    }
                }
                if (pending.isEmpty() && replayed.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.scheme_search_empty),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SchemeFilterBar(
    selected: SchemeFilter,
    onSetFilter: (SchemeFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SchemeFilter.entries.forEach { filter ->
            val isSelected = selected == filter
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSetFilter(filter) }
                    .semantics { this.selected = isSelected },
                shape = RoundedCornerShape(50),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = BorderStroke(
                    1.dp,
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(
                            when (filter) {
                                SchemeFilter.ALL -> R.string.scheme_filter_all
                                SchemeFilter.PENDING -> R.string.scheme_filter_pending
                                SchemeFilter.REPLAYED -> R.string.scheme_filter_replayed
                            },
                        ),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplaySummaryBanner(count: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(9.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.scheme_replay_summary, count),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 7.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.view),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SchemeGroupHeader(
    title: String,
    count: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = count.toString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun CompactSchemeRow(
    item: SchemeWithReplay,
    onClick: () -> Unit,
) {
    val replay = item.replay
    val description = if (replay == null) {
        "${item.scheme.issue}期，${item.scheme.title}，等待开奖"
    } else {
        val result = if (replay.covered) "包含" else "未包含"
        "${item.scheme.issue}期，${item.scheme.title}，${result}开奖号${replay.winningNumber.value}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (replay == null) 78.dp else 96.dp)
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.scheme_issue, item.scheme.issue),
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.scheme.title,
                    modifier = Modifier.padding(start = 7.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            Text(
                text = stringResource(
                    R.string.scheme_compact_summary,
                    playTypeLabel(item.scheme.playType),
                    item.scheme.betCount,
                    item.scheme.amountYuan,
                    item.scheme.candidates.size,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                maxLines = 1,
            )
            if (replay != null) {
                CompactWinningDigits(replay.winningNumber.value)
            }
        }
        if (replay == null) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                StatusPill(
                    text = stringResource(R.string.scheme_waiting),
                    icon = Icons.Outlined.Schedule,
                    covered = null,
                )
                Text(
                    text = stringResource(R.string.scheme_view_detail),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            StatusPill(
                text = stringResource(
                    if (replay.covered) {
                        R.string.scheme_covered_short
                    } else {
                        R.string.scheme_not_covered_short
                    },
                ),
                icon = Icons.Outlined.CheckCircle,
                covered = replay.covered,
            )
        }
    }
}

@Composable
private fun CompactWinningDigits(number: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        number.forEach { digit ->
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = digit.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    covered: Boolean?,
) {
    val container = when (covered) {
        true -> MaterialTheme.colorScheme.primaryContainer
        false -> MaterialTheme.colorScheme.surfaceVariant
        null -> MaterialTheme.colorScheme.secondaryContainer
    }
    val content = when (covered) {
        true -> MaterialTheme.colorScheme.onPrimaryContainer
        false -> MaterialTheme.colorScheme.onSurfaceVariant
        null -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = text,
                modifier = Modifier.padding(start = 4.dp),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FlowingSchemeDetailScreen(
    item: SchemeWithReplay,
    onBack: () -> Unit,
    onCopy: (String) -> Unit,
    onUpdateNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCopyDialog by rememberSaveable(item.scheme.id) { mutableStateOf(false) }
    var showNoteDialog by rememberSaveable(item.scheme.id) { mutableStateOf(false) }
    var copyIssue by rememberSaveable(item.scheme.id) { mutableStateOf("") }
    var noteDraft by rememberSaveable(item.scheme.id) { mutableStateOf(item.scheme.note) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        FlowingDetailHeader(
            onBack = onBack,
            onMore = { showNoteDialog = true },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SchemeReplayHero(item)
            SchemeStatsCard(item.scheme)
            SchemeCandidatesCard(item.scheme)
            SchemeConditionsCard(item.scheme)
            SchemeNoteCard(item.scheme)
            if (item.scheme.isDrawn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.scheme_detail_locked),
                        modifier = Modifier.padding(start = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                    )
                }
            }
        }
        Button(
            onClick = { showCopyDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(58.dp)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.scheme_copy_current),
                modifier = Modifier.padding(start = 8.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    if (showCopyDialog) {
        AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            title = { Text(stringResource(R.string.scheme_copy_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.scheme_copy_dialog_support),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                    OutlinedTextField(
                        value = copyIssue,
                        onValueChange = { copyIssue = it.filter(Char::isDigit).take(7) },
                        label = { Text(stringResource(R.string.scheme_copy_issue)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCopy(copyIssue)
                        showCopyDialog = false
                    },
                    enabled = copyIssue.length == 7,
                ) {
                    Text(stringResource(R.string.scheme_copy_current))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCopyDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text(stringResource(R.string.scheme_edit_note)) },
            text = {
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    label = { Text(stringResource(R.string.scheme_note)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateNote(noteDraft)
                        showNoteDialog = false
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun FlowingDetailHeader(
    onBack: () -> Unit,
    onMore: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryDeep = com.lucky3d.app.core.ui.Lucky3dDesign.colors.primaryDeep
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            val arc = Path().apply {
                moveTo(size.width * 0.58f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height * 0.36f)
                cubicTo(
                    size.width * 0.86f,
                    size.height * 0.70f,
                    size.width * 0.68f,
                    size.height * 0.58f,
                    size.width * 0.58f,
                    0f,
                )
                close()
            }
            drawPath(
                arc,
                brush = Brush.horizontalGradient(listOf(primary, primaryDeep)),
            )
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.close),
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onBack)
                    .padding(13.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.scheme_detail),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = stringResource(R.string.scheme_more_actions),
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onMore)
                    .padding(13.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SchemeReplayHero(item: SchemeWithReplay) {
    val replay = item.replay
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(14.dp),
    ) {
        Box {
            Canvas(modifier = Modifier.fillMaxSize()) {
                repeat(2) { index ->
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color.White.copy(
                            alpha = 0.08f + index * 0.03f,
                        ),
                        radius = size.width * (0.28f + index * 0.08f),
                        center = Offset(
                            size.width * 0.88f,
                            size.height * (1.12f + index * 0.08f),
                        ),
                        style = Stroke(width = 14.dp.toPx()),
                    )
                }
            }
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            R.string.scheme_detail_issue_play,
                            item.scheme.issue,
                            playTypeLabel(item.scheme.playType),
                        ),
                        modifier = Modifier.weight(1f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(
                            if (replay == null) {
                                R.string.scheme_waiting
                            } else {
                                R.string.scheme_detail_replayed
                            },
                        ),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = item.scheme.title,
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (replay != null) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            replay.winningNumber.value.forEach { digit ->
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onPrimary,
                                            CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = digit.toString(),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                        Surface(
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f),
                            ),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = stringResource(
                                    if (replay.covered) {
                                        R.string.scheme_covered_short
                                    } else {
                                        R.string.scheme_not_covered_short
                                    },
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SchemeStatsCard(scheme: SavedScheme) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailStat(
                label = stringResource(R.string.scheme_stat_bets),
                value = stringResource(R.string.scheme_stat_bets_value, scheme.betCount),
                modifier = Modifier.weight(1f),
            )
            DetailStatDivider()
            DetailStat(
                label = stringResource(R.string.scheme_stat_multiplier),
                value = stringResource(R.string.scheme_stat_multiplier_value, scheme.multiplier),
                modifier = Modifier.weight(1f),
            )
            DetailStatDivider()
            DetailStat(
                label = stringResource(R.string.scheme_stat_amount),
                value = stringResource(R.string.scheme_stat_amount_value, scheme.amountYuan),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DetailStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DetailStatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun SchemeCandidatesCard(scheme: SavedScheme) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.scheme_candidates_title),
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(
                        R.string.scheme_candidates_compact,
                        scheme.candidates.size,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                scheme.candidates.take(6).forEach { candidate ->
                    DetailChip(text = candidate.value)
                }
                if (scheme.candidates.size > 6) {
                    DetailChip(
                        text = stringResource(
                            R.string.scheme_candidates_more,
                            scheme.candidates.size - 6,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SchemeConditionsCard(scheme: SavedScheme) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.scheme_conditions),
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(
                        R.string.scheme_conditions_observation,
                        scheme.observationWindow,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                val conditionTitles = scheme.conditions.map { it.title }
                    .ifEmpty { listOf(stringResource(R.string.scheme_conditions_none)) }
                conditionTitles.take(5).forEach { title ->
                    DetailChip(text = title)
                }
            }
        }
    }
}

@Composable
private fun SchemeNoteCard(scheme: SavedScheme) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = stringResource(R.string.scheme_note),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = scheme.note.ifBlank {
                    stringResource(R.string.scheme_note_empty)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun DetailChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(7.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TemplateListContent(
    state: SchemeUiState,
    onSelectTemplate: (String?) -> Unit,
    onSetBacktestRange: (String, String) -> Unit,
    onRunBacktest: () -> Unit,
    onStartPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.templates.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.scheme_template_empty_title),
            detail = stringResource(R.string.scheme_template_empty_detail),
            actionLabel = stringResource(R.string.scheme_start_pick),
            onAction = onStartPick,
            modifier = modifier.fillMaxWidth(),
        )
        return
    }
    var expandedBacktestTemplateId by rememberSaveable { mutableStateOf<String?>(null) }
    val commonTemplates = state.templates.take(2)
    val otherTemplates = state.templates.drop(2)
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 2.dp),
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.scheme_template_explanation),
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                    )
                }
            }
        }
        item {
            SchemeGroupHeader(
                title = stringResource(R.string.scheme_template_group_common),
                count = commonTemplates.size,
            )
        }
        items(commonTemplates, key = SavedTemplate::id) { template ->
            CompactTemplateRow(
                template = template,
                onBacktest = {
                    onSelectTemplate(template.id)
                    expandedBacktestTemplateId =
                        template.id.takeUnless { it == expandedBacktestTemplateId }
                },
                onApply = {
                    onSelectTemplate(template.id)
                    onStartPick()
                },
            )
            if (expandedBacktestTemplateId == template.id) {
                TemplateBacktestPanel(
                    state = state,
                    onSetBacktestRange = onSetBacktestRange,
                    onRunBacktest = onRunBacktest,
                )
            }
        }
        if (otherTemplates.isNotEmpty()) {
            item {
                SchemeGroupHeader(
                    title = stringResource(R.string.scheme_template_group_all),
                    count = otherTemplates.size,
                )
            }
            items(otherTemplates, key = SavedTemplate::id) { template ->
                CompactTemplateRow(
                    template = template,
                    onBacktest = {
                        onSelectTemplate(template.id)
                        expandedBacktestTemplateId =
                            template.id.takeUnless { it == expandedBacktestTemplateId }
                    },
                    onApply = {
                        onSelectTemplate(template.id)
                        onStartPick()
                    },
                )
                if (expandedBacktestTemplateId == template.id) {
                    TemplateBacktestPanel(
                        state = state,
                        onSetBacktestRange = onSetBacktestRange,
                        onRunBacktest = onRunBacktest,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactTemplateRow(
    template: SavedTemplate,
    onBacktest: () -> Unit,
    onApply: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = template.name,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = playTypeLabel(template.playType),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = stringResource(
                R.string.scheme_template_compact_summary,
                template.observationWindow,
                template.conditions.size,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            lineHeight = 11.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onBacktest,
                modifier = Modifier.height(34.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = stringResource(R.string.backtest_history_action),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Button(
                onClick = onApply,
                modifier = Modifier
                    .height(34.dp)
                    .padding(start = 6.dp),
                contentPadding = PaddingValues(horizontal = 11.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = stringResource(R.string.scheme_apply_current),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TemplateBacktestPanel(
    state: SchemeUiState,
    onSetBacktestRange: (String, String) -> Unit,
    onRunBacktest: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(10.dp),
    ) {
        BacktestSection(
            startIssue = state.backtestStartIssue,
            endIssue = state.backtestEndIssue,
            isRunning = state.isBacktesting,
            report = state.backtest,
            onRangeChange = onSetBacktestRange,
            onRun = onRunBacktest,
        )
    }
}

@Composable
private fun playTypeLabel(playType: PlayType): String = stringResource(
    when (playType) {
        PlayType.STRAIGHT -> R.string.play_straight
        PlayType.GROUP3 -> R.string.play_group3
        PlayType.GROUP6 -> R.string.play_group6
    },
)

@Composable
private fun SchemeCard(
    item: SchemeWithReplay,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(R.string.scheme_issue, item.scheme.issue),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(item.scheme.title, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.scheme_summary,
                    item.scheme.betCount,
                    item.scheme.multiplier,
                    item.scheme.amountYuan,
                ),
            )
            Text(
                replayStatus(item),
                style = MaterialTheme.typography.bodySmall,
                color = if (item.replay?.covered == true) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: SavedTemplate,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(template.name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.scheme_template_window,
                    template.observationWindow,
                    template.ruleVersion,
                ),
            )
            Text(template.conditions.joinToString(" · ") { it.title })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchemeDetailSheet(
    item: SchemeWithReplay,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
    onUpdateNote: (String) -> Unit,
) {
    var note by rememberSaveable(item.scheme.id) { mutableStateOf(item.scheme.note) }
    var copyIssue by rememberSaveable(item.scheme.id) { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.scheme_detail), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.scheme_issue, item.scheme.issue),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(item.scheme.title, style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(
                    R.string.scheme_summary,
                    item.scheme.betCount,
                    item.scheme.multiplier,
                    item.scheme.amountYuan,
                ),
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(replayStatus(item), modifier = Modifier.padding(12.dp))
            }
            Text(stringResource(R.string.scheme_conditions), style = MaterialTheme.typography.titleMedium)
            Text(item.scheme.conditions.joinToString("\n") { "• ${it.title}" })
            Text(
                stringResource(R.string.scheme_candidates, item.scheme.candidates.size),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                item.scheme.candidates.joinToString(" ") { it.value },
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
            HorizontalDivider()
            if (item.scheme.isDrawn) {
                Text(
                    stringResource(R.string.scheme_drawn_locked),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.scheme_note)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onUpdateNote(note) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.scheme_save_note))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = copyIssue,
                    onValueChange = { copyIssue = it.filter(Char::isDigit).take(7) },
                    label = { Text(stringResource(R.string.scheme_copy_issue)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = { onCopy(copyIssue) },
                    enabled = copyIssue.length == 7,
                ) {
                    Text(stringResource(R.string.scheme_copy))
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.close))
            }
        }
    }
}

@Composable
private fun replayStatus(item: SchemeWithReplay): String {
    val replay = item.replay ?: return stringResource(R.string.scheme_waiting)
    return stringResource(
        if (replay.covered) R.string.scheme_covered else R.string.scheme_not_covered,
        replay.winningNumber.value,
    )
}
