package com.fadlan.fireporter.repository

import com.fadlan.fireporter.network.CredentialProvider
import io.kotest.core.spec.style.ExpectSpec
import io.ktor.client.*

class SummaryRepositoryTest : ExpectSpec({
    val ktor = HttpClient()
    val cred = CredentialProvider
    cred.host
    cred.token

    context("/summary/basic") {
        expect("response type should be HashMap<String, BasicSummaryDto>") {
        }
    }
})
