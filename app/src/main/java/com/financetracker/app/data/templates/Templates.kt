package com.financetracker.app.data.templates

import com.financetracker.app.data.model.Transaction
import com.financetracker.app.data.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

private fun rnd(range: IntRange): Double = Random.nextInt(range.first, range.last + 1).toDouble()
private fun <T> pick(list: List<T>): T = list[Random.nextInt(list.size)]

data class IncomeItem(val category: String, val note: String, val day: Int, val amountRange: IntRange, val prob: Double = 1.0)
data class FixedExpenseItem(val category: String, val note: String, val day: Int, val amountRange: IntRange)
data class VariableExpenseGroup(val category: String, val countRange: IntRange, val amountRange: IntRange, val notes: List<String>)
data class OccasionalExpenseItem(val category: String, val note: String, val day: Int, val amountRange: IntRange, val prob: Double)
data class SavingsItem(val category: String, val note: String, val day: Int, val amountRange: IntRange, val prob: Double = 1.0)

data class SpendingProfile(
    val income: List<IncomeItem>,
    val fixedExpenses: List<FixedExpenseItem>,
    val variableExpenses: List<VariableExpenseGroup>,
    val occasionalExpenses: List<OccasionalExpenseItem>,
    val savings: List<SavingsItem>
)

/** [n] complete months ending at last month — excludes the current,
 * still-in-progress month so every generated month can hit the
 * 20-transaction minimum without piling everything onto today. */
fun monthsBack(n: Int): List<YearMonth> {
    val now = YearMonth.now()
    return (n downTo 1).map { now.minusMonths(it.toLong()) }
}

/** A fixed run of calendar months, e.g. fixedMonths(2026, 1, 7) = Jan–Jul 2026. */
fun fixedMonths(year: Int, startMonth1: Int, endMonth1: Int): List<YearMonth> =
    (startMonth1..endMonth1).map { YearMonth.of(year, it) }

fun buildProfileTransactions(profile: SpendingProfile, months: List<YearMonth>): List<Transaction> {
    val today = LocalDate.now()
    val rows = mutableListOf<Transaction>()

    months.forEach { month ->
        fun clampedDate(day: Int): LocalDate {
            val dim = month.lengthOfMonth()
            val date = month.atDay(day.coerceIn(1, dim))
            return if (date.isAfter(today)) today else date
        }

        profile.income.forEach { item ->
            if (Random.nextDouble() < item.prob) {
                rows.add(
                    Transaction(
                        type = TransactionType.INCOME, category = item.category, note = item.note,
                        amount = rnd(item.amountRange), date = clampedDate(item.day)
                    )
                )
            }
        }
        profile.fixedExpenses.forEach { item ->
            rows.add(
                Transaction(
                    type = TransactionType.EXPENSE, category = item.category, note = item.note,
                    amount = rnd(item.amountRange), date = clampedDate(item.day)
                )
            )
        }
        profile.variableExpenses.forEach { group ->
            val count = Random.nextInt(group.countRange.first, group.countRange.last + 1)
            repeat(count) {
                rows.add(
                    Transaction(
                        type = TransactionType.EXPENSE, category = group.category, note = pick(group.notes),
                        amount = rnd(group.amountRange), date = clampedDate(Random.nextInt(1, 28))
                    )
                )
            }
        }
        profile.occasionalExpenses.forEach { item ->
            if (Random.nextDouble() < item.prob) {
                rows.add(
                    Transaction(
                        type = TransactionType.EXPENSE, category = item.category, note = item.note,
                        amount = rnd(item.amountRange), date = clampedDate(item.day)
                    )
                )
            }
        }
        profile.savings.forEach { item ->
            if (Random.nextDouble() < item.prob) {
                rows.add(
                    Transaction(
                        type = TransactionType.SAVINGS, category = item.category, note = item.note,
                        amount = rnd(item.amountRange), date = clampedDate(item.day)
                    )
                )
            }
        }
    }

    return rows.sortedBy { it.date }
}

val STUDENT_PROFILE = SpendingProfile(
    income = listOf(
        IncomeItem("Salary", "Part-time job — campus café", 5, 340..430),
        IncomeItem("Gift", "Allowance from parents", 1, 250..330),
        IncomeItem("Other", "Scholarship disbursement", 3, 600..900, prob = 0.4),
        IncomeItem("Freelance", "Tutoring side gig", 18, 80..160, prob = 0.5)
    ),
    fixedExpenses = listOf(
        FixedExpenseItem("Housing", "Dorm rent", 2, 420..460),
        FixedExpenseItem("Utilities", "Phone bill", 12, 20..32)
    ),
    variableExpenses = listOf(
        VariableExpenseGroup("Food", 7..9, 8..35, listOf("Groceries", "Meal plan top-up", "Campus cafeteria", "Coffee run", "Late-night snack")),
        VariableExpenseGroup("Transport", 3..4, 8..40, listOf("Bus pass top-up", "Ride-hailing", "Bike rental", "Train ticket")),
        VariableExpenseGroup("Entertainment", 2..3, 10..40, listOf("Movie night", "Concert ticket", "Karaoke with friends", "Board game café")),
        VariableExpenseGroup("Subscriptions", 1..1, 8..16, listOf("Streaming subscription", "Music subscription")),
        VariableExpenseGroup("Shopping", 1..2, 20..70, listOf("Stationery", "Phone case", "Dorm supplies")),
        VariableExpenseGroup("Clothing", 1..1, 25..70, listOf("New clothes", "Sneakers", "Winter jacket")),
        VariableExpenseGroup("Health", 1..1, 15..55, listOf("Pharmacy run", "Clinic visit", "Vitamins")),
        VariableExpenseGroup("Personal Care", 1..1, 10..35, listOf("Haircut", "Toiletries")),
        VariableExpenseGroup("Education", 1..2, 15..60, listOf("Textbooks & course materials", "Printing & supplies", "Exam fee"))
    ),
    occasionalExpenses = listOf(
        OccasionalExpenseItem("Education", "Tuition installment", 7, 1000..1300, 0.35),
        OccasionalExpenseItem("Travel", "Weekend trip home", 20, 40..120, 0.3),
        OccasionalExpenseItem("Gifts & Donations", "Birthday gift for a friend", 22, 15..50, 0.3)
    ),
    savings = listOf(
        SavingsItem("Goal", "Laptop fund", 15, 80..120),
        SavingsItem("Emergency Fund", "Emergency cushion", 26, 30..60)
    )
)

val STAFF_PROFILE = SpendingProfile(
    income = listOf(
        IncomeItem("Salary", "Monthly salary", 1, 3100..3350),
        IncomeItem("Investment", "Dividend payout", 20, 40..100, prob = 0.5),
        IncomeItem("Freelance", "Weekend consulting gig", 24, 150..260, prob = 0.4),
        IncomeItem("Other", "Year-end bonus", 28, 500..900, prob = 0.15)
    ),
    fixedExpenses = listOf(
        FixedExpenseItem("Housing", "Rent", 1, 1150..1260),
        FixedExpenseItem("Utilities", "Electricity & water", 8, 120..165),
        FixedExpenseItem("Utilities", "Internet & phone", 10, 55..80),
        FixedExpenseItem("Insurance", "Health insurance premium", 12, 75..95)
    ),
    variableExpenses = listOf(
        VariableExpenseGroup("Food", 7..9, 10..55, listOf("Groceries", "Dining out", "Coffee run", "Work lunch", "Weekend brunch")),
        VariableExpenseGroup("Transport", 4..5, 12..50, listOf("Fuel", "Parking", "Ride-hailing", "Toll")),
        VariableExpenseGroup("Entertainment", 2..3, 15..60, listOf("Movie night", "Concert ticket", "Bowling night", "Karaoke")),
        VariableExpenseGroup("Subscriptions", 1..2, 8..20, listOf("Streaming subscription", "Cloud storage", "Gym app")),
        VariableExpenseGroup("Shopping", 1..2, 30..100, listOf("Home essentials", "Electronics accessory", "Kitchenware")),
        VariableExpenseGroup("Clothing", 1..1, 30..90, listOf("New outfit", "Shoes", "Work attire")),
        VariableExpenseGroup("Health", 1..1, 15..60, listOf("Pharmacy", "Clinic visit", "Dental checkup")),
        VariableExpenseGroup("Personal Care", 1..1, 15..45, listOf("Haircut", "Skincare", "Spa"))
    ),
    occasionalExpenses = listOf(
        OccasionalExpenseItem("Travel", "Weekend getaway", 22, 80..250, 0.3),
        OccasionalExpenseItem("Gifts & Donations", "Birthday gift / charity donation", 25, 20..80, 0.35)
    ),
    savings = listOf(
        SavingsItem("Retirement", "EPF / retirement contribution", 18, 280..330),
        SavingsItem("Emergency Fund", "Emergency fund", 26, 120..190)
    )
)

val BUDGET_EXAMPLE_PROFILE = SpendingProfile(
    income = listOf(
        IncomeItem("Salary", "Monthly salary", 1, 3900..4100),
        IncomeItem("Other", "Rental income", 5, 280..320),
        IncomeItem("Other", "Performance bonus", 28, 300..600, prob = 0.15)
    ),
    fixedExpenses = listOf(
        FixedExpenseItem("Housing", "Rent", 1, 1150..1250),
        FixedExpenseItem("Utilities", "Electricity, water & internet", 8, 140..230),
        FixedExpenseItem("Insurance", "Home & health insurance", 12, 85..105)
    ),
    variableExpenses = listOf(
        VariableExpenseGroup("Food", 6..8, 15..60, listOf("Groceries", "Dining out", "Coffee run", "Market run", "Family dinner", "Snacks")),
        VariableExpenseGroup("Transport", 3..4, 15..55, listOf("Fuel", "Parking", "Public transit", "Ride-hailing")),
        VariableExpenseGroup("Entertainment", 2..3, 15..50, listOf("Movie night", "Family outing", "Weekend activity", "Streaming rental")),
        VariableExpenseGroup("Shopping", 2..3, 25..90, listOf("Household items", "Clothing basics", "Electronics accessory", "Home decor")),
        VariableExpenseGroup("Health", 1..1, 15..60, listOf("Pharmacy", "Clinic visit", "Dental checkup")),
        VariableExpenseGroup("Personal Care", 1..1, 10..40, listOf("Haircut", "Skincare", "Toiletries")),
        VariableExpenseGroup("Subscriptions", 1..1, 8..20, listOf("Streaming service", "Cloud storage")),
        VariableExpenseGroup("Clothing", 1..1, 25..80, listOf("New outfit", "Shoes"))
    ),
    occasionalExpenses = listOf(
        OccasionalExpenseItem("Travel", "Family trip", 20, 100..300, 0.25),
        OccasionalExpenseItem("Gifts & Donations", "Birthday gift / charity donation", 24, 20..80, 0.3),
        OccasionalExpenseItem("Education", "Course / certification fee", 15, 50..200, 0.2)
    ),
    savings = listOf(
        SavingsItem("Emergency Fund", "Emergency fund contribution", 20, 140..180),
        SavingsItem("Retirement", "Retirement contribution", 22, 180..220)
    )
)

val BUDGET_EXAMPLE_LIMITS: Map<String, Double> = mapOf(
    "Housing" to 1200.0,
    "Utilities" to 220.0,
    "Insurance" to 100.0,
    "Food" to 500.0,
    "Transport" to 200.0,
    "Entertainment" to 150.0,
    "Shopping" to 220.0
)

data class AppTemplate(
    val id: String,
    val label: String,
    val description: String,
    val budgets: Map<String, Double>? = null,
    val generate: () -> List<Transaction>
)

// Add more templates here later — each just needs an id, label, description,
// optional starter budgets, and a generate() that calls buildProfileTransactions().
val TEMPLATES: List<AppTemplate> = listOf(
    AppTemplate(
        id = "student",
        label = "Student expenses",
        description = "6 months of typical student income, spending, and savings — 20+ transactions per month.",
        generate = { buildProfileTransactions(STUDENT_PROFILE, monthsBack(6)) }
    ),
    AppTemplate(
        id = "staff",
        label = "Working professional",
        description = "6 months of a typical salaried employee's income, bills, and savings — 20+ transactions per month.",
        generate = { buildProfileTransactions(STAFF_PROFILE, monthsBack(6)) }
    ),
    AppTemplate(
        id = "budget2026",
        label = "Budget example (Jan–Jul 2026)",
        description = "Regular income and everyday expenses dated Jan–Jul 2026, with example category budget limits included.",
        budgets = BUDGET_EXAMPLE_LIMITS,
        generate = { buildProfileTransactions(BUDGET_EXAMPLE_PROFILE, fixedMonths(2026, 1, 7)) }
    )
)
