package com.example.bookkeeping.notification

import com.example.bookkeeping.notification.model.PaymentSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SmsPaymentParser 纯 JVM 单元测试。
 *
 * 覆盖：
 * - 可信发件方白名单判断
 * - 各主流银行短信格式
 * - 非支付短信过滤
 * - 商户名提取
 */
class SmsPaymentParserTest {

    // ── 发件方可信判断 ────────────────────────────────────────────────────

    @Test
    fun `发件方可信_工商银行`() {
        assertTrue(SmsPaymentParser.isTrustedSender("工商银行"))
    }

    @Test
    fun `发件方可信_95533`() {
        assertTrue(SmsPaymentParser.isTrustedSender("95533"))
    }

    @Test
    fun `发件方可信_支付宝`() {
        assertTrue(SmsPaymentParser.isTrustedSender("支付宝"))
    }

    @Test
    fun `发件方不可信_陌生号码`() {
        assertFalse(SmsPaymentParser.isTrustedSender("+8613212345678"))
    }

    @Test
    fun `发件方不可信_普通联系人`() {
        assertFalse(SmsPaymentParser.isTrustedSender("张三"))
    }

    // ── 支付短信判断 ──────────────────────────────────────────────────────

    @Test
    fun `支付短信判断_含消费关键词_返回true`() {
        assertTrue(SmsPaymentParser.isPaymentSms("您账户消费¥28.50"))
    }

    @Test
    fun `支付短信判断_含扣款关键词_返回true`() {
        assertTrue(SmsPaymentParser.isPaymentSms("账户扣款人民币100.00元"))
    }

    @Test
    fun `支付短信判断_验证码短信_返回false`() {
        assertFalse(SmsPaymentParser.isPaymentSms("您的验证码为123456，5分钟内有效"))
    }

    @Test
    fun `支付短信判断_无金额_返回false`() {
        assertFalse(SmsPaymentParser.isPaymentSms("您的账户发生了消费行为"))
    }

    // ── 各银行格式解析 ────────────────────────────────────────────────────

    @Test
    fun `工商银行_消费格式`() {
        val body = "【工商银行】您账户尾号1234消费人民币88.00元，商户：麦当劳，余额1234.56元"
        val payment = SmsPaymentParser.parse(sender = "工商银行", body = body, receivedAt = 1000L)
        assertNotNull(payment)
        assertEquals(PaymentSource.BANK_SMS, payment!!.source)
        assertEquals(8800L, payment.amountFen)
        assertEquals("麦当劳", payment.merchantName)
        assertEquals(1000L, payment.occurredAt)
    }

    @Test
    fun `建设银行_消费格式`() {
        val body = "【建行】您尾号6789的卡消费￥28.50，商户：星巴克"
        val payment = SmsPaymentParser.parse(sender = "建设银行", body = body)
        assertNotNull(payment)
        assertEquals(2850L, payment!!.amountFen)
        assertEquals("星巴克", payment.merchantName)
    }

    @Test
    fun `招商银行_消费格式`() {
        val body = "招商银行信用卡消费¥100.00，商户名：京东"
        val payment = SmsPaymentParser.parse(sender = "招商银行", body = body)
        assertNotNull(payment)
        assertEquals(10000L, payment!!.amountFen)
        assertEquals("京东", payment.merchantName)
    }

    @Test
    fun `95533号码发送_工商银行`() {
        val body = "您尾号XXXX账户消费¥50.00，商户名称：便利蜂"
        val payment = SmsPaymentParser.parse(sender = "95533", body = body)
        assertNotNull(payment)
        assertEquals(5000L, payment!!.amountFen)
        assertEquals("便利蜂", payment.merchantName)
    }

    // ── 不可信发件方过滤 ──────────────────────────────────────────────────

    @Test
    fun `不可信发件方_即使内容匹配也返回null`() {
        val body = "您的账户消费¥100.00，商户：某某商家"
        val payment = SmsPaymentParser.parse(sender = "13800138000", body = body)
        assertNull("陌生号码短信不应被导入", payment)
    }

    // ── 无商户名处理 ──────────────────────────────────────────────────────

    @Test
    fun `无商户名称_merchantName为null`() {
        val body = "您的账户消费¥200.00，请注意账户安全"
        val payment = SmsPaymentParser.parse(sender = "工商银行", body = body)
        // 有支付信息但无商户名
        assertNotNull(payment)
        assertNull(payment!!.merchantName)
    }
}
