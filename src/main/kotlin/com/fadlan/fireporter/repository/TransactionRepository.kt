package com.fadlan.fireporter.repository

import com.fadlan.fireporter.dto.TransactionDto
import com.fadlan.fireporter.dto.TransactionResponse
import com.fadlan.fireporter.model.Attachment
import com.fadlan.fireporter.model.DateRangeBoundaries
import com.fadlan.fireporter.model.GeneralOverview
import com.fadlan.fireporter.model.TransactionJournal
import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.network.safeRequest
import com.fadlan.fireporter.utils.exceptions.MultipleCurrencyException
import com.fadlan.fireporter.utils.getOrZero
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TransactionRepository(
    private val ktor: HttpClient,
    private val cred: CredentialProvider,
    private val attachmentRepository: AttachmentRepository
) {
    private suspend fun fetchSinglePageTransactions(page: Int, dateRange: DateRangeBoundaries): TransactionResponse {
        val response: HttpResponse = safeRequest {
            ktor.request(cred.host) {
                url {
                    appendPathSegments("api", "v1", "transactions")
                    parameters.append("start", dateRange.startDate.toString())
                    parameters.append("end", dateRange.endDate.toString())
                    parameters.append("page", page.toString())
                    parameters.append("type", "all")
                }

                headers.append(HttpHeaders.Authorization, "Bearer ${cred.token}")
                method = HttpMethod.Get
            }
        }
        return response.body()
    }

    suspend fun fetchTransactions(dateRange: DateRangeBoundaries): MutableList<TransactionDto> {
        var currentPage = 1
        val transactionResponse = fetchSinglePageTransactions(currentPage, dateRange)
        val totalPages = transactionResponse.meta.pagination.totalPages

        val transactions: MutableList<TransactionDto> = transactionResponse.data

        while (currentPage <= totalPages) {
            currentPage++
            transactions += fetchSinglePageTransactions(currentPage, dateRange).data
        }

        transactions.sortBy { it.attributes.transactions.first().date }
        return transactions
    }

    suspend fun getTransactionJournals(dateRange: DateRangeBoundaries, generalOverview: GeneralOverview): MutableList<TransactionJournal> {
        val textDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

        val fetchedTransactions = fetchTransactions(dateRange)
        val journals: MutableList<TransactionJournal> = mutableListOf()
        var currentBalance = generalOverview.initialBalance

        val journalIds: MutableList<Int> = mutableListOf()

        for (transaction in fetchedTransactions) {
            val transactionJournals = transaction.attributes.transactions
            val attachments = attachmentRepository.getAttachmentsByTransactionId(transaction.id)

            for (journal in transactionJournals) {
                if (!journalIds.add(journal.transactionJournalId.toInt())) {
                    continue
                }

                val amount: BigDecimal = when (journal.type) {
                    "withdrawal", "withdrawals", "expense" -> journal.amount.toBigDecimal() * BigDecimal(-1)
                    else -> journal.amount.toBigDecimal()
                }

                currentBalance = when (journal.type) {
                    "withdrawal", "withdrawals", "expense", "deposit", "deposits", "income" -> currentBalance + amount
                    "opening balance" -> {
                        val openingBalanceDate = LocalDate.parse(journal.date, textDateFormat)
                        if (
                                !openingBalanceDate.isBefore(dateRange.startDate.plusDays(1)) &&
                                openingBalanceDate.isBefore(dateRange.endDate)
                            ) {
                            // count opening balance within period as income
                            currentBalance + amount
                        } else {
                            currentBalance
                        }
                    }
                    else -> currentBalance
                }

                val datetime = ZonedDateTime.parse(journal.date)
                val epochTime = datetime.toEpochSecond()
                val journalElementId = "$epochTime-${journal.transactionJournalId}"
                var firstAttachmentElementId: String? = null

                val journalAttachment = mutableListOf<Attachment>()
                val iterator = attachments.iterator()
                while (iterator.hasNext()) {
                    val attachment = iterator.next()
                    if (attachment.attachableId == journal.transactionJournalId) {
                        attachment.elementId = "$journalElementId-${attachment.id}"
                        attachment.parentId = journalElementId
                        attachment.parentDescription = journal.description

                        journalAttachment += attachment
                        if (firstAttachmentElementId.isNullOrBlank()) firstAttachmentElementId = attachment.elementId
                        iterator.remove()
                    }
                }

                journals.add(
                    TransactionJournal(
                        journal.transactionJournalId,
                        journal.type,
                        datetime,
                        journal.order,
                        journal.currencyCode,
                        journal.currencySymbol,
                        journal.currencyDecimalPlaces,
                        amount,
                        journal.description,
                        journal.sourceId,
                        journal.sourceName,
                        journal.sourceType,
                        journal.destinationId,
                        journal.destinationName,
                        journal.destinationType,
                        journal.budgetId,
                        journal.budgetName,
                        journal.categoryId,
                        journal.categoryName,
                        journal.billId,
                        journal.billName,
                        journal.tags,
                        journal.hasAttachments,
                        journalAttachment,
                        currentBalance,
                        currentBalance,
                        currentBalance,
                        journalElementId,
                        firstAttachmentElementId
                    )
                )
            }
        }

        return journals
    }

    suspend fun getTransactionJournals(dateRange: DateRangeBoundaries, initialBalances: HashMap<String, BigDecimal>): MutableList<TransactionJournal> {
        val fetchedTransactions = fetchTransactions(dateRange)
        val journals: MutableList<TransactionJournal> = mutableListOf()
        val currentBalances = HashMap(initialBalances)
        val journalIds = mutableListOf<Int>()

        for (transaction in fetchedTransactions) {
            val transactionJournals = transaction.attributes.transactions
            val attachments = attachmentRepository.getAttachmentsByTransactionId(transaction.id)

            for (journal in transactionJournals) {
                if (!journalIds.add(journal.transactionJournalId.toInt())) continue
                if (journal.foreignCurrencyCode!=null) {
                    if (journal.currencyCode!=journal.foreignCurrencyCode) throw MultipleCurrencyException()
                }

                currentBalances[journal.sourceId] = currentBalances.getOrZero(journal.sourceId) - journal.amount.toBigDecimal()
                currentBalances[journal.destinationId] = currentBalances.getOrZero(journal.destinationId) + journal.amount.toBigDecimal()

                val datetime = ZonedDateTime.parse(journal.date)
                val epochTime = datetime.toEpochSecond()
                val journalElementId = "$epochTime-${journal.transactionJournalId}"
                var firstAttachmentElementId: String? = null

                val journalAttachment = mutableListOf<Attachment>()
                val iterator = attachments.iterator()
                while (iterator.hasNext()) {
                    val attachment = iterator.next()
                    if (attachment.attachableId == journal.transactionJournalId) {
                        attachment.elementId = "$journalElementId-${attachment.id}"
                        attachment.parentId = journalElementId
                        attachment.parentDescription = journal.description

                        journalAttachment += attachment
                        if (firstAttachmentElementId.isNullOrBlank()) firstAttachmentElementId = attachment.elementId
                        iterator.remove()
                    }
                }
                
                journals.add(
                    TransactionJournal(
                        journal.transactionJournalId,
                        journal.type,
                        datetime,
                        journal.order,
                        journal.currencyCode,
                        journal.currencySymbol,
                        journal.currencyDecimalPlaces,
                        journal.amount.toBigDecimal(),
                        journal.description,
                        journal.sourceId,
                        journal.sourceName,
                        journal.sourceType,
                        journal.destinationId,
                        journal.destinationName,
                        journal.destinationType,
                        journal.budgetId,
                        journal.budgetName,
                        journal.categoryId,
                        journal.categoryName,
                        journal.billId,
                        journal.billName,
                        journal.tags,
                        journal.hasAttachments,
                        journalAttachment,
                        balanceLeft=null,
                        currentBalances[journal.sourceId],
                        currentBalances[journal.destinationId],
                        journalElementId,
                        firstAttachmentElementId
                    )
                )
            }
        }

        return journals
    }
}