package com.financetracker.evolva

import android.app.Application
import com.financetracker.evolva.data.prefs.SettingsDataStore
import com.financetracker.evolva.data.profile.ActiveProfileSession
import com.financetracker.evolva.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Manual DI: app-wide settings + an [ActiveProfileSession] that owns the
 * active profile's Room DB and money prefs.
 */
class FinanceApp : Application() {

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var profileSession: ActiveProfileSession
        private set

    /** Active profile repository — prefer [profileSession] for switches. */
    val repository: FinanceRepository
        get() = profileSession.requireRepository()

    override fun onCreate() {
        super.onCreate()
        settingsDataStore = SettingsDataStore(this)
        profileSession = ActiveProfileSession(this, settingsDataStore)
        runBlocking(Dispatchers.IO) {
            profileSession.initialize()
        }
    }
}
