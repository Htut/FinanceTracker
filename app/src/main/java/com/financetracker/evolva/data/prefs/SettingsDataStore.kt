package com.financetracker.evolva.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.financetracker.evolva.R
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.AppLanguage
import com.financetracker.evolva.data.profile.ProfileIds
import com.financetracker.evolva.data.profile.ProfileKind
import com.financetracker.evolva.data.profile.TrackerProfile
import com.financetracker.evolva.data.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "finance_settings")

/**
 * App-wide preferences: language, theme, password, and profile registry / active profile.
 * Money settings live in [com.financetracker.evolva.data.profile.ProfileSettingsStore].
 */
class SettingsDataStore(private val context: Context) {
    private val appPasswordHashKey = stringPreferencesKey("app_password_hash")
    private val appThemeKey = stringPreferencesKey("app_theme")
    private val languageKey = stringPreferencesKey("app_language")
    private val activeProfileIdKey = stringPreferencesKey("active_profile_id")
    private val profileRegistryKey = stringPreferencesKey("profile_registry")
    private val prefsMigratedKey = stringPreferencesKey("profile_prefs_migrated_v1")

    // Legacy keys — read once during migration into personal ProfileSettingsStore.
    private val legacyCurrencyKey = stringPreferencesKey("currency")
    private val legacyFilterAutoCloseSecondsKey =
        androidx.datastore.preferences.core.intPreferencesKey("filter_auto_close_seconds")
    private val legacyCustomExpenseCategoriesKey =
        stringPreferencesKey("custom_expense_categories")
    private val legacyBudgetAlertsEnabledKey =
        androidx.datastore.preferences.core.booleanPreferencesKey("budget_alerts_enabled")
    private val legacyExchangeRatesKey = stringPreferencesKey("exchange_rates")

    private val json = Json { ignoreUnknownKeys = true }
    private val profileListSerializer = ListSerializer(TrackerProfile.serializer())

