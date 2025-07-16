package com.fadlan.fireporter.utils

import com.fadlan.fireporter.model.DateRangeBoundaries
import java.math.BigDecimal
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ReportUtils {
    companion object {
        @JvmStatic
        fun formatCurrency(
            currencySymbol: String = "",
            amount: BigDecimal = BigDecimal.ZERO,
            decimalPlaces: Int = 2
        ): String {
            val patternBuilder = StringBuilder("#,##0")
            if (decimalPlaces > 0) {
                patternBuilder.append(".")
                for (i in 0 until decimalPlaces) {
                    patternBuilder.append("0")
                }
            }

            val formatter = DecimalFormat(patternBuilder.toString())
            return currencySymbol + " " + formatter.format(amount)
        }

        @JvmStatic
        fun formatDate(date: LocalDate?): String {
            if (date == null) return ""

            return date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        }

        @JvmStatic
        fun formatTime(time: LocalTime?): String {
            if (time == null) return ""

            return time.format(DateTimeFormatter.ofPattern("HH:mm"))
        }

        @JvmStatic
        fun getPeriod(dateRange: DateRangeBoundaries): String {
            return if (dateRange.endDate == LocalDate.now()) {
                "${dateRange.period} ${dateRange.year} (${formatDate(dateRange.startDate)}—${formatDate(dateRange.endDate)})"
            } else {
                when (dateRange.period) {
                    "Q1", "Q2", "Q3", "H1", "H2" -> "${dateRange.period} ${dateRange.year} (${formatDate(dateRange.startDate)}—${formatDate(dateRange.endDate)})"
                    else -> "${dateRange.period} ${dateRange.year}"
                }
            }
        }

        @JvmStatic
        fun formatScientific(value: Int, unit: String = ""): String {
            if (value < 1000) return "$value $unit".trim()

            val suffixes = listOf("", "K", "M", "G")
            var scaledValue = value.toDouble()
            var exponent = 0

            while (scaledValue >= 1000 && exponent < suffixes.size - 1) {
                scaledValue /= 1000
                exponent++
            }

            val rounded = if (scaledValue % 1.0 == 0.0) scaledValue.toInt() else "%.1f".format(scaledValue)
            return "$rounded ${suffixes[exponent]}$unit".trim()
        }
    }
}