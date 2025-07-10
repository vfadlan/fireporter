package com.fadlan.fireporter.service

import com.fadlan.fireporter.model.Attachment
import com.fadlan.fireporter.model.AttachmentImage
import com.fadlan.fireporter.model.TransactionJournal
import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.utils.FxProgressTracker
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.tools.imageio.ImageIOUtil
import org.slf4j.Logger
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import javax.imageio.ImageIO


class AttachmentService(
    private val progressTracker: FxProgressTracker,
    private val ktor: HttpClient,
    private val cred: CredentialProvider,
    private val logger: Logger
) {
    private val tempDir: Path = Paths.get(".", "temp", "attachments")
        .toAbsolutePath()
        .normalize()
        .also { Files.createDirectories(it) }

    private suspend fun downloadFile(url: String, attachment: Attachment): File {
        val safeFilename = "att_${attachment.id}-${attachment.filename.replace(Regex("[^\\w.-]"), "_")}"
        val tempPath = tempDir.resolve(safeFilename)

        if (!Files.exists(tempPath)) {
            val response: HttpResponse = ktor.get(url) {
                headers.append("Authorization", "Bearer ${cred.token}")
            }

            withContext(Dispatchers.IO) {
                Files.write(tempPath, response.readRawBytes(), StandardOpenOption.CREATE)
            }
            logger.debug("File $safeFilename downloaded successfully.")
        } else {
            logger.debug("File $safeFilename already exists, skipping download.")
        }

        return tempPath.toFile().apply { deleteOnExit() }
    }

    private suspend fun pdf2Img(file: File): MutableList<BufferedImage> = withContext(Dispatchers.Default) {
        val outputImages = mutableListOf<BufferedImage>()
        val document = Loader.loadPDF(file)
        val renderer = PDFRenderer(document)
        val baseName = file.nameWithoutExtension

        for (page in 0 until document.numberOfPages) {
            val safePageName = "${baseName}-pg_${page + 1}.png".replace(Regex("[^\\w.-]"), "_")
            val pagePath = tempDir.resolve(safePageName)

            if (!Files.exists(pagePath)) {
                val image = renderer.renderImageWithDPI(page, 300f, ImageType.RGB)
                ImageIOUtil.writeImage(image, pagePath.toAbsolutePath().toString(), 300)
                logger.debug("Page image $safePageName generated successfully.")
            } else {
                logger.debug("Page image $safePageName already exists, skipping.")
            }

            val pageFile = pagePath.toFile().apply { deleteOnExit() }

            outputImages.add(withContext(Dispatchers.IO) {
                ImageIO.read(pageFile)
            })
        }

        document.close()
        outputImages
    }

    suspend fun downloadAttachments(transactionJournals: List<TransactionJournal>): MutableList<Attachment> =
        withContext(Dispatchers.Default) {
            val downloadedAttachments = mutableListOf<Attachment>()

            for ((currentJournal, journal) in transactionJournals.withIndex()) {
                for (attachment in journal.attachments) {
                    withContext(Dispatchers.Main) {
                        progressTracker.sendMessage("Downloading attachments (${currentJournal+1}/${transactionJournals.size}): ${attachment.filename}")
                    }
                    try {
                        val file = downloadFile(attachment.downloadUrl, attachment)
                        attachment.file = file

                        attachment.imageFiles = if (attachment.mime.startsWith("image/")) {
                            mutableListOf(
                                AttachmentImage(
                                    withContext(Dispatchers.IO) { ImageIO.read(file) }
                                )
                            )
                        } else if (attachment.mime == "application/pdf") {
                            pdf2Img(file).map {
                                AttachmentImage(it)
                            }
                        } else {
                            mutableListOf()
                        }

                        downloadedAttachments += attachment
                    } catch (exception: TimeoutCancellationException) {
                        logger.error("Timeout Exception: ${exception.message}")
                    } catch (exception: Exception) {
                        logger.error("Unknown exception: ${exception.message}")
                    }
                }
            }
            logger.info("${downloadedAttachments.size} attachments downloaded successfully.")

            downloadedAttachments
        }
}