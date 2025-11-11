package com.fadlan.fireporter.repository

import com.fadlan.fireporter.dto.InsightItemDto
import com.fadlan.fireporter.model.DateRangeBoundaries
import com.fadlan.fireporter.model.InsightGroup
import com.fadlan.fireporter.model.InsightItem
import com.fadlan.fireporter.model.InsightType
import com.fadlan.fireporter.network.CredentialProvider
import com.fadlan.fireporter.network.safeRequest
import com.fadlan.fireporter.utils.titleCase
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class InsightRepository(
    private val ktor: HttpClient,
    private val cred: CredentialProvider
) {
    private suspend fun fetchInsight(type: String, filter: String, dateRange: DateRangeBoundaries): MutableList<InsightItemDto> {
        val response: HttpResponse = safeRequest {
            ktor.request(cred.host) {
                url {
                    appendPathSegments("api", "v1", "insight", type, filter)
                    parameters.append("start", dateRange.startDate.toString())
                    parameters.append("end", dateRange.endDate.toString())
                }

                headers.append(HttpHeaders.Authorization, "Bearer ${cred.token}")
                method = HttpMethod.Get
            }
        }
        val accountsResponse: MutableList<InsightItemDto> = response.body()
        accountsResponse.sortByDescending { it.differenceFloat }
        return accountsResponse
    }

    private suspend fun getInsightGroup(type: InsightType, filter: String, dateRange: DateRangeBoundaries): InsightGroup {
        val fetchedAccountInsight = fetchInsight(type.toString().lowercase(), filter, dateRange)
        val accountInsightList: List<InsightItem> = fetchedAccountInsight.map {
            InsightItem(
                it.id,
                it.name,
                it.difference.toBigDecimal(),
                it.currencyId,
                it.currencyCode
            )
        }

        val filterName = when (filter) {
            "revenue", "expense" -> "Account"
            else -> filter.titleCase()
        }

        return InsightGroup(
            type,
            filterName,
            when (type) {
                InsightType.INCOME -> accountInsightList.sortedByDescending { it.difference }
                InsightType.EXPENSE -> accountInsightList.sortedBy { it.difference }
            }
        )
    }

    suspend fun getInsights(type: InsightType, dateRange: DateRangeBoundaries): MutableList<InsightGroup> {
        val accountFilter: String = when (type) {
            InsightType.INCOME -> "revenue"
            InsightType.EXPENSE -> "expense"
        }
        val insights = mutableListOf<InsightGroup>()

        val insightByAccount = getInsightGroup(type, accountFilter, dateRange)
        if (insightByAccount.insights.isNotEmpty()) insights.add(insightByAccount)

        val insightByCategory = getInsightGroup(type, "category", dateRange)
        if (insightByCategory.insights.isNotEmpty()) insights.add(insightByCategory)

        val insightByTag = getInsightGroup(type, "tag", dateRange)
        if (insightByTag.insights.isNotEmpty()) insights.add(insightByTag)

        return insights
    }
}