package com.example.bookkeeping.domain.model

/**
 * 单个周期的收支汇总。
 * 所有金额单位：分（Long）。
 */
data class PeriodSummary(
    val totalIncome: Long  = 0L,
    val totalExpense: Long = 0L,
) {
    /** 结余 = 收入 - 支出（可为负）。 */
    val balance: Long get() = totalIncome - totalExpense
}

/** 趋势折线/柱状图的单个数据点。 */
data class TrendPoint(
    /** x 轴标签，如 "周一""02-15""01" */
    val label: String,
    val income: Long  = 0L,
    val expense: Long = 0L,
)

/** 收支构成中的单个分类条目。 */
data class CategoryItem(
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String?,
    /** 该分类金额（分） */
    val amount: Long,
    /** 占总支出/收入的百分比（0..100） */
    val percentage: Float,
)

/**
 * 完整的报表数据。
 *
 * @param period            报表周期（含类型、标签、时间范围）
 * @param current           当期收支汇总
 * @param previous          上一期收支汇总（用于同比/环比对比）
 * @param trend             趋势数据点列表（按时间升序）
 * @param expenseCategories 支出分类构成（按金额降序）
 * @param incomeCategories  收入分类构成（按金额降序）
 */
data class ReportData(
    val period: ReportPeriod,
    val current: PeriodSummary,
    val previous: PeriodSummary,
    val trend: List<TrendPoint>,
    val expenseCategories: List<CategoryItem>,
    val incomeCategories: List<CategoryItem>,
) {
    /** 支出环比变化（正：增加，负：减少），单位：分。 */
    val expenseDelta: Long get() = current.totalExpense - previous.totalExpense

    /** 收入环比变化，单位：分。 */
    val incomeDelta: Long  get() = current.totalIncome  - previous.totalIncome

    /** 结余环比变化，单位：分。 */
    val balanceDelta: Long get() = current.balance - previous.balance
}
