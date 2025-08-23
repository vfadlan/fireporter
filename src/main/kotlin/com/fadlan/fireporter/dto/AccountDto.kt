package com.fadlan.fireporter.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountResponse(
    val data: Array<AccountDto>,
    val meta: MetaDto?,
    val links: OpenApiLinkDto?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AccountResponse

        if (!data.contentEquals(other.data)) return false
        if (meta != other.meta) return false
        if (links != other.links) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + (meta?.hashCode() ?: 0)
        result = 31 * result + (links?.hashCode() ?: 0)
        return result
    }
}

@Serializable
data class AccountDto (
    val type: String,
    val id: String,
    val attributes: AccountAttributes,
    val links: OpenApiLinkDto
)

@Serializable
data class AccountAttributes(
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val active: Boolean,
    val order: Int?=null,
    val name: String,
    val type: String,
    @SerialName("account_role") val accountRole: String?=null,
    @SerialName("currency_id") val currencyId: String?=null,
    @SerialName("currency_code") val currencyCode: String?=null,
    @SerialName("currency_symbol") val currencySymbol: String?=null,
    @SerialName("currency_decimal_places") val currencyDecimalPlaces: Int?=null,
    @SerialName("native_currency_id") val nativeCurrencyId: String? = null,
    @SerialName("native_currency_code") val nativeCurrencyCode: String? = null,
    @SerialName("native_currency_symbol") val nativeCurrencySymbol: String? = null,
    @SerialName("native_currency_decimal_places") val nativeCurrencyDecimalPlaces: Int? = null,
    @SerialName("current_balance") val currentBalance: String,
    @SerialName("native_current_balance") val nativeCurrentBalance: String? = null,
    @SerialName("current_balance_date") val currentBalanceDate: String,
    val notes: String? = null,
    @SerialName("monthly_payment_date") val monthlyPaymentDate: String? = null,
    @SerialName("credit_card_type") val creditCardType: String? = null,
    @SerialName("account_number") val accountNumber: String? = null,
    val iban: String? = null,
    val bic: String? = null,
    @SerialName("virtual_balance") val virtualBalance: String? = null,
    @SerialName("native_virtual_balance") val nativeVirtualBalance: String? = null,
    @SerialName("opening_balance") var openingBalance: String?=null,
    @SerialName("native_opening_balance") val nativeOpeningBalance: String?=null,
    @SerialName("opening_balance_date") val openingBalanceDate: String? = null,
    @SerialName("liability_type") val liabilityType: String? = null,
    @SerialName("liability_direction") val liabilityDirection: String? = null,
    val interest: String? = null,
    @SerialName("interest_period") val interestPeriod: String? = null,
    @SerialName("current_debt") val currentDebt: String? = null,
    @SerialName("include_net_worth") val includeNetWorth: Boolean? = null,
    val longitude: String? = null,
    val latitude: String? = null,
    @SerialName("zoom_level")  val zoomLevel: String? = null
)

