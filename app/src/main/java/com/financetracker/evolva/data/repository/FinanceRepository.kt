package com.financetracker.evolva.data.repository

import com.financetracker.evolva.data.calc.RecurringEngine
import com.financetracker.evolva.data.db.BudgetEntity
import com.financetracker.evolva.data.db.FinanceDao
import com.financetracker.evolva.data.db.toDomain
import com.financetracker.evolva.data.db.toEntity
import com.financetracker.evolva.data.model.Account
import com.financetracker.evolva.data.model.Budget
import com.financetracker.evolva.data.model.RecurringRule
import com.financetracker.evolva.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for all persisted data. Mirrors the web app's
 * approach of keeping derived stats (totals, averages, forecasts, budget
 * progress) as pure functions over the full in-memory list rather than SQL
 * aggregations — see data.calc.FinanceCalculator / ForecastCalculator.
 */
class FinanceRepository(private val dao: FinanceDao) {

    val accounts: Flow<List<Account>> =
        dao.getAllAccounts().map { list -> list.map { it.toDomain() } }

    val transactions: Flow<List<Transaction>> =
        dao.getAllTransactions().map { list -> list.map { it.toDomain() } }

    val budgets: Flow<List<Budget>> =
        dao.getAllBudgets().map { list -> list.map { it.toDomain() } }

    val recurringRules: Flow<List<RecurringRule>> =
        dao.getAllRecurringRules().map { list -> list.map { it.toDomain() } }

    suspend fun ensureDefaultAccounts() {
        if (dao.accountCount() == 0) {
            dao.upsertAccounts(Account.defaults().map { it.toEntity() })
        }
    }

    suspend fun upsertAccount(account: Account) = dao.upsertAccount(account.toEntity())

    suspend fun deleteAccount(id: String) = dao.deleteAccount(id)

    suspend fun addTransaction(transaction: Transaction) =
        dao.insertTransaction(transaction.toEntity())

    suspend fun addTransactions(transactions: List<Transaction>) =
        dao.insertTransactions(transactions.map { it.toEntity() })

    suspend fun updateTransaction(transaction: Transaction) =
        dao.updateTransaction(transaction.toEntity())

    suspend fun deleteTransaction(id: String) = dao.deleteTransactionById(id)

    suspend fun setBudget(category: String, limit: Double) {
        if (limit <= 0) {
            dao.deleteBudget(category)
        } else {
            val existing = budgets.first().find { it.category == category }
            dao.upsertBudget(
                BudgetEntity(
                    category = category,
                    limitAmount = limit,
                    locked = existing?.locked ?: false
                )
            )
        }
    }

    suspend fun upsertBudget(budget: Budget) = dao.upsertBudget(budget.toEntity())

    suspend fun deleteBudget(category: String) = dao.deleteBudget(category)

    suspend fun setBudgetLocked(category: String, locked: Boolean) {
        val existing = budgets.first().find { it.category == category } ?: return
        if (existing.locked == locked) return
        dao.upsertBudget(existing.copy(locked = locked).toEntity())
    }

    suspend fun updateRecurringRule(rule: RecurringRule) =
        dao.updateRecurringRule(rule.toEntity())

    /** Used when loading a starter template: never overwrites a limit the user already set. */
    suspend fun setBudgetsIfAbsent(newBudgets: Map<String, Double>) {
        val existingCats = budgets.first().map { it.category }.toSet()
        newBudgets.forEach { (cat, limit) ->
            if (cat !in existingCats) {
                dao.upsertBudget(BudgetEntity(category = cat, limitAmount = limit, locked = false))
            }
        }
    }

    suspend fun addRecurringRule(rule: RecurringRule) = dao.insertRecurringRule(rule.toEntity())

    suspend fun setRecurringActive(id: String, active: Boolean) {
        val rule = recurringRules.first().find { it.id == id } ?: return
        dao.updateRecurringRule(rule.copy(active = active).toEntity())
    }

    suspend fun deleteRecurringRule(id: String) = dao.deleteRecurringRule(id)

    /** Backfills any recurring occurrences that are due but not yet generated. Call on app start. */
    suspend fun runRecurringEngine() {
        val rules = recurringRules.first()
        val result = RecurringEngine.runOnce(rules)
        if (result.newTransactions.isNotEmpty()) {
            dao.insertTransactions(result.newTransactions.map { it.toEntity() })
        }
        result.updatedRules.forEach { dao.updateRecurringRule(it.toEntity()) }
    }

    suspend fun replaceAll(
        transactions: List<Transaction>,
        budgets: List<Budget>,
        rules: List<RecurringRule>,
        accounts: List<Account> = emptyList()
    ) {
        dao.deleteAllTransactions()
        dao.deleteAllBudgets()
        dao.deleteAllRecurringRules()
        dao.deleteAllAccounts()
        if (accounts.isEmpty()) {
            dao.upsertAccounts(Account.defaults().map { it.toEntity() })
        } else {
            dao.upsertAccounts(accounts.map { it.toEntity() })
        }
        dao.insertTransactions(transactions.map { it.toEntity() })
        budgets.forEach { dao.upsertBudget(it.toEntity()) }
        dao.insertRecurringRules(rules.map { it.toEntity() })
    }

    suspend fun mergeIn(
        transactions: List<Transaction>,
        budgets: List<Budget>,
        rules: List<RecurringRule>,
        accounts: List<Account> = emptyList()
    ) {
        ensureDefaultAccounts()
        accounts.forEach { dao.upsertAccount(it.toEntity()) }
        dao.insertTransactions(transactions.map { it.toEntity() })
        val existingCats = this.budgets.first().map { it.category }.toSet()
        budgets.forEach { if (it.category !in existingCats) dao.upsertBudget(it.toEntity()) }
        dao.insertRecurringRules(rules.map { it.toEntity() })
    }

    suspend fun clearTransactions() = dao.deleteAllTransactions()
}
