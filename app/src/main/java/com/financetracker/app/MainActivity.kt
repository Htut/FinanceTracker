package com.financetracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.financetracker.app.ui.MainViewModel
import com.financetracker.app.ui.MainViewModelFactory
import com.financetracker.app.ui.navigation.AppNavGraph
import com.financetracker.app.ui.theme.FinanceTrackerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as FinanceApp
        MainViewModelFactory(app.repository, app.settingsDataStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinanceTrackerTheme {
                AppNavGraph(viewModel = viewModel)
            }
        }
    }
}
