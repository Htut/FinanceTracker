package com.financetracker.evolva.data.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

sealed class DateFilter {
    data object All : DateFilter()
    data class Month(val month: YearMonth) : DateFilter()
    data class Year(val year: Int) : DateFilter()
    data class Range(val start: LocalDate, val end: LocalDate) : DateFilter()

    fun contains(date: LocalDate): Boolean = when (this) {
        All -> true
        is Month -> YearMonth.from(date) == month
        is Year -> date.year == year
        is Range -> !date.isBefore(start) && !date.isAfter(end)
    }

    fun label(allDatesLabel: String): String = when (this) {
        All -> allDatesLabel
        is Month -> month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.year
        is Year -> year.toString()
        is Range -> {
            val fmt = DateTimeFormatter.ISO_LOCAL_DATE
            "${start.format(fmt)} → ${end.format(fmt)}"
        }
    }

}

fun List<Transaction>.filteredBy(filter: DateFilter): List<Transaction> =
    filter { filter.contains(it.date) }
