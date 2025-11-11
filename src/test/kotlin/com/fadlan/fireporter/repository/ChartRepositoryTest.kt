package com.fadlan.fireporter.repository

import com.fadlan.fireporter.model.DateRangeBoundaries
import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.network.Ktor
import com.fadlan.fireporter.utils.DateRangeResolver
import com.fadlan.fireporter.utils.exceptions.UnusedCurrencyException
import com.fadlan.fireporter.utils.getProperty
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent
import java.time.LocalDate

class ChartRepositoryTest: ExpectSpec({
    val testModule = module {
        single<HttpClient> { Ktor.client }
        single<DateRangeResolver> { DateRangeResolver }
        single<CredentialProvider> { CredentialProvider }

        single {
            AttachmentRepository(
                get<HttpClient>(),
                get<CredentialProvider>(),
            )
        }

        single {
            TransactionRepository(
                get<HttpClient>(),
                get<CredentialProvider>(),
                get<AttachmentRepository>()
            )
        }

        single {
            ChartRepository(
                get<HttpClient>(),
                get<CredentialProvider>()
            )
        }

        single {
            AccountRepository(
                get<HttpClient>(),
                get<CredentialProvider>()
            )
        }
    }

    beforeSpec {
        startKoin {
            modules(testModule)
        }

        val cred = KoinJavaComponent.get<CredentialProvider>(CredentialProvider::class.java)
        cred.host = getProperty("config/secret.properties", "firefly.host")
        cred.token = getProperty("config/secret.properties", "firefly.pat")
    }

    context("ChartRepository") {
        val chartRepository = KoinJavaComponent.get<ChartRepository>(ChartRepository::class.java)
        val transactionRepository = KoinJavaComponent.get<TransactionRepository>(TransactionRepository::class.java)
        val accountRepository = KoinJavaComponent.get<AccountRepository>(AccountRepository::class.java)

        expect("should return with correct currency and amount") {
            val dateRange = DateRangeBoundaries(
                LocalDate.parse("2025-02-01"),
                LocalDate.parse("2025-02-28")
            )
            var mainCurrencyCode = ""
            val transactions = transactionRepository.fetchTransactions(dateRange)
            if (transactions.size > 0) {
                mainCurrencyCode = transactions[0].attributes.transactions[0].currencyCode
            }
            val charts = chartRepository.getCharts(dateRange, mainCurrencyCode)
            val accountsAtEnd = accountRepository.fetchAccounts(dateRange.endDate, "asset")

            for (account in accountsAtEnd) {
                val chart = charts[account.attributes.name]
                if (chart!=null) {
                    chart[chart.size-1].value.compareTo(account.attributes.currentBalance.toBigDecimal()) shouldBe 0
                }
            }
        }

        expect("should throw UnusedCurrencyError on unused currency") {
            val dateRange = DateRangeBoundaries(
                LocalDate.parse("2025-02-01"),
                LocalDate.parse("2025-02-28")
            )
            val mainCurrency = "UNUSED"

            shouldThrow<UnusedCurrencyException> {
                chartRepository.getCharts(dateRange, mainCurrency)
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})