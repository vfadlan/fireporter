package com.fadlan.fireporter.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrencyResponseDto(
    val data: CurrencyDto?=null,
    val links: OpenApiLinkDto?=null,
)

@Serializable
data class CurrencyDto(
    val type: String,
    val id: String,
    val attributes: CurrencyAttributesDto
)

@Serializable
data class CurrencyAttributesDto(
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val native: Boolean,
    val default: Boolean,
    val enabled: Boolean,
    val name: String,
    val code: String,
    val symbol: String,
    @SerialName("decimal_places") val decimalPlaces: Int?=0
)
