package com.example.bookkeeping.notification

import com.example.bookkeeping.notification.model.ParsedPayment
import com.example.bookkeeping.notification.model.PaymentSource

/**
 * 银行扣款短信解析器。
 *
 * ## 支持格式（持续扩充）
 *
 * | 银行       | 样本                                                          |
 * |------------|---------------------------------------------------------------|
 * | 工商银行   | 【工商银行】您账户尾号XXXX消费人民币88.00元，商户：超市      |
 * | 建设银行   | 【建行】尾号XXXX，消费￥28.50，可用余额...                  |
 * | 招商银行   | 招商银行信用卡消费¥100.00，商户名：京东                     |
 * | 农业银行   | 农行账户尾号XXXX于...消费人民币200.00元，商户：Costco        |
 * | 中国银行   | 【中国银行】您尾号XXXX账户...消费¥300.00 商户:沃尔玛       |
 * | 支付宝通知 | 支付宝告知: 尾号XXXX...消费¥50，商户: 饿了么               |
 * | 通用兜底   | 包含 ¥/¥/人民币 且含"消费"/"扣款"/"付款" 的任意短信        |
 */
object SmsPaymentParser {

    /** 短信发件方（号码或名称）白名单用于预过滤，避免误解析私信 */
    private val TRUSTED_SENDERS = setOf(
        "工商银行", "icbc", "建设银行", "ccb",
        "招商银行", "cmbchina", "农业银行", "abc",
        "中国银行", "boc", "交通银行", "bocom",
        "邮储银行", "民生银行", "平安银行",
        "兴业银行", "光大银行", "浦发银行",
        "95533", "95588", "95559", "95555",
        "95566", "95568", "95561", "95595",
        "支付宝", "alipay",
    )

    /**
     * 解析短信消息。
     *
     * @param sender    短信发件方（originatingAddress）
     * @param body      短信正文
     * @param receivedAt 接收时间（毫秒）
     * @return 解析成功返回 [ParsedPayment]，否则 null
     */
    fun parse(sender: String, body: String, receivedAt: Long = System.currentTimeMillis()): ParsedPayment? {
        if (!isTrustedSender(sender)) return null
        if (!isPaymentSms(body)) return null

        val amount = PaymentNotificationParser.extractAmount(body) ?: return null
        val merchant = extractMerchant(body)

        return ParsedPayment(
            source       = PaymentSource.BANK_SMS,
            amountFen    = amount,
            merchantName = merchant,
            rawText      = body,
            occurredAt   = receivedAt,
        )
    }

    // ── 工具函数 ──────────────────────────────────────────────────────────

    /** 判断发件方是否可信（银行 or 支付平台） */
    internal fun isTrustedSender(sender: String): Boolean {
        val lower = sender.lowercase()
        return TRUSTED_SENDERS.any { lower.contains(it.lowercase()) }
    }

    /**
     * 判断短信是否为支付/消费类短信。
     * 需要同时满足：有金额 + 有支付关键词。
     */
    private val PAYMENT_KEYWORDS = listOf("消费", "扣款", "付款", "支付", "刷卡", "转出")

    internal fun isPaymentSms(body: String): Boolean {
        val hasAmount = PaymentNotificationParser.extractAmount(body) != null
        val hasKeyword = PAYMENT_KEYWORDS.any { body.contains(it) }
        return hasAmount && hasKeyword
    }

    /**
     * 从短信正文中提取商户名称。
     * 尝试多种格式：
     * - "商户：xxx"、"商户名称：xxx"
     * - "商户:xxx"、"商户名:xxx"
     */
    private val MERCHANT_PATTERNS = listOf(
        Regex("""商户名称[：:]\s*([^\s，,。\n]{1,20})"""),
        Regex("""商户[：:]\s*([^\s，,。\n]{1,20})"""),
        Regex("""商户名[：:]\s*([^\s，,。\n]{1,20})"""),
        Regex("""收款方[：:]\s*([^\s，,。\n]{1,20})"""),
    )

    internal fun extractMerchant(body: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            pattern.find(body)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let {
                return it
            }
        }
        return null
    }
}
