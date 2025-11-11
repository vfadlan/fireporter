package com.fadlan.fireporter.utils

import com.fadlan.fireporter.model.DateRangeBoundaries
import java.time.*
import java.time.format.DateTimeFormatter

object DateRangeResolver {
    fun resolve(period: String, year: Int): DateRangeBoundaries {
        val textDateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy")

        val (startMonth, endMonth) = when (period) {
            "Q1" -> "January" to "March"
            "Q2" -> "April" to "June"
            "Q3" -> "July" to "September"
            "Q4" -> "October" to "November"
            "H1" -> "January" to "June"
            "H2" -> "July" to "December"
            "All Year" -> "January" to "December"
            else -> period to period
        }

        val startDate = LocalDate.parse("1 $startMonth $year", textDateFormat)
        val endDate = YearMonth.parse("1 $endMonth $year", textDateFormat)
            .atEndOfMonth()
            .coerceAtMost(LocalDate.now())

        return DateRangeBoundaries(
            startDate,
            endDate,
            period,
            year
        )
    }
}

