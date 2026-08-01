package com.financetracker.app.data.repository

import com.financetracker.app.data.calc.RecurringEngine
import com.financetracker.app.data.db.BudgetEntity
import com.financetracker.app.data.db.FinanceDao
import com.financetracker.app.data.db.toDomain
import com.financetracker.app.data.db.toEntity
import com.financetracker.app.data.model.Budget
import com.financetracker.app.data.model.RecurringRule
import com.financetracker.app.data.model.Transaction
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

    val transactions: Flow<List<Transaction>> =
        dao.getAllTransactions().map { list -> list.map { it.toDomain() } }

    val budgets: Flow<List<Budget>> =
        dao.getAllBudgets().map { list -> list.map { it.toDomain() } }

    val recurringRules: Flow<List<RecurringRule>> =
        dao.getAllRecurringRules().map { list -> list.map { it.toDomain() } }

    suspend fun addTransaction(transaction: Transaction) =
        dao.insertTransaction(transaction.toEntity())

    suspend fun addTransactions(transactions: List<Transaction>) =
        dao.insertTransactions(transactions.map { it.toEntity() })

    suspend fun updateTransaction(transaction: Transaction) =
        dao.updateTransaction(transaction.toEntity())

    suspend fun deleteTransaction(id: String) = dao.deleteTransactionById(id)

    suspend fun setBudget(category: String, limit: Double) {
        if (limit <= 0) dao.deleteBudget(category) else dao.upsertBudget(BudgetEntity(category, limit))
    }

    /** Used when loading a starter template: never overwrites a limit the user already set. */
    suspend fun setBudgetsIfAbsent(newBudgets: Map<String, Double>) {
        val existingCats = budgets.first().map { it.category }.toSet()
        newBudgets.forEach { (cat, limit) ->
            if (cat !in existingCats) dao.upsertBudget(BudgetEntity(cat, limit))
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

    suspend fun replaceAll(transactions: List<Transaction>, budgets: List<Budget>, rules: List<RecurringRule>) {
        dao.deleteAllTransactions()
        dao.deleteAllBudgets()
        dao.deleteAllRecurringRules()
        dao.insertTransactions(transactions.map { it.toEntity() })
        budgets.forEach { dao.upsertBudget(it.toEntity()) }
        dao.insertRecurringRules(rules.map { it.toEntity() })
    }

    suspend fun mergeIn(transactions: List<Transaction>, budgets: List<Budget>, rules: List<RecurringRule>) {
        dao.insertTransactions(transactions.map { it.toEntity() })
        val existingCats = this.budgets.first().map { it.category }.toSet()
        budgets.forEach { if (it.category !in existingCats) dao.upsertBudget(it.toEntity()) }
        dao.insertRecurringRules(rules.map { it.toEntity() })
    }

    suspend fun clearTransactions() = dao.deleteAllTransactions()
}
