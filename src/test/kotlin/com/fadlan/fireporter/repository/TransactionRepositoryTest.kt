package com.fadlan.fireporter.repository

import com.fadlan.fireporter.model.DateRangeBoundaries
import com.fadlan.fireporter.model.GroupBy
import com.fadlan.fireporter.model.TimeOfDayBoundary
import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.network.Ktor
import com.fadlan.fireporter.utils.DateRangeResolver
import com.fadlan.fireporter.utils.getOrZero
import com.fadlan.fireporter.utils.getProperty
import io.kotest.assertions.fail
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent
import java.math.BigDecimal
import java.time.LocalDate

/*
NOTE: To run this test, create a new file on: src/main/resources/secret.properties
firefly.host=http://your-firefly-address
firefly.pat=personal-access-token
 */

class TransactionRepositoryTest: ExpectSpec({
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
            AccountRepository(
                get<HttpClient>(),
                get<CredentialProvider>()
            )
        }

        single {
            SummaryRepository(
                get<HttpClient>(),
                get<CredentialProvider>(),
                get<AccountRepository>(),
                get<TransactionRepository>()
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

    context("TransactionRepository") {
        val summaryRepository = KoinJavaComponent.get<SummaryRepository>(SummaryRepository::class.java)
        val transactionRepository = KoinJavaComponent.get<TransactionRepository>(TransactionRepository::class.java)

        context("getTransactionJournals()") {
            expect("should calculate balances correctly FMr-25") {
                val dateRange = DateRangeBoundaries(
                    LocalDate.parse("2025-02-01"),
                    LocalDate.parse("2025-03-31")
                )
                val initialBalances = summaryRepository.getAssetBalanceAtDate(dateRange.startDate, GroupBy.ACCOUNT, TimeOfDayBoundary.START)
                val currentBalances = HashMap<String, BigDecimal>(initialBalances)
                val transactionJournals = transactionRepository.getTransactionJournals(dateRange, initialBalances)

                for (journal in transactionJournals) {
                    currentBalances[journal.sourceId] = currentBalances.getOrZero(journal.sourceId) - journal.amount
                    currentBalances[journal.destinationId] = currentBalances.getOrZero(journal.destinationId) + journal.amount

                    if (journal.sourceBalanceLeft != null && journal.destinationBalanceLeft != null) {
                        journal.sourceBalanceLeft!!.compareTo(currentBalances.getOrZero(journal.sourceId)).shouldBe(0)
                        journal.destinationBalanceLeft!!.compareTo(currentBalances.getOrZero(journal.destinationId)).shouldBe(0)
                    } else {
                        fail(("Balance left on source or dest. is null."))
                    }
                }
            }

            expect("should calculate balances correctly MA-25") {
                val dateRange = DateRangeBoundaries(
                    LocalDate.parse("2025-03-01"),
                    LocalDate.parse("2025-04-30")
                )
                val initialBalances = summaryRepository.getAssetBalanceAtDate(dateRange.startDate, GroupBy.ACCOUNT, TimeOfDayBoundary.START)
                val currentBalances = HashMap<String, BigDecimal>(initialBalances)
                val transactionJournals = transactionRepository.getTransactionJournals(dateRange, initialBalances)

                for (journal in transactionJournals) {
                    currentBalances[journal.sourceId] = currentBalances.getOrZero(journal.sourceId) - journal.amount
                    currentBalances[journal.destinationId] = currentBalances.getOrZero(journal.destinationId) + journal.amount

                    if (journal.sourceBalanceLeft != null && journal.destinationBalanceLeft != null) {
                        journal.sourceBalanceLeft!!.compareTo(currentBalances.getOrZero(journal.sourceId)).shouldBe(0)
                        journal.destinationBalanceLeft!!.compareTo(currentBalances.getOrZero(journal.destinationId)).shouldBe(0)
                    } else {
                        fail(("Balance left on source or dest. is null."))
                    }
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})