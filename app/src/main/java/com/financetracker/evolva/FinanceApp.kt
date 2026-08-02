package com.financetracker.evolva

import android.app.Application
import com.financetracker.evolva.data.db.FinanceDatabase
import com.financetracker.evolva.data.prefs.SettingsDataStore
import com.financetracker.evolva.data.repository.FinanceRepository

/**
 * Simple manual DI: one Room database, one repository, one settings store,
 * all owned by the Application so they survive configuration changes and
 * are shared by every screen through [com.financetracker.evolva.ui.MainViewModel].
 * No Hilt/Koin — for a single-screen-graph app like this it's not worth the
 * extra moving parts.
 */
class FinanceApp : Application() {

    lateinit var repository: FinanceRepository
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    override fun onCreate() {
        super.onCreate()
        val database = FinanceDatabase.getInstance(this)
        repository = FinanceRepository(database.financeDao())
        settingsDataStore = SettingsDataStore(this)
    }
}
