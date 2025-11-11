package com.fadlan.fireporter.model

data class Currency(
    val code: String,
    val id: String,
    val symbol: String,
    val decimalPlaces: Int?=0,
    val name: String?=""
)
