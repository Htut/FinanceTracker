package com.financetracker.evolva.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.budget.BudgetScreen
import com.financetracker.evolva.ui.dashboard.DashboardScreen
import com.financetracker.evolva.ui.settings.SettingsScreen
import com.financetracker.evolva.ui.transactions.TransactionsScreen

private sealed class Tab(val route: String, val label: String) {
    object Dashboard : Tab("dashboard", "Dashboard")
    object Transactions : Tab("transactions", "Transactions")
    object Budget : Tab("budget", "Budget")
    object Settings : Tab("settings", "Settings")
}

private val tabs = listOf(Tab.Dashboard, Tab.Transactions, Tab.Budget, Tab.Settings)

private fun iconFor(tab: Tab) = when (tab) {
    Tab.Dashboard -> Icons.Filled.Dashboard
    Tab.Transactions -> Icons.Filled.List
    Tab.Budget -> Icons.Filled.AccountBalanceWallet
    Tab.Settings -> Icons.Filled.Settings
}

/** Bottom-nav shell matching the web app's Dashboard / Transactions / Budget
 * / Settings tabs. One [MainViewModel] instance, scoped to the Activity, is
 * shared by every tab so they always show the same live data. */
@Composable
fun AppNavGraph(viewModel: MainViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                tabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(iconFor(tab), contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tab.Dashboard.route) { DashboardScreen(viewModel) }
            composable(Tab.Transactions.route) { TransactionsScreen(viewModel) }
            composable(Tab.Budget.route) { BudgetScreen(viewModel) }
            composable(Tab.Settings.route) { SettingsScreen(viewModel) }
        }
    }
}
