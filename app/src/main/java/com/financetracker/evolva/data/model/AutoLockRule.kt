package com.financetracker.evolva.data.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * Per-profile rule that auto-locks older Activity records.
 * [days] = 0 means off; -1 means lock all months before the current month.
 */
enum class AutoLockRule(val days: Int) {
    OFF(0),
    AFTER_7_DAYS(7),
    AFTER_30_DAYS(30),
    AFTER_90_DAYS(90),
    PREVIOUS_MONTHS(-1);

    fun shouldLock(date: LocalDate, today: LocalDate = LocalDate.now()): Boolean = when (this) {
        OFF -> false
        PREVIOUS_MONTHS -> YearMonth.from(date).isBefore(YearMonth.from(today))
        else -> !date.isAfter(today.minusDays(days.toLong()))
    }

    companion object {
        fun fromDays(days: Int?): AutoLockRule =
            entries.find { it.days == days } ?: OFF
    }
}
