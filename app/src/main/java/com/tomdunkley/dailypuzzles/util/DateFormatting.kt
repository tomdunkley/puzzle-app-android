package com.tomdunkley.dailypuzzles.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun ordinalSuffix(day: Int): String = when {
    day in 11..13 -> "th"
    day % 10 == 1 -> "st"
    day % 10 == 2 -> "nd"
    day % 10 == 3 -> "rd"
    else -> "th"
}

fun formatDisplayDate(date: LocalDate): String {
    val day = date.dayOfMonth
    return "$day${ordinalSuffix(day)} ${date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))}"
}
