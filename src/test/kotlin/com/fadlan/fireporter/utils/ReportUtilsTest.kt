package com.fadlan.fireporter.utils

import com.fadlan.fireporter.model.DateRangeBoundaries
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReportUtilsTest : StringSpec({
    // formatCurrency
    "should format currency with 2 decimal places and symbol" {
        ReportUtils.formatCurrency("$", 1234.5.toBigDecimal(), 2) shouldBe "$ 1,234.50"
    }

    "should format currency with 0 decimal places" {
        ReportUtils.formatCurrency("Rp", BigDecimal("1000000"), 0) shouldBe "Rp 1,000,000"
    }

    "should format with 3 decimals" {
        ReportUtils.formatCurrency("¢", 56.78.toBigDecimal(), 3) shouldBe "¢ 56.780"
    }

    "should format large number with 3 decimals" {
        ReportUtils.formatCurrency("£", BigDecimal("1234567.891"), 3) shouldBe "£ 1,234,567.891"
    }


    // formatDate
    "should format LocalDate to full English date" {
        val date = LocalDate.of(2023, 6, 18)
        ReportUtils.formatDate(date) shouldBe "18 Jun 2023"
    }

    "should return empty string for null date" {
        ReportUtils.formatDate(null) shouldBe ""
    }

    "should format single-digit day and month correctly" {
        val date = LocalDate.of(2021, 1, 5)
        ReportUtils.formatDate(date) shouldBe "5 Jan 2021"
    }

    // formatTime
    "should format LocalTime to 24h format time" {
        val dateTime = LocalDateTime.of(LocalDate.of(2022, 5, 1), LocalTime.of(20, 15, 0))
        ReportUtils.formatTime(dateTime.toLocalTime()) shouldBe "20:15"
    }

    "should return an empty string for null input" {
        ReportUtils.formatTime(null) shouldBe ""
    }

    "should format a typical time correctly" {
        val time = LocalTime.of(14, 30, 15) // 2:30:15 PM
        ReportUtils.formatTime(time) shouldBe "14:30"
    }

    "should handle midnight (00:00:00) as 00:00:00 due to 'HH' pattern" {
        val time = LocalTime.of(0, 0, 0)
        ReportUtils.formatTime(time) shouldBe "00:00"
    }

    "should handle 12 PM (noon) correctly" {
        val time = LocalTime.of(12, 0, 0)
        ReportUtils.formatTime(time) shouldBe "12:00"
    }

    "should handle times just before midnight (23:59:59) correctly" {
        val time = LocalTime.of(23, 59, 59)
        ReportUtils.formatTime(time) shouldBe "23:59"
    }

    "should handle single digit hour/minute/second with leading zeros" {
        val time = LocalTime.of(5, 7, 9)
        ReportUtils.formatTime(time) shouldBe "05:07"
    }

    // getPeriod
    "should return formatted string for Q1" {
        val range = DateRangeBoundaries(
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 3, 31),
            "Q1",
            2023
        )
        ReportUtils.getPeriod(range) shouldBe "Q1 2023 (1 Jan 2023—31 Mar 2023)"
    }

    "should return formatted string for H2" {
        val range = DateRangeBoundaries(
            LocalDate.of(2022, 7, 1),
            LocalDate.of(2022, 12, 31),
            "H2",
            2022
        )
        ReportUtils.getPeriod(range) shouldBe "H2 2022 (1 Jul 2022—31 Dec 2022)"
    }

    "should return simple string for single month period" {
        val range = DateRangeBoundaries(
            LocalDate.of(2021, 5, 1),
            LocalDate.of(2021, 5, 31),
            "May",
            2021
        )
        ReportUtils.getPeriod(range) shouldBe "May 2021"
    }

    "should return simple string for All Year" {
        val range = DateRangeBoundaries(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 12, 31),
            "All Year",
            2020
        )
        ReportUtils.getPeriod(range) shouldBe "All Year 2020"
    }

    "should return full string with date range when endDate is today" {
        val today = LocalDate.now()
        val start = today.minusDays(6)
        val range = DateRangeBoundaries(
            start,
            today,
            "Weekly",
            today.year
        )
        ReportUtils.getPeriod(range) shouldBe "Weekly ${today.year} (${ReportUtils.formatDate(start)}—${ReportUtils.formatDate(today)})"
    }

    "should return simple string when endDate is not today and period is not Qx or Hx" {
        val range = DateRangeBoundaries(
            LocalDate.of(2023, 9, 1),
            LocalDate.of(2023, 9, 30),
            "September",
            2023
        )
        ReportUtils.getPeriod(range) shouldBe "September 2023"
    }

    // formatScientific
    "should format numbers less than 1000 without suffix" {
        ReportUtils.formatScientific(999, "B") shouldBe "999 B"
    }

    "should format 1000 as 1 K" {
        ReportUtils.formatScientific(1000, "B") shouldBe "1 KB"
    }

    "should format 1 million as 1 M" {
        ReportUtils.formatScientific(1000000, "B") shouldBe "1 MB"
    }

    "should format 1.5 million as 1.5 M" {
        ReportUtils.formatScientific(1500000, "B") shouldBe "1.5 MB"
    }

    "should format 1 billion as 1 G" {
        ReportUtils.formatScientific(1000000000, "B") shouldBe "1 GB"
    }

    "should format without unit" {
        ReportUtils.formatScientific(1_000) shouldBe "1 K"
    }

    "should round non-integer properly" {
        ReportUtils.formatScientific(1_234_567, "B") shouldBe "1.2 MB"
    }
})
