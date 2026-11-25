package ru.moneyapp.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import ru.moneyapp.ui.UiFlowViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.moneyapp.ui.categories.CategoriesScreen
import ru.moneyapp.ui.dashboard.DashboardScreen
import ru.moneyapp.ui.analytics.AnalyticsScreen
import ru.moneyapp.ui.budget.BudgetScreen
import ru.moneyapp.ui.goals.GoalsScreen
import ru.moneyapp.ui.settings.SettingsScreen
import ru.moneyapp.ui.transactions.TransactionsScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object Budget : Screen("budget")
    object Goals : Screen("goals")
    object Analytics : Screen("analytics")
    object Settings : Screen("settings")
    object Categories : Screen("categories")
    object EditTransaction : Screen("editTransaction?id={id}") {
        fun createRoute(id: String) = "editTransaction?id=$id"
    }
}

@androidx.compose.runtime.Composable
fun MoneyAppNavGraph(
    navController: androidx.navigation.NavHostController,
    onNavigateToSettings: () -> Unit,
    startDestination: String = Screen.Dashboard.route
) {
    // Activity-scoped one-shot UI events (the dashboard FAB → add sheet).
    // Scoped to the activity's store so the same instance is seen by both the
    // dashboard (which sets it) and the operations screen (which consumes it).
    val uiFlowViewModel: UiFlowViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToTransactions = {
                    // The "Добавить" FAB: jump to the operations tab and open
                    // the transaction form right away.
                    // v1.11.8: the sheet is requested via a one-shot
                    // activity-scoped event (UiFlowViewModel.openAdd) instead
                    // of a nav arg. The navigate MUST use the exact same
                    // options as the bottom tab click: popUpTo(start) with
                    // saveState seeds the controller's saved-state map even
                    // when nothing is popped. A bare navigate() leaves that
                    // map unseeded, so the next «Дашборд» tab tap pops the
                    // operations entry and restoreState silently re-adds it
                    // ON TOP of the dashboard — the user could never get back.
                    uiFlowViewModel.requestOpenAdd()
                    navController.navigate(Screen.Transactions.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Screen.Transactions.route) {
            TransactionsScreen(
                onNavigateToSettings = onNavigateToSettings,
                uiFlowViewModel = uiFlowViewModel
            )
        }

        composable(Screen.Budget.route) {
            BudgetScreen()
        }

        composable(Screen.Goals.route) {
            GoalsScreen()
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToCategories = {
                    navController.navigate(Screen.Categories.route)
                }
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen()
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Edit transaction screen - handled within TransactionsScreen for now
            val id = backStackEntry.arguments?.getString("id")
            TransactionsScreen(
                onNavigateToSettings = onNavigateToSettings,
                editTransactionId = id,
                uiFlowViewModel = uiFlowViewModel
            )
        }
    }
}
