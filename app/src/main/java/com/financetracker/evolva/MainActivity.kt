package com.financetracker.evolva

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.financetracker.evolva.ui.MainViewModel
import com.financetracker.evolva.ui.MainViewModelFactory
import com.financetracker.evolva.ui.lock.AppLockScreen
import com.financetracker.evolva.ui.navigation.AppNavGraph
import com.financetracker.evolva.ui.theme.FinanceTrackerTheme

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as FinanceApp
        MainViewModelFactory(app.profileSession, app.settingsDataStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val theme by viewModel.appTheme.collectAsState()
            val hasAppPassword by viewModel.hasAppPassword.collectAsState()
            val unlocked by viewModel.unlocked.collectAsState()
            FinanceTrackerTheme(theme = theme) {
                if (hasAppPassword && !unlocked) {
                    AppLockScreen(
                        onUnlockWithPassword = viewModel::unlockWithPassword,
                        onUnlocked = viewModel::unlockSession
                    )
                } else {
                    AppNavGraph(viewModel = viewModel)
                }
            }
        }
    }
}