    val language: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        AppLanguage.fromTag(prefs[languageKey])
    }

    val appThemeId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[appThemeKey].orEmpty()
    }

    /** Empty string means no app password is set (default). */
    val appPasswordHash: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[appPasswordHashKey].orEmpty()
    }

    val hasAppPassword: Flow<Boolean> = appPasswordHash.map { it.isNotEmpty() }

    val activeProfileId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[activeProfileIdKey] ?: ProfileIds.PERSONAL
    }

    val profiles: Flow<List<TrackerProfile>> = context.dataStore.data.map { prefs ->
        decodeProfiles(prefs[profileRegistryKey])
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[languageKey] = language.tag }
    }

    suspend fun setAppThemeId(themeId: String) {
        context.dataStore.edit { it[appThemeKey] = themeId }
    }

    suspend fun setActiveProfileId(profileId: String) {
        context.dataStore.edit { it[activeProfileIdKey] = profileId }
    }

    suspend fun ensurePersonalProfile() {
        context.dataStore.edit { prefs ->
            val list = decodeProfiles(prefs[profileRegistryKey]).toMutableList()
            if (list.none { it.id == ProfileIds.PERSONAL }) {
                list.add(0, TrackerProfile.personal())
            }
            prefs[profileRegistryKey] = json.encodeToString(profileListSerializer, list)
            if (prefs[activeProfileIdKey].isNullOrBlank()) {
                prefs[activeProfileIdKey] = ProfileIds.PERSONAL
            }
        }
    }

    suspend fun addTemplateProfile(profile: TrackerProfile) {
        require(profile.kind == ProfileKind.TEMPLATE)
        require(!profile.templateId.isNullOrBlank())
        context.dataStore.edit { prefs ->
            val list = decodeProfiles(prefs[profileRegistryKey]).toMutableList()
            if (list.any { it.templateId == profile.templateId }) {
                throw IllegalStateException(context.getString(R.string.msg_template_already_setup))
            }
            list.add(profile)
            prefs[profileRegistryKey] = json.encodeToString(profileListSerializer, list)
        }
    }

    suspend fun removeProfile(profileId: String) {
        require(profileId != ProfileIds.PERSONAL)
        context.dataStore.edit { prefs ->
            val list = decodeProfiles(prefs[profileRegistryKey])
                .filterNot { it.id == profileId }
            prefs[profileRegistryKey] = json.encodeToString(profileListSerializer, list)
            if (prefs[activeProfileIdKey] == profileId) {
                prefs[activeProfileIdKey] = ProfileIds.PERSONAL
            }
        }
    }

    /**
     * One-shot: copy legacy money prefs into [onImport], then strip them from the global store.
     * Returns true if migration ran.
     */
    suspend fun migrateLegacyMoneyPrefsIfNeeded(
        onImport: suspend (
            currencyCode: String?,
            ratesJson: String?,
            categoriesJson: String?,
            filterSeconds: Int?,
            budgetAlerts: Boolean?
        ) -> Unit
    ): Boolean {
        val prefs = context.dataStore.data.first()
        if (prefs[prefsMigratedKey] == "1") return false
        onImport(
            prefs[legacyCurrencyKey],
            prefs[legacyExchangeRatesKey],
            prefs[legacyCustomExpenseCategoriesKey],
            prefs[legacyFilterAutoCloseSecondsKey],
            prefs[legacyBudgetAlertsEnabledKey]
        )
        context.dataStore.edit { edit ->
            edit.remove(legacyCurrencyKey)
            edit.remove(legacyExchangeRatesKey)
            edit.remove(legacyCustomExpenseCategoriesKey)
            edit.remove(legacyFilterAutoCloseSecondsKey)
            edit.remove(legacyBudgetAlertsEnabledKey)
            edit[prefsMigratedKey] = "1"
        }
        return true
    }

    private fun decodeProfiles(raw: String?): List<TrackerProfile> {
        if (raw.isNullOrBlank()) return listOf(TrackerProfile.personal())
        return try {
            val decoded = json.decodeFromString(profileListSerializer, raw)
            if (decoded.none { it.id == ProfileIds.PERSONAL }) {
                listOf(TrackerProfile.personal()) + decoded
            } else {
                decoded
            }
        } catch (_: Exception) {
            listOf(TrackerProfile.personal())
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

    companion object {
        /** Approximate units of [currency] per 1 USD (for seed rates only). */
        private fun unitsPerUsd(currency: AppCurrency): Double = when (currency) {
            AppCurrency.USD -> 1.0
            AppCurrency.EUR -> 0.92
            AppCurrency.GBP -> 0.79
            AppCurrency.JPY -> 150.0
            AppCurrency.CNY -> 7.2
            AppCurrency.KRW -> 1350.0
            AppCurrency.INR -> 83.0
            AppCurrency.MYR -> 4.20
            AppCurrency.SGD -> 1.35
            AppCurrency.IDR -> 15800.0
            AppCurrency.THB -> 35.0
            AppCurrency.VND -> 25000.0
            AppCurrency.PHP -> 58.0
            AppCurrency.MMK -> 2100.0
            AppCurrency.AUD -> 1.52
            AppCurrency.CAD -> 1.36
            AppCurrency.CHF -> 0.88
            AppCurrency.HKD -> 7.8
            AppCurrency.TWD -> 32.0
            AppCurrency.NZD -> 1.65
            AppCurrency.SAR -> 3.75
            AppCurrency.AED -> 3.67
            AppCurrency.TRY -> 32.0
            AppCurrency.RUB -> 92.0
            AppCurrency.BRL -> 5.0
            AppCurrency.MXN -> 17.0
            AppCurrency.ZAR -> 18.5
            AppCurrency.PKR -> 278.0
            AppCurrency.BDT -> 110.0
            AppCurrency.EGP -> 48.0
        }

        /** Foreign code → units of home currency per 1 foreign unit. */
        fun defaultRatesFor(home: AppCurrency): Map<String, Double> {
            val homePerUsd = unitsPerUsd(home)
            return AppCurrency.entries
                .filter { it != home }
                .associate { it.code to (homePerUsd / unitsPerUsd(it)) }
        }
    }
}

enum class PasswordChangeResult {
    Success,
    WrongCurrentPassword
}
