package com.fadlan.fireporter.repository

import com.fadlan.fireporter.dto.AccountDto
import com.fadlan.fireporter.dto.BasicSummaryDto
import com.fadlan.fireporter.dto.BasicSummaryResponse
import com.fadlan.fireporter.model.DateRangeBoundaries
import com.fadlan.fireporter.model.GeneralOverview
import com.fadlan.fireporter.model.GroupBy
import com.fadlan.fireporter.model.TimeOfDayBoundary
import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.utils.getOrZero
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SummaryRepository(
    private val ktor: HttpClient,
    private val cred: CredentialProvider,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
){
    // get balance at date time:23.59.59
    private suspend fun getBalanceAtEndOfDate(date: LocalDate): BigDecimal {
        val accounts: Array<AccountDto> = accountRepository.fetchAccounts(date, "asset")
        var totalBalance = BigDecimal(0)

        for (account in accounts) {
            totalBalance += account.attributes.currentBalance.toBigDecimal()
        }
        return totalBalance
    }

    // get balance at date time:00.00.00
    private suspend fun getBalanceAtStartOfDate(date: LocalDate): BigDecimal {
        val accounts = accountRepository.fetchAccounts(date, "asset")
        var totalBalance = BigDecimal(0)
        for (account in accounts) {
            totalBalance += account.attributes.currentBalance.toBigDecimal()
        }

        val cashFlowYesterday = getCashFlowAtPeriod(DateRangeBoundaries(date.minusDays(1), date))
        val cashFlow = getCashFlowAtPeriod(DateRangeBoundaries(date, date.plusDays(1)))
        val expense = cashFlowYesterday.spentInIDR.monetaryValue.toBigDecimal()
        val income = cashFlow.earnedInIDR.monetaryValue.toBigDecimal()
        return totalBalance + income - expense
    }

    private suspend fun getCashFlowAtPeriod(dateRange: DateRangeBoundaries): BasicSummaryResponse {
        val summaryResponse = fetchSummaryBasic(dateRange)
        val summary = BasicSummaryResponse(
            summaryResponse["balance-in-IDR"]!!,
            summaryResponse["spent-in-IDR"]!!,
            summaryResponse["earned-in-IDR"]!!,
            summaryResponse["bills-paid-in-IDR"]!!,
            summaryResponse["bills-unpaid-in-IDR"]!!,
            summaryResponse["left-to-spend-in-IDR"]!!,
            summaryResponse["net-worth-in-IDR"]!!,
        )
        return summary
    }

    suspend fun getOverview(dateRange: DateRangeBoundaries): GeneralOverview {
        val periodicSummary = getCashFlowAtPeriod(dateRange)
        println("c: ${getBalanceAtEndOfDate(dateRange.endDate)}")
        println("b: ${periodicSummary.balanceInIDR.monetaryValue}")

        return GeneralOverview(
            getBalanceAtStartOfDate(dateRange.startDate),
            getBalanceAtEndOfDate(dateRange.endDate),
            periodicSummary.earnedInIDR.monetaryValue.toBigDecimal(),
            periodicSummary.spentInIDR.monetaryValue.toBigDecimal(),
            periodicSummary.balanceInIDR.monetaryValue.toBigDecimal(),
            currencyId=periodicSummary.balanceInIDR.currencyId,
            currencyCode=periodicSummary.balanceInIDR.currencyCode,
            currencySymbol=periodicSummary.balanceInIDR.currencySymbol,
            currencyDecimalPlaces=periodicSummary.balanceInIDR.currencyDecimalPlaces
        )
    }

    suspend fun fetchSummaryBasic(dateRange: DateRangeBoundaries): HashMap<String, BasicSummaryDto> {
        val response: HttpResponse = ktor.request(cred.host) {
            url {
                appendPathSegments("api", "v1", "summary", "basic")
                parameters.append("start", dateRange.startDate.toString())
                parameters.append("end", dateRange.endDate.toString())
            }

            headers.append(HttpHeaders.Authorization, "Bearer ${cred.token}")
            method = HttpMethod.Get
        }
        val summaryResponse: HashMap<String, BasicSummaryDto> = response.body()
        return summaryResponse
    }

    suspend fun calculateCashFlow(dateRange: DateRangeBoundaries, groupBy: GroupBy): HashMap<String, BigDecimal> {
        val transactions = transactionRepository.fetchTransactions(dateRange)
        val cashFlows = hashMapOf<String, BigDecimal>()

        for (transaction in transactions) {
            for (journal in transaction.attributes.transactions) {
                val amount = journal.amount.toBigDecimal()
                if (groupBy == GroupBy.ACCOUNT) {
                    cashFlows[journal.sourceId] = cashFlows.getOrZero(journal.sourceId) - amount
                    cashFlows[journal.destinationId] = cashFlows.getOrZero(journal.destinationId) + amount
                } else if (groupBy == GroupBy.CURRENCY_CODE) {
                    when (journal.type) {
                        "withdrawal", "withdrawals", "expense" -> {
                            cashFlows[journal.currencyCode] = cashFlows.getOrZero(journal.currencyCode) - amount
                        }
                        "deposit", "deposits", "income" -> {
                            cashFlows[journal.currencyCode] = cashFlows.getOrZero(journal.currencyCode) + amount
                        }
                    }
                }
            }
        }

        return cashFlows
    }

    suspend fun getAssetBalanceAtDate(date: LocalDate, groupBy: GroupBy, timeOfDay: TimeOfDayBoundary): HashMap<String, BigDecimal> {
        val textDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        val accountsType = if (groupBy==GroupBy.ACCOUNT) "all" else "asset"
        val fetchedAccounts = accountRepository.fetchAccounts(date, accountsType)
        val cashFlows = calculateCashFlow(DateRangeBoundaries(date, date), groupBy)
        val balances = hashMapOf<String, BigDecimal>()

        for (account in fetchedAccounts) {
            val key = when (groupBy) {
                GroupBy.ACCOUNT -> account.id
                GroupBy.CURRENCY_CODE -> account.attributes.currencyCode?:"UNKNOWN"
            }

            balances[key] = balances.getOrZero(key) + account.attributes.currentBalance.toBigDecimal()
            if (timeOfDay == TimeOfDayBoundary.START) balances[key] = balances.getOrZero(key) - cashFlows.getOrZero(key)

            if (account.attributes.openingBalanceDate!=null && groupBy==GroupBy.CURRENCY_CODE) {
                val openingBalanceDate = LocalDate.parse(account.attributes.openingBalanceDate,textDateFormat)
                if (openingBalanceDate == date) balances[key] = balances.getOrZero(key) - account.attributes.openingBalance.toBigDecimal()
            }
        }

        return balances
    }

    suspend fun getOpeningBalanceByCurrency(dateRange: DateRangeBoundaries): HashMap<String, BigDecimal> {
        val textDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        val fetchedAccounts = accountRepository.fetchAccounts(dateRange.endDate, "asset")
        val openingBalances = hashMapOf<String, BigDecimal>()

        for (account in fetchedAccounts) {
            if (account.attributes.openingBalanceDate!=null) {
                val key = account.attributes.currencyCode?:"UNKNOWN"
                val openingBalanceDate = LocalDate.parse(account.attributes.openingBalanceDate,textDateFormat)
                if (
                    !openingBalanceDate.isBefore(dateRange.startDate) &&
                    openingBalanceDate.isBefore(dateRange.endDate)
                ) {
                    openingBalances[key] = openingBalances.getOrZero(key) + account.attributes.openingBalance.toBigDecimal()
                }
            }
        }

        return openingBalances
    }

    suspend fun getFullOverview(dateRange: DateRangeBoundaries): HashMap<String, GeneralOverview> {
        val fetchedSummary = fetchSummaryBasic(dateRange)
        val initialAssets = getAssetBalanceAtDate(dateRange.startDate.minusDays(1), GroupBy.CURRENCY_CODE, TimeOfDayBoundary.START)
        val cashFlows = calculateCashFlow(dateRange, GroupBy.CURRENCY_CODE)
        val openingBalances = getOpeningBalanceByCurrency(dateRange)

        val generalOverview = hashMapOf<String, GeneralOverview>()

        for (currencyCode in cashFlows.keys) {
            val initialAsset = initialAssets.getOrZero(currencyCode)
            val endingAsset = initialAsset + cashFlows.getOrZero(currencyCode) + openingBalances.getOrZero(currencyCode)

            val balanceDto = fetchedSummary["balance-in-$currencyCode"]
            val earned = fetchedSummary["earned-in-$currencyCode"]?.monetaryValue?.toBigDecimal() ?: BigDecimal(0)
            val spent = fetchedSummary["spent-in-$currencyCode"]?.monetaryValue?.toBigDecimal() ?: BigDecimal(0)

            generalOverview[currencyCode] = GeneralOverview(
                initialAsset,
                endingAsset,
                earned,
                spent,
                earned + spent,
                openingBalances.getOrZero(currencyCode),
                balanceDto?.currencyId?:"",
                balanceDto?.currencyCode?:"UNKNOWN",
                balanceDto?.currencySymbol?:"",
                balanceDto?.currencyDecimalPlaces?:2
            )
        }

        return generalOverview
    }
}