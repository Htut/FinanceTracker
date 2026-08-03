package com.financetracker.evolva.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.financetracker.evolva.R
import com.financetracker.evolva.data.profile.ProfileIds
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.UndoAction
import com.financetracker.evolva.ui.budget.BudgetScreen
import com.financetracker.evolva.ui.dashboard.DashboardScreen
import com.financetracker.evolva.ui.report.ReportScreen
import com.financetracker.evolva.ui.settings.SettingsScreen
import com.financetracker.evolva.ui.theme.FinanceColors
import com.financetracker.evolva.ui.transactions.TransactionsScreen

private sealed class Tab(val route: String, val labelRes: Int) {
    data object Dashboard : Tab("dashboard", R.string.tab_dashboard)
    data object Transactions : Tab("transactions", R.string.tab_transactions)
    data object Budget : Tab("budget", R.string.tab_budget)
    data object Report : Tab("report", R.string.tab_report)
    data object Settings : Tab("settings", R.string.tab_settings)
}

private val tabs = listOf(
    Tab.Dashboard,
    Tab.Transactions,
    Tab.Budget,
    Tab.Report,
    Tab.Settings
)

private fun iconFor(tab: Tab) = when (tab) {
    Tab.Dashboard -> Icons.Filled.Home
    Tab.Transactions -> Icons.Filled.ReceiptLong
    Tab.Budget -> Icons.Filled.PieChart
    Tab.Report -> Icons.AutoMirrored.Filled.ShowChart
    Tab.Settings -> Icons.Filled.Settings
}

/** Bottom-nav shell. One [MainViewModel] is shared by every tab. */
@Composable
fun AppNavGraph(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val infoMessage by viewModel.infoMessage.collectAsState()
    val context = LocalContext.current
    val budgetAlertsEnabled by viewModel.budgetAlertsEnabled.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val undoAction by viewModel.undoAction.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(budgetAlertsEnabled, transactions, budgets) {
        if (budgetAlertsEnabled) {
            viewModel.refreshBudgetAlerts(context)
        }
    }

    val activeProfile by viewModel.activeProfile.collectAsState()
    val clearedMessage = stringResource(R.string.all_transactions_cleared)
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(undoAction) {
        if (undoAction is UndoAction.ClearAll) {
            val result = snackbarHostState.showSnackbar(
                message = clearedMessage,
                actionLabel = undoLabel
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoLastAction()
            else viewModel.dismissUndo()
        }
    }

    Scaffold(
        containerColor = FinanceColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (activeProfile.id != ProfileIds.PERSONAL) {
                Surface(color = FinanceColors.Surface) {
                    Text(
                        stringResource(R.string.active_profile_label, activeProfile.displayName),
                        fontSize = 13.sp,
                        color = FinanceColors.TextSoft,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = FinanceColors.Surface) {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                tabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    val label = stringResource(tab.labelRes)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(iconFor(tab), contentDescription = label) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        alwaysShowLabel = true
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
            composable(Tab.Report.route) { ReportScreen(viewModel) }
            composable(Tab.Settings.route) { SettingsScreen(viewModel) }
        }
    }

    infoMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissInfoMessage,
            title = { Text(stringResource(R.string.notice_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissInfoMessage) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }
}
