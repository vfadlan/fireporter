package com.fadlan.fireporter.service

import com.fadlan.fireporter.dto.SystemInfoResponse
import com.fadlan.fireporter.model.*
import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.repository.*
import com.fadlan.fireporter.utils.FxProgressTracker
import com.fadlan.fireporter.utils.exceptions.IllegalDateRangeException
import com.fadlan.fireporter.utils.exceptions.InactiveAccountException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.math.BigDecimal
import kotlin.collections.LinkedHashMap

class DataCollectorService(
    private val progressTracker: FxProgressTracker,
    private val accountRepository: AccountRepository,
    private val chartRepository: ChartRepository,
    private val summaryRepository: SummaryRepository,
    private val insightRepository: InsightRepository,
    private val transactionRepository: TransactionRepository,
    private val attachmentService: AttachmentService,
    private val ktor: HttpClient,
    private val cred: CredentialProvider,
) {
    private var currencyCode: String? = null
    private var currencySymbol: String? = null
    private var currencyDecimalPlaces: Int = 0

    private lateinit var accounts: MutableList<Account>
    private lateinit var chart: LinkedHashMap<String, BigDecimal>
    private lateinit var generalOverview: GeneralOverview
    private lateinit var incomeInsight: MutableList<InsightGroup>
    private lateinit var expenseInsight: MutableList<InsightGroup>
    private lateinit var transactionJournals: MutableList<TransactionJournal>
    private lateinit var downloadedAttachments: MutableList<Attachment>
    private lateinit var apiSysInfo: SystemInfoResponse

    private suspend fun collectData(dateRange: DateRangeBoundaries, withAttachment: Boolean) {
        progressTracker.report("Collecting accounts and charts data")
        accounts = accountRepository.getAccountStatistics(dateRange)
        if (!accountRepository.hasActiveAccountInRange(dateRange, accounts)) throw InactiveAccountException()
        chart = chartRepository.getMergedChart(dateRange)

        progressTracker.report("Collecting general overview data")
        generalOverview = summaryRepository.getOverview(dateRange)

        progressTracker.report("Collecting income insight")
        incomeInsight = insightRepository.getInsights(InsightType.INCOME, dateRange)

        progressTracker.report("Collecting expense insight")
        expenseInsight = insightRepository.getInsights(InsightType.EXPENSE, dateRange)

        progressTracker.report("Collecting transactions data")
        transactionJournals = transactionRepository.getTransactionJournals(dateRange, generalOverview)

        progressTracker.report("Downloading attachments")
        downloadedAttachments = if (withAttachment) attachmentService.downloadAttachments(transactionJournals.filter { it.hasAttachments })
                                else mutableListOf()

        apiSysInfo = requestApiInfo(cred.host, cred.token).body()
    }

    private fun setMainCurrency() {
        for (account in accounts) {
            if (account.currencyDecimalPlaces > currencyDecimalPlaces) {
                currencyDecimalPlaces = account.currencyDecimalPlaces
            }
            if (account.name == "Cash wallet") {
                currencyCode = account.currencyCode
                currencySymbol = account.currencySymbol
            }
        }
    }

    private fun checkDateRange(dateRange: DateRangeBoundaries) {
        if (
            dateRange.startDate.toString() == dateRange.endDate.toString() ||
            dateRange.startDate.isAfter(dateRange.endDate)
            ){
            throw IllegalDateRangeException()
        }
    }

    suspend fun getData(dateRange: DateRangeBoundaries, theme: Theme, withAttachment: Boolean): ReportData {
        checkDateRange(dateRange)
        collectData(dateRange, withAttachment)
        setMainCurrency()

        return ReportData(
            dateRange,
            theme,
            currencyCode.toString(),
            currencySymbol.toString(),
            currencyDecimalPlaces,
            accounts,
            chart,
            generalOverview,
            incomeInsight,
            expenseInsight,
            transactionJournals,
            downloadedAttachments,
            apiSysInfo.data
        )
    }

    suspend fun requestApiInfo(host: String, token: String): HttpResponse {
        val response: HttpResponse = ktor.request(host) {
            url { appendPathSegments("api", "v1", "about") }
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            method = HttpMethod.Get
        }
        return response
    }

    suspend fun printData(dateRange: DateRangeBoundaries) {
        println("==${dateRange.period} ${dateRange.year}==")
        println("==GENERAL OVERVIEW==")
        println("Total Income                 : ${generalOverview.income}")
        println("Total Expense                : ${generalOverview.expense}")
        println("Init - Expense + Income      : ${generalOverview.initialBalance + generalOverview.income + generalOverview.expense}")
        println()

        println("Initial balance at ${dateRange.startDate}: ${generalOverview.initialBalance}")
        println("Ending balance at ${dateRange.endDate} : ${generalOverview.endingBalance}")
        if (transactionJournals.isNotEmpty()) {
            println("Calculated ending balance    : ${transactionJournals.last().balanceLeft}")
            println("Difference                   : ${transactionJournals.last().balanceLeft?.minus((generalOverview.endingBalance))}")
            println()
            println("Last Transactions            : ${transactionJournals.last().datetime}")
            println("Total Transactions           : ${transactionJournals.size}")
        }

        println()
        println()
    }
}