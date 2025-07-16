package com.fadlan.fireporter.repository

import com.fadlan.fireporter.dto.BasicSummaryDto
import com.fadlan.fireporter.dto.TransactionJournalDto
import com.fadlan.fireporter.model.DateRangeBoundaries
import com.fadlan.fireporter.model.GeneralOverview
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
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.*
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/*
NOTE: To run this test, create a new file on: src/main/resources/secret.properties
firefly.host=http://your-firefly-address
firefly.pat=personal-access-token
 */

class SummaryRepositoryTest: ExpectSpec({
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
        startKoin() {
            modules(testModule)
        }

        val cred = KoinJavaComponent.get<CredentialProvider>(CredentialProvider::class.java)
        cred.host = getProperty("config/secret.properties", "firefly.host")
        cred.token = getProperty("config/secret.properties", "firefly.pat")
    }

    context("SummaryRepository") {
        val summaryRepository = KoinJavaComponent.get<SummaryRepository>(SummaryRepository::class.java)
        val accountRepository = KoinJavaComponent.get<AccountRepository>(AccountRepository::class.java)
        val transactionRepository = KoinJavaComponent.get<TransactionRepository>(TransactionRepository::class.java)
        val textDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

        context("fetchSummaryBasic() /summary/basic") {
            expect("response type should be HashMap<String, BasicSummaryDto>") {
                val res = summaryRepository.fetchSummaryBasic(
                    DateRangeBoundaries(
                        LocalDate.parse("2025-01-01"),
                        LocalDate.parse("2025-01-31")
                    )
                )
                for (key in res.keys) {
                    res[key].shouldBeInstanceOf<BasicSummaryDto>()
                }
            }
        }

        context("calculateCashFlow()") {
            context("GroupBy.ACCOUNT") {
                expect("initial balance + calculated cashFlow = account ending balance Jan-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-01-01"),
                        LocalDate.parse("2025-01-31")
                    )
                    val accountAtStart = accountRepository.fetchAccounts(dateRange.startDate.minusDays(1), "all")
                    val accountAtEnd = accountRepository.fetchAccounts(dateRange.endDate, "all")
                    val cashFlows = summaryRepository.calculateCashFlow(dateRange, GroupBy.ACCOUNT)

                    for (accountId in cashFlows.keys) {
                        val initialBalance = accountAtStart.find { it.id == accountId }?.attributes?.currentBalance?.toBigDecimal()
                        val endingBalance = accountAtEnd.find { it.id == accountId }?.attributes?.currentBalance?.toBigDecimal()
                        if (initialBalance!=null && endingBalance!=null) {
                            val cashFlow = cashFlows.getOrZero(accountId)
                            endingBalance.compareTo(initialBalance + cashFlow) shouldBe 0
                        } else {
                            fail("Some account not found")
                        }
                    }
                }

                expect("initial balance + calculated cashFlow = account ending balance Q1-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-01-01"),
                        LocalDate.parse("2025-04-30")
                    )
                    val accountAtStart = accountRepository.fetchAccounts(dateRange.startDate.minusDays(1), "all")
                    val accountAtEnd = accountRepository.fetchAccounts(dateRange.endDate, "all")
                    val cashFlows = summaryRepository.calculateCashFlow(dateRange, GroupBy.ACCOUNT)

                    for (accountId in cashFlows.keys) {
                        val initialBalance = accountAtStart.find { it.id == accountId }?.attributes?.currentBalance?.toBigDecimal()
                        val endingBalance = accountAtEnd.find { it.id == accountId }?.attributes?.currentBalance?.toBigDecimal()
                        if (initialBalance!=null && endingBalance!=null) {
                            val cashFlow = cashFlows.getOrZero(accountId)
                            endingBalance.compareTo(initialBalance + cashFlow) shouldBe 0
                        } else {
                            fail("Some account not found")
                        }
                    }
                }

                expect("initial balance + calculated cashFlow = account ending balance H1-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-01-01"),
                        LocalDate.parse("2025-06-30")
                    )
                    val accountAtStart = accountRepository.fetchAccounts(dateRange.startDate.minusDays(1), "all")
                    val accountAtEnd = accountRepository.fetchAccounts(dateRange.endDate, "all")
                    val cashFlows = summaryRepository.calculateCashFlow(dateRange, GroupBy.ACCOUNT)

                    for (accountId in cashFlows.keys) {
                        val initialBalance = accountAtStart.find { it.id == accountId }?.attributes?.currentBalance?.toBigDecimal()
                        val endingBalance = accountAtEnd.find { it.id == accountId }?.attributes?.currentBalance?.toBigDecimal()
                        if (initialBalance!=null && endingBalance!=null) {
                            val cashFlow = cashFlows.getOrZero(accountId)
                            endingBalance.compareTo(initialBalance + cashFlow) shouldBe 0
                        } else {
                            fail("Some account not found")
                        }
                    }
                }

                expect("initial balance + calculated cashFlow = account ending balance MJ-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-05-01"),
                        LocalDate.parse("2025-06-30")
                    )
                    val accountAtStart = accountRepository.fetchAccounts(dateRange.startDate.minusDays(1), "all")
                    val accountAtEnd = accountRepository.fetchAccounts(dateRange.endDate, "all")
                    val cashFlows = summaryRepository.calculateCashFlow(dateRange, GroupBy.ACCOUNT)

                    for (accountId in cashFlows.keys) {
                        val initialBalance = accountAtStart.find { it.id == accountId }?.attributes?.currentBalance?.toBigDecimal()
                        val endingBalance = accountAtEnd.find { it.id == accountId }?.attributes?.currentBalance?.toBigDecimal()
                        if (initialBalance!=null && endingBalance!=null) {
                            val cashFlow = cashFlows.getOrZero(accountId)
                            endingBalance.compareTo(initialBalance + cashFlow) shouldBe 0
                        } else {
                            fail("Some account not found")
                        }
                    }
                }
            }
            context("GroupBy.CURRENCY_CODE") {
                expect("calculated cashFlow = fetched earned + spent Jan-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-01-01"),
                        LocalDate.parse("2025-01-31")
                    )
                    val jan25Summary = summaryRepository.fetchSummaryBasic(dateRange)
                    val jan25CashFlowCurrency = summaryRepository.calculateCashFlow(dateRange, GroupBy.CURRENCY_CODE)
                    for (currencyCode in jan25CashFlowCurrency.keys) {
                        val earned = jan25Summary["earned-in-$currencyCode"]?.monetaryValue?.toBigDecimal()
                        val spent = jan25Summary["spent-in-$currencyCode"]?.monetaryValue?.toBigDecimal()
                        val calculatedCashFlow = jan25CashFlowCurrency[currencyCode]
                        if (earned!=null && spent!=null && calculatedCashFlow!=null) {
                            calculatedCashFlow.compareTo(earned + spent) shouldBe 0
                        } else fail("earned/spent/calculatedCashFlow is null")
                    }
                }

                expect("calculated cashFlow = fetched earned + spent Q1-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-01-01"),
                        LocalDate.parse("2025-04-30")
                    )
                    val q1Summary = summaryRepository.fetchSummaryBasic(dateRange)
                    val q1CashFlowCurrency = summaryRepository.calculateCashFlow(dateRange, GroupBy.CURRENCY_CODE)
                    for (currencyCode in q1CashFlowCurrency.keys) {
                        val earned = q1Summary["earned-in-$currencyCode"]?.monetaryValue?.toBigDecimal()
                        val spent = q1Summary["spent-in-$currencyCode"]?.monetaryValue?.toBigDecimal()
                        val calculatedCashFlow = q1CashFlowCurrency[currencyCode]
                        if (earned!=null && spent!=null && calculatedCashFlow!=null) {
                            calculatedCashFlow.compareTo(earned + spent) shouldBe 0
                        } else fail("earned/spent/calculatedCashFlow is null")
                    }
                }

                expect("calculated cashFlow = fetched earned + spent H1-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-01-01"),
                        LocalDate.parse("2025-06-30")
                    )
                    val h1Summary = summaryRepository.fetchSummaryBasic(dateRange)
                    val h1CashFlowCurrency = summaryRepository.calculateCashFlow(dateRange, GroupBy.CURRENCY_CODE)
                    for (currencyCode in h1CashFlowCurrency.keys) {
                        val earned = h1Summary["earned-in-$currencyCode"]?.monetaryValue?.toBigDecimal()
                        val spent = h1Summary["spent-in-$currencyCode"]?.monetaryValue?.toBigDecimal()
                        val calculatedCashFlow = h1CashFlowCurrency[currencyCode]
                        if (earned!=null && spent!=null && calculatedCashFlow!=null) {
                            calculatedCashFlow.compareTo(earned + spent) shouldBe 0
                        } else fail("earned/spent/calculatedCashFlow is null")
                    }
                }

                expect("calculated cashFlow = fetched earned + spent MJ-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-05-01"),
                        LocalDate.parse("2025-06-30")
                    )
                    val h1Summary = summaryRepository.fetchSummaryBasic(dateRange)
                    val h1CashFlowCurrency = summaryRepository.calculateCashFlow(dateRange, GroupBy.CURRENCY_CODE)
                    for (currencyCode in h1CashFlowCurrency.keys) {
                        val earned = h1Summary["earned-in-$currencyCode"]?.monetaryValue?.toBigDecimal()
                        val spent = h1Summary["spent-in-$currencyCode"]?.monetaryValue?.toBigDecimal()
                        val calculatedCashFlow = h1CashFlowCurrency[currencyCode]
                        if (earned!=null && spent!=null && calculatedCashFlow!=null) {
                            calculatedCashFlow.compareTo(earned + spent) shouldBe 0
                        } else fail("earned/spent/calculatedCashFlow is null")
                    }
                }
            }
        }

        context("getAssetBalanceAtDate() & calculateCashFlow() & getOpeningBalanceByCurrency()") {
            context("GroupBy.ACCOUNT") {
                expect("initial balance + calculated cashFlow = account ending balance Jan-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-01-01"),
                        LocalDate.parse("2025-01-31")
                    )
                    val accounts = accountRepository.fetchAccounts(dateRange.endDate, "asset")
                    val accountAtStart = summaryRepository.getAssetBalanceAtDate(dateRange.startDate, GroupBy.ACCOUNT, TimeOfDayBoundary.START)
                    val accountAtEnd = summaryRepository.getAssetBalanceAtDate(dateRange.endDate, GroupBy.ACCOUNT, TimeOfDayBoundary.END)
                    val cashFlows = summaryRepository.calculateCashFlow(dateRange, GroupBy.ACCOUNT)

                    for (account in accounts) {
                        val initialBalance = accountAtStart.getOrZero(account.id)
                        val endingBalance = accountAtEnd.getOrZero(account.id)
                        val cashFlow = cashFlows.getOrZero(account.id)
                        var openingBalance = 0.toBigDecimal()

                        if (account.attributes.openingBalanceDate!=null) {
                            val openingBalanceDate = LocalDate.parse(account.attributes.openingBalanceDate?:"",textDateFormat)
                            openingBalance = if (
                                !openingBalanceDate.isBefore(dateRange.startDate) &&
                                openingBalanceDate.isBefore(dateRange.endDate)
                            ) account.attributes.openingBalance.toBigDecimal()
                            else 0.toBigDecimal()
                        }
                        endingBalance.compareTo(initialBalance + cashFlow) shouldBe 0
                    }
                }

                expect("initial balance + calculated cashFlow = account ending balance Q1-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-01-01"),
                        LocalDate.parse("2025-04-30")
                    )
                    val accounts = accountRepository.fetchAccounts(dateRange.endDate, "asset")
                    val accountAtStart = summaryRepository.getAssetBalanceAtDate(dateRange.startDate, GroupBy.ACCOUNT, TimeOfDayBoundary.START)
                    val accountAtEnd = summaryRepository.getAssetBalanceAtDate(dateRange.endDate, GroupBy.ACCOUNT, TimeOfDayBoundary.END)
                    val cashFlows = summaryRepository.calculateCashFlow(dateRange, GroupBy.ACCOUNT)

                    for (account in accounts) {
                        val initialBalance = accountAtStart.getOrZero(account.id)
                        val endingBalance = accountAtEnd.getOrZero(account.id)
                        val cashFlow = cashFlows.getOrZero(account.id)
                        var openingBalance = 0.toBigDecimal()

                        if (account.attributes.openingBalanceDate!=null) {
                            val openingBalanceDate = LocalDate.parse(account.attributes.openingBalanceDate?:"",textDateFormat)
                            openingBalance = if (
                                !openingBalanceDate.isBefore(dateRange.startDate) &&
                                openingBalanceDate.isBefore(dateRange.endDate)
                            ) account.attributes.openingBalance.toBigDecimal()
                            else 0.toBigDecimal()
                        }
                        endingBalance.compareTo(initialBalance + cashFlow) shouldBe 0
                    }
                }

                expect("initial balance + calculated cashFlow = account ending balance H1-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-01-01"),
                        LocalDate.parse("2025-06-30")
                    )
                    val accounts = accountRepository.fetchAccounts(dateRange.endDate, "asset")
                    val accountAtStart = summaryRepository.getAssetBalanceAtDate(dateRange.startDate, GroupBy.ACCOUNT, TimeOfDayBoundary.START)
                    val accountAtEnd = summaryRepository.getAssetBalanceAtDate(dateRange.endDate, GroupBy.ACCOUNT, TimeOfDayBoundary.END)
                    val cashFlows = summaryRepository.calculateCashFlow(dateRange, GroupBy.ACCOUNT)

                    for (account in accounts) {
                        val initialBalance = accountAtStart.getOrZero(account.id)
                        val endingBalance = accountAtEnd.getOrZero(account.id)
                        val cashFlow = cashFlows.getOrZero(account.id)
                        var openingBalance = 0.toBigDecimal()

                        if (account.attributes.openingBalanceDate!=null) {
                            val openingBalanceDate = LocalDate.parse(account.attributes.openingBalanceDate?:"",textDateFormat)
                            openingBalance = if (
                                !openingBalanceDate.isBefore(dateRange.startDate) &&
                                openingBalanceDate.isBefore(dateRange.endDate)
                            ) account.attributes.openingBalance.toBigDecimal()
                            else 0.toBigDecimal()
                        }
                        endingBalance.compareTo(initialBalance + cashFlow) shouldBe 0
                    }
                }

                expect("initial balance + calculated cashFlow = account ending balance MJ-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-05-01"),
                        LocalDate.parse("2025-06-30")
                    )
                    val accounts = accountRepository.fetchAccounts(dateRange.endDate, "asset")
                    val accountAtStart = summaryRepository.getAssetBalanceAtDate(dateRange.startDate, GroupBy.ACCOUNT, TimeOfDayBoundary.START)
                    val accountAtEnd = summaryRepository.getAssetBalanceAtDate(dateRange.endDate, GroupBy.ACCOUNT, TimeOfDayBoundary.END)
                    val cashFlows = summaryRepository.calculateCashFlow(dateRange, GroupBy.ACCOUNT)

                    for (account in accounts) {
                        val initialBalance = accountAtStart.getOrZero(account.id)
                        val endingBalance = accountAtEnd.getOrZero(account.id)
                        val cashFlow = cashFlows.getOrZero(account.id)
                        var openingBalance = 0.toBigDecimal()

                        if (account.attributes.openingBalanceDate!=null) {
                            val openingBalanceDate = LocalDate.parse(account.attributes.openingBalanceDate?:"",textDateFormat)
                            openingBalance = if (
                                !openingBalanceDate.isBefore(dateRange.startDate) &&
                                openingBalanceDate.isBefore(dateRange.endDate)
                            ) account.attributes.openingBalance.toBigDecimal()
                            else 0.toBigDecimal()
                        }
                        endingBalance.compareTo(initialBalance + cashFlow) shouldBe 0
                    }
                }
            }

            context("GroupBy.CURRENCY_CODE") {
                expect("initial balance + calculated cashFlow = account ending balance Jan-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-01-01"),
                        LocalDate.parse("2025-01-31")
                    )
                    val accounts = accountRepository.fetchAccounts(dateRange.endDate, "asset")
                    val currencyAtStart = summaryRepository.getAssetBalanceAtDate(dateRange.startDate, GroupBy.CURRENCY_CODE, TimeOfDayBoundary.START)
                    val currencyAtEnd = summaryRepository.getAssetBalanceAtDate(dateRange.endDate, GroupBy.CURRENCY_CODE, TimeOfDayBoundary.END)
                    val cashFlows = summaryRepository.calculateCashFlow(dateRange, GroupBy.CURRENCY_CODE)
                    val openingBalances = summaryRepository.getOpeningBalanceByCurrency(dateRange)

                    for (currencyCode in currencyAtEnd.keys) {
                        val initialBalance = currencyAtStart.getOrZero(currencyCode)
                        val endingBalance = currencyAtEnd.getOrZero(currencyCode)
                        val cashFlow = cashFlows.getOrZero(currencyCode)
                        val openingBalance = openingBalances.getOrZero(currencyCode)
                        endingBalance.compareTo(initialBalance + cashFlow + openingBalance) shouldBe 0
                    }
                }

                expect("initial balance + calculated cashFlow = account ending balance Q1-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-01-01"),
                        LocalDate.parse("2025-04-30")
                    )
                    val accounts = accountRepository.fetchAccounts(dateRange.endDate, "asset")
                    val currencyAtStart = summaryRepository.getAssetBalanceAtDate(dateRange.startDate, GroupBy.CURRENCY_CODE, TimeOfDayBoundary.START)
                    val currencyAtEnd = summaryRepository.getAssetBalanceAtDate(dateRange.endDate, GroupBy.CURRENCY_CODE, TimeOfDayBoundary.END)
                    val cashFlows = summaryRepository.calculateCashFlow(dateRange, GroupBy.CURRENCY_CODE)
                    val openingBalances = summaryRepository.getOpeningBalanceByCurrency(dateRange)

                    for (currencyCode in currencyAtEnd.keys) {
                        val initialBalance = currencyAtStart.getOrZero(currencyCode)
                        val endingBalance = currencyAtEnd.getOrZero(currencyCode)
                        val cashFlow = cashFlows.getOrZero(currencyCode)
                        val openingBalance = openingBalances.getOrZero(currencyCode)
                        endingBalance.compareTo(initialBalance + cashFlow + openingBalance) shouldBe 0
                    }
                }

                expect("initial balance + calculated cashFlow = account ending balance H1-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-01-01"),
                        LocalDate.parse("2025-06-30")
                    )
                    val accounts = accountRepository.fetchAccounts(dateRange.endDate, "asset")
                    val currencyAtStart = summaryRepository.getAssetBalanceAtDate(dateRange.startDate, GroupBy.CURRENCY_CODE, TimeOfDayBoundary.START)
                    val currencyAtEnd = summaryRepository.getAssetBalanceAtDate(dateRange.endDate, GroupBy.CURRENCY_CODE, TimeOfDayBoundary.END)
                    val cashFlows = summaryRepository.calculateCashFlow(dateRange, GroupBy.CURRENCY_CODE)
                    val openingBalances = summaryRepository.getOpeningBalanceByCurrency(dateRange)

                    for (currencyCode in currencyAtEnd.keys) {
                        val initialBalance = currencyAtStart.getOrZero(currencyCode)
                        val endingBalance = currencyAtEnd.getOrZero(currencyCode)
                        val cashFlow = cashFlows.getOrZero(currencyCode)
                        val openingBalance = openingBalances.getOrZero(currencyCode)
                        endingBalance.compareTo(initialBalance + cashFlow + openingBalance) shouldBe 0
                    }
                }

                expect("initial balance + calculated cashFlow = account ending balance MJ-25") {
                    val dateRange = DateRangeBoundaries(
                        LocalDate.parse("2025-05-01"),
                        LocalDate.parse("2025-06-30")
                    )
                    val accounts = accountRepository.fetchAccounts(dateRange.endDate, "asset")
                    val currencyAtStart = summaryRepository.getAssetBalanceAtDate(dateRange.startDate, GroupBy.CURRENCY_CODE, TimeOfDayBoundary.START)
                    val currencyAtEnd = summaryRepository.getAssetBalanceAtDate(dateRange.endDate, GroupBy.CURRENCY_CODE, TimeOfDayBoundary.END)
                    val cashFlows = summaryRepository.calculateCashFlow(dateRange, GroupBy.CURRENCY_CODE)
                    val openingBalances = summaryRepository.getOpeningBalanceByCurrency(dateRange)

                    for (currencyCode in currencyAtEnd.keys) {
                        val initialBalance = currencyAtStart.getOrZero(currencyCode)
                        val endingBalance = currencyAtEnd.getOrZero(currencyCode)
                        val cashFlow = cashFlows.getOrZero(currencyCode)
                        val openingBalance = openingBalances.getOrZero(currencyCode)
                        endingBalance.compareTo(initialBalance + cashFlow + openingBalance) shouldBe 0
                    }
                }
            }
        }

        context("getOpeningBalanceByCurrency()") {
            val dateRange = DateRangeBoundaries(
                LocalDate.parse("2025-01-01"),
                LocalDate.parse("2025-06-30")
            )
            val transactions = transactionRepository.fetchTransactions(dateRange)
            val totalOpeningBalance = hashMapOf<String, BigDecimal>()

            for (transaction in transactions) {
                for (journal in transaction.attributes.transactions) {
                    if (journal.type == "opening balance") {
                        totalOpeningBalance[journal.currencyCode] = totalOpeningBalance.getOrZero(journal.currencyCode) + journal.amount.toBigDecimal()
                    }
                }
            }

            val openingBalances = summaryRepository.getOpeningBalanceByCurrency(dateRange)

            for (currencyCode in openingBalances.keys) {
                openingBalances.getOrZero(currencyCode).compareTo(totalOpeningBalance.getOrZero(currencyCode)) shouldBe 0
            }
        }

        context("getFullOverview()") {
            context("ending = initial + earned + spent + openingBalance Jan-25") {
                val dateRange = DateRangeBoundaries(
                    LocalDate.parse("2025-01-01"),
                    LocalDate.parse("2025-01-31")
                )
                val generalOverviews = summaryRepository.getFullOverview(dateRange)
                for (currencyCode in generalOverviews.keys) {
                    val go = generalOverviews[currencyCode]
                    if (go!=null) {
                        if (
                            go.openingBalance!=null&&
                            go.currencyDecimalPlaces!=null
                        ) {
                            val calcEnding = go.initialBalance + go.income + go.expense + go.openingBalance!!
                            calcEnding.compareTo(go.endingBalance) shouldBe 0
                        } else {
                            fail("Some property of GeneralOverview are null")
                        }
                    }
                }
            }

            context("ending = initial + earned + spent + openingBalance H1-25") {
                val dateRange = DateRangeBoundaries(
                    LocalDate.parse("2025-01-01"),
                    LocalDate.parse("2025-06-30")
                )
                val generalOverviews = summaryRepository.getFullOverview(dateRange)
                for (currencyCode in generalOverviews.keys) {
                    val go = generalOverviews[currencyCode]
                    if (go!=null) {
                        if (
                            go.openingBalance!=null&&
                            go.currencyDecimalPlaces!=null
                        ) {
                            val calcEnding = go.initialBalance + go.income + go.expense + go.openingBalance!!
                            calcEnding.compareTo(go.endingBalance) shouldBe 0
                        } else {
                            fail("Some property of GeneralOverview are null")
                        }
                    }
                }
            }

            context("ending = initial + earned + spent + openingBalance MJ-25") {
                val dateRange = DateRangeBoundaries(
                    LocalDate.parse("2025-05-01"),
                    LocalDate.parse("2025-06-30")
                )
                val generalOverviews = summaryRepository.getFullOverview(dateRange)
                for (currencyCode in generalOverviews.keys) {
                    val go = generalOverviews[currencyCode]
                    if (go!=null) {
                        if (
                            go.openingBalance!=null&&
                            go.currencyDecimalPlaces!=null
                        ) {
                            val calcEnding = go.initialBalance + go.income + go.expense + go.openingBalance!!
                            calcEnding.compareTo(go.endingBalance) shouldBe 0
                        } else {
                            fail("Some property of GeneralOverview are null")
                        }
                    }
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
