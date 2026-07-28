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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.lucky3d.app.R
import com.lucky3d.app.feature.caibao.CaibaoScreen
import kotlinx.serialization.Serializable

@Serializable
private data object RootDestination : NavKey

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(RootDestination)

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<RootDestination> {
                MainShell()
            }
        },
    )
}

@Composable
private fun MainShell() {
    var selectedTabName by rememberSaveable { mutableStateOf(MainTab.HOME.name) }
    val selectedTab = MainTab.valueOf(selectedTabName)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTabName = tab.name },
                        icon = {
                            Icon(
                                imageVector = tab.icon(),
                                contentDescription = null,
                            )
                        },
                        label = { Text(tab.label) },
                        alwaysShowLabel = true,
                        modifier = Modifier.semantics {
                            contentDescription = tab.accessibilityLabel
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
            when (selectedTab) {
                MainTab.HOME -> PendingFeatureScreen("首页")
                MainTab.TREND -> PendingFeatureScreen("走势")
                MainTab.PICK -> PendingFeatureScreen("选号")
                MainTab.PLANS -> PendingFeatureScreen("方案")
                MainTab.CAIBAO -> CaibaoScreen()
            }
        }
    }
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
