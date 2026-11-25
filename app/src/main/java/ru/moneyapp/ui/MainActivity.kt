package ru.moneyapp.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.moneyapp.R
import ru.moneyapp.ui.l10n.L10n
import ru.moneyapp.ui.navigation.Screen
import ru.moneyapp.ui.navigation.MoneyAppNavGraph
import ru.moneyapp.ui.settings.SettingsViewModel
import ru.moneyapp.ui.theme.MoneyAppTheme

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

fun bottomNavIcon(screen: Screen): ImageVector = when (screen) {
    Screen.Dashboard -> Icons.Rounded.SpaceDashboard
    Screen.Transactions -> Icons.Rounded.ReceiptLong
    Screen.Budget -> Icons.Rounded.AccountBalance
    Screen.Goals -> Icons.Rounded.Flag
    Screen.Analytics -> Icons.Rounded.Insights
    else -> Icons.Rounded.SpaceDashboard
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Default auto style: transparent bars, icons adapt to the theme.
        enableEdgeToEdge()

        // The Scaffold has a NavigationBar: without this the system paints a
        // translucent scrim over the bottom nav on SDK 29+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            AppThemeRoot()
        }
    }
}

@Composable
private fun AppThemeRoot() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle("system")
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    MoneyAppTheme(darkTheme = darkTheme) {
        MoneyAppScaffold()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyAppScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // v1.10.0: labels come from the in-app L10n bundle (not Android resources),
    // so they follow the language switch without an activity recreation.
    val bottomNavItems = listOf(
        BottomNavItem(Screen.Dashboard, L10n.s.navDashboard, bottomNavIcon(Screen.Dashboard)),
        BottomNavItem(Screen.Transactions, L10n.s.navTransactions, bottomNavIcon(Screen.Transactions)),
        BottomNavItem(Screen.Budget, L10n.s.navBudget, bottomNavIcon(Screen.Budget)),
        BottomNavItem(Screen.Goals, L10n.s.navGoals, bottomNavIcon(Screen.Goals)),
        BottomNavItem(Screen.Analytics, L10n.s.navAnalytics, bottomNavIcon(Screen.Analytics)),
    )

    // Check if current route should show bottom nav
    val showBottomNav = currentRoute !in listOf(Screen.Settings.route, Screen.Categories.route)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // substringBefore('?') — routes with optional args carry
                    // placeholders in destination.route
                    // v1.10.0: in-app L10n bundle — the title follows the
                    // language switch (Android resources did not).
                    val title = when (currentRoute?.substringBefore('?')) {
                        Screen.Dashboard.route -> L10n.s.navDashboard
                        Screen.Transactions.route -> L10n.s.navTransactions
                        Screen.Budget.route -> L10n.s.navBudget
                        Screen.Goals.route -> L10n.s.navGoals
                        Screen.Analytics.route -> L10n.s.navAnalytics
                        Screen.Settings.route -> L10n.s.navSettings
                        Screen.Categories.route -> L10n.s.navCategories
                        else -> stringResource(R.string.app_name)
                    }
                    Text(text = title)
                },
                actions = {
                    if (currentRoute != Screen.Settings.route && currentRoute != Screen.Categories.route) {
                        androidx.compose.material3.IconButton(
                            onClick = {
                                navController.navigate(Screen.Settings.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    restoreState = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = L10n.s.navSettings
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        // Routes with optional arguments carry placeholders in destination.route
// ("transactions?add={add}"), so compare the part before the query string.
val isSelected = currentRoute?.substringBefore('?') == item.screen.route
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = null) },
                            // Small single-line label: "Транзакции" used to wrap
                            // into two lines in the 5th-width slot
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier.padding(innerPadding)
        ) {
            MoneyAppNavGraph(
                navController = navController,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
    }
}
