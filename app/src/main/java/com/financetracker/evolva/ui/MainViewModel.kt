package com.financetracker.evolva.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.financetracker.evolva.data.backup.BackupManager
import com.financetracker.evolva.data.backup.toDomain
import com.financetracker.evolva.data.model.AppCurrency
import com.financetracker.evolva.data.model.Budget
import com.financetracker.evolva.data.model.DateFilter
import com.financetracker.evolva.data.model.RecurringRule
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.filteredBy
import com.financetracker.evolva.data.prefs.PasswordChangeResult
import com.financetracker.evolva.data.prefs.SettingsDataStore
import com.financetracker.evolva.data.repository.FinanceRepository
import com.financetracker.evolva.data.templates.AppTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

sealed class ImportResult {
    data class Success(val transactionCount: Int) : ImportResult()
    object Invalid : ImportResult()
}

/**
 * Single shared ViewModel for the whole app (Dashboard/Transactions/Budget/
 * Settings all read from this), scoped to the Activity so every tab sees
 * the same live data — matching the web app, where everything reads from
 * one in-memory `transactions` array.
 */
class MainViewModel(
    private val repository: FinanceRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = repository.budgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringRules: StateFlow<List<RecurringRule>> = repository.recurringRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currency: StateFlow<AppCurrency> = settingsDataStore.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppCurrency.MYR)

    val hasAppPassword: StateFlow<Boolean> = settingsDataStore.hasAppPassword
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _dateFilter = MutableStateFlow<DateFilter>(DateFilter.All)
    val dateFilter: StateFlow<DateFilter> = _dateFilter

    val filteredTransactions: StateFlow<List<Transaction>> =
        combine(transactions, _dateFilter) { txs, filter -> txs.filteredBy(filter) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage

    private val _forecastHorizon = MutableStateFlow(6)
    val forecastHorizon: StateFlow<Int> = _forecastHorizon

    init {
        viewModelScope.launch { repository.runRecurringEngine() }
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

    fun showInfoMessage(message: String) {
        _infoMessage.value = message
    }

    fun setCurrency(currency: AppCurrency) {
        viewModelScope.launch { settingsDataStore.setCurrency(currency) }
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

    fun deleteTransaction(id: String) {
        viewModelScope.launch { repository.deleteTransaction(id) }
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

    fun loadTemplate(template: AppTemplate) {
        viewModelScope.launch {
            val rows = template.generate()
            repository.addTransactions(rows)
            template.budgets?.let { repository.setBudgetsIfAbsent(it) }
            val budgetNote = if (template.budgets != null) " Budget limits were applied where missing." else ""
            _infoMessage.value =
                "Template \"${template.label}\" loaded.\n\n${rows.size} transactions were added.$budgetNote"
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            val count = transactions.value.size
            repository.clearTransactions()
            _infoMessage.value =
                "All transactions cleared.\n\n$count transaction${if (count == 1) "" else "s"} removed. Budgets and recurring rules were kept."
        }
    }

    fun exportBackupJson(): String = BackupManager.toJson(
        BackupManager.buildPayload(currency.value, transactions.value, budgets.value, recurringRules.value)
    )

    fun exportCsv(): String = BackupManager.toCsv(transactions.value, currency.value.code)

    fun importBackup(json: String, replace: Boolean): ImportResult {
        val payload = BackupManager.parseJson(json) ?: return ImportResult.Invalid
        val importedTransactions = payload.transactions.map { it.toDomain() }
        val importedBudgets = payload.budgets.map { it.toDomain() }
        val importedRules = payload.recurring.map { it.toDomain() }
        viewModelScope.launch {
            if (replace) {
                repository.replaceAll(importedTransactions, importedBudgets, importedRules)
                settingsDataStore.setCurrency(AppCurrency.fromCode(payload.currency))
                _infoMessage.value =
                    "Backup imported and replaced existing data.\n\n${importedTransactions.size} transactions restored."
            } else {
                repository.mergeIn(importedTransactions, importedBudgets, importedRules)
                _infoMessage.value =
                    "Backup imported and merged.\n\n${importedTransactions.size} transactions added."
            }
        }
        return ImportResult.Success(importedTransactions.size)
    }
}

class MainViewModelFactory(
    private val repository: FinanceRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
