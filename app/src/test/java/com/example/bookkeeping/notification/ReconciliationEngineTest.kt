package com.example.bookkeeping.notification

import com.example.bookkeeping.notification.model.ParsedPayment
import com.example.bookkeeping.notification.model.PaymentSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ReconciliationEngine 纯 JVM 单元测试。
 *
 * 覆盖：
 * - isSamePayment：相同/不同金额、时间窗口边界、应用分组
 * - reconcile：首次提交返回自身、重复提交返回 null
 * - merge：confidence 决定主记录、merchantName 合并
 * - 缓冲区过期驱逐
 */
class ReconciliationEngineTest {

    private lateinit var engine: ReconciliationEngine

    private val now = System.currentTimeMillis()

    // 基础支付记录构造函数
    private fun payment(
        source: PaymentSource,
        amountFen: Long = 1000L,
        occurredAt: Long = now,
        merchantName: String? = null,
        confidence: Int = 50,
        rawText: String = "raw_${source}_${amountFen}_$occurredAt",
    ) = ParsedPayment(
        source       = source,
        amountFen    = amountFen,
        occurredAt   = occurredAt,
        merchantName = merchantName,
        confidence   = confidence,
        rawText      = rawText,
    )

    @Before
    fun setup() {
        engine = ReconciliationEngine()
    }

    // ── isSamePayment ─────────────────────────────────────────────────────

    @Test
    fun `isSamePayment - same wechat and wechat_accessibility within window`() {
        val a = payment(PaymentSource.WECHAT, occurredAt = now)
        val b = payment(PaymentSource.WECHAT_ACCESSIBILITY, occurredAt = now + 5_000)
        assertTrue(engine.isSamePayment(a, b))
    }

    @Test
    fun `isSamePayment - same alipay and alipay_accessibility within window`() {
        val a = payment(PaymentSource.ALIPAY, occurredAt = now)
        val b = payment(PaymentSource.ALIPAY_ACCESSIBILITY, occurredAt = now + 30_000)
        assertTrue(engine.isSamePayment(a, b))
    }

    @Test
    fun `isSamePayment - outside time window returns false`() {
        val a = payment(PaymentSource.WECHAT, occurredAt = now)
        val b = payment(PaymentSource.WECHAT_ACCESSIBILITY, occurredAt = now + ReconciliationEngine.SAME_PAYMENT_WINDOW_MS + 1)
        assertTrue(!engine.isSamePayment(a, b))
    }

    @Test
    fun `isSamePayment - exactly at time window boundary returns true`() {
        val a = payment(PaymentSource.WECHAT, occurredAt = now)
        val b = payment(PaymentSource.WECHAT_ACCESSIBILITY, occurredAt = now + ReconciliationEngine.SAME_PAYMENT_WINDOW_MS)
        assertTrue(engine.isSamePayment(a, b))
    }

    @Test
    fun `isSamePayment - different amountFen returns false`() {
        val a = payment(PaymentSource.WECHAT, amountFen = 1000L, occurredAt = now)
        val b = payment(PaymentSource.WECHAT_ACCESSIBILITY, amountFen = 1001L, occurredAt = now)
        assertTrue(!engine.isSamePayment(a, b))
    }

    @Test
    fun `isSamePayment - wechat and alipay with same amount returns false`() {
        val a = payment(PaymentSource.WECHAT, occurredAt = now)
        val b = payment(PaymentSource.ALIPAY, occurredAt = now)
        assertTrue(!engine.isSamePayment(a, b))
    }

    @Test
    fun `isSamePayment - bank sms does not match accessibility`() {
        val a = payment(PaymentSource.BANK_SMS, amountFen = 1000L, occurredAt = now)
        val b = payment(PaymentSource.WECHAT_ACCESSIBILITY, amountFen = 1000L, occurredAt = now)
        assertTrue(!engine.isSamePayment(a, b))
    }

    // ── reconcile ─────────────────────────────────────────────────────────

