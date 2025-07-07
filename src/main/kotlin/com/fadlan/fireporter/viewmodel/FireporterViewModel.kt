package com.fadlan.fireporter.viewmodel

import com.fadlan.fireporter.dto.SystemInfoResponse
import com.fadlan.fireporter.model.DateRangeBoundaries
import com.fadlan.fireporter.model.Theme
import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.service.DataCollectorService
import com.fadlan.fireporter.service.JasperReportService
import com.fadlan.fireporter.utils.DateRangeResolver
import com.fadlan.fireporter.utils.FxProgressTracker
import com.fadlan.fireporter.utils.IconizedAlert
import com.fadlan.fireporter.utils.exceptions.IllegalDateRangeException
import com.fadlan.fireporter.utils.exceptions.InactiveAccountException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import javafx.scene.control.Alert
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.stage.FileChooser
import javafx.stage.Window
import java.io.File

class FireporterViewModel(
        private val ktor: HttpClient,
        private val cred: CredentialProvider,
        private val progressTracker: FxProgressTracker,
        private val dateRangeResolver: DateRangeResolver,
        private val dataCollectorService: DataCollectorService,
        private val jasperReportService: JasperReportService
    ) {
    init {
        progressTracker.totalSteps = 8
    }

    suspend fun generate(host: String, token: String, periodComboBox: ComboBox<String>, yearComboBox: ComboBox<Int>, theme: Theme, includeAttachmentCheckBox: CheckBox) {
        cred.host = host
        cred.token = token
        if (!testConnection(host, token)) return

        val period = periodComboBox.selectionModel.selectedItem
        val year = yearComboBox.selectionModel.selectedItem
        val dateRange: DateRangeBoundaries = dateRangeResolver.resolve(period, year)
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
        } catch (exception: InactiveAccountException) {
            progressTracker.sendMessage(exception.message)
            progressTracker.resetProgress()

            IconizedAlert(
                Alert.AlertType.ERROR,
                "No account Found",
                "${exception.message}",
                "You were inactive at $period $year"
            ).showAndWait()
        } catch (exception: IllegalDateRangeException) {
            progressTracker.sendMessage(exception.message)
            progressTracker.resetProgress()

            IconizedAlert(
                Alert.AlertType.ERROR,
                "Invalid Date Given",
                "${exception.message}",
                "Try again tomorrow."
            ).showAndWait()
        } catch (exception: IllegalStateException) {
            if (exception.message == "StandaloneCoroutine was cancelled") {
                progressTracker.sendMessage("Process canceled.")
                progressTracker.resetProgress()

                IconizedAlert(
                    Alert.AlertType.ERROR,
                    "Process Canceled",
                    "Process canceled",
                    "You can close this pop-up."
                ).showAndWait()
            } else {
                progressTracker.sendMessage("An unknown error occur.")
                progressTracker.resetProgress()

                IconizedAlert(
                    Alert.AlertType.ERROR,
                    "Unknown Error",
                    "${exception.message}",
                    "Try again",
                ).showAndWait()
            }
        }
    }

    suspend fun testConnection(host: String, token: String): Boolean {
        if (host.isBlank() || token.isBlank()) {
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
                IconizedAlert(
                    Alert.AlertType.ERROR,
                    "Unauthorized",
                    "Unauthorized: Invalid Token",
                    "Enter a valid Firefly III Personal Access Token"
                ).showAndWait()
            } else if (sysInfo !is SystemInfoResponse) { // If response structure is not as expected
                IconizedAlert(
                    Alert.AlertType.ERROR,
                    "Invalid Address",
                    "Invalid Address",
                    "The given host is not a valid Firefly III installation."
                ).showAndWait()
            } else {
                return true
            }
        } catch (exception: ServerResponseException) {
            IconizedAlert(
                Alert.AlertType.ERROR,
                "Error",
                "Internal Server Error",
                "exception.message"
            ).showAndWait()
        } catch (exception: Exception) {
            IconizedAlert(
                Alert.AlertType.ERROR,
                "Unknown Error",
                "Unknown Error",
                "An unknown error occured: ${exception.message}"
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