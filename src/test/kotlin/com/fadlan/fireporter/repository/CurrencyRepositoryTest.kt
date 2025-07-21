package com.fadlan.fireporter.repository

import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.network.Ktor
import com.fadlan.fireporter.utils.DateRangeResolver
import com.fadlan.fireporter.utils.exceptions.InvalidCurrencyCodeException
import com.fadlan.fireporter.utils.getProperty
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent

class CurrencyRepositoryTest: ExpectSpec({
    val testModule = module {
        single<HttpClient> { Ktor.client }
        single<DateRangeResolver> { DateRangeResolver }
        single<CredentialProvider> { CredentialProvider }

        single {
            CurrencyRepository(
                get<HttpClient>(),
                get<CredentialProvider>(),
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

    context("CurrencyRepository") {
        val currencyRepository = KoinJavaComponent.get<CurrencyRepository>(CurrencyRepository::class.java)

        expect("should return correct currency data USD") {
            val currency = currencyRepository.fetchCurrency("USD")
            currency.attributes.name.shouldBe("US Dollar")
            currency.attributes.code.shouldBe("USD")
            currency.attributes.symbol.shouldBe("$")
        }

        expect("should throw InvalidCurrencyCodeException on nonexistent currency") {
            shouldThrow<InvalidCurrencyCodeException> {
                currencyRepository.fetchCurrency("NONEXISTENT")
            }
        }

        expect("should throw InvalidCurrencyCodeException on disabled currency") {
            shouldThrow<InvalidCurrencyCodeException> {
                currencyRepository.fetchCurrency("CZK")
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})