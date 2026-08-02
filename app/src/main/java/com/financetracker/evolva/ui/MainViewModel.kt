package com.financetracker.evolva.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.financetracker.evolva.data.AppConstants
import com.financetracker.evolva.data.backup.BackupManager
import com.financetracker.evolva.data.backup.toDomain
import com.financetracker.evolva.data.locale.LocaleHelper
import com.financetracker.evolva.data.model.Account
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.AppLanguage
import com.financetracker.evolva.data.model.Budget
import com.financetracker.evolva.data.model.Categories
import com.financetracker.evolva.data.model.DateFilter
import com.financetracker.evolva.data.model.RecurringRule
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import com.financetracker.evolva.data.model.filteredBy
import com.financetracker.evolva.data.notify.BudgetAlertNotifier
import com.financetracker.evolva.data.prefs.PasswordChangeResult
import com.financetracker.evolva.data.prefs.SettingsDataStore
import com.financetracker.evolva.data.profile.ActiveProfileSession
import com.financetracker.evolva.data.profile.ProfileIds
import com.financetracker.evolva.data.profile.TrackerProfile
import com.financetracker.evolva.data.receipt.ReceiptStore
import com.financetracker.evolva.data.templates.AppTemplate
import com.financetracker.evolva.ui.theme.AppThemeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

sealed class ImportResult {
    data class Success(val transactionCount: Int) : ImportResult()
    object Invalid : ImportResult()
}

sealed class UndoAction {
    data class DeleteTransaction(val transaction: Transaction) : UndoAction()
    data class ClearAll(val transactions: List<Transaction>) : UndoAction()
}

