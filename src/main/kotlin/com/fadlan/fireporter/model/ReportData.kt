package com.fadlan.fireporter.model

import com.fadlan.fireporter.dto.SystemInfoDto

data class  ReportData(
    val dateRange: DateRangeBoundaries,
    val theme: Theme,
    val currency: Currency,
    val accounts: MutableList<Account>,
    val chart: HashMap<String, MutableList<ChartEntry>>,
    val generalOverview: GeneralOverview,
    val incomeInsight: MutableList<InsightGroup>,
    val expenseInsight: MutableList<InsightGroup>,
    val transactionJournals: MutableList<TransactionJournal>,
    val downloadedAttachments: MutableList<Attachment>,
    val apiSysInfo: SystemInfoDto
)