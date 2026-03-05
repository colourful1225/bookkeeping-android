package com.example.bookkeeping.notification

import com.example.bookkeeping.notification.model.PaymentSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * PaymentNotificationParser 纯 JVM 单元测试。
 *
 * 覆盖：
 * - 微信/支付宝各常见通知格式
 * - 非支付通知的过滤（聊天消息、其他 App）
 * - 金额解析边界值
 * - 商户名提取
 */
class PaymentNotificationParserTest {

    // ── 金额解析 ──────────────────────────────────────────────────────────

    @Test
    fun `金额解析_人民币符号_整数`() {
        assertEquals(2000L, PaymentNotificationParser.extractAmount("¥20"))
    }

    @Test
    fun `金额解析_人民币符号_小数`() {
        assertEquals(2850L, PaymentNotificationParser.extractAmount("付款给超市 ¥28.50"))
    }

    @Test
    fun `金额解析_全角符号`() {
        assertEquals(1500L, PaymentNotificationParser.extractAmount("￥15.00"))
    }

    @Test
    fun `金额解析_人民币文字格式`() {
        assertEquals(8800L, PaymentNotificationParser.extractAmount("消费人民币88.00元"))
    }

    @Test
    fun `金额解析_支付语境下纯元格式`() {
        assertEquals(1288L, PaymentNotificationParser.extractAmount("支付成功 金额12.88元"))
    }

    @Test
    fun `金额解析_无金额信息返回null`() {
        assertNull(PaymentNotificationParser.extractAmount("这是一条普通聊天消息"))
    }

    @Test
    fun `金额解析_金额为零点零一`() {
        assertEquals(1L, PaymentNotificationParser.extractAmount("¥0.01"))
    }

    // ── 商户名提取 ────────────────────────────────────────────────────────

    @Test
    fun `商户名提取_付款给关键词`() {
        val merchant = PaymentNotificationParser.extractMerchantAfterKeyword(
            "付款给 超市 备注",
            listOf("付款给"),
        )
        assertEquals("超市", merchant)
    }

    @Test
    fun `商户名提取_给关键词`() {
        val merchant = PaymentNotificationParser.extractMerchantAfterKeyword(
            "你已成功付款¥5.00给星巴克咖啡 付款时间",
            listOf("给"),
        )
        assertEquals("星巴克咖啡", merchant)
    }

    @Test
    fun `商户名提取_无关键词返回null`() {
        val merchant = PaymentNotificationParser.extractMerchantAfterKeyword(
            "支付成功¥10.00",
            listOf("付款给"),
        )
        assertNull(merchant)
    }

    // ── 微信支付通知解析 ──────────────────────────────────────────────────

    @Test
    fun `微信_付款给商户_解析成功`() {
        val payment = PaymentNotificationParser.parse(
            packageName = PaymentNotificationParser.WECHAT_PACKAGE,
            title       = "微信支付",
            text        = "付款给 超市 ¥28.50",
            bigText     = null,
        )
        assertNotNull(payment)
        assertEquals(PaymentSource.WECHAT, payment!!.source)
        assertEquals(2850L, payment.amountFen)
        assertEquals("超市", payment.merchantName)
    }

    @Test
    fun `微信_成功付款_解析金额`() {
        val payment = PaymentNotificationParser.parse(
            packageName = PaymentNotificationParser.WECHAT_PACKAGE,
            title       = "微信支付",
            text        = "你已成功付款¥5.00",
            bigText     = null,
        )
        assertNotNull(payment)
        assertEquals(500L, payment!!.amountFen)
    }

    @Test
    fun `微信_普通聊天消息_应被过滤`() {
        val payment = PaymentNotificationParser.parse(
            packageName = PaymentNotificationParser.WECHAT_PACKAGE,
            title       = "张三",
            text        = "今晚一起吃饭吗？",
            bigText     = null,
        )
        assertNull("普通聊天消息不应被解析为支付", payment)
    }

    @Test
    fun `微信_bigText优先于text`() {
        val payment = PaymentNotificationParser.parse(
            packageName = PaymentNotificationParser.WECHAT_PACKAGE,
            title       = "微信支付",
            text        = "收到新消息",
            bigText     = "付款给 星巴克 ¥38.00 备注：咖啡",
        )
        assertNotNull(payment)
        assertEquals(3800L, payment!!.amountFen)
        assertEquals("星巴克", payment.merchantName)
    }

    @Test
    fun `微信_标题与正文组合可解析`() {
        val payment = PaymentNotificationParser.parse(
            packageName = PaymentNotificationParser.WECHAT_PACKAGE,
            title       = "微信支付",
            text        = "交易提醒 金额12.50元",
            bigText     = null,
        )
        assertNotNull(payment)
        assertEquals(1250L, payment!!.amountFen)
    }

    // ── 支付宝通知解析 ────────────────────────────────────────────────────

    @Test
    fun `支付宝_成功支付_基本格式`() {
        val payment = PaymentNotificationParser.parse(
            packageName = PaymentNotificationParser.ALIPAY_PACKAGE,
            title       = "支付宝",
            text        = "你已成功支付¥10.00给肯德基",
            bigText     = null,
        )
        assertNotNull(payment)
        assertEquals(PaymentSource.ALIPAY, payment!!.source)
        assertEquals(1000L, payment.amountFen)
        assertEquals("肯德基", payment.merchantName)
    }

    @Test
    fun `支付宝_支出格式`() {
        val payment = PaymentNotificationParser.parse(
            packageName = PaymentNotificationParser.ALIPAY_PACKAGE,
            title       = "支付宝",
            text        = "支出:¥100.00",
            bigText     = null,
        )
        assertNotNull(payment)
        assertEquals(10000L, payment!!.amountFen)
    }

    @Test
    fun `支付宝_账单金额元格式`() {
        val payment = PaymentNotificationParser.parse(
            packageName = PaymentNotificationParser.ALIPAY_PACKAGE,
            title       = "支付宝",
            text        = "账单提醒 支付成功 金额10.00元",
            bigText     = null,
        )
        assertNotNull(payment)
        assertEquals(1000L, payment!!.amountFen)
    }

    @Test
    fun `支付宝_非支付通知_过滤`() {
        val payment = PaymentNotificationParser.parse(
            packageName = PaymentNotificationParser.ALIPAY_PACKAGE,
            title       = "支付宝",
            text        = "您有一条新的账单提醒",
            bigText     = null,
        )
        assertNull(payment)
    }

    // ── 非目标 App ────────────────────────────────────────────────────────

    @Test
    fun `未知应用包名_返回null`() {
        val payment = PaymentNotificationParser.parse(
            packageName = "com.unknown.app",
            title       = "付款成功",
            text        = "付款 ¥50.00",
            bigText     = null,
        )
        assertNull(payment)
    }

    // ── amountStringToFen 边界测试 ────────────────────────────────────────

    @Test
    fun `分转换_整数`() {
        assertEquals(10000L, PaymentNotificationParser.amountStringToFen("100"))
    }

    @Test
    fun `分转换_一位小数`() {
        assertEquals(150L, PaymentNotificationParser.amountStringToFen("1.5"))
    }

    @Test
    fun `分转换_非法字符串`() {
        assertNull(PaymentNotificationParser.amountStringToFen("abc"))
    }
}