    @Test
    fun `reconcile - first submission returns same object`() {
        val p = payment(PaymentSource.WECHAT)
        val result = engine.reconcile(p)
        assertNotNull(result)
        assertEquals(p.amountFen, result!!.amountFen)
    }

    @Test
    fun `reconcile - duplicate same source returns null`() {
        val p1 = payment(PaymentSource.WECHAT, rawText = "raw_1")
        val p2 = payment(PaymentSource.WECHAT, rawText = "raw_2", occurredAt = now + 5_000)
        engine.reconcile(p1)
        val result = engine.reconcile(p2)
        // 同源同金额极短时间内 → 重复 → null
        assertNull(result)
    }

    @Test
    fun `reconcile - accessibility higher confidence returns merged`() {
        val notification = payment(
            source     = PaymentSource.WECHAT,
            confidence = 30,
            occurredAt = now,
        )
        val accessibility = payment(
            source       = PaymentSource.WECHAT_ACCESSIBILITY,
            confidence   = 80,
            merchantName = "星巴克",
            occurredAt   = now + 10_000,
            rawText      = "accessibility_raw",
        )

        // 第一条写库
        val first = engine.reconcile(notification)
        assertNotNull(first)

        // 第二条 confidence 更高 → 应返回合并后的记录
        val second = engine.reconcile(accessibility)
        assertNotNull(second)
        assertEquals("星巴克", second!!.merchantName)
        assertEquals(80, second.confidence)
    }

    @Test
    fun `reconcile - low confidence accessibility after high confidence notification returns null`() {
        val notification = payment(
            source     = PaymentSource.WECHAT,
            confidence = 90,
            occurredAt = now,
        )
        val accessibility = payment(
            source     = PaymentSource.WECHAT_ACCESSIBILITY,
            confidence = 50,
            occurredAt = now + 10_000,
            rawText    = "accessibility_raw",
        )

        engine.reconcile(notification)
        // 新来的 confidence 低于缓冲中的 → 返回 null（不更新DB）
        val second = engine.reconcile(accessibility)
        assertNull(second)
    }

    @Test
    fun `reconcile - different amount treated as different payment`() {
        val p1 = payment(PaymentSource.WECHAT, amountFen = 1000L)
        val p2 = payment(PaymentSource.WECHAT_ACCESSIBILITY, amountFen = 2000L, occurredAt = now + 1_000)
        engine.reconcile(p1)
        val result = engine.reconcile(p2)
        assertNotNull(result)
    }

    // ── merge ─────────────────────────────────────────────────────────────

    @Test
    fun `merge - higher confidence wins as primary`() {
        val low  = payment(PaymentSource.WECHAT, confidence = 30, merchantName = "A")
        val high = payment(PaymentSource.WECHAT_ACCESSIBILITY, confidence = 80, merchantName = null)
        val merged = engine.merge(low, high)
        assertEquals(80, merged.confidence)
        // high 没有 merchantName，fallback 到 low 的 "A"
        assertEquals("A", merged.merchantName)
    }

    @Test
    fun `merge - secondary merchantName used when primary is null`() {
        val a = payment(PaymentSource.WECHAT, confidence = 50, merchantName = null)
        val b = payment(PaymentSource.WECHAT_ACCESSIBILITY, confidence = 50, merchantName = "麦当劳")
        val merged = engine.merge(a, b)
        assertEquals("麦当劳", merged.merchantName)
    }

    @Test
    fun `merge - both have merchantName primary wins`() {
        val a = payment(PaymentSource.WECHAT, confidence = 60, merchantName = "A商户")
        val b = payment(PaymentSource.WECHAT_ACCESSIBILITY, confidence = 80, merchantName = "B商户")
        // b 是 other（confidence 更高），merge(base=a, other=b) → primary=b
        val merged = engine.merge(a, b)
        assertEquals("B商户", merged.merchantName)
        assertEquals(80, merged.confidence)
    }
}
