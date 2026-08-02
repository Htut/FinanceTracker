package com.financetracker.evolva.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "finance_settings")

class SettingsDataStore(private val context: Context) {
    private val currencyKey = stringPreferencesKey("currency")
    private val appPasswordHashKey = stringPreferencesKey("app_password_hash")

    val currency: Flow<AppCurrency> = context.dataStore.data.map { prefs ->
        AppCurrency.fromCode(prefs[currencyKey])
    }

    /** Empty string means no app password is set (default). */
    val appPasswordHash: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[appPasswordHashKey].orEmpty()
    }

    val hasAppPassword: Flow<Boolean> = appPasswordHash.map { it.isNotEmpty() }

    suspend fun setCurrency(currency: AppCurrency) {
        context.dataStore.edit { it[currencyKey] = currency.code }
    }

    suspend fun verifyAppPassword(password: String): Boolean {
        val stored = context.dataStore.data.map { it[appPasswordHashKey].orEmpty() }.first()
        return PasswordHasher.matches(password, stored)
    }

    /**
     * Sets or clears the app password.
     * [currentPassword] must match when a password is already set.
     * Pass empty [newPassword] to clear (requires current password if one exists).
     */
    suspend fun setAppPassword(currentPassword: String, newPassword: String): PasswordChangeResult {
        val stored = context.dataStore.data.map { it[appPasswordHashKey].orEmpty() }.first()
        if (stored.isNotEmpty() && !PasswordHasher.matches(currentPassword, stored)) {
            return PasswordChangeResult.WrongCurrentPassword
        }
        context.dataStore.edit {
            if (newPassword.isEmpty()) {
                it.remove(appPasswordHashKey)
            } else {
                it[appPasswordHashKey] = PasswordHasher.hash(newPassword)
            }
        }
        return PasswordChangeResult.Success
    }
}

enum class PasswordChangeResult {
    Success,
    WrongCurrentPassword
}
