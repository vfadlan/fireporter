package com.fadlan.fireporter.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
data class TransactionResponse(
    val data: MutableList<TransactionDto>,
    val meta: MetaDto,
    val links: OpenApiLinkDto
)

@Serializable
data class TransactionDto(
    val type: String,
    val id: String,
    val attributes: TransactionAttributeDto,
    val links: OpenApiLinkDto
)

@Serializable
data class TransactionAttributeDto(
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val user: String,
    @SerialName("user_group") val userGroup: String,
    @SerialName("group_title") val groupTitle: String?=null,
    val transactions: Array<TransactionJournalDto>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransactionAttributeDto

        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        if (user != other.user) return false
        if (userGroup != other.userGroup) return false
        if (groupTitle != other.groupTitle) return false
        if (!transactions.contentEquals(other.transactions)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + user.hashCode()
        result = 31 * result + userGroup.hashCode()
        result = 31 * result + (groupTitle?.hashCode() ?: 0)
        result = 31 * result + transactions.contentHashCode()
        return result
    }
}

@Serializable
data class TransactionJournalDto(
    val user: String,
    @SerialName("transaction_journal_id")val transactionJournalId: String,
    val type: String,
    val date: String,
    val order: Int,
    @SerialName("currency_id")val currencyId: String,
    @SerialName("currency_code")val currencyCode: String,
    @SerialName("currency_name")val currencyName: String,
    @SerialName("currency_symbol")val currencySymbol: String,
    @SerialName("currency_decimal_places")val currencyDecimalPlaces: Int,
    @SerialName("foreign_currency_id")val foreignCurrencyId: String?,
    @SerialName("foreign_currency_code")val foreignCurrencyCode: String?,
    @SerialName("foreign_currency_symbol")val foreignCurrencySymbol: String?,
    @SerialName("foreign_currency_decimal_places")val foreignCurrencyDecimalPlaces: Int,
    val amount: String,
    @SerialName("foreign_amount")val foreignAmount: String?,
    val description: String,
    @SerialName("source_id")val sourceId: String,
    @SerialName("source_name")val sourceName: String,
    @SerialName("source_iban")val sourceIban: String?=null,
    @SerialName("source_type")val sourceType: String,
    @SerialName("destination_id")val destinationId: String,
    @SerialName("destination_name")val destinationName: String,
    @SerialName("destination_iban")val destinationIban: String?=null,
    @SerialName("destination_type")val destinationType: String,
    @SerialName("budget_id")val budgetId: String?=null,
    @SerialName("budget_name")val budgetName: String?=null,
    @SerialName("category_id")val categoryId: String?=null,
    @SerialName("category_name")val categoryName: String?=null,
    @SerialName("bill_id")val billId: String?=null,
    @SerialName("bill_name")val billName: String?=null,
    val reconciled: Boolean?=false,
    val notes: String?=null,
    val tags: List<String>,
    @SerialName("internal_reference")val internalReference: String?=null,
    @SerialName("external_id")val externalId: String?=null,
    @SerialName("original_source")val originalSource: String?=null,
    @SerialName("recurrence_id")val recurrenceId: String?=null,
    @SerialName("recurrence_total")val recurrenceTotal: String?=null,
    @SerialName("recurrence_count")val recurrenceCount: String?=null,
    @SerialName("bunq_payment_id")val bunqPaymentId: String?=null,
    @SerialName("external_url")val externalUrl: String?=null,
    @SerialName("import_hash_v2")val importHashV2: String?=null,
    @SerialName("sepa_cc")val sepaCc: String?=null,
    @SerialName("sepa_ct_op")val sepaCtOp: String?=null,
    @SerialName("sepa_ct_id")val sepaCtId: String?=null,
    @SerialName("sepa_db")val sepaDb: String?=null,
    @SerialName("sepa_country")val sepaCountry: String?=null,
    @SerialName("sepa_ep")val sepaEp: String?=null,
    @SerialName("sepa_ci")val sepaCi: String?=null,
    @SerialName("sepa_batch_id")val sepaBatchId: String?=null,
    @SerialName("interest_date")val interestDate: String?=null,
    @SerialName("book_date")val bookDate: String?=null,
    @SerialName("process_date")val processDate: String?=null,
    @SerialName("due_date")val dueDate: String?=null,
    @SerialName("payment_date")val paymentDate: String?=null,
    @SerialName("invoice_date")val invoiceDate: String?=null,
    val longitude: String?=null,
    val latitude: String?=null,
    @SerialName("zoom_level")val zoomLevel: String?=null,
    @SerialName("has_attachments")val hasAttachments: Boolean
)

