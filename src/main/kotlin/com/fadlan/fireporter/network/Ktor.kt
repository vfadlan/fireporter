package com.fadlan.fireporter.network

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object Ktor {
    val client = HttpClient() {
        install(ContentNegotiation)  {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }
}

class ClientErrorException(val status: HttpStatusCode, message: String? = null) :
    Exception("Client error ${status.value}: ${message ?: status.description}")

class ServerErrorException(val status: HttpStatusCode, message: String? = null) :
    Exception("Server error ${status.value}: ${message ?: status.description}")

suspend inline fun <reified T> safeRequest(
    crossinline block: suspend () -> HttpResponse
): T {
    val response = block()

    when (response.status.value) {
        in 200..299 -> return response.body()
        in 400..499 -> throw ClientErrorException(response.status, response.bodyAsText())
        in 500..599 -> throw ServerErrorException(response.status, response.bodyAsText())
        else -> throw Exception("Unexpected response ${response.status}")
    }
}