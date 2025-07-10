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
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.tools.imageio.ImageIOUtil
import org.slf4j.Logger
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO


class AttachmentService(
    private val progressTracker: FxProgressTracker,
    private val ktor: HttpClient,
    private val cred: CredentialProvider,
    private val logger: Logger
) {
    private val tempDir: Path = Files.createTempDirectory("fireporter-attachments-")
        .apply { toFile().deleteOnExit() }

    private suspend fun downloadFile(url: String, filename: String): File {
        val response: HttpResponse = ktor.get(url) {
            headers.append("Authorization", "Bearer ${cred.token}")
        }

        val tempFile: Path = withContext(Dispatchers.IO) {
            val createdFile = Files.createTempFile(tempDir, "att-", filename)
            createdFile.toFile().deleteOnExit()
            createdFile
        }

        response.readRawBytes().also { bytes ->
            Files.write(tempFile, bytes)
        }

        return tempFile.toFile()
    }

    private suspend fun pdf2Img(file: File): MutableList<BufferedImage> = withContext(Dispatchers.Default) {
        val outputFiles = mutableListOf<BufferedImage>()
        val document: PDDocument = Loader.loadPDF(file)
        val pdfRenderer = PDFRenderer(document)

        val baseName = file.nameWithoutExtension
        val parentDir = file.parentFile ?: File(".")

        for (page in 0 until document.numberOfPages) {
            val bim = pdfRenderer.renderImageWithDPI(page, 300f, ImageType.RGB)
            val outFile = File(parentDir, "${baseName}-${page + 1}.png").apply { deleteOnExit() }
            ImageIOUtil.writeImage(bim, outFile.absolutePath, 300)
            outputFiles.add(withContext(Dispatchers.IO) {
                ImageIO.read(outFile)
            })
        }

        document.close()
        outputFiles
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
                        val file = downloadFile(attachment.downloadUrl, attachment.filename)
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