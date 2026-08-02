package com.financetracker.evolva.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.financetracker.evolva.data.AppConstants
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.Categories
import com.financetracker.evolva.data.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "finance_settings")

class SettingsDataStore(private val context: Context) {
    private val currencyKey = stringPreferencesKey("currency")
    private val appPasswordHashKey = stringPreferencesKey("app_password_hash")
    private val filterAutoCloseSecondsKey = intPreferencesKey("filter_auto_close_seconds")
    private val appThemeKey = stringPreferencesKey("app_theme")
    private val customExpenseCategoriesKey = stringPreferencesKey("custom_expense_categories")
    private val budgetAlertsEnabledKey = booleanPreferencesKey("budget_alerts_enabled")

    private val json = Json { ignoreUnknownKeys = true }

    val currency: Flow<AppCurrency> = context.dataStore.data.map { prefs ->
        AppCurrency.fromCode(prefs[currencyKey])
    }

    val appThemeId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[appThemeKey].orEmpty()
    }

    val customExpenseCategories: Flow<List<String>> = context.dataStore.data.map { prefs ->
        decodeCategoryList(prefs[customExpenseCategoriesKey])
    }

    /** Empty string means no app password is set (default). */
    val appPasswordHash: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[appPasswordHashKey].orEmpty()
    }

    val hasAppPassword: Flow<Boolean> = appPasswordHash.map { it.isNotEmpty() }

    /** Seconds before date filter auto-collapses. 0 disables auto-close. */
    val filterAutoCloseSeconds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[filterAutoCloseSecondsKey] ?: AppConstants.DEFAULT_FILTER_AUTO_CLOSE_SECONDS
    }

    /** When true, notify if a category is near (≥80%) or over its monthly budget. */
    val budgetAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[budgetAlertsEnabledKey] ?: false
    }

    suspend fun setCurrency(currency: AppCurrency) {
        context.dataStore.edit { it[currencyKey] = currency.code }
    }

    suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[budgetAlertsEnabledKey] = enabled }
    }

    suspend fun setFilterAutoCloseSeconds(seconds: Int) {
        context.dataStore.edit {
            it[filterAutoCloseSecondsKey] = seconds.coerceIn(0, 120)
        }
    }

    suspend fun setAppThemeId(themeId: String) {
        context.dataStore.edit { it[appThemeKey] = themeId }
    }

    suspend fun setCustomExpenseCategories(categories: List<String>) {
        val normalized = Categories.normalizeCustom(categories)
            .filterNot { Categories.isBuiltInExpense(it) }
        context.dataStore.edit {
            if (normalized.isEmpty()) {
                it.remove(customExpenseCategoriesKey)
            } else {
                it[customExpenseCategoriesKey] =
                    json.encodeToString(ListSerializer(String.serializer()), normalized)
            }
        }
    }

    suspend fun addCustomExpenseCategory(name: String): Boolean {
        val trimmed = name.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isEmpty() || Categories.isBuiltInExpense(trimmed)) return false
        val current = customExpenseCategories.first()
        if (current.any { it.equals(trimmed, ignoreCase = true) }) return false
        setCustomExpenseCategories(current + trimmed)
        return true
    }

    suspend fun removeCustomExpenseCategory(name: String) {
        val current = customExpenseCategories.first()
        setCustomExpenseCategories(current.filterNot { it.equals(name, ignoreCase = true) })
    }

    private fun decodeCategoryList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            Categories.normalizeCustom(
                json.decodeFromString(ListSerializer(String.serializer()), raw)
            ).filterNot { Categories.isBuiltInExpense(it) }
        } catch (_: Exception) {
            Categories.normalizeCustom(raw.split('\n'))
                .filterNot { Categories.isBuiltInExpense(it) }
        }
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
