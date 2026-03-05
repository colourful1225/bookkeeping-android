package com.example.bookkeeping.notification

import android.util.Log
import com.example.bookkeeping.data.local.dao.TransactionDao
import com.example.bookkeeping.data.local.entity.TransactionEntity
import com.example.bookkeeping.notification.model.ConflictAlert
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自动记账与手动记账冲突检测器。
 *
 * ## 问题
 * 用户在 App 内手动记账 + 自动记账同时触发 → 相同交易被重复记录。
 *
 * ## 解决策略：时间+金额+分类+类型四维对比
 * 若检测到冲突，自动记账会被跳过。
 *
 * 四维匹配规则（需同时满足）：
 * 1. **金额相同**：自动记的金额 == 数据库内最近记录
 * 2. **时间接近**：距离 ≤ [MANUAL_AUTO_CONFLICT_WINDOW_MS]（默认 30 秒）
 * 3. **分类一致**：两者分类 ID 相同或兼容
 * 4. **类型相同**：均为 EXPENSE 或均为 INCOME
 *
 * ## 优化说明
 * - 不使用精确商户名匹配（自动记账商户名可能格式化后与手动输入差异）
 * - 优先查询最近 5 分钟内的记录（高效）
 * - 失败时 fallback 全表扫描但加上分类+类型索引加速
 */
@Singleton
class ConflictDetector @Inject constructor(
    private val transactionDao: TransactionDao,
) {

    companion object {
        private const val TAG = "ConflictDetector"

        /** 手动 + 自动记账冲突检测时间窗口（毫秒） */
        const val MANUAL_AUTO_CONFLICT_WINDOW_MS = 30_000L

        /** 查询范围（为了性能，只查最近 N 分钟） */
        private const val RECENT_TX_WINDOW_MS = 5 * 60 * 1000L
    }

    /**
     * 检测是否与手动记账冲突。
     *
     * @param amountFen 自动记账的金额（分）
     * @param categoryId 自动推断的分类 ID
     * @param occurredAt 交易发生时间
     * @param type 交易类型（"EXPENSE" 或 "INCOME"）
     * @return true:检测到冲突，不应写入; false:无冲突，可写入
     */
    suspend fun hasConflict(
        amountFen: Long,
        categoryId: String,
        occurredAt: Long,
        type: String = "EXPENSE",
    ): Boolean {
        val now = System.currentTimeMillis()
        val windowStart = maxOf(
            occurredAt - MANUAL_AUTO_CONFLICT_WINDOW_MS / 2,
            now - RECENT_TX_WINDOW_MS
        )
        val windowEnd = minOf(
            occurredAt + MANUAL_AUTO_CONFLICT_WINDOW_MS / 2,
            now + 1000 // 允许轻微未来偏差
        )

        try {
            // 获取全部交易，再在内存中过滤（简单可靠）
            val allTxs = transactionDao.getAll()
            val recentTxs = allTxs.filter { tx ->
                tx.type == type
                    && tx.occurredAt >= windowStart
                    && tx.occurredAt <= windowEnd
                    && tx.amount >= amountFen - 100  // 容差 ±1元
                    && tx.amount <= amountFen + 100
            }

            val conflict = recentTxs.find { tx ->
                tx.amount == amountFen
                    && tx.type == type
                    && isCompatibleCategory(tx.categoryId, categoryId)
                    && isTimeClose(tx.occurredAt, occurredAt)
            }

            if (conflict != null) {
                Log.w(TAG, "检测到冲突: id=${conflict.id} ¥${amountFen / 100.0} cat=$categoryId type=$type")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "冲突检测异常: ${e.message}", e)
            // 失败时采保守策略（不丢弃记录），但需要监控
        }
        return false
    }

    // ── 工具方法 ────────────────────────────────
    private fun isTimeClose(recordTime: Long, autoTime: Long): Boolean {
        val delta = kotlin.math.abs(recordTime - autoTime)
        return delta <= MANUAL_AUTO_CONFLICT_WINDOW_MS
    }

    /**
     * 检测冲突并返回详细信息（用于 UI 显示对话框）。
     *
     * @return ConflictAlert 若检测到冲突，否则 null
     */
    suspend fun detectConflictWithDetails(
        autoAmountFen: Long,
        autoCategoryId: String,
        autoOccurredAt: Long,
        autoNote: String,
        autoSource: String,
        type: String = "EXPENSE",
    ): ConflictAlert? {
        val now = System.currentTimeMillis()
        val windowStart = maxOf(
            autoOccurredAt - MANUAL_AUTO_CONFLICT_WINDOW_MS / 2,
            now - RECENT_TX_WINDOW_MS
        )
        val windowEnd = minOf(
            autoOccurredAt + MANUAL_AUTO_CONFLICT_WINDOW_MS / 2,
            now + 1000
        )

        try {
            val allTxs = transactionDao.getAll()
            val recentTxs = allTxs.filter { tx ->
                tx.type == type
                    && tx.occurredAt >= windowStart
                    && tx.occurredAt <= windowEnd
                    && tx.amount >= autoAmountFen - 100
                    && tx.amount <= autoAmountFen + 100
            }

            val conflict = recentTxs.find { tx ->
                tx.amount == autoAmountFen
                    && tx.type == type
                    && isCompatibleCategory(tx.categoryId, autoCategoryId)
                    && isTimeClose(tx.occurredAt, autoOccurredAt)
            }

            if (conflict != null) {
                // ▶ 构建冲突详情对象，供 UI 层显示
                return ConflictAlert(
                    existingTxId = conflict.id,
                    existingAmount = conflict.amount,
                    existingNote = conflict.note ?: "",
                    existingCategory = conflict.categoryId,
                    existingOccurredAt = conflict.occurredAt,
                    autoAmount = autoAmountFen,
                    autoNote = autoNote,
                    autoCategory = autoCategoryId,
                    source = autoSource,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "冲突检测异常: ${e.message}", e)
        }
        return null
    }

    /**
     * 判断两个分类是否兼容。
     *
     * 场景：自动推断为 "shopping"，手动为 "consume" → 应视为兼容
     * （假设 shopping 和 consume 有别名映射）
     */
    private fun isCompatibleCategory(recorded: String, auto: String): Boolean {
        if (recorded == auto) return true

        // 定义分类别名组（同组内视为兼容）
        val aliases = mapOf(
            "shopping" to setOf("shopping", "consume", "shopping"),
            "dining" to setOf("dining", "consume", "food"),
            "transportation" to setOf("transportation", "travel"),
            "income_others" to setOf("income_others", "refund", "investment"),
        )

        val group1 = aliases[recorded] ?: setOf(recorded)
        val group2 = aliases[auto] ?: setOf(auto)
        return group1.intersect(group2).isNotEmpty()
    }
}
