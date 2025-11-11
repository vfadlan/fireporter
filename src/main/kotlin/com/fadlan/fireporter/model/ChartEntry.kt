package com.fadlan.fireporter.model

import java.math.BigDecimal
import java.util.*

data class ChartEntry(
    val label: Date,
    val value: BigDecimal
)

data class ChartEntryWrapper(
    val seriesName: String,
    val label: Date,
    val value: BigDecimal
)