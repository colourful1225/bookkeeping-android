package com.example.bookkeeping.notification

import com.example.bookkeeping.notification.model.ParsedPayment
import com.example.bookkeeping.notification.model.PaymentSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 多源支付记录对账去重引擎。
 *
 * ## 问题背景
 * 同一笔支付可能被多个渠道捕获：
 * - 通知监听（快速但文字不完整）
 * - 无障碍服务（信息完整但可能比通知晚几秒）
 *
 * ## 核心策略：时间窗口 + 金额匹配
 *
 * 两条记录被判定为"同一笔支付"的条件（同时满足）：
 * 1. **来源应用相同**：同属微信或同属支付宝
 * 2. **金额完全一致**
 * 3. **时间差 ≤ [SAME_PAYMENT_WINDOW_MS]**（默认 120 秒）
 *
 * 符合条件时保留 [ParsedPayment.confidence] 更高者（优先用无障碍信息），
 * 并将两者的 merchantName 合并（谁有就取谁的）。
 *
 * ## 线程安全
 * 内部使用 [pendingBuffer] 存储等待对账的记录，所有操作通过 [bufferLock] 同步。
 */
@Singleton
class ReconciliationEngine @Inject constructor() {

    companion object {
        /**
         * 同一笔支付的时间窗口（毫秒）。
         *
         * 无障碍事件通常比通知延迟 5～30 秒。
         * 设置 120 秒（2分钟）确保即使页面加载慢也能正确配对。
         */
        const val SAME_PAYMENT_WINDOW_MS = 120_000L
    }

    /** 等待对账的缓冲池，最多保留 [MAX_BUFFER_SIZE] 条 */
    private val MAX_BUFFER_SIZE = 100
    private val pendingBuffer = ArrayDeque<ParsedPayment>()
    private val bufferLock = Any()

    /**
     * 提交一条新支付记录进行对账处理。
     *
     * @return 经对账后应真正写入数据库的记录，若判定为重复则返回 null
     */
    fun reconcile(incoming: ParsedPayment): ParsedPayment? = synchronized(bufferLock) {
        evictExpired(incoming.occurredAt)

        val match = findMatch(incoming)
        return if (match != null) {
            // 找到匹配记录：合并后替换缓冲区中旧记录，返回 null 表示"本次不直接写库"
            val merged = merge(match, incoming)
            val idx = pendingBuffer.indexOf(match)
            if (idx >= 0) pendingBuffer[idx] = merged
            // 如果新来的 confidence 更高（无障碍），需通知调用方用合并结果更新已写入的记录
            if (incoming.confidence > match.confidence) merged else null
        } else {
            // 无重复：加入缓冲池，返回直接写库
            if (pendingBuffer.size >= MAX_BUFFER_SIZE) pendingBuffer.removeFirst()
            pendingBuffer.addLast(incoming)
            incoming
        }
    }

    /**
     * 判断两条记录是否为同一笔支付。
     *
     * 跨源匹配规则：
     * - WECHAT      ↔ WECHAT_ACCESSIBILITY
     * - ALIPAY      ↔ ALIPAY_ACCESSIBILITY
     * - BANK_SMS 不参与无障碍对账（无对应来源）
     */
    fun isSamePayment(a: ParsedPayment, b: ParsedPayment): Boolean {
        if (!sameApp(a.source, b.source)) return false
        if (a.amountFen != b.amountFen) return false
        val timeDiff = kotlin.math.abs(a.occurredAt - b.occurredAt)
        return timeDiff <= SAME_PAYMENT_WINDOW_MS
    }

    /**
     * 合并两条配对记录：保留 confidence 高者为主，取双方中不为空的 merchantName。
     */
    fun merge(base: ParsedPayment, other: ParsedPayment): ParsedPayment {
        val primary   = if (other.confidence >= base.confidence) other else base
        val secondary = if (other.confidence >= base.confidence) base else other
        return primary.copy(
            merchantName = primary.merchantName ?: secondary.merchantName,
            confidence   = primary.confidence,
        )
    }

    // ── 私有工具 ──────────────────────────────────────────────────────────

    private fun findMatch(incoming: ParsedPayment): ParsedPayment? =
        pendingBuffer.firstOrNull { isSamePayment(it, incoming) }

    /** 清理时间窗口外的过期记录 */
    private fun evictExpired(now: Long) {
        val threshold = now - SAME_PAYMENT_WINDOW_MS
        while (pendingBuffer.isNotEmpty() && pendingBuffer.first().occurredAt < threshold) {
            pendingBuffer.removeFirst()
        }
    }

    /**
     * 判断两个来源是否属于同一应用。
     *
     * 微信 + 微信无障碍 → 同应用  
     * 支付宝 + 支付宝无障碍 → 同应用
     */
    internal fun sameApp(a: PaymentSource, b: PaymentSource): Boolean {
        val wechatGroup = setOf(PaymentSource.WECHAT, PaymentSource.WECHAT_ACCESSIBILITY)
        val alipayGroup = setOf(PaymentSource.ALIPAY, PaymentSource.ALIPAY_ACCESSIBILITY)
        return (a in wechatGroup && b in wechatGroup)
            || (a in alipayGroup && b in alipayGroup)
    }
}
