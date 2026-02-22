package com.example.bookkeeping.data.local.dao

import androidx.room.Dao
import androidx.room.Query

/** 按交易类型聚合的简单结果。 */
data class TypeSum(val type: String, val total: Long)

/** 按 label（日/周/月）和类型聚合的趋势点。 */
data class TrendRow(val label: String, val type: String, val total: Long)

/** 按分类聚合的金额结果。 */
data class CategorySum(val categoryId: String, val total: Long)

@Dao
interface ReportDao {

    /**
     * 指定时间范围内，按交易类型求和。
     * 返回 INCOME / EXPENSE 两行（若某类型无数据则无对应行）。
     */
    @Query(
        """
        SELECT type, SUM(amount) AS total
        FROM   transactions
        WHERE  occurredAt >= :startMs
          AND  occurredAt <  :endMs
        GROUP BY type
        """
    )
    suspend fun sumByType(startMs: Long, endMs: Long): List<TypeSum>

    /**
     * 按"自然日"聚合趋势（用于周报）。
     * label 格式：MM-dd（例如 02-15）
     */
    @Query(
        """
        SELECT strftime('%m-%d', occurredAt / 1000, 'unixepoch', 'localtime') AS label,
               type,
               SUM(amount) AS total
        FROM   transactions
        WHERE  occurredAt >= :startMs
          AND  occurredAt <  :endMs
        GROUP BY label, type
        ORDER BY label ASC
        """
    )
    suspend fun dailyTrend(startMs: Long, endMs: Long): List<TrendRow>

    /**
     * 按"周序号"聚合趋势（用于月报）。
     * label 格式：W01 … W53
     */
    @Query(
        """
        SELECT ('W' || printf('%02d', strftime('%W', occurredAt / 1000, 'unixepoch', 'localtime'))) AS label,
               type,
               SUM(amount) AS total
        FROM   transactions
        WHERE  occurredAt >= :startMs
          AND  occurredAt <  :endMs
        GROUP BY label, type
        ORDER BY label ASC
        """
    )
    suspend fun weeklyTrend(startMs: Long, endMs: Long): List<TrendRow>

    /**
     * 按"月份"聚合趋势（用于年报）。
     * label 格式：01 … 12
     */
    @Query(
        """
        SELECT strftime('%m', occurredAt / 1000, 'unixepoch', 'localtime') AS label,
               type,
               SUM(amount) AS total
        FROM   transactions
        WHERE  occurredAt >= :startMs
          AND  occurredAt <  :endMs
        GROUP BY label, type
        ORDER BY label ASC
        """
    )
    suspend fun monthlyTrend(startMs: Long, endMs: Long): List<TrendRow>

    /**
     * 指定时间范围内，按分类和类型聚合（收支构成）。
     */
    @Query(
        """
        SELECT categoryId, SUM(amount) AS total
        FROM   transactions
        WHERE  occurredAt >= :startMs
          AND  occurredAt <  :endMs
          AND  type        =  :type
        GROUP BY categoryId
        ORDER BY total DESC
        """
    )
    suspend fun categoryBreakdown(
        startMs: Long,
        endMs: Long,
        type: String = "EXPENSE",
    ): List<CategorySum>
}
