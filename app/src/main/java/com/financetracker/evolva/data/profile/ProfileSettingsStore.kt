package com.financetracker.evolva.data.profile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.financetracker.evolva.data.AppConstants
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.AutoLockRule
import com.financetracker.evolva.data.model.Categories
import com.financetracker.evolva.data.prefs.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Money-related preferences scoped to a single tracker profile.
 */
class ProfileSettingsStore private constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val currencyKey = stringPreferencesKey("currency")
    private val filterAutoCloseSecondsKey = intPreferencesKey("filter_auto_close_seconds")
    private val customExpenseCategoriesKey = stringPreferencesKey("custom_expense_categories")
    private val budgetAlertsEnabledKey = booleanPreferencesKey("budget_alerts_enabled")
    private val exchangeRatesKey = stringPreferencesKey("exchange_rates")
    private val autoLockDaysKey = intPreferencesKey("auto_lock_days")

    private val json = Json { ignoreUnknownKeys = true }
    private val rateSerializer = MapSerializer(String.serializer(), Double.serializer())

    val currency: Flow<AppCurrency> = dataStore.data.map { prefs ->
        AppCurrency.fromCode(prefs[currencyKey])
    }

    val customExpenseCategories: Flow<List<String>> = dataStore.data.map { prefs ->
        decodeCategoryList(prefs[customExpenseCategoriesKey])
    }

    val filterAutoCloseSeconds: Flow<Int> = dataStore.data.map { prefs ->
        prefs[filterAutoCloseSecondsKey] ?: AppConstants.DEFAULT_FILTER_AUTO_CLOSE_SECONDS
    }

    val budgetAlertsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[budgetAlertsEnabledKey] ?: false
    }

    val autoLockRule: Flow<AutoLockRule> =
        dataStore.data.map { prefs ->
            AutoLockRule.fromDays(prefs[autoLockDaysKey])
        }

    val exchangeRates: Flow<Map<String, Double>> = dataStore.data.map { prefs ->
        val stored = decodeRates(prefs[exchangeRatesKey])
        val home = AppCurrency.fromCode(prefs[currencyKey])
        SettingsDataStore.defaultRatesFor(home) + stored
    }

    suspend fun setCurrency(currency: AppCurrency) {
        dataStore.edit { it[currencyKey] = currency.code }
    }

    suspend fun setExchangeRate(foreignCode: String, rateToHome: Double) {
        if (rateToHome <= 0) return
        dataStore.edit { prefs ->
            val current = decodeRates(prefs[exchangeRatesKey]).toMutableMap()
            current[foreignCode] = rateToHome
            prefs[exchangeRatesKey] = json.encodeToString(rateSerializer, current)
        }
    }

    suspend fun setExchangeRates(rates: Map<String, Double>) {
        dataStore.edit { prefs ->
            val cleaned = rates.filter { it.value > 0 }
            if (cleaned.isEmpty()) prefs.remove(exchangeRatesKey)
            else prefs[exchangeRatesKey] = json.encodeToString(rateSerializer, cleaned)
        }
    }

    suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        dataStore.edit { it[budgetAlertsEnabledKey] = enabled }
    }

    suspend fun setAutoLockRule(rule: AutoLockRule) {
        dataStore.edit { it[autoLockDaysKey] = rule.days }
    }

    suspend fun setFilterAutoCloseSeconds(seconds: Int) {
        dataStore.edit {
            it[filterAutoCloseSecondsKey] = seconds.coerceIn(0, 120)
        }
    }

    suspend fun setCustomExpenseCategories(categories: List<String>) {
        val normalized = Categories.normalizeCustom(categories)
            .filterNot { Categories.isBuiltInExpense(it) }
        dataStore.edit {
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

    /**
     * Seed from legacy global prefs during first migration.
     */
    suspend fun importLegacy(
        currencyCode: String?,
        ratesJson: String?,
        categoriesJson: String?,
        filterSeconds: Int?,
        budgetAlerts: Boolean?
    ) {
        dataStore.edit { prefs ->
            if (!currencyCode.isNullOrBlank()) prefs[currencyKey] = currencyCode
            if (!ratesJson.isNullOrBlank()) prefs[exchangeRatesKey] = ratesJson
            if (!categoriesJson.isNullOrBlank()) prefs[customExpenseCategoriesKey] = categoriesJson
            if (filterSeconds != null) prefs[filterAutoCloseSecondsKey] = filterSeconds
            if (budgetAlerts != null) prefs[budgetAlertsEnabledKey] = budgetAlerts
        }
    }

    private fun decodeRates(raw: String?): Map<String, Double> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            json.decodeFromString(rateSerializer, raw)
        } catch (_: Exception) {
            emptyMap()
        }
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

    companion object {
        private val cache = ConcurrentHashMap<String, ProfileSettingsStore>()

        fun prefsFileName(profileId: String): String = "profile_${profileId}_settings"

        fun get(context: Context, profileId: String): ProfileSettingsStore =
            cache.getOrPut(profileId) {
                val app = context.applicationContext
                val store = PreferenceDataStoreFactory.create(
                    produceFile = { app.preferencesDataStoreFile(prefsFileName(profileId)) }
                )
                ProfileSettingsStore(store)
            }

        fun evict(profileId: String) {
            cache.remove(profileId)
        }

        fun deleteFiles(context: Context, profileId: String) {
            if (profileId == ProfileIds.PERSONAL) return
            evict(profileId)
            val file = context.applicationContext.preferencesDataStoreFile(prefsFileName(profileId))
            file.delete()
        }
    }
}
