package com.example.bookkeeping.notification.accessibility

import com.example.bookkeeping.notification.PaymentNotificationParser
import com.example.bookkeeping.notification.model.ParsedPayment
import com.example.bookkeeping.notification.model.PaymentSource

/**
 * 支付宝支付页面无障碍节点解析器。
 *
 * ## 支持的页面类型
 * - **付款成功页**："付款成功" / "支付成功" / "转账成功"
 * - **付款码支付确认页**（仅读取已完成状态）
 *
 * ## 典型节点文本
 * ```
 * 付款成功
 * ¥10.00
 * 付款给
 * 肯德基
 * 2024-01-01 12:00:00
 * 交易号：xxxxxxx
 * ```
 */
object AlipayAccessibilityParser {

    private val SUCCESS_KEYWORDS = listOf("付款成功", "支付成功", "转账成功", "收款成功")

    /**
     * 从支付宝页面节点文本列表中解析支付信息。
     *
     * @param nodeTexts 当前页面所有节点文本列表
     * @param timestamp 页面触发时间（毫秒）
     */
    fun parse(
        nodeTexts: List<String>,
        timestamp: Long = System.currentTimeMillis(),
    ): ParsedPayment? {
        if (SUCCESS_KEYWORDS.none { kw -> nodeTexts.any { it.contains(kw) } }) return null

        val amount = WeChatAccessibilityParser.extractLargestAmount(nodeTexts) ?: return null
        val merchant = extractMerchant(nodeTexts)
        val confidence = calcConfidence(amount, merchant, nodeTexts)

        return ParsedPayment(
            source       = PaymentSource.ALIPAY_ACCESSIBILITY,
            amountFen    = amount,
            merchantName = merchant,
            rawText      = nodeTexts.take(15).joinToString("|"),
            occurredAt   = timestamp,
            confidence   = confidence,
        )
    }

    /**
     * 从支付宝页面提取商户名。
     *
     * 支付宝布局特点：
     * - "付款给" 标签后紧跟商户名（可能同行也可能下一行）
     * - 商户名通常在账单金额下方
     */
    internal fun extractMerchant(texts: List<String>): String? {
        for (i in texts.indices) {
            val t = texts[i]
            listOf("付款给", "转账给", "收款方", "商家名称", "商户名").forEach { kw ->
                if (t.contains(kw)) {
                    val inline = t.substringAfter(kw).trim()
                        .split(Regex("""[\s，,。\n]""")).firstOrNull { it.isNotBlank() }
                    if (!inline.isNullOrBlank() && isValidMerchantName(inline)) {
                        return inline.take(20)
                    }
                    if (i + 1 <= texts.lastIndex) {
                        val next = texts[i + 1].trim()
                        if (next.isNotBlank() && isValidMerchantName(next)) return next.take(20)
                    }
                }
            }
        }

        // 退路：成功关键词下方的第一个有效候选
        val successIdx = texts.indexOfFirst { t -> SUCCESS_KEYWORDS.any { t.contains(it) } }
        if (successIdx >= 0) {
            for (i in (successIdx + 1)..minOf(successIdx + 5, texts.lastIndex)) {
                val candidate = texts[i].trim()
                if (candidate.isNotBlank()
                    && PaymentNotificationParser.extractAmount(candidate) == null
                    && isValidMerchantName(candidate)
                ) return candidate.take(20)
            }
        }
        return null
    }

    private val SYSTEM_TOKENS = setOf(
        "付款成功", "支付成功", "转账成功", "收款成功",
        "确认付款", "确定", "完成", "返回首页",
        "交易号", "流水号", "备注", "时间", "手续费",
        "金额", "账单详情", "查看详情", "联系客服"
    )

    private fun isValidMerchantName(s: String): Boolean {
        if (s.length < 2 || s.length > 20) return false
        if (SYSTEM_TOKENS.any { s.contains(it) }) return false
        if (s.matches(Regex("""[\d\s\-:/.]+"""))) return false
        return true
    }

    private fun calcConfidence(amount: Long, merchant: String?, texts: List<String>): Int {
        var score = 50
        if (merchant != null) score += 20
        if (texts.size >= 5)  score += 15
        if (texts.any { it.contains("交易号") || it.contains("流水") }) score += 15
        return score.coerceIn(0, 100)
    }
}
