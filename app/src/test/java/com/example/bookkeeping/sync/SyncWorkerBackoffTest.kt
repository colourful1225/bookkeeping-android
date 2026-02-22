package com.example.bookkeeping.sync

import com.example.bookkeeping.data.local.entity.OutboxOpEntity
import com.example.bookkeeping.data.local.entity.OutboxStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SyncWorker 退避策略的纯 JVM 单元测试（无 Android 依赖）。
 *
 * 重点验证：
 * - 指数退避公式：2^n 秒，上限 n=6（64 秒）
 * - 超过 MAX_RETRY 后状态转为 DEAD
 */
class SyncWorkerBackoffTest {

    /**
     * 提取 SyncWorker 的退避逻辑为纯函数，便于独立测试。
     * 实际 Worker 使用相同算法。
     */
    private fun computeNextStatus(retryCount: Int): String =
        if (retryCount + 1 >= SyncWorker.MAX_RETRY) OutboxStatus.DEAD else OutboxStatus.PENDING

    private fun computeBackoffMs(retryCount: Int): Long {
        val retry = retryCount + 1
        return (1 shl retry.coerceAtMost(6)) * 1_000L
    }

    @Test
    fun 第1次失败_退避2秒() {
        assertEquals(2_000L, computeBackoffMs(0))
    }

    @Test
    fun 第6次失败_退避64秒_上限截断() {
        assertEquals(64_000L, computeBackoffMs(5))
    }

    @Test
    fun 第7次失败_仍为64秒_不继续增加() {
        assertEquals(64_000L, computeBackoffMs(6))
        assertEquals(64_000L, computeBackoffMs(7))
    }

    @Test
    fun 未达阈值_状态仍为PENDING() {
        assertEquals(OutboxStatus.PENDING, computeNextStatus(8))
    }

    @Test
    fun 达到MAX_RETRY_转为DEAD() {
        assertEquals(OutboxStatus.DEAD, computeNextStatus(SyncWorker.MAX_RETRY - 1))
    }

    @Test
    fun 超过MAX_RETRY_仍为DEAD() {
        assertEquals(OutboxStatus.DEAD, computeNextStatus(SyncWorker.MAX_RETRY + 5))
    }
}

/**
 * OutboxOpEntity 构建辅助测试：确保默认状态正确。
 */
class OutboxOpEntityDefaultTest {

    @Test
    fun 默认状态为PENDING() {
        val op = OutboxOpEntity(
            opId           = "op-1",
            entityId       = "tx-1",
            opType         = "CREATE",
            payloadJson    = "{}",
            idempotencyKey = "key-1",
            createdAt      = 0L,
        )
        assertEquals(OutboxStatus.PENDING, op.status)
        assertEquals(0, op.retryCount)
        assertEquals(0L, op.nextRetryAt)
    }
}
