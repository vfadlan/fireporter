package com.fadlan.fireporter.repository

import com.fadlan.fireporter.dto.ChartDto
import com.fadlan.fireporter.model.ChartEntry
import com.fadlan.fireporter.model.DateRangeBoundaries
import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.network.safeRequest
import com.fadlan.fireporter.utils.exceptions.UnusedCurrencyException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

class ChartRepository(
    private val ktor: HttpClient,
    private val cred: CredentialProvider
) {
    private suspend fun fetchCharts(dateRange: DateRangeBoundaries): MutableList<ChartDto> {
        val response: HttpResponse = safeRequest {
            ktor.request(cred.host) {
                url {
                    appendPathSegments("api", "v1", "chart", "account", "overview")
                    parameters.append("start", dateRange.startDate.toString())
                    parameters.append("end", dateRange.endDate.toString())
                }

                headers.append(HttpHeaders.Authorization, "Bearer ${cred.token}")
                method = HttpMethod.Get
            }
        }
        val chart: MutableList<ChartDto> = response.body()
        return chart
    }

    suspend fun getCharts(dateRange: DateRangeBoundaries, currencyCode: String): HashMap<String, MutableList<ChartEntry>> {
        val textDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        val fetchedCharts = fetchCharts(dateRange)
        val charts = HashMap<String, MutableList<ChartEntry>>()

        for (chart in fetchedCharts) {
            val entries = mutableListOf<ChartEntry>()
            for (entry in chart.entries) {
                val localDate = LocalDate.parse(entry.key, textDateFormat)
                val date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                entries.add(
                    ChartEntry(date, entry.value.toBigDecimal())
                )
            }
            if (chart.currencyCode == currencyCode) charts[chart.label] = entries
        }

        if (charts.size==0) throw UnusedCurrencyException()
        return charts
    }

    suspend fun getMergedChart(dateRange: DateRangeBoundaries): LinkedHashMap<String, BigDecimal> {
        val fetchedCharts = fetchCharts(dateRange)
        val mergedChart = LinkedHashMap<String, BigDecimal>()

        for (chart in fetchedCharts) {
            val entries = chart.entries
            for (chartEntry in entries) {
                mergedChart[chartEntry.key] = mergedChart.getOrDefault(chartEntry.key, BigDecimal.ZERO).add(chartEntry.value.toBigDecimal())
            }
        }

        return mergedChart
    }

}