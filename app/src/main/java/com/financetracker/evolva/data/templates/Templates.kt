package com.financetracker.evolva.data.templates

import android.content.Context
import androidx.annotation.StringRes
import com.financetracker.evolva.data.model.Transaction
import com.financetracker.evolva.data.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

private fun rnd(range: IntRange): Double = Random.nextInt(range.first, range.last + 1).toDouble()
private fun <T> pick(list: List<T>): T = list[Random.nextInt(list.size)]

data class IncomeItem(val category: String, @StringRes val noteRes: Int, val day: Int, val amountRange: IntRange, val prob: Double = 1.0)
data class FixedExpenseItem(val category: String, @StringRes val noteRes: Int, val day: Int, val amountRange: IntRange)
data class VariableExpenseGroup(val category: String, val countRange: IntRange, val amountRange: IntRange, val noteResList: List<Int>)
data class OccasionalExpenseItem(val category: String, @StringRes val noteRes: Int, val day: Int, val amountRange: IntRange, val prob: Double)
data class SavingsItem(val category: String, @StringRes val noteRes: Int, val day: Int, val amountRange: IntRange, val prob: Double = 1.0)

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

/** Inclusive month span that may cross years, e.g. Jan 2025–Jul 2026. */
fun monthRange(start: YearMonth, end: YearMonth): List<YearMonth> {
    require(!end.isBefore(start)) { "end must be on or after start" }
    val months = mutableListOf<YearMonth>()
    var current = start
    while (!current.isAfter(end)) {
        months.add(current)
        current = current.plusMonths(1)
    }
    return months
}

fun buildProfileTransactions(context: Context, profile: SpendingProfile, months: List<YearMonth>): List<Transaction> {
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
                        type = TransactionType.INCOME, category = item.category, note = context.getString(item.noteRes),
                        amount = rnd(item.amountRange), date = clampedDate(item.day)
                    )
                )
            }
        }
        profile.fixedExpenses.forEach { item ->
            rows.add(
                Transaction(
                    type = TransactionType.EXPENSE, category = item.category, note = context.getString(item.noteRes),
                    amount = rnd(item.amountRange), date = clampedDate(item.day)
                )
            )
        }
        profile.variableExpenses.forEach { group ->
            val count = Random.nextInt(group.countRange.first, group.countRange.last + 1)
            repeat(count) {
                rows.add(
                    Transaction(
                        type = TransactionType.EXPENSE, category = group.category, note = context.getString(pick(group.noteResList)),
                        amount = rnd(group.amountRange), date = clampedDate(Random.nextInt(1, 28))
                    )
                )
            }
        }
        profile.occasionalExpenses.forEach { item ->
            if (Random.nextDouble() < item.prob) {
                rows.add(
                    Transaction(
                        type = TransactionType.EXPENSE, category = item.category, note = context.getString(item.noteRes),
                        amount = rnd(item.amountRange), date = clampedDate(item.day)
                    )
                )
            }
        }
        profile.savings.forEach { item ->
            if (Random.nextDouble() < item.prob) {
                rows.add(
                    Transaction(
                        type = TransactionType.SAVINGS, category = item.category, note = context.getString(item.noteRes),
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
        IncomeItem("Salary", com.financetracker.evolva.R.string.tpl_note_001, 5, 340..430),
        IncomeItem("Gift", com.financetracker.evolva.R.string.tpl_note_002, 1, 250..330),
        IncomeItem("Other", com.financetracker.evolva.R.string.tpl_note_003, 3, 600..900, prob = 0.4),
        IncomeItem("Freelance", com.financetracker.evolva.R.string.tpl_note_004, 18, 80..160, prob = 0.5)
    ),
    fixedExpenses = listOf(
        FixedExpenseItem("Housing", com.financetracker.evolva.R.string.tpl_note_005, 2, 420..460),
        FixedExpenseItem("Utilities", com.financetracker.evolva.R.string.tpl_note_006, 12, 20..32)
    ),
    variableExpenses = listOf(
        VariableExpenseGroup("Food", 7..9, 8..35, listOf(com.financetracker.evolva.R.string.tpl_note_007, com.financetracker.evolva.R.string.tpl_note_008, com.financetracker.evolva.R.string.tpl_note_009, com.financetracker.evolva.R.string.tpl_note_010, com.financetracker.evolva.R.string.tpl_note_011)),
        VariableExpenseGroup("Transport", 3..4, 8..40, listOf(com.financetracker.evolva.R.string.tpl_note_012, com.financetracker.evolva.R.string.tpl_note_013, com.financetracker.evolva.R.string.tpl_note_014, com.financetracker.evolva.R.string.tpl_note_015)),
        VariableExpenseGroup("Entertainment", 2..3, 10..40, listOf(com.financetracker.evolva.R.string.tpl_note_016, com.financetracker.evolva.R.string.tpl_note_017, com.financetracker.evolva.R.string.tpl_note_018, com.financetracker.evolva.R.string.tpl_note_019)),
        VariableExpenseGroup("Subscriptions", 1..1, 8..16, listOf(com.financetracker.evolva.R.string.tpl_note_020, com.financetracker.evolva.R.string.tpl_note_021)),
        VariableExpenseGroup("Shopping", 1..2, 20..70, listOf(com.financetracker.evolva.R.string.tpl_note_022, com.financetracker.evolva.R.string.tpl_note_023, com.financetracker.evolva.R.string.tpl_note_024)),
        VariableExpenseGroup("Clothing", 1..1, 25..70, listOf(com.financetracker.evolva.R.string.tpl_note_025, com.financetracker.evolva.R.string.tpl_note_026, com.financetracker.evolva.R.string.tpl_note_027)),
        VariableExpenseGroup("Health", 1..1, 15..55, listOf(com.financetracker.evolva.R.string.tpl_note_028, com.financetracker.evolva.R.string.tpl_note_029, com.financetracker.evolva.R.string.tpl_note_030)),
        VariableExpenseGroup("Personal Care", 1..1, 10..35, listOf(com.financetracker.evolva.R.string.tpl_note_031, com.financetracker.evolva.R.string.tpl_note_032)),
        VariableExpenseGroup("Education", 1..2, 15..60, listOf(com.financetracker.evolva.R.string.tpl_note_033, com.financetracker.evolva.R.string.tpl_note_034, com.financetracker.evolva.R.string.tpl_note_035))
    ),
    occasionalExpenses = listOf(
        OccasionalExpenseItem("Education", com.financetracker.evolva.R.string.tpl_note_036, 7, 1000..1300, 0.35),
        OccasionalExpenseItem("Travel", com.financetracker.evolva.R.string.tpl_note_037, 20, 40..120, 0.3),
        OccasionalExpenseItem("Gifts & Donations", com.financetracker.evolva.R.string.tpl_note_038, 22, 15..50, 0.3)
    ),
    savings = listOf(
        SavingsItem("Goal", com.financetracker.evolva.R.string.tpl_note_039, 15, 80..120),
        SavingsItem("Emergency Fund", com.financetracker.evolva.R.string.tpl_note_040, 26, 30..60)
    )
)

val STAFF_PROFILE = SpendingProfile(
    income = listOf(
        IncomeItem("Salary", com.financetracker.evolva.R.string.tpl_note_041, 1, 3100..3350),
        IncomeItem("Investment", com.financetracker.evolva.R.string.tpl_note_042, 20, 40..100, prob = 0.5),
        IncomeItem("Freelance", com.financetracker.evolva.R.string.tpl_note_043, 24, 150..260, prob = 0.4),
        IncomeItem("Other", com.financetracker.evolva.R.string.tpl_note_044, 28, 500..900, prob = 0.15)
    ),
    fixedExpenses = listOf(
        FixedExpenseItem("Housing", com.financetracker.evolva.R.string.tpl_note_091, 1, 1150..1260),
        FixedExpenseItem("Utilities", com.financetracker.evolva.R.string.tpl_note_045, 8, 120..165),
        FixedExpenseItem("Utilities", com.financetracker.evolva.R.string.tpl_note_046, 10, 55..80),
        FixedExpenseItem("Insurance", com.financetracker.evolva.R.string.tpl_note_047, 12, 75..95)
    ),
    variableExpenses = listOf(
        VariableExpenseGroup("Food", 7..9, 10..55, listOf(com.financetracker.evolva.R.string.tpl_note_007, com.financetracker.evolva.R.string.tpl_note_048, com.financetracker.evolva.R.string.tpl_note_010, com.financetracker.evolva.R.string.tpl_note_049, com.financetracker.evolva.R.string.tpl_note_050)),
        VariableExpenseGroup("Transport", 4..5, 12..50, listOf(com.financetracker.evolva.R.string.tpl_note_051, com.financetracker.evolva.R.string.tpl_note_052, com.financetracker.evolva.R.string.tpl_note_013, com.financetracker.evolva.R.string.tpl_note_053)),
        VariableExpenseGroup("Entertainment", 2..3, 15..60, listOf(com.financetracker.evolva.R.string.tpl_note_016, com.financetracker.evolva.R.string.tpl_note_017, com.financetracker.evolva.R.string.tpl_note_054, com.financetracker.evolva.R.string.tpl_note_055)),
        VariableExpenseGroup("Subscriptions", 1..2, 8..20, listOf(com.financetracker.evolva.R.string.tpl_note_020, com.financetracker.evolva.R.string.tpl_note_056, com.financetracker.evolva.R.string.tpl_note_057)),
        VariableExpenseGroup("Shopping", 1..2, 30..100, listOf(com.financetracker.evolva.R.string.tpl_note_058, com.financetracker.evolva.R.string.tpl_note_059, com.financetracker.evolva.R.string.tpl_note_060)),
        VariableExpenseGroup("Clothing", 1..1, 30..90, listOf(com.financetracker.evolva.R.string.tpl_note_061, com.financetracker.evolva.R.string.tpl_note_062, com.financetracker.evolva.R.string.tpl_note_063)),
        VariableExpenseGroup("Health", 1..1, 15..60, listOf(com.financetracker.evolva.R.string.tpl_note_064, com.financetracker.evolva.R.string.tpl_note_029, com.financetracker.evolva.R.string.tpl_note_065)),
        VariableExpenseGroup("Personal Care", 1..1, 15..45, listOf(com.financetracker.evolva.R.string.tpl_note_031, com.financetracker.evolva.R.string.tpl_note_066, com.financetracker.evolva.R.string.tpl_note_067))
    ),
    occasionalExpenses = listOf(
        OccasionalExpenseItem("Travel", com.financetracker.evolva.R.string.tpl_note_068, 22, 80..250, 0.3),
        OccasionalExpenseItem("Gifts & Donations", com.financetracker.evolva.R.string.tpl_note_069, 25, 20..80, 0.35)
    ),
    savings = listOf(
        SavingsItem("Retirement", com.financetracker.evolva.R.string.tpl_note_070, 18, 280..330),
        SavingsItem("Emergency Fund", com.financetracker.evolva.R.string.tpl_note_071, 26, 120..190)
    )
)

val BUDGET_EXAMPLE_PROFILE = SpendingProfile(
    income = listOf(
        IncomeItem("Salary", com.financetracker.evolva.R.string.tpl_note_041, 1, 3900..4100),
        IncomeItem("Other", com.financetracker.evolva.R.string.tpl_note_072, 5, 280..320),
        IncomeItem("Other", com.financetracker.evolva.R.string.tpl_note_073, 28, 300..600, prob = 0.15)
    ),
    fixedExpenses = listOf(
        FixedExpenseItem("Housing", com.financetracker.evolva.R.string.tpl_note_091, 1, 1150..1250),
        FixedExpenseItem("Utilities", com.financetracker.evolva.R.string.tpl_note_074, 8, 140..230),
        FixedExpenseItem("Insurance", com.financetracker.evolva.R.string.tpl_note_075, 12, 85..105)
    ),
    variableExpenses = listOf(
        VariableExpenseGroup("Food", 6..8, 15..60, listOf(com.financetracker.evolva.R.string.tpl_note_007, com.financetracker.evolva.R.string.tpl_note_048, com.financetracker.evolva.R.string.tpl_note_010, com.financetracker.evolva.R.string.tpl_note_076, com.financetracker.evolva.R.string.tpl_note_077, com.financetracker.evolva.R.string.tpl_note_078)),
        VariableExpenseGroup("Transport", 3..4, 15..55, listOf(com.financetracker.evolva.R.string.tpl_note_051, com.financetracker.evolva.R.string.tpl_note_052, com.financetracker.evolva.R.string.tpl_note_079, com.financetracker.evolva.R.string.tpl_note_013)),
        VariableExpenseGroup("Entertainment", 2..3, 15..50, listOf(com.financetracker.evolva.R.string.tpl_note_016, com.financetracker.evolva.R.string.tpl_note_080, com.financetracker.evolva.R.string.tpl_note_081, com.financetracker.evolva.R.string.tpl_note_082)),
        VariableExpenseGroup("Shopping", 2..3, 25..90, listOf(com.financetracker.evolva.R.string.tpl_note_083, com.financetracker.evolva.R.string.tpl_note_084, com.financetracker.evolva.R.string.tpl_note_059, com.financetracker.evolva.R.string.tpl_note_085)),
        VariableExpenseGroup("Health", 1..1, 15..60, listOf(com.financetracker.evolva.R.string.tpl_note_064, com.financetracker.evolva.R.string.tpl_note_029, com.financetracker.evolva.R.string.tpl_note_065)),
        VariableExpenseGroup("Personal Care", 1..1, 10..40, listOf(com.financetracker.evolva.R.string.tpl_note_031, com.financetracker.evolva.R.string.tpl_note_066, com.financetracker.evolva.R.string.tpl_note_032)),
        VariableExpenseGroup("Subscriptions", 1..1, 8..20, listOf(com.financetracker.evolva.R.string.tpl_note_086, com.financetracker.evolva.R.string.tpl_note_056)),
        VariableExpenseGroup("Clothing", 1..1, 25..80, listOf(com.financetracker.evolva.R.string.tpl_note_061, com.financetracker.evolva.R.string.tpl_note_062))
    ),
    occasionalExpenses = listOf(
        OccasionalExpenseItem("Travel", com.financetracker.evolva.R.string.tpl_note_087, 20, 100..300, 0.25),
        OccasionalExpenseItem("Gifts & Donations", com.financetracker.evolva.R.string.tpl_note_069, 24, 20..80, 0.3),
        OccasionalExpenseItem("Education", com.financetracker.evolva.R.string.tpl_note_088, 15, 50..200, 0.2)
    ),
    savings = listOf(
        SavingsItem("Emergency Fund", com.financetracker.evolva.R.string.tpl_note_089, 20, 140..180),
        SavingsItem("Retirement", com.financetracker.evolva.R.string.tpl_note_090, 22, 180..220)
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

val PROFESSIONAL_LONG_LIMITS: Map<String, Double> = mapOf(
    "Housing" to 1250.0,
    "Utilities" to 250.0,
    "Insurance" to 100.0,
    "Food" to 550.0,
    "Transport" to 220.0,
    "Entertainment" to 160.0,
    "Shopping" to 200.0,
    "Health" to 120.0
)

data class AppTemplate(
    val id: String,
    val labelRes: Int,
    val descriptionRes: Int,
    val budgets: Map<String, Double>? = null,
    val generate: (Context) -> List<Transaction>
)

// Add more templates here later — each just needs an id, labelRes, descriptionRes,
// optional starter budgets, and a generate() that calls buildProfileTransactions().
val TEMPLATES: List<AppTemplate> = listOf(
    AppTemplate(
        id = "student",
        labelRes = com.financetracker.evolva.R.string.tpl_student_label,
        descriptionRes = com.financetracker.evolva.R.string.tpl_student_desc,
        generate = { ctx -> buildProfileTransactions(ctx, STUDENT_PROFILE, monthsBack(6)) }
    ),
    AppTemplate(
        id = "staff",
        labelRes = com.financetracker.evolva.R.string.tpl_staff_label,
        descriptionRes = com.financetracker.evolva.R.string.tpl_staff_desc,
        generate = { ctx -> buildProfileTransactions(ctx, STAFF_PROFILE, monthsBack(6)) }
    ),
    AppTemplate(
        id = "budget2026",
        labelRes = com.financetracker.evolva.R.string.tpl_budget2026_label,
        descriptionRes = com.financetracker.evolva.R.string.tpl_budget2026_desc,
        budgets = BUDGET_EXAMPLE_LIMITS,
        generate = { ctx -> buildProfileTransactions(ctx, BUDGET_EXAMPLE_PROFILE, fixedMonths(2026, 1, 7)) }
    ),
    AppTemplate(
        id = "professional_2025_2026",
        labelRes = com.financetracker.evolva.R.string.tpl_pro_label,
        descriptionRes = com.financetracker.evolva.R.string.tpl_pro_desc,
        budgets = PROFESSIONAL_LONG_LIMITS,
        generate = { ctx ->
            buildProfileTransactions(
                ctx,
                STAFF_PROFILE,
                monthRange(YearMonth.of(2025, 1), YearMonth.of(2026, 7))
            )
        }
    )
)
