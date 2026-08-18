package com.lucky3d.app.app.navigation

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.lucky3d.app.R
import com.lucky3d.app.feature.caibao.CaibaoRoute
import com.lucky3d.app.feature.home.HistoryScreen
import com.lucky3d.app.feature.home.HistoryViewModel
import com.lucky3d.app.feature.home.HomeScreen
import com.lucky3d.app.feature.home.HomeViewModel
import com.lucky3d.app.feature.pick.PickScreen
import com.lucky3d.app.feature.pick.PickPersistenceViewModel
import com.lucky3d.app.feature.pick.PickViewModel
import com.lucky3d.app.feature.scheme.SchemeScreen
import com.lucky3d.app.feature.scheme.SchemeViewModel
import com.lucky3d.app.feature.settings.ReminderViewModel
import com.lucky3d.app.feature.settings.SettingsScreen
import com.lucky3d.app.feature.settings.SettingsViewModel
import com.lucky3d.app.feature.trend.TrendScreen
import com.lucky3d.app.feature.trend.TrendViewModel
import kotlinx.serialization.Serializable

@Serializable
private data object RootDestination : NavKey

@Serializable
private data class HistoryDestination(
    val issue: String? = null,
    val date: String? = null,
) : NavKey

@Serializable
private data object SettingsDestination : NavKey

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(RootDestination)
    val context = LocalContext.current
    val view = LocalView.current
    val topDestination = backStack.lastOrNull()
    SideEffect {
        val window = (context as? Activity)?.window
        window?.let { currentWindow ->
            WindowCompat.getInsetsController(currentWindow, view).isAppearanceLightStatusBars =
                topDestination !is HistoryDestination
        }
    }

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<RootDestination> {
                MainShell(
                    onOpenHistory = { issue, date ->
                        backStack.add(HistoryDestination(issue = issue, date = date))
                    },
                    onOpenSettings = { backStack.add(SettingsDestination) },
                )
            }
            entry<HistoryDestination> { destination ->
                HistoryRoute(
                    issue = destination.issue,
                    date = destination.date,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
            entry<SettingsDestination> {
                SettingsRoute(
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}

@Composable
private fun MainShell(
    onOpenHistory: (issue: String?, date: String?) -> Unit,
    onOpenSettings: () -> Unit,
    reminderViewModel: ReminderViewModel = hiltViewModel(),
) {
    var selectedTabName by rememberSaveable { mutableStateOf(MainTab.HOME.name) }
    var schemeDetailVisible by rememberSaveable { mutableStateOf(false) }
    val selectedTab = MainTab.valueOf(selectedTabName)
    val stateHolder = rememberSaveableStateHolder()
    val reminderState by reminderViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentReminder = reminderState.pending.firstOrNull()
    LaunchedEffect(currentReminder?.key) {
        if (currentReminder != null) {
            snackbarHostState.showSnackbar(currentReminder.message)
            reminderViewModel.dismissCurrent()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!(selectedTab == MainTab.PLANS && schemeDetailVisible)) {
                if (selectedTab == MainTab.HOME) {
                    ApprovedHomeNavigationBar(
                        onSelect = { selectedTabName = it.name },
                    )
                } else {
                    StandardNavigationBar(
                        selectedTab = selectedTab,
                        onSelect = { selectedTabName = it.name },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            stateHolder.SaveableStateProvider(selectedTab.stateKey) {
                when (selectedTab) {
                    MainTab.HOME -> HomeRoute(
                        onOpenHistory = onOpenHistory,
                        onOpenSettings = onOpenSettings,
                    )
                    MainTab.TREND -> TrendRoute()
                    MainTab.PICK -> PickRoute()
                    MainTab.PLANS -> SchemeRoute(
                        onStartPick = { selectedTabName = MainTab.PICK.name },
                        onDetailVisibilityChanged = { schemeDetailVisible = it },
                    )
                    MainTab.CAIBAO -> CaibaoRoute()
                }
            }
        }
    }
}

@Composable
private fun ApprovedHomeNavigationBar(
    onSelect: (MainTab) -> Unit,
) {
    Surface(
        modifier = Modifier.navigationBarsPadding(),
        color = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1080f / 200f),
        ) {
            Image(
                painter = painterResource(R.drawable.home_crystal_navigation_shell),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            Row(modifier = Modifier.fillMaxSize()) {
                MainTab.entries.forEach { tab ->
                    val tabAccessibilityLabel =
                        androidx.compose.ui.res.stringResource(tab.accessibilityLabelRes)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelect(tab) }
                            .semantics {
                                contentDescription = tabAccessibilityLabel
                                selected = tab == MainTab.HOME
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun StandardNavigationBar(
    selectedTab: MainTab,
    onSelect: (MainTab) -> Unit,
) {
    Surface(
        modifier = Modifier.navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            MainTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                val tabLabel = androidx.compose.ui.res.stringResource(tab.labelRes)
                val tabAccessibilityLabel =
                    androidx.compose.ui.res.stringResource(tab.accessibilityLabelRes)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(tab) }
                        .semantics {
                            contentDescription = tabAccessibilityLabel
                            this.selected = selected
                        }
                        .padding(top = 3.dp, bottom = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        2.dp,
                        Alignment.CenterVertically,
                    ),
                ) {
                    if (selected && tab == MainTab.HOME) {
                        CrystalHomeIcon()
                    } else {
                        Icon(
                            imageVector = tab.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    Text(
                        text = tabLabel,
                        fontWeight = if (selected) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Medium
                        },
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .background(
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CrystalHomeIcon() {
    val primary = MaterialTheme.colorScheme.primary
    val highlight = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f)
    Canvas(modifier = Modifier.size(24.dp)) {
        val gem = Path().apply {
            moveTo(size.width * 0.50f, 0f)
            lineTo(size.width * 0.92f, size.height * 0.30f)
            lineTo(size.width * 0.78f, size.height * 0.92f)
            lineTo(size.width * 0.22f, size.height * 0.92f)
            lineTo(size.width * 0.08f, size.height * 0.30f)
            close()
        }
        val facet = Path().apply {
            moveTo(size.width * 0.50f, 0f)
            lineTo(size.width * 0.50f, size.height * 0.92f)
            lineTo(size.width * 0.08f, size.height * 0.30f)
            close()
        }
        drawPath(gem, primary)
        drawPath(facet, highlight)
    }
}

@Composable
private fun HomeRoute(
    onOpenHistory: (issue: String?, date: String?) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onHomeVisible()
                Lifecycle.Event.ON_STOP -> viewModel.onHomeHidden()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.onHomeVisible()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onHomeHidden()
        }
    }
    HomeScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onRefreshTrial = viewModel::refreshTrial,
        onOpenIssue = { issue -> onOpenHistory(issue, null) },
        onOpenSettings = onOpenSettings,
    )
}

@Composable
private fun HistoryRoute(
    issue: String?,
    date: String?,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(issue, date) {
        issue?.let(viewModel::searchIssue)
    }
    HistoryScreen(
        state = state,
        onBack = onBack,
        onSearchIssue = viewModel::searchIssue,
    )
}

@Composable
private fun TrendRoute(
    viewModel: TrendViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    var defaultsApplied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settings.defaultObservationWindow, defaultsApplied) {
        if (!defaultsApplied) {
            val approvedTrendWindow = settings.defaultObservationWindow
                .takeIf { it == 10 || it == 30 || it == 60 || it == 100 }
                ?: 30
            viewModel.setWindow(approvedTrendWindow)
            defaultsApplied = true
        }
    }
    TrendScreen(
        state = state,
        onSetWindow = viewModel::setWindow,
        onSetScale = viewModel::setScale,
        onSelectPoint = viewModel::selectPoint,
    )
}

@Composable
private fun PickRoute(
    viewModel: PickViewModel = hiltViewModel(),
    persistenceViewModel: PickPersistenceViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val persistenceState by persistenceViewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
    var defaultsApplied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settings.defaultObservationWindow, defaultsApplied) {
        if (!defaultsApplied) {
            viewModel.setObservationWindow(settings.defaultObservationWindow)
            defaultsApplied = true
        }
    }
    LaunchedEffect(persistenceState.suggestedIssue, state.targetIssue) {
        if (state.targetIssue.isEmpty() && persistenceState.suggestedIssue.isNotEmpty()) {
            viewModel.setTargetIssue(persistenceState.suggestedIssue)
        }
    }
    PickScreen(
        state = state,
        onSetTargetIssue = viewModel::setTargetIssue,
        onSetObservationWindow = viewModel::setObservationWindow,
        onSetPlayType = viewModel::setPlayType,
        onSetMode = viewModel::setMode,
        onSelectManualPosition = viewModel::selectManualPosition,
        onSelectManualDigit = viewModel::selectManualDigit,
        onRemoveManualBet = viewModel::removeManualBet,
        onClearManual = viewModel::clearManual,
        onAddCondition = { viewModel.addCondition(it) },
        onEditCondition = viewModel::editCondition,
        onSetConditionEnabled = viewModel::setConditionEnabled,
        onRemoveCondition = viewModel::removeCondition,
        onSetDanDigits = viewModel::setDanDigits,
        onSetTuoDigits = viewModel::setTuoDigits,
        onSetMultiplier = viewModel::setMultiplier,
        onUndo = viewModel::undoLastChange,
        saveStatus = persistenceState.saveStatus,
        onDismissSaveStatus = persistenceViewModel::dismissSaveStatus,
        onSaveTemplate = { name -> persistenceViewModel.saveTemplate(name, state) },
        onSaveScheme = { title, note ->
            persistenceViewModel.saveScheme(title, note, state)
        },
    )
}

@Composable
private fun SchemeRoute(
    onStartPick: () -> Unit,
    onDetailVisibilityChanged: (Boolean) -> Unit,
    viewModel: SchemeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.selectedSchemeId) {
        onDetailVisibilityChanged(state.selectedSchemeId != null)
    }
    SchemeScreen(
        state = state,
        onShowSection = viewModel::showSection,
        onSelectScheme = viewModel::selectScheme,
        onSelectTemplate = viewModel::selectTemplate,
        onSetBacktestRange = viewModel::setBacktestRange,
        onRunBacktest = viewModel::runBacktest,
        onCopyScheme = viewModel::copyScheme,
        onUpdateNote = viewModel::updateNote,
        onDismissStatus = viewModel::dismissOperationStatus,
        onStartPick = onStartPick,
        onSetQuery = viewModel::setQuery,
        onSetFilter = viewModel::setFilter,
    )
}

@Composable
private fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBack = onBack,
        onSetDefaultObservationWindow = viewModel::setDefaultObservationWindow,
        onSetReminderEnabled = viewModel::setReminderEnabled,
        onSetOmissionRule = viewModel::setOmissionRule,
    )
}

@Composable
private fun PendingFeatureScreen(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = androidx.compose.ui.res.stringResource(R.string.feature_pending),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun MainTab.icon(): ImageVector = when (this) {
    MainTab.HOME -> Icons.Outlined.Home
    MainTab.TREND -> Icons.AutoMirrored.Outlined.ShowChart
    MainTab.PICK -> Icons.Outlined.Tune
    MainTab.PLANS -> Icons.Outlined.FolderOpen
    MainTab.CAIBAO -> Icons.AutoMirrored.Outlined.Article
}
