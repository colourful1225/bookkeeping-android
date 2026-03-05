package com.example.bookkeeping.notification

import com.example.bookkeeping.notification.model.ParsedPayment
import com.example.bookkeeping.notification.model.PaymentSource
import java.math.BigDecimal

/**
 * 微信支付 & 支付宝通知内容解析器。
 *
 * ## 支持的通知格式
 *
 * ### 微信支付（com.tencent.mm）
 * - "付款给 超市 ¥28.50"
 * - "微信支付成功，¥28.50"
 * - "[微信支付] 你已付款¥5.00给xxx"
 * - 消费通知: "你已成功付款¥5.00"
 *
 * ### 支付宝（com.eg.android.AlipayGphone）
 * - "你已成功支付¥10.00给xxx"
 * - "成功付款¥15.50"
 * - "账单 付款金额¥88.00 付款账号..."
 * - "支出:¥100.00"
 */
object PaymentNotificationParser {

    /** 微信支付包名 */
    const val WECHAT_PACKAGE = "com.tencent.mm"

    /** 支付宝包名 */
    const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"

    /**
     * 尝试从通知中解析支付信息。
     *
     * @param packageName 通知来源应用包名
     * @param title       通知标题
     * @param text        通知正文（短）
     * @param bigText     通知展开文本（长，优先使用）
     * @param postedAt    通知时间戳（毫秒）
     * @return 解析成功返回 [ParsedPayment]，否则返回 null
     */
    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        bigText: String?,
        postedAt: Long = System.currentTimeMillis(),
    ): ParsedPayment? {
        val candidates = buildTextCandidates(title, text, bigText)
        if (candidates.isEmpty()) return null
        return when (packageName) {
            WECHAT_PACKAGE  -> parseWechat(candidates, title, postedAt)
            ALIPAY_PACKAGE  -> parseAlipay(candidates, title, postedAt)
            else            -> null
        }
    }

    // ── 微信 ──────────────────────────────────────────────────────────────

    /**
     * 微信支付通知关键词白名单（任一命中才尝试解析）。
     * 过滤普通微信消息。
     */
    private val WECHAT_PAY_KEYWORDS = listOf(
        "付款", "支付", "收款", "微信支付", "交易提醒", "账单", "扣款", "成功"
    )

    private fun parseWechat(candidates: List<String>, title: String?, postedAt: Long): ParsedPayment? {
        // 标题或正文须包含付款相关关键词，避免误抓普通聊天消息
        val combined = (listOfNotNull(title) + candidates).joinToString(" ")
        if (WECHAT_PAY_KEYWORDS.none { combined.contains(it) }) return null

        val amount = candidates.firstNotNullOfOrNull { extractAmount(it) } ?: return null
        val merchant = candidates.firstNotNullOfOrNull {
            extractMerchantAfterKeyword(it, listOf("付款给", "给", "向", "付款到", "收款方", "商户"))
        }

        return ParsedPayment(
            source       = PaymentSource.WECHAT,
            amountFen    = amount,
            merchantName = merchant,
            rawText      = candidates.joinToString(" | "),
            occurredAt   = postedAt,
        )
    }

    // ── 支付宝 ────────────────────────────────────────────────────────────

    private val ALIPAY_PAY_KEYWORDS = listOf(
        "支付", "付款", "支出", "成功付款", "已支付", "交易提醒", "账单", "扣款", "消费"
    )

    private fun parseAlipay(candidates: List<String>, title: String?, postedAt: Long): ParsedPayment? {
        val combined = (listOfNotNull(title) + candidates).joinToString(" ")
        if (ALIPAY_PAY_KEYWORDS.none { combined.contains(it) }) return null

        val amount = candidates.firstNotNullOfOrNull { extractAmount(it) } ?: return null
        val merchant = candidates.firstNotNullOfOrNull {
            extractMerchantAfterKeyword(it, listOf("给", "付款给", "支付给", "支付成功 ", "收款方", "商户"))
        }

        return ParsedPayment(
            source       = PaymentSource.ALIPAY,
            amountFen    = amount,
            merchantName = merchant,
            rawText      = candidates.joinToString(" | "),
            occurredAt   = postedAt,
        )
    }

    // ── 公共工具 ──────────────────────────────────────────────────────────

    /**
     * 从文本中提取金额（人民币）。
     *
     * 匹配：`¥28.50`、`￥28.5`、`人民币28.50元`、`28.50元`（需在支付关键词附近）
     *
     * @return 金额（分），失败返回 null
     */
    internal fun extractAmount(text: String): Long? {
        // 优先匹配 ¥/￥ 后跟数字
        val yenPattern = Regex("""[¥￥]\s*([\d,]{1,12}(?:\.\d{1,2})?)""")
        yenPattern.find(text)?.let { match ->
            return amountStringToFen(match.groupValues[1])
        }

        // 匹配 "人民币 12.50 元" 格式
        val rmbPattern = Regex("""人民币\s*([\d,]{1,12}(?:\.\d{1,2})?)\s*元""")
        rmbPattern.find(text)?.let { match ->
            return amountStringToFen(match.groupValues[1])
        }

        // 匹配 "10.00元"；仅在支付语境下启用，避免误识别普通文本
        if (PAY_CONTEXT_KEYWORDS.any { text.contains(it) }) {
            val yuanPattern = Regex("""(^|[^\d])([\d,]{1,12}(?:\.\d{1,2})?)\s*元""")
            yuanPattern.find(text)?.let { match ->
                return amountStringToFen(match.groupValues[2])
            }
        }

        return null
    }

    /**
     * 尝试在特定关键词之后提取商户名（取随后非空白的第一个词段）。
     *
     * 示例：
     * - "付款给 超市" → "超市"
     * - "给 星巴克咖啡 备注" → "星巴克咖啡"
     */
    internal fun extractMerchantAfterKeyword(text: String, keywords: List<String>): String? {
        for (kw in keywords) {
            val idx = text.indexOf(kw)
            if (idx < 0) continue
            val after = text.substring(idx + kw.length).trimStart()
            if (after.isBlank()) continue
            // 取到下一个空白/标点为止，最长 20 字符
            val merchant = after.split(Regex("""[\s，,。.！!？?¥￥\n]""")).firstOrNull { it.isNotBlank() }
            if (!merchant.isNullOrBlank()) return merchant.take(20)
        }
        return null
    }

    /** "28.50" → 2850L（分） */
    internal fun amountStringToFen(amountStr: String): Long? {
        return try {
            BigDecimal(amountStr.replace(",", "")).multiply(BigDecimal(100)).toLong()
        } catch (_: NumberFormatException) {
            null
        }
    }

    private val PAY_CONTEXT_KEYWORDS = listOf("支付", "付款", "支出", "消费", "收款", "扣款", "账单", "交易")

    private fun buildTextCandidates(title: String?, text: String?, bigText: String?): List<String> {
        val result = linkedSetOf<String>()
        title?.trim()?.takeIf { it.isNotEmpty() }?.let { result.add(it) }
        text?.trim()?.takeIf { it.isNotEmpty() }?.let { result.add(it) }
        bigText?.trim()?.takeIf { it.isNotEmpty() }?.let { result.add(it) }
        if (!title.isNullOrBlank() && !text.isNullOrBlank()) {
            result.add("$title $text")
        }
        if (!title.isNullOrBlank() && !bigText.isNullOrBlank()) {
            result.add("$title $bigText")
        }
        return result.toList()
    }
}
