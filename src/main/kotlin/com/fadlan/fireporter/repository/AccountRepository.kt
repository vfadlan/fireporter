package com.fadlan.fireporter.repository

import com.fadlan.fireporter.dto.AccountDto
import com.fadlan.fireporter.dto.AccountResponse
import com.fadlan.fireporter.model.Account
import com.fadlan.fireporter.model.DateRangeBoundaries
import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.network.safeRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AccountRepository(
    private val ktor: HttpClient,
    private val cred: CredentialProvider,
) {
    private suspend fun fetchSinglePageAccounts(page: Int, date: LocalDate, type: String): AccountResponse {
        val response: HttpResponse = safeRequest {
            ktor.request(cred.host) {
                url {
                    appendPathSegments("api", "v1", "accounts")
                    parameters.append("type", type)
                    parameters.append("date", date.toString())
                    parameters.append("page", page.toString())
                }

                headers.append(HttpHeaders.Authorization, "Bearer ${cred.token}")
                method = HttpMethod.Get
            }
        }
        return response.body()
    }

    suspend fun fetchAccounts(date: LocalDate, type: String): Array<AccountDto> {
        var currentPage = 1
        val accountResponse = fetchSinglePageAccounts(currentPage, date, type)
        val totalPages = accountResponse.meta?.pagination?.totalPages ?: 1

        var accounts: Array<AccountDto> = accountResponse.data

        while (currentPage < totalPages) {
            currentPage++
            accounts += fetchSinglePageAccounts(currentPage, date, type).data
        }
        return accounts
    }

    suspend fun getAssetAccounts(dateRange: DateRangeBoundaries):  MutableList<Account> {
        val fetchedAccounts = fetchAccounts(dateRange.endDate, "asset")

        val accounts = mutableListOf<Account>()
        for (account in fetchedAccounts) {
            val attr = account.attributes
            if (attr.openingBalance == null) attr.openingBalance = "0.00"
            accounts.add(
                Account(
                    account.id,
                    attr.name,
                    attr.type,
                    attr.currencyCode?:"",
                    attr.currencySymbol?:"",
                    attr.currencyDecimalPlaces?:2,
                    attr.currentBalance.toBigDecimal(),
                    attr.currentBalanceDate,
                    attr.accountNumber,
                    attr.iban,
                    attr.openingBalance!!.toBigDecimal(),
                    attr.openingBalanceDate
                )
            )
        }

        return accounts
    }

    fun hasActiveAccountInRange(dateRange: DateRangeBoundaries, accounts: MutableList<Account>): Boolean {
        val textDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        var result = false

        for (account in accounts) {
            val openingBalanceDateStr = account.openingBalanceDate
            if (openingBalanceDateStr != null) {
                val openingDate = openingBalanceDateStr.let { LocalDate.parse(it, textDateFormat) }
                if (openingDate.isBefore(dateRange.startDate) || openingDate.isBefore(dateRange.endDate)) {
                    result = true
                    break
                }
            }
        }

        return result
    }
}