package com.example.bookkeeping.notification.accessibility

import com.example.bookkeeping.notification.PaymentNotificationParser
import com.example.bookkeeping.notification.model.ParsedPayment
import com.example.bookkeeping.notification.model.PaymentSource

/**
 * 微信支付页面无障碍节点解析器。
 *
 * ## 工作原理
 * [PaymentAccessibilityService] 在检测到微信支付相关页面时，
 * 收集当前窗口所有 `AccessibilityNodeInfo` 的文本内容，
 * 传入本解析器进行提取。
 *
 * ## 支持的页面类型
 * - **支付成功页**："支付成功" / "付款成功" 关键词
 * - **转账成功页**："转账成功" 关键词
 *
 * ## 典型节点文本（随版本变化，使用灵活匹配）
 * ```
 * 支付成功
 * ¥28.50
 * 超市
 * 2024-01-01 12:00
 * 单号：xxxxxxxxxx
 * ```
 */
object WeChatAccessibilityParser {

    /** 页面包含这些关键词才视为支付成功页 */
    private val SUCCESS_KEYWORDS = listOf("支付成功", "付款成功", "转账成功")

    /**
     * 尝试从微信页面节点文本列表中解析支付信息。
     *
     * @param nodeTexts 当前页面所有节点文本（从上到下顺序收集）
     * @param packageName 应该为 [PaymentNotificationParser.WECHAT_PACKAGE]
     * @param timestamp 页面触发时间（毫秒）
     * @return 解析成功返回 [ParsedPayment]，否则 null
     */
    fun parse(
        nodeTexts: List<String>,
        packageName: String,
        timestamp: Long = System.currentTimeMillis(),
    ): ParsedPayment? {
        if (SUCCESS_KEYWORDS.none { kw -> nodeTexts.any { it.contains(kw) } }) return null

        val amount = extractLargestAmount(nodeTexts) ?: return null
        val merchant = extractMerchant(nodeTexts)
        val confidence = calcConfidence(amount, merchant, nodeTexts)

        return ParsedPayment(
            source       = PaymentSource.WECHAT_ACCESSIBILITY,
            amountFen    = amount,
            merchantName = merchant,
            rawText      = nodeTexts.take(15).joinToString("|"),
            occurredAt   = timestamp,
            confidence   = confidence,
        )
    }

    /**
     * 在所有节点文本中提取金额，优先取最大值（通常支付金额展示最显眼）。
     *
     * 过滤掉"余额"、"手续费"等附属文字旁的小金额。
     */
    internal fun extractLargestAmount(texts: List<String>): Long? {
        // 过滤掉这些标签附近的金额，避免把余额/手续费当作支付金额
        val excludeContexts = setOf("余额", "手续费", "优惠", "立减", "抵扣", "红包")

        return texts.mapIndexedNotNull { idx, text ->
            val amt = PaymentNotificationParser.extractAmount(text) ?: return@mapIndexedNotNull null
            // 检查前后节点是否包含排除关键词
            val contextBefore = if (idx > 0) texts[idx - 1] else ""
            val contextAfter  = if (idx < texts.lastIndex) texts[idx + 1] else ""
            val context = contextBefore + text + contextAfter
            if (excludeContexts.any { context.contains(it) }) null else amt
        }.maxOrNull()
    }

    /**
     * 从页面节点文本中推断商户/收款方名称。
     *
     * 策略（按优先级）：
     * 1. "付款给 <name>" / "转账给 <name>" 后跟随的文本
     * 2. 成功关键词相邻节点（通常是商户名）
     * 3. 避免把"备注"、"单号"等系统字段误识别为商户名
     */
    internal fun extractMerchant(texts: List<String>): String? {
        // 策略1：在"付款给"/"转账给"关键词后取推断
        for (i in texts.indices) {
            val t = texts[i]
            listOf("付款给", "转账给", "收款方", "商家").forEach { kw ->
                if (t.contains(kw)) {
                    val candidate = t.substringAfter(kw).trim()
                        .split(Regex("""[\s，,。\n]""")).firstOrNull { it.isNotBlank() }
                    if (!candidate.isNullOrBlank() && isValidMerchantName(candidate)) {
                        return candidate.take(20)
                    }
                    // 如果关键词独占一行，取下一行
                    if (i + 1 <= texts.lastIndex) {
                        val next = texts[i + 1].trim()
                        if (next.isNotBlank() && isValidMerchantName(next)) return next.take(20)
                    }
                }
            }
        }

        // 策略2：成功关键词下方第一个非金额、非系统字段的文本
        val successIdx = texts.indexOfFirst { t -> SUCCESS_KEYWORDS.any { t.contains(it) } }
        if (successIdx >= 0) {
            for (i in (successIdx + 1)..minOf(successIdx + 4, texts.lastIndex)) {
                val candidate = texts[i].trim()
                if (candidate.isNotBlank()
                    && PaymentNotificationParser.extractAmount(candidate) == null
                    && isValidMerchantName(candidate)
                ) return candidate.take(20)
            }
        }
        return null
    }

    /** 排除已知系统字段，确保候选文本是商户名 */
    private val SYSTEM_TOKENS = setOf(
        "支付成功", "付款成功", "转账成功", "确定", "完成", "返回",
        "单号", "备注", "时间", "手续费", "金额", "账单", "详情", "更多"
    )

    private fun isValidMerchantName(s: String): Boolean {
        if (s.length < 2 || s.length > 20) return false
        if (SYSTEM_TOKENS.any { s.contains(it) }) return false
        // 纯数字/纯字母不是商户名
        if (s.matches(Regex("""[\d\s\-:/.]+"""))) return false
        return true
    }

    private fun calcConfidence(amount: Long, merchant: String?, texts: List<String>): Int {
        var score = 50
        if (merchant != null) score += 20
        if (texts.size >= 5)  score += 15   // 页面信息充足
        if (texts.any { it.contains("单号") || it.contains("交易") }) score += 15
        return score.coerceIn(0, 100)
    }
}
