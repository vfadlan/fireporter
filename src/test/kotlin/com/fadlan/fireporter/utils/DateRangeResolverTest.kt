package com.fadlan.fireporter.utils

import com.fadlan.fireporter.model.DateRangeBoundaries
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class DateRangeResolverTest : StringSpec({

    "should resolve Q1 correctly" {
        val result = DateRangeResolver.resolve("Q1", 2023)
        result shouldBe DateRangeBoundaries(
            startDate = LocalDate.of(2023, 1, 1),
            endDate = LocalDate.of(2023, 3, 31),
            period = "Q1",
            year = 2023
        )
    }

    "should resolve H2 correctly" {
        val result = DateRangeResolver.resolve("H2", 2022)
        result shouldBe DateRangeBoundaries(
            startDate = LocalDate.of(2022, 7, 1),
            endDate = LocalDate.of(2022, 12, 31),
            period = "H2",
            year = 2022
        )
    }

    "should resolve All Year correctly" {
        val result = DateRangeResolver.resolve("All Year", 2020)
        result shouldBe DateRangeBoundaries(
            startDate = LocalDate.of(2020, 1, 1),
            endDate = LocalDate.of(2020, 12, 31),
            period = "All Year",
            year = 2020
        )
    }

    "should resolve custom month correctly" {
        val result = DateRangeResolver.resolve("March", 2021)
        result shouldBe DateRangeBoundaries(
            startDate = LocalDate.of(2021, 3, 1),
            endDate = LocalDate.of(2021, 3, 31),
            period = "March",
            year = 2021
        )
    }

    "should resolve leap year correctly" {
        val result = DateRangeResolver.resolve("February", 2024)
        result shouldBe DateRangeBoundaries(
            startDate = LocalDate.of(2024, 2, 1),
            endDate = LocalDate.of(2024, 2, 29),
            period = "February",
            year = 2024
        )
    }

    "should cap future end date to today" {
        val nextYear = LocalDate.now().year + 1
        val result = DateRangeResolver.resolve("All Year", nextYear)

        result.startDate shouldBe LocalDate.of(nextYear, 1, 1)
        result.endDate shouldBe LocalDate.now()
        result.period shouldBe "All Year"
        result.year shouldBe nextYear
    }
})
