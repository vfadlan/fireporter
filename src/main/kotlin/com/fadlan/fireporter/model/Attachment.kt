package com.fadlan.fireporter.model

import java.awt.image.BufferedImage
import java.io.File
import java.time.ZonedDateTime

data class Attachment(
    val type: String,
    val id: String,

    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,

    val attachableId: String,
    val attachableType: String,

    val md5: String?=null,
    val hash: String?=null,
    var filename: String,

    val downloadUrl: String,
    val uploadUrl: String,

    var title: String?=null,
    val notes: String?=null,

    val mime: String,
    val size: Int,
    var file: File?=null,
    var imageFiles: List<AttachmentImage>,

    var elementId: String?=null,
    var parentId: String?=null,
    var parentDescription: String=""
)

data class AttachmentImage(
    val image: BufferedImage,
)