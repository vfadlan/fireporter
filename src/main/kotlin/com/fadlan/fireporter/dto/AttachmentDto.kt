package com.fadlan.fireporter.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttachmentResponse(
    val data: List<AttachmentDto>,
    val meta: MetaDto,
    val links: OpenApiLinkDto
)

@Serializable
data class AttachmentDto(
    val type: String,
    val id: String,
    val attributes: AttachmentAttributeDto,
    val links: OpenApiLinkDto
)

@Serializable
data class AttachmentAttributeDto(
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("attachable_id") val attachableId: String,
    @SerialName("attachable_type") val attachableType: String,
    val md5: String?=null,
    val hash: String?=null,
    val filename: String,
    @SerialName("download_url") val downloadUrl: String,
    @SerialName("upload_url") val uploadUrl: String,
    val title: String?=null,
    val notes: String?=null,
    val mime: String,
    val size: Int
)