package com.example.bookkeeping.notification.model

/**
 * 从通知、短信或无障碍服务中解析出的支付信息。
 *
 * @param source         来源类型
 * @param amountFen      金额（分），Long 避免浮点精度问题
 * @param merchantName   商户/收款方名称（可空）
 * @param rawText        原始文本，用于调试与跨源去重
 * @param occurredAt     发生时间（毫秒时间戳）
 * @param confidence     信息完整度评分 0-100，分越高越完整，用于多源合并时择优
 */
data class ParsedPayment(
    val source: PaymentSource,
    val amountFen: Long,
    val merchantName: String?,
    val rawText: String,
    val occurredAt: Long,
    val confidence: Int = 50,
)

/**
 * 支付来源类型。
 *
 * - [WECHAT]                微信支付通知（com.tencent.mm）
 * - [ALIPAY]                支付宝通知（com.eg.android.AlipayGphone）
 * - [BANK_SMS]              银行扣款短信
 * - [WECHAT_ACCESSIBILITY]  微信支付成功页无障碍扫描（信息更完整）
 * - [ALIPAY_ACCESSIBILITY]  支付宝支付成功页无障碍扫描（信息更完整）
 */
enum class PaymentSource {
    WECHAT,
    ALIPAY,
    BANK_SMS,
    WECHAT_ACCESSIBILITY,
    ALIPAY_ACCESSIBILITY,
}
