package com.fadlan.fireporter.model

import java.math.BigDecimal

data class GeneralOverview(
    val initialBalance: BigDecimal,
    val endingBalance: BigDecimal,
    val income: BigDecimal,
    val expense: BigDecimal,
    val netFlow: BigDecimal,
    val openingBalance: BigDecimal?=0.toBigDecimal(),
    val currencyId: String,
    val currencyCode: String,
    val currencySymbol: String,
    val currencyDecimalPlaces: Int?=2
)