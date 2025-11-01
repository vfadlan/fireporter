package com.fadlan.fireporter.service

import com.fadlan.fireporter.dto.SystemInfoResponse
import com.fadlan.fireporter.model.*
import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.network.safeRequest
import com.fadlan.fireporter.repository.*
import com.fadlan.fireporter.utils.FxProgressTracker
import com.fadlan.fireporter.utils.exceptions.IllegalDateRangeException
import com.fadlan.fireporter.utils.exceptions.InactiveAccountException
import com.fadlan.fireporter.utils.getOrZero
import com.fadlan.fireporter.utils.prettyPrint
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.Logger
import java.math.BigDecimal

class DataCollectorService(
    private val progressTracker: FxProgressTracker,
    private val accountRepository: AccountRepository,
    private val chartRepository: ChartRepository,
    private val summaryRepository: SummaryRepository,
    private val insightRepository: InsightRepository,
    private val transactionRepository: TransactionRepository,
    private val attachmentService: AttachmentService,
    private val currencyRepository: CurrencyRepository,
    private val ktor: HttpClient,
    private val cred: CredentialProvider,
    private val logger: Logger
) {
    private lateinit var mainCurrency: Currency
    private lateinit var accounts: MutableList<Account>
    private lateinit var chart: HashMap<String, MutableList<ChartEntry>>
    private lateinit var generalOverview: GeneralOverview
    private lateinit var initialBalances: HashMap<String, BigDecimal>
    private lateinit var endingBalances: HashMap<String, BigDecimal>
    private lateinit var incomeInsight: MutableList<InsightGroup>
    private lateinit var expenseInsight: MutableList<InsightGroup>
    private lateinit var transactionJournals: MutableList<TransactionJournal>
    private lateinit var downloadedAttachments: MutableList<Attachment>
    private lateinit var apiSysInfo: SystemInfoResponse

    private suspend fun collectAccounts(dateRange: DateRangeBoundaries) {
        logger.info("Collecting accounts and charts data...")
        progressTracker.report("Collecting accounts and charts data")
        accounts = accountRepository.getAssetAccounts(dateRange)
        if (!accountRepository.hasActiveAccountInRange(dateRange, accounts)) throw InactiveAccountException()
    }

    private suspend fun collectData(dateRange: DateRangeBoundaries, withAttachment: Boolean) {
        chart = chartRepository.getCharts(dateRange, mainCurrency.code)

        logger.info("Collecting general overview data...")
        progressTracker.report("Collecting general overview data")
        generalOverview = summaryRepository.getFullOverview(dateRange)[mainCurrency.code]!!
        initialBalances = summaryRepository.getAssetBalanceAtDate(dateRange.startDate, GroupBy.ACCOUNT, TimeOfDayBoundary.START)
        endingBalances = summaryRepository.getAssetBalanceAtDate(dateRange.endDate, GroupBy.ACCOUNT, TimeOfDayBoundary.END)

        for (account in accounts) {
            account.initialBalance =  initialBalances.getOrZero(account.id)
            account.initialBalanceDate = dateRange.startDate
            account.currentBalance = endingBalances.getOrZero(account.id)
        }

        logger.info("Collecting transactions data...")
        progressTracker.report("Collecting transactions data")
        transactionJournals = transactionRepository.getTransactionJournals(dateRange, initialBalances)

        logger.info("Collecting income insight...")
        progressTracker.report("Collecting income insight")
        incomeInsight = insightRepository.getInsights(InsightType.INCOME, dateRange)

        logger.info("Collecting expense insight...")
        progressTracker.report("Collecting expense insight")
        expenseInsight = insightRepository.getInsights(InsightType.EXPENSE, dateRange)

        logger.info("Data collected successfully.")

        progressTracker.report("Downloading attachments")
        downloadedAttachments = if (withAttachment) {
            logger.info("Downloading attachments...")
            attachmentService.downloadAttachments(transactionJournals.filter { it.hasAttachments })
        } else mutableListOf()

        apiSysInfo = requestApiInfo(cred.host, cred.token).body()
    }

    private suspend fun setMainCurrency() {
        val fetchedCurrency = currencyRepository.fetchCurrency(accounts.first().currencyCode)
        mainCurrency = Currency(
            fetchedCurrency.attributes.code,
            fetchedCurrency.id,
            fetchedCurrency.attributes.symbol,
            fetchedCurrency.attributes.decimalPlaces,
            fetchedCurrency.attributes.name
        )
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
        collectAccounts(dateRange)
        setMainCurrency()
        collectData(dateRange, withAttachment)

        return ReportData(
            dateRange,
            theme,
            mainCurrency,
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
        val response: HttpResponse = safeRequest {
            ktor.request(host) {
                url { appendPathSegments("api", "v1", "about") }
                headers.append(HttpHeaders.Authorization, "Bearer $token")
                method = HttpMethod.Get
            }
        }
        return response
    }
}