package com.fadlan.fireporter.repository

import com.fadlan.fireporter.dto.AttachmentDto
import com.fadlan.fireporter.dto.AttachmentResponse
import com.fadlan.fireporter.model.Attachment
import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.network.safeRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AttachmentRepository(
    private val ktor: HttpClient,
    private val cred: CredentialProvider
) {
    private suspend fun fetchSinglePageAttachmentsByTransactionId(page: Int, transactionId: String): AttachmentResponse {
        val response: HttpResponse = safeRequest {
            ktor.request(cred.host) {
                url {
                    appendPathSegments("api", "v1", "transactions", transactionId, "attachments")
                    parameters.append("page", page.toString())
                }

                headers.append(HttpHeaders.Authorization, "Bearer ${cred.token}")
                method = HttpMethod.Get
            }
        }
        return response.body()
    }

    private suspend fun fetchAttachmentsByTransactionId(transactionId: String): MutableList<AttachmentDto> {
        var currentPage = 1
        val attachmentResponse = fetchSinglePageAttachmentsByTransactionId(currentPage, transactionId)
        val totalPages = attachmentResponse.meta.pagination.totalPages

        val attachments: MutableList<AttachmentDto> = attachmentResponse.data.toMutableList()

        while (currentPage < totalPages) {
            currentPage++
            attachments += fetchSinglePageAttachmentsByTransactionId(currentPage, transactionId).data
        }
        return attachments
    }

    suspend fun getAttachmentsByTransactionId(transactionId: String): MutableList<Attachment> {
        val textDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        val fetchedAttachments = fetchAttachmentsByTransactionId(transactionId)

        val attachments: MutableList<Attachment> = mutableListOf()

        for (attachment in fetchedAttachments) {
            attachments += Attachment(
                attachment.type,
                attachment.id,
                ZonedDateTime.parse(attachment.attributes.createdAt, textDateFormat),
                ZonedDateTime.parse(attachment.attributes.updatedAt, textDateFormat),
                attachment.attributes.attachableId,
                attachment.attributes.attachableType,
                attachment.attributes.md5,
                attachment.attributes.hash,
                attachment.attributes.filename,
                attachment.attributes.downloadUrl,
                attachment.attributes.uploadUrl,
                attachment.attributes.title,
                attachment.attributes.notes,
                attachment.attributes.mime,
                attachment.attributes.size,
                file=null,
                mutableListOf()
            )
        }

        return attachments
    }
}