package com.fadlan.fireporter.service

import com.fadlan.fireporter.FireporterApp
import com.fadlan.fireporter.model.ChartEntry
import com.fadlan.fireporter.model.ReportData
import com.fadlan.fireporter.model.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sf.jasperreports.engine.*
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum
import net.sf.jasperreports.engine.xml.JRXmlLoader
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.imageio.ImageIO
import kotlin.collections.HashMap

class JasperReportService {
    private val compiledReports = mutableMapOf<String, JasperReport>()

    private val jrxmlFiles = listOf(
        "report-book-cover.jrxml",
        "report-summary.jrxml",
        "report-transaction.jrxml",
        "report-attachment.jrxml",
        "report-disclaimer.jrxml",
        "book.jrxml",
    )

    init {
        System.setProperty("net.sf.jasperreports.compiler.class", "net.sf.jasperreports.compilers.JRGroovyCompiler")

        for (jrxmlName in jrxmlFiles) {
            val resourcePath = "/com/fadlan/fireporter/jasper/$jrxmlName"
            val stream = FireporterApp::class.java.getResourceAsStream(resourcePath)
                ?: throw IllegalArgumentException("Missing jrxml resource: $resourcePath")
            val design = JRXmlLoader.load(stream)

            try {
                design.language = "groovy"
                val report = JasperCompileManager.compileReport(design)
                val reportKey = jrxmlName.removeSuffix(".jrxml")
                compiledReports[reportKey] = report
            } catch (e: JRException) {
                throw IllegalStateException("Failed to compile: $jrxmlName: ${e.message}", e)
            }
        }
    }

    private fun loadCompiledReport(name: String): JasperReport {
        return compiledReports[name]
            ?.apply { whenNoDataType = WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL }
            ?: throw IllegalArgumentException("Compiled report not found: $name")
    }

    private fun loadCoverImage(theme: Theme): BufferedImage {
        val resourcePath = "/com/fadlan/fireporter/cover-images/report-cover_${theme.name}.png"
        val coverStream: InputStream = FireporterApp::class.java.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Error: Report cover image not found for theme ${theme.name} at path: $resourcePath")

        return coverStream.use { ImageIO.read(it) }
    }

    suspend fun generatePdf(
        data: ReportData,
        outputFile: File,
        theme: Theme,
        withAttachment: Boolean
    ) {
        withContext(Dispatchers.IO) {
            val params = HashMap<String, Any>()

            params.loadCommonParameters(data, theme)
            params.loadCover(loadCoverImage(theme), loadCompiledReport("report-book-cover"))
            params.loadSummary(data, loadCompiledReport("report-summary"))
            params.loadTransactionHistory(data, loadCompiledReport("report-transaction"))
            params.loadAttachments(data, loadCompiledReport("report-attachment"))
            params.loadSysInfo(data)

            params["WITH_ATTACHMENT"] = withAttachment
            params["DISCLAIMER_REPORT"] = loadCompiledReport("report-disclaimer")

            val print = JasperFillManager.fillReport(loadCompiledReport("book"), params, JREmptyDataSource())
            JasperExportManager.exportReportToPdfFile(print, outputFile.path)
        }
    }
}

fun HashMap<String, Any>.loadCommonParameters(data: ReportData, theme: Theme) {
    this["REPORT_TITLE"] = if (data.dateRange.period == "All Year") {
        "${data.dateRange.year}"
    } else {
        "${data.dateRange.period} ${data.dateRange.year}"
    }

    this["THEME_COLOR"] = theme.colorHex
    this["THEME_DARK_COLOR"] = theme.darkColorHex

    this["CURRENCY_CODE"] = data.currencyCode
    this["CURRENCY_SYMBOL"] = data.currencySymbol
    this["CURRENCY_DECIMAL_PLACES"] = data.currencyDecimalPlaces

    this["DATE_RANGE"] = data.dateRange
}

fun HashMap<String, Any>.loadCover(coverImage: BufferedImage, coverReport: JasperReport) {
    this["COVER_REPORT"] = coverReport
    this["THEME_COVER"] = coverImage
}

fun HashMap<String, Any>.loadSummary(data: ReportData, summaryReport: JasperReport) {
    val textDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

    this["SUMMARY_REPORT"] = summaryReport

    this["GENERAL_OVERVIEW"] = data.generalOverview
    this["ASSET_ACCOUNTS"] = JRBeanCollectionDataSource(data.accounts)
    this["INCOME_INSIGHT"] = JRBeanCollectionDataSource(data.incomeInsight)
    this["EXPENSE_INSIGHT"] = JRBeanCollectionDataSource(data.expenseInsight)

    val dataSource = JRBeanCollectionDataSource(
        data.chart.mapNotNull { (key, value) ->
            try {
                if (key.isBlank()) return@mapNotNull null
                val localDate = LocalDate.parse(key, textDateFormat)
                val date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())

                ChartEntry(date, value)
            } catch (e: Exception) {
                null
            }
        }
    )

    this["SUMMARY_CHART_DATASET"] = dataSource
}

fun HashMap<String, Any>.loadTransactionHistory(data: ReportData, transactionReport: JasperReport) {
    this["TRANSACTION_REPORT"] = transactionReport
    this["TRANSACTION_HISTORY_DATASET"] = JRBeanCollectionDataSource(
        data.transactionJournals
    )
}

fun HashMap<String, Any>.loadAttachments(data: ReportData, attachmentReport: JasperReport) {
    this["ATTACHMENT_REPORT"] = attachmentReport
    this["ATTACHMENT_DATASET"] = JRBeanCollectionDataSource(
        data.downloadedAttachments
    )
}

fun HashMap<String, Any>.loadSysInfo(data: ReportData) {
    val fireporterVersion: String = object {}.javaClass
        .getResourceAsStream("/version.properties")
        ?.use { stream ->
            java.util.Properties().apply { load(stream) }
        }?.getProperty("app.version") ?: "Unknown"

    this["FIREPORTER_VERSION"] = fireporterVersion
    this["FIREFLY_VERSION"] = data.apiSysInfo.version
    this["JAVA_VERSION"] = System.getProperty("java.version")
    this["OS"] = System.getProperty("os.name")
}