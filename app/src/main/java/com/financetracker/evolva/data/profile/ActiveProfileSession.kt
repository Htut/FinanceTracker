package com.financetracker.evolva.data.profile

import android.content.Context
import com.financetracker.evolva.R
import com.financetracker.evolva.data.AppConstants
import com.financetracker.evolva.data.db.FinanceDatabase
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.prefs.SettingsDataStore
import com.financetracker.evolva.data.repository.FinanceRepository
import com.financetracker.evolva.data.templates.AppTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

data class ProfileBinding(
    val profile: TrackerProfile,
    val database: FinanceDatabase,
    val repository: FinanceRepository,
    val settings: ProfileSettingsStore
)

/**
 * Owns the open Room DB + prefs for the active tracker profile.
 * Call [initialize] once from [com.financetracker.evolva.FinanceApp].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveProfileSession(
    private val appContext: Context,
    private val appSettings: SettingsDataStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()

    private val _binding = MutableStateFlow<ProfileBinding?>(null)

    val profiles: StateFlow<List<TrackerProfile>> = appSettings.profiles
        .stateIn(scope, SharingStarted.Eagerly, listOf(TrackerProfile.personal()))

    val activeProfileId: StateFlow<String> = appSettings.activeProfileId
        .stateIn(scope, SharingStarted.Eagerly, ProfileIds.PERSONAL)

    val activeProfile: StateFlow<TrackerProfile> =
        combine(profiles, activeProfileId) { list, id ->
            list.find { it.id == id } ?: TrackerProfile.personal()
        }.stateIn(scope, SharingStarted.Eagerly, TrackerProfile.personal())

    val accounts = _binding.flatMapLatest { it?.repository?.accounts ?: flowOf(emptyList()) }
    val transactions = _binding.flatMapLatest { it?.repository?.transactions ?: flowOf(emptyList()) }
    val budgets = _binding.flatMapLatest { it?.repository?.budgets ?: flowOf(emptyList()) }
    val recurringRules =
        _binding.flatMapLatest { it?.repository?.recurringRules ?: flowOf(emptyList()) }
    val currency = _binding.flatMapLatest { it?.settings?.currency ?: flowOf(AppCurrency.MYR) }
    val exchangeRates = _binding.flatMapLatest { it?.settings?.exchangeRates ?: flowOf(emptyMap()) }
    val customExpenseCategories =
        _binding.flatMapLatest { it?.settings?.customExpenseCategories ?: flowOf(emptyList()) }
    val filterAutoCloseSeconds = _binding.flatMapLatest {
        it?.settings?.filterAutoCloseSeconds ?: flowOf(AppConstants.DEFAULT_FILTER_AUTO_CLOSE_SECONDS)
    }
    val budgetAlertsEnabled =
        _binding.flatMapLatest { it?.settings?.budgetAlertsEnabled ?: flowOf(false) }

    fun requireRepository(): FinanceRepository =
        _binding.value?.repository ?: error("Profile session not initialized")

    fun requireSettings(): ProfileSettingsStore =
        _binding.value?.settings ?: error("Profile session not initialized")

    suspend fun initialize() = mutex.withLock {
        ProfileDatabaseProvider.migrateLegacyDatabaseIfNeeded(appContext)
        appSettings.ensurePersonalProfile()
        val personalSettings = ProfileSettingsStore.get(appContext, ProfileIds.PERSONAL)
        appSettings.migrateLegacyMoneyPrefsIfNeeded { currency, rates, cats, filter, alerts ->
            personalSettings.importLegacy(currency, rates, cats, filter, alerts)
        }
        val activeId = appSettings.activeProfileId.first()
        openBindingLocked(activeId)
        requireRepository().ensureDefaultAccounts()
        requireRepository().runRecurringEngine()
    }

    suspend fun switchTo(profileId: String) = mutex.withLock {
        val list = appSettings.profiles.first()
        if (list.none { it.id == profileId }) {
            throw IllegalArgumentException("Unknown profile: $profileId")
        }
        if (_binding.value?.profile?.id == profileId) return@withLock
        openBindingLocked(profileId)
        appSettings.setActiveProfileId(profileId)
        requireRepository().ensureDefaultAccounts()
        requireRepository().runRecurringEngine()
    }

    /**
     * Creates a template profile, seeds generated data, and switches to it.
     */
    suspend fun setupTemplate(template: AppTemplate, displayName: String): TrackerProfile = mutex.withLock {
        val existing = appSettings.profiles.first()
        if (existing.any { it.templateId == template.id }) {
            throw IllegalStateException(appContext.getString(R.string.msg_template_already_setup))
        }
        val profile = TrackerProfile(
            id = UUID.randomUUID().toString(),
            kind = ProfileKind.TEMPLATE,
            templateId = template.id,
            displayName = displayName
        )
        appSettings.addTemplateProfile(profile)
        openBindingLocked(profile.id)
        appSettings.setActiveProfileId(profile.id)
        val repo = requireRepository()
        repo.ensureDefaultAccounts()
        val rows = template.generate(appContext)
        repo.addTransactions(rows)
        template.budgets?.let { repo.setBudgetsIfAbsent(it) }
        profile
    }

    suspend fun deleteTemplateProfile(profileId: String) = mutex.withLock {
        require(profileId != ProfileIds.PERSONAL) { "Personal profile cannot be deleted" }
        val receiptPaths = mutableListOf<String>()
        val wasActive = _binding.value?.profile?.id == profileId
        if (wasActive) {
            receiptPaths += requireRepository().transactions.first().mapNotNull { it.receiptUri }
            openBindingLocked(ProfileIds.PERSONAL)
            appSettings.setActiveProfileId(ProfileIds.PERSONAL)
            requireRepository().ensureDefaultAccounts()
        } else {
            withContext(Dispatchers.IO) {
                val db = ProfileDatabaseProvider.open(appContext, profileId)
                try {
                    val paths = FinanceRepository(db.financeDao()).transactions.first()
                        .mapNotNull { it.receiptUri }
                    receiptPaths += paths
                } finally {
                    db.close()
                }
            }
        }
        appSettings.removeProfile(profileId)
        withContext(Dispatchers.IO) {
            receiptPaths.forEach { path ->
                com.financetracker.evolva.data.receipt.ReceiptStore.deleteIfOwned(appContext, path)
            }
            ProfileDatabaseProvider.deleteDatabase(appContext, profileId)
            ProfileSettingsStore.deleteFiles(appContext, profileId)
        }
    }

    private suspend fun openBindingLocked(profileId: String) {
        val list = appSettings.profiles.first()
        val profile = list.find { it.id == profileId }
            ?: list.find { it.id == ProfileIds.PERSONAL }
            ?: TrackerProfile.personal()
        val previous = _binding.value
        val db = withContext(Dispatchers.IO) {
            ProfileDatabaseProvider.open(appContext, profile.id)
        }
        val settings = ProfileSettingsStore.get(appContext, profile.id)
        _binding.value = ProfileBinding(
            profile = profile,
            database = db,
            repository = FinanceRepository(db.financeDao()),
            settings = settings
        )
        withContext(Dispatchers.IO) {
            previous?.database?.close()
        }
    }
}