class MainViewModel(
    private val profileSession: ActiveProfileSession,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val repository get() = profileSession.requireRepository()
    private val profileSettings get() = profileSession.requireSettings()

    val profiles: StateFlow<List<TrackerProfile>> = profileSession.profiles
    val activeProfile: StateFlow<TrackerProfile> = profileSession.activeProfile

    val accounts: StateFlow<List<Account>> = profileSession.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = profileSession.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = profileSession.budgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringRules: StateFlow<List<RecurringRule>> = profileSession.recurringRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currency: StateFlow<AppCurrency> = profileSession.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppCurrency.MYR)

    val language: StateFlow<AppLanguage> = settingsDataStore.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.ENGLISH)

    val exchangeRates: StateFlow<Map<String, Double>> = profileSession.exchangeRates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val hasAppPassword: StateFlow<Boolean> = settingsDataStore.hasAppPassword
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val filterAutoCloseSeconds: StateFlow<Int> = profileSession.filterAutoCloseSeconds
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AppConstants.DEFAULT_FILTER_AUTO_CLOSE_SECONDS
        )

    val appTheme: StateFlow<AppThemeOption> = settingsDataStore.appThemeId
        .map { AppThemeOption.fromId(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeOption.CLASSIC)

    val customExpenseCategories: StateFlow<List<String>> = profileSession.customExpenseCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetAlertsEnabled: StateFlow<Boolean> = profileSession.budgetAlertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _dateFilter = MutableStateFlow<DateFilter>(DateFilter.All)
    val dateFilter: StateFlow<DateFilter> = _dateFilter

    val filteredTransactions: StateFlow<List<Transaction>> =
        combine(transactions, _dateFilter) { txs, filter -> txs.filteredBy(filter) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage

    private val _undoAction = MutableStateFlow<UndoAction?>(null)
    val undoAction: StateFlow<UndoAction?> = _undoAction

    private val _forecastHorizon = MutableStateFlow(6)
    val forecastHorizon: StateFlow<Int> = _forecastHorizon

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked

    init {
        viewModelScope.launch {
            LocaleHelper.apply(settingsDataStore.language.first())
            if (!settingsDataStore.hasAppPassword.first()) {
                _unlocked.value = true
            }
        }
        viewModelScope.launch {
            settingsDataStore.hasAppPassword.collect { has ->
                if (!has) _unlocked.value = true
            }
        }
    }

    fun unlockWithPassword(password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = settingsDataStore.verifyAppPassword(password)
            if (ok) _unlocked.value = true
            onResult(ok)
        }
    }

    fun unlockSession() {
        _unlocked.value = true
    }

    fun lockSession() {
        if (hasAppPassword.value) _unlocked.value = false
    }

    fun setForecastHorizon(months: Int) {
        _forecastHorizon.value = months
    }

    fun setDateFilter(filter: DateFilter) {
        _dateFilter.value = filter
    }

    fun dismissInfoMessage() {
        _infoMessage.value = null
    }

    fun dismissUndo() {
        _undoAction.value = null
    }

    fun setCurrency(currency: AppCurrency) {
        viewModelScope.launch { profileSettings.setCurrency(currency) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsDataStore.setLanguage(language)
            LocaleHelper.apply(language)
        }
    }

    fun setExchangeRate(foreignCode: String, rateToHome: Double) {
        viewModelScope.launch { profileSettings.setExchangeRate(foreignCode, rateToHome) }
    }

    fun setFilterAutoCloseSeconds(seconds: Int) {
        viewModelScope.launch { profileSettings.setFilterAutoCloseSeconds(seconds) }
    }

    fun setAppTheme(theme: AppThemeOption) {
        viewModelScope.launch { settingsDataStore.setAppThemeId(theme.id) }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { profileSettings.setBudgetAlertsEnabled(enabled) }
    }

    fun refreshBudgetAlerts(context: Context) {
        viewModelScope.launch {
            if (!profileSession.budgetAlertsEnabled.first()) return@launch
            val alerts = BudgetAlertNotifier.collectAlerts(transactions.value, budgets.value)
            BudgetAlertNotifier.notifyAlerts(context.applicationContext, alerts, currency.value)
        }
    }

    fun addCustomExpenseCategory(name: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val ok = profileSettings.addCustomExpenseCategory(name)
            onResult?.invoke(ok)
            if (!ok) {
                _infoMessage.value =
                    "Could not add expense type. Use a non-empty name that is not already listed."
            }
        }
    }

    fun removeCustomExpenseCategory(name: String) {
        viewModelScope.launch { profileSettings.removeCustomExpenseCategory(name) }
    }

    fun upsertAccount(account: Account) {
        viewModelScope.launch { repository.upsertAccount(account) }
    }

    fun deleteAccount(id: String) {
        viewModelScope.launch { repository.deleteAccount(id) }
    }

    suspend fun verifyAppPassword(password: String): Boolean =
        settingsDataStore.verifyAppPassword(password)

    fun changeAppPassword(
        currentPassword: String,
        newPassword: String,
        onResult: (PasswordChangeResult) -> Unit
    ) {
        viewModelScope.launch {
            onResult(settingsDataStore.setAppPassword(currentPassword, newPassword))
        }
    }

    fun addTransaction(transaction: Transaction, repeatMonthly: Boolean) {
        viewModelScope.launch {
            if (repeatMonthly) {
                val rule = RecurringRule(
                    type = transaction.type,
                    category = transaction.category,
                    amount = transaction.amount,
                    note = transaction.note,
                    direction = transaction.direction,
                    day = transaction.date.dayOfMonth,
                    startMonth = YearMonth.from(transaction.date),
                    lastGeneratedMonth = YearMonth.from(transaction.date)
                )
                repository.addRecurringRule(rule)
                repository.addTransaction(transaction.copy(recurringId = rule.id))
            } else {
                repository.addTransaction(transaction)
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch { repository.updateTransaction(transaction) }
    }

    fun deleteTransaction(id: String, context: Context? = null) {
        viewModelScope.launch {
            val existing = transactions.value.find { it.id == id } ?: return@launch
            repository.deleteTransaction(id)
            context?.let { ReceiptStore.deleteIfOwned(it, existing.receiptUri) }
            _undoAction.value = UndoAction.DeleteTransaction(existing)
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            val snapshot = transactions.value
            repository.clearTransactions()
            _undoAction.value = UndoAction.ClearAll(snapshot)
            _infoMessage.value = null
        }
    }

    fun undoLastAction() {
        viewModelScope.launch {
            when (val action = _undoAction.value) {
                is UndoAction.DeleteTransaction -> repository.addTransaction(action.transaction)
                is UndoAction.ClearAll -> repository.addTransactions(action.transactions)
                null -> Unit
            }
            _undoAction.value = null
        }
    }

    fun stopRecurring(ruleId: String) {
        viewModelScope.launch { repository.setRecurringActive(ruleId, active = false) }
    }

    fun toggleRecurringActive(rule: RecurringRule) {
        viewModelScope.launch { repository.setRecurringActive(rule.id, !rule.active) }
    }

    fun deleteRecurringRule(id: String) {
        viewModelScope.launch { repository.deleteRecurringRule(id) }
    }

    fun setBudget(category: String, limit: Double) {
        viewModelScope.launch { repository.setBudget(category, limit) }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch { repository.deleteBudget(category) }
    }

    fun updateRecurringRule(rule: RecurringRule) {
        viewModelScope.launch { repository.updateRecurringRule(rule) }
    }

    fun switchProfile(profileId: String) {
        viewModelScope.launch {
            try {
                profileSession.switchTo(profileId)
                _undoAction.value = null
                _dateFilter.value = DateFilter.All
                _infoMessage.value = "Switched to \"${activeProfile.value.displayName}\"."
            } catch (e: Exception) {
                _infoMessage.value = e.message ?: "Could not switch profile."
            }
        }
    }

    fun setupTemplateProfile(template: AppTemplate) {
        viewModelScope.launch {
            try {
                val profile = profileSession.setupTemplate(template)
                _undoAction.value = null
                _dateFilter.value = DateFilter.All
                _infoMessage.value =
                    "Profile \"${profile.displayName}\" is ready with sample data."
            } catch (e: Exception) {
                _infoMessage.value = e.message ?: "Could not set up template."
            }
        }
    }

    fun deleteTemplateProfile(profileId: String) {
        viewModelScope.launch {
            if (profileId == ProfileIds.PERSONAL) {
                _infoMessage.value = "My Tracker cannot be deleted."
                return@launch
            }
            try {
                val name = profiles.value.find { it.id == profileId }?.displayName ?: "profile"
                profileSession.deleteTemplateProfile(profileId)
                _undoAction.value = null
                _infoMessage.value = "Deleted \"$name\"."
            } catch (e: Exception) {
                _infoMessage.value = e.message ?: "Could not delete profile."
            }
        }
    }

    fun exportBackupJson(): String = BackupManager.toJson(
        BackupManager.buildPayload(
            currency = currency.value,
            transactions = transactions.value,
            budgets = budgets.value,
            recurring = recurringRules.value,
            customExpenseCategories = customExpenseCategories.value,
            accounts = accounts.value,
            exchangeRates = exchangeRates.value
        )
    )

    fun exportCsv(): String = BackupManager.toCsv(transactions.value, currency.value.code)

    fun importBackup(json: String, replace: Boolean): ImportResult {
        val payload = BackupManager.parseJson(json) ?: return ImportResult.Invalid
        val importedTransactions = payload.transactions.map { it.toDomain() }
        val importedBudgets = payload.budgets.map { it.toDomain() }
        val importedRules = payload.recurring.map { it.toDomain() }
        val importedAccounts = payload.accounts.map { it.toDomain() }
        val categoriesFromData = importedTransactions
            .asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .map { it.category }
            .plus(importedBudgets.map { it.category })
            .filterNot { Categories.isBuiltInExpense(it) }
            .toList()
        val importedCustomCategories = Categories.normalizeCustom(
            payload.customExpenseCategories + categoriesFromData
        ).filterNot { Categories.isBuiltInExpense(it) }

        viewModelScope.launch {
            if (replace) {
                repository.replaceAll(
                    importedTransactions, importedBudgets, importedRules, importedAccounts
                )
                profileSettings.setCurrency(AppCurrency.fromCode(payload.currency))
                profileSettings.setCustomExpenseCategories(importedCustomCategories)
                if (payload.exchangeRates.isNotEmpty()) {
                    profileSettings.setExchangeRates(payload.exchangeRates)
                }
                _infoMessage.value =
                    "Backup imported and replaced data in this profile.\n\n${importedTransactions.size} transactions restored."
            } else {
                repository.mergeIn(
                    importedTransactions, importedBudgets, importedRules, importedAccounts
                )
                val merged = Categories.normalizeCustom(
                    customExpenseCategories.value + importedCustomCategories
                )
                profileSettings.setCustomExpenseCategories(merged)
                if (payload.exchangeRates.isNotEmpty()) {
                    profileSettings.setExchangeRates(
                        exchangeRates.value + payload.exchangeRates
                    )
                }
                _infoMessage.value =
                    "Backup imported and merged into this profile.\n\n${importedTransactions.size} transactions added."
            }
        }
        return ImportResult.Success(importedTransactions.size)
    }
}

class MainViewModelFactory(
    private val profileSession: ActiveProfileSession,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(profileSession, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
