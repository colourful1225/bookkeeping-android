package com.example.bookkeeping.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** 报表周期类型。 */
enum class ReportPeriodType { WEEK, MONTH, YEAR }

/**
 * 代表一个具体的报表周期，持有当期和上一期的时间范围。
 *
 * @param type         周期类型（周/月/年）
 * @param currentStart 当期开始时间（ms，含）
 * @param currentEnd   当期结束时间（ms，不含）
 * @param prevStart    上一期开始时间（ms，含）
 * @param prevEnd      上一期结束时间（ms，不含）
 * @param label        展示用标签，如"2026年第8周"/"2026年2月"/"2026年"
 */
data class ReportPeriod(
    val type: ReportPeriodType,
    val currentStart: Long,
    val currentEnd: Long,
    val prevStart: Long,
    val prevEnd: Long,
    val label: String,
)

/** 工厂方法：以 [reference] 为基准日期（默认今天）生成对应周期。 */
object ReportPeriodFactory {

    private val zone = ZoneId.systemDefault()

    fun week(reference: LocalDate = LocalDate.now()): ReportPeriod {
        val weekStart = reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd   = weekStart.plusWeeks(1)
        val prevStart = weekStart.minusWeeks(1)
        val weekNum   = reference.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return ReportPeriod(
            type         = ReportPeriodType.WEEK,
            currentStart = weekStart.toEpochMilli(zone),
            currentEnd   = weekEnd.toEpochMilli(zone),
            prevStart    = prevStart.toEpochMilli(zone),
            prevEnd      = weekStart.toEpochMilli(zone),
            label        = "${reference.year}年第${weekNum}周",
        )
    }

    fun month(reference: LocalDate = LocalDate.now()): ReportPeriod {
        val ym        = YearMonth.from(reference)
        val monthStart = ym.atDay(1)
        val monthEnd   = ym.plusMonths(1).atDay(1)
        val prevYm     = ym.minusMonths(1)
        return ReportPeriod(
            type         = ReportPeriodType.MONTH,
            currentStart = monthStart.toEpochMilli(zone),
            currentEnd   = monthEnd.toEpochMilli(zone),
            prevStart    = prevYm.atDay(1).toEpochMilli(zone),
            prevEnd      = monthStart.toEpochMilli(zone),
            label        = "${ym.year}年${ym.monthValue}月",
        )
    }

    fun year(reference: LocalDate = LocalDate.now()): ReportPeriod {
        val yearStart = LocalDate.of(reference.year, 1, 1)
        val yearEnd   = LocalDate.of(reference.year + 1, 1, 1)
        val prevStart = LocalDate.of(reference.year - 1, 1, 1)
        return ReportPeriod(
            type         = ReportPeriodType.YEAR,
            currentStart = yearStart.toEpochMilli(zone),
            currentEnd   = yearEnd.toEpochMilli(zone),
            prevStart    = prevStart.toEpochMilli(zone),
            prevEnd      = yearStart.toEpochMilli(zone),
            label        = "${reference.year}年",
        )
    }

    private fun LocalDate.toEpochMilli(zone: ZoneId): Long =
        atStartOfDay(zone).toInstant().toEpochMilli()
}
