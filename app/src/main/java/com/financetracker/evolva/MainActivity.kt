package com.financetracker.evolva

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.MainViewModelFactory
import com.financetracker.evolva.ui.navigation.AppNavGraph
import com.financetracker.evolva.ui.theme.FinanceTrackerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as FinanceApp
        MainViewModelFactory(app.repository, app.settingsDataStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val theme by viewModel.appTheme.collectAsState()
            FinanceTrackerTheme(theme = theme) {
                AppNavGraph(viewModel = viewModel)
            }
        }
    }
}
