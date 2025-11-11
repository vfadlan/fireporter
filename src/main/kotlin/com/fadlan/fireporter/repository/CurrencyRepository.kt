package com.fadlan.fireporter.repository

import com.fadlan.fireporter.dto.CurrencyDto
import com.fadlan.fireporter.dto.CurrencyResponseDto
import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.network.safeRequest
import com.fadlan.fireporter.utils.exceptions.InvalidCurrencyCodeException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class CurrencyRepository(
    private val ktor: HttpClient,
    private val cred: CredentialProvider
) {
    suspend fun fetchCurrency(currencyCode: String): CurrencyDto {
        val response: HttpResponse = safeRequest {
            ktor.request(cred.host) {
                url {
                    appendPathSegments("api", "v1", "currencies", currencyCode)
                }

                headers.append(HttpHeaders.Authorization, "Bearer ${cred.token}")
                method = HttpMethod.Get
            }
        }
        if (response.status == HttpStatusCode.NotFound) throw InvalidCurrencyCodeException()

        val currencyAttributes = response.body<CurrencyResponseDto>().data ?: throw InvalidCurrencyCodeException()
        if (!currencyAttributes.attributes.enabled) throw InvalidCurrencyCodeException()

        return currencyAttributes
    }
}