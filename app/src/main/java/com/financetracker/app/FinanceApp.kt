package com.financetracker.app

import android.app.Application
import com.financetracker.app.data.db.FinanceDatabase
import com.financetracker.app.data.prefs.SettingsDataStore
import com.financetracker.app.data.repository.FinanceRepository

/**
 * Simple manual DI: one Room database, one repository, one settings store,
 * all owned by the Application so they survive configuration changes and
 * are shared by every screen through [MainViewModel]. No Hilt/Koin — for a
 * single-screen-graph app like this it's not worth the extra moving parts.
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
