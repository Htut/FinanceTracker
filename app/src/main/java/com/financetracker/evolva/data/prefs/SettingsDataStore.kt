package com.financetracker.evolva.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.financetracker.evolva.data.model.AppCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "finance_settings")

class SettingsDataStore(private val context: Context) {
    private val currencyKey = stringPreferencesKey("currency")

    val currency: Flow<AppCurrency> = context.dataStore.data.map { prefs ->
        AppCurrency.fromCode(prefs[currencyKey])
    }

    suspend fun setCurrency(currency: AppCurrency) {
        context.dataStore.edit { it[currencyKey] = currency.code }
    }
}
