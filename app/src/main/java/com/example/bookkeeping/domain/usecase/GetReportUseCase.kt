package com.example.bookkeeping.domain.usecase

import com.example.bookkeeping.data.local.dao.CategoryDao
import com.example.bookkeeping.data.local.dao.ReportDao
import com.example.bookkeeping.data.local.dao.TrendRow
import com.example.bookkeeping.domain.model.CategoryItem
import com.example.bookkeeping.domain.model.PeriodSummary
import com.example.bookkeeping.domain.model.ReportData
import com.example.bookkeeping.domain.model.ReportPeriod
import com.example.bookkeeping.domain.model.ReportPeriodType
import com.example.bookkeeping.domain.model.TrendPoint
import javax.inject.Inject

/**
 * 查询指定 [ReportPeriod] 的完整报表数据，包含：
 * - 当期/上期收支汇总
 * - 趋势数据（周→按日，月→按周，年→按月）
 * - 支出/收入分类构成
 */
class GetReportUseCase @Inject constructor(
    private val reportDao: ReportDao,
    private val categoryDao: CategoryDao,
) {

    suspend operator fun invoke(period: ReportPeriod): ReportData {
        // ---------- 汇总 ----------
        val current  = summarize(period.currentStart, period.currentEnd)
        val previous = summarize(period.prevStart,    period.prevEnd)

        // ---------- 趋势 ----------
        val trendRows = when (period.type) {
            ReportPeriodType.WEEK  -> reportDao.dailyTrend  (period.currentStart, period.currentEnd)
            ReportPeriodType.MONTH -> reportDao.weeklyTrend (period.currentStart, period.currentEnd)
            ReportPeriodType.YEAR  -> reportDao.monthlyTrend(period.currentStart, period.currentEnd)
        }
        val trend = buildTrendPoints(trendRows, period)

        // ---------- 分类构成 ----------
        val expenseCats = buildCategoryItems(
            reportDao.categoryBreakdown(period.currentStart, period.currentEnd, "EXPENSE"),
            current.totalExpense,
        )
        val incomeCats = buildCategoryItems(
            reportDao.categoryBreakdown(period.currentStart, period.currentEnd, "INCOME"),
            current.totalIncome,
        )

        return ReportData(
            period             = period,
            current            = current,
            previous           = previous,
            trend              = trend,
            expenseCategories  = expenseCats,
            incomeCategories   = incomeCats,
        )
    }

    // ------------------------------------------------------------------ helpers

    private suspend fun summarize(startMs: Long, endMs: Long): PeriodSummary {
        val rows = reportDao.sumByType(startMs, endMs)
        val income  = rows.firstOrNull { it.type == "INCOME"  }?.total ?: 0L
        val expense = rows.firstOrNull { it.type == "EXPENSE" }?.total ?: 0L
        return PeriodSummary(totalIncome = income, totalExpense = expense)
    }

    private fun buildTrendPoints(rows: List<TrendRow>, period: ReportPeriod): List<TrendPoint> {
        // 生成完整的时间轴标签（补零）
        val allLabels = generateLabels(period)
        // 将 TrendRow 按 label 分组
        val byLabel = rows.groupBy { it.label }

        return allLabels.map { label ->
            val group = byLabel[label] ?: emptyList()
            TrendPoint(
                label   = label,
                income  = group.firstOrNull { it.type == "INCOME"  }?.total ?: 0L,
                expense = group.firstOrNull { it.type == "EXPENSE" }?.total ?: 0L,
            )
        }
    }

    /** 根据周期类型生成完整的 x 轴标签序列（无数据时填 0）。 */
    private fun generateLabels(period: ReportPeriod): List<String> = when (period.type) {
        ReportPeriodType.WEEK -> {
            // 7 天：MM-dd 格式
            val start = java.time.Instant.ofEpochMilli(period.currentStart)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            (0L..6L).map { offset ->
                val d = start.plusDays(offset)
                "%02d-%02d".format(d.monthValue, d.dayOfMonth)
            }
        }
        ReportPeriodType.MONTH -> {
            // 当月所跨的完整周序号：W01 … W05
            val start = java.time.Instant.ofEpochMilli(period.currentStart)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val end = java.time.Instant.ofEpochMilli(period.currentEnd)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate().minusDays(1)
            val isoField = java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR
            val first = start.get(isoField)
            val last  = end.get(isoField)
            // 跨年处理：若 last < first，说明最后一周跨到下年，用 52/53 兜底
            val range = if (last >= first) first..last else first..53
            range.map { w -> "W%02d".format(w) }
        }
        ReportPeriodType.YEAR -> {
            (1..12).map { m -> "%02d".format(m) }
        }
    }

    private suspend fun buildCategoryItems(
        rows: List<com.example.bookkeeping.data.local.dao.CategorySum>,
        total: Long,
    ): List<CategoryItem> {
        if (total == 0L) return emptyList()
        val allCategories = categoryDao.getAll()
        val catMap = allCategories.associateBy { it.id }
        return rows.map { row ->
            val cat = catMap[row.categoryId]
            CategoryItem(
                categoryId   = row.categoryId,
                categoryName = cat?.name ?: row.categoryId,
                categoryIcon = cat?.icon,
                amount       = row.total,
                percentage   = (row.total * 100f / total).coerceIn(0f, 100f),
            )
        }
    }
}
