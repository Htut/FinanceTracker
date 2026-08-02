package com.financetracker.evolva.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
    data object Dashboard : Tab("dashboard", "Dashboard")
    data object Transactions : Tab("transactions", "Transactions")
    data object Budget : Tab("budget", "Budget")
    data object Settings : Tab("settings", "Settings")
}

private val tabs = listOf(Tab.Dashboard, Tab.Transactions, Tab.Budget, Tab.Settings)

private fun iconFor(tab: Tab) = when (tab) {
    Tab.Dashboard -> Icons.Filled.Dashboard
    Tab.Transactions -> Icons.AutoMirrored.Filled.List
    Tab.Budget -> Icons.Filled.AccountBalanceWallet
    Tab.Settings -> Icons.Filled.Settings
}

/** Bottom-nav shell. One [MainViewModel] is shared by every tab. */
@Composable
fun AppNavGraph(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val infoMessage by viewModel.infoMessage.collectAsState()

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

    infoMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissInfoMessage,
            title = { Text("Notice") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissInfoMessage) { Text("OK") }
            }
        )
    }
}
