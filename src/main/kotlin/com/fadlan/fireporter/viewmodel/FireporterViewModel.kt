package com.fadlan.fireporter.viewmodel

import com.fadlan.fireporter.dto.CurrencyAttributesDto
import com.fadlan.fireporter.dto.SystemInfoResponse
import com.fadlan.fireporter.model.Theme
import com.fadlan.fireporter.network.ClientErrorException
import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.network.ServerErrorException
import com.fadlan.fireporter.repository.CurrencyRepository
import com.fadlan.fireporter.service.DataCollectorService
import com.fadlan.fireporter.service.JasperReportService
import com.fadlan.fireporter.utils.DateRangeResolver
import com.fadlan.fireporter.utils.FxProgressTracker
import com.fadlan.fireporter.utils.IconizedAlert
import com.fadlan.fireporter.utils.exceptions.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import javafx.scene.control.Alert
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.stage.FileChooser
import javafx.stage.Window
import org.slf4j.Logger
import java.io.File

class FireporterViewModel(
        private val ktor: HttpClient,
        private val cred: CredentialProvider,
        private val progressTracker: FxProgressTracker,
        private val dateRangeResolver: DateRangeResolver,
        private val dataCollectorService: DataCollectorService,
        private val jasperReportService: JasperReportService,
        private val logger: Logger
    ) {
    init {
        progressTracker.totalSteps = 8
    }

    suspend fun generate(periodComboBox: ComboBox<String>, yearComboBox: ComboBox<Int>, theme: Theme, includeAttachmentCheckBox: CheckBox) {
        val period = periodComboBox.selectionModel.selectedItem
        val year = yearComboBox.selectionModel.selectedItem
        val dateRange = dateRangeResolver.resolve(period, year)
        val mainWindow = periodComboBox.scene.window

        try {
            val data = dataCollectorService.getData(dateRange, theme, includeAttachmentCheckBox.isSelected)
            progressTracker.report("Generating PDF File")
            val outputFile = showSaveDialog(mainWindow, period, year.toString()) ?: throw IllegalStateException("StandaloneCoroutine was cancelled")

            jasperReportService.generatePdf(data, outputFile, theme, includeAttachmentCheckBox.isSelected)

            progressTracker.report("Complete")

            IconizedAlert(
                Alert.AlertType.INFORMATION,
                "Report Saved",
                "Financial Report Generated",
                "The financial report has been successfully exported as a PDF file."
            ).showAndWait()
            logger.info("PDF Generated successfully.")
        } catch (exception: InactiveAccountException) {
            logger.error("Account inactive at period $period $year.")
            progressTracker.sendMessage("No active account found at period $period $year.")
            progressTracker.resetProgress()

            IconizedAlert(
                Alert.AlertType.ERROR,
                "No active account found",
                "${exception.message}",
                "You were inactive at $period $year"
            ).showAndWait()
        } catch (exception: IllegalDateRangeException) {
            logger.error("Invalid date given.")
            progressTracker.sendMessage(exception.message)
            progressTracker.resetProgress()

            IconizedAlert(
                Alert.AlertType.ERROR,
                "Invalid Date Given",
                "${exception.message}",
                "Try again tomorrow."
            ).showAndWait()
        } catch (exception: IllegalStateException) {
            logger.error("Process cancelled.")
            if (exception.message == "StandaloneCoroutine was cancelled") {
                progressTracker.sendMessage("Process cancelled.")
                progressTracker.resetProgress()

                IconizedAlert(
                    Alert.AlertType.ERROR,
                    "Process Canceled",
                    "Process canceled",
                    "You can close this pop-up."
                ).showAndWait()
            } else {
                logger.error("IllegalStateException: ${exception.message}")
                progressTracker.sendMessage("An unknown error occur.")
                progressTracker.resetProgress()

                IconizedAlert(
                    Alert.AlertType.ERROR,
                    "Illegal State Error",
                    "${exception.message}",
                    "Try again",
                ).showAndWait()
            }
        } catch (exception: MultipleCurrencyException) {
            logger.error("Unsupported Action: Multi-currency transaction detected. ${exception.message}")
            progressTracker.sendMessage("Multi-currency transaction found; this operation is not yet supported.")
            progressTracker.resetProgress()
            IconizedAlert(
                Alert.AlertType.ERROR,
                "Unsupported Action",
                "Multiple-Currency Transaction Detected",
                "Fireporter does not support transactions involving multiple currencies for this action."
            ).showAndWait()
        } catch (exception: ClientErrorException) {
            logger.error("Client Error: ${exception.message}")
            progressTracker.sendMessage("Client Error. Update to latest Fireporter and try again.")
            progressTracker.resetProgress()
            IconizedAlert(
                Alert.AlertType.ERROR,
                "Client Error",
                "Client Error, update to latest version of Fireporter and try again.",
                "If this error persist, please report to Issue on the Github Repository (vFadlan011/fireporter). Detailed error available on logs."
            ).showAndWait()
        } catch (exception: ServerErrorException) {
            logger.error("Firefly III Internal Server Error: ${exception.message}")
            progressTracker.sendMessage("Firefly III Internal Server Error.")
            progressTracker.resetProgress()
            IconizedAlert(
                Alert.AlertType.ERROR,
                "Firefly III Internal Server Error",
                "Firefly III Internal Server Error. Update your Firefly III to latest 6.x version and try again.",
                "If this error persist, please report to Issue on the Github Repository (vFadlan011/fireporter). Detailed error available on logs."
            ).showAndWait()
        } catch (exception: Exception) {
            logger.error("Unknown exception: ${exception.message}")
            progressTracker.sendMessage("An unknown error occur.")
            progressTracker.resetProgress()
            IconizedAlert(
                Alert.AlertType.ERROR,
                "Unknown Error",
                "Unknown Error",
                "An unknown error occurred: ${exception.message}"
            ).showAndWait()
        }
    }

    suspend fun testConnection(host: String, token: String): Boolean {
        cred.host = host
        cred.token = token
        logger.info("Testing connection")
        if (host.isBlank() || token.isBlank()) {
            logger.warn("Incomplete field: host address and access token.")
            IconizedAlert(
                Alert.AlertType.WARNING,
                "Incomplete Data",
                "Incomplete Data",
                "Please enter host address and access token."
            ).showAndWait()
            return false
        }

        try {
            val apiInfo = dataCollectorService.requestApiInfo(host, token)
            val sysInfo: SystemInfoResponse? = try { apiInfo.body<SystemInfoResponse>() } catch (e: Exception) { null }

            if (apiInfo.status == HttpStatusCode.Unauthorized) {
                logger.error("Unauthorized: invalid Firefly III Personal Access Token.")
                IconizedAlert(
                    Alert.AlertType.ERROR,
                    "Unauthorized",
                    "Unauthorized: Invalid Token",
                    "Enter a valid Firefly III Personal Access Token"
                ).showAndWait()
            } else if (sysInfo !is SystemInfoResponse) { // If response structure is not as expected
                logger.error("Invalid address: given host is not a valid Firefly III installation.")
                IconizedAlert(
                    Alert.AlertType.ERROR,
                    "Invalid Address",
                    "Invalid Address",
                    "The given host is not a valid Firefly III installation."
                ).showAndWait()
            } else {
                logger.debug("Connection tested successfully.")
                return true
            }
        } catch (exception: ServerResponseException) {
            logger.error("Internal Server Error: ${exception.message}")
            IconizedAlert(
                Alert.AlertType.ERROR,
                "Error",
                "Internal Server Error",
                exception.message
            ).showAndWait()
        } catch (exception: Exception) {
            logger.error("Unknown exception: ${exception.message}")
            IconizedAlert(
                Alert.AlertType.ERROR,
                "Unknown Error",
                "Unknown Error",
                "An unknown error occurred: ${exception.message}"
            ).showAndWait()
        }
        return false
    }

    private fun showSaveDialog(ownerWindow: Window, period: String, year: String): File? {
        val fileChooser = FileChooser()
        fileChooser.title = "Save Report As"
        fileChooser.initialFileName = "$period $year Financial Report.pdf"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("PDF Files", "*.pdf"))

        return fileChooser.showSaveDialog(ownerWindow)
    }
}
