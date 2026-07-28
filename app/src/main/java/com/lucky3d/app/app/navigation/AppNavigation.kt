package com.lucky3d.app.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.lucky3d.app.R
import com.lucky3d.app.feature.caibao.CaibaoScreen
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
private data object HistoryDestination : NavKey

@Serializable
private data object SettingsDestination : NavKey

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(RootDestination)

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<RootDestination> {
                MainShell(
                    onOpenHistory = { backStack.add(HistoryDestination) },
                    onOpenSettings = { backStack.add(SettingsDestination) },
                )
            }
            entry<HistoryDestination> {
                HistoryRoute(
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
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    reminderViewModel: ReminderViewModel = hiltViewModel(),
) {
    var selectedTabName by rememberSaveable { mutableStateOf(MainTab.HOME.name) }
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
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    val tabLabel = androidx.compose.ui.res.stringResource(tab.labelRes)
                    val tabAccessibilityLabel =
                        androidx.compose.ui.res.stringResource(tab.accessibilityLabelRes)
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTabName = tab.name },
                        icon = {
                            Icon(
                                imageVector = tab.icon(),
                                contentDescription = null,
                            )
                        },
                        label = { Text(tabLabel) },
                        alwaysShowLabel = true,
                        modifier = Modifier.semantics {
                            contentDescription = tabAccessibilityLabel
                        },
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
                        onOpenTrend = { selectedTabName = MainTab.TREND.name },
                        onOpenPick = { selectedTabName = MainTab.PICK.name },
                        onOpenSchemes = { selectedTabName = MainTab.PLANS.name },
                        onOpenSettings = onOpenSettings,
                    )
                    MainTab.TREND -> TrendRoute()
                    MainTab.PICK -> PickRoute()
                    MainTab.PLANS -> SchemeRoute(
                        onStartPick = { selectedTabName = MainTab.PICK.name },
                    )
                    MainTab.CAIBAO -> CaibaoScreen()
                }
            }
        }
    }
}

@Composable
private fun HomeRoute(
    onOpenHistory: () -> Unit,
    onOpenTrend: () -> Unit,
    onOpenPick: () -> Unit,
    onOpenSchemes: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onOpenHistory = onOpenHistory,
        onOpenTrend = onOpenTrend,
        onOpenPick = onOpenPick,
        onOpenSchemes = onOpenSchemes,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
private fun HistoryRoute(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        onBack = onBack,
        onShowRecent = viewModel::showRecent,
        onSearchIssue = viewModel::searchIssue,
        onSearchYear = viewModel::searchYear,
        onSearchDateRange = viewModel::searchDateRange,
        onSelectDraw = viewModel::selectDraw,
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
            viewModel.setWindow(settings.defaultObservationWindow)
            defaultsApplied = true
        }
    }
    TrendScreen(
        state = state,
        onSetWindow = viewModel::setWindow,
        onTogglePosition = viewModel::togglePosition,
        onSelectPoint = viewModel::selectPoint,
        onShowStatistics = viewModel::showStatistics,
        onScaleChange = viewModel::setScale,
        onReturnLatest = viewModel::selectLatest,
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
    viewModel: SchemeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
