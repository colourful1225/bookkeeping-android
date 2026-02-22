package com.example.bookkeeping.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookkeeping.data.local.AppDatabase
import com.example.bookkeeping.data.local.entity.OutboxOpEntity
import com.example.bookkeeping.data.local.entity.OutboxStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * OutboxDao 的仪器化测试（跑在设备/模拟器上，使用 in-memory Room）。
 *
 * 重点验证：
 * - [OutboxDao.fetchPending] 的时间过滤（nextRetryAt 未到期不应被拉取）
 * - [OutboxDao.updateStatus] 正确更新状态与重试元数据
 */
@RunWith(AndroidJUnit4::class)
class OutboxDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: OutboxDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db  = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.outboxDao()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun fetchPending_返回到期记录_忽略未到期() = runTest {
        val now = System.currentTimeMillis()

        // 到期记录
        val ready = outboxOp(nextRetryAt = now - 1_000)
        // 未到期记录（下次重试在 60 秒后）
        val notYet = outboxOp(nextRetryAt = now + 60_000)

        dao.insert(ready)
        dao.insert(notYet)

        val result = dao.fetchPending(now)

        assertEquals(1, result.size)
        assertEquals(ready.opId, result[0].opId)
    }

    @Test
    fun fetchPending_不返回DEAD状态记录() = runTest {
        val now  = System.currentTimeMillis()
        val dead = outboxOp(nextRetryAt = 0L)
        dao.insert(dead)
        dao.updateStatus(dead.opId, OutboxStatus.DEAD, 10, 0L)

        val result = dao.fetchPending(now)
        assertTrue(result.isEmpty())
    }

    @Test
    fun updateStatus_正确写入重试元数据() = runTest {
        val op = outboxOp(nextRetryAt = 0L)
        dao.insert(op)
        dao.updateStatus(op.opId, OutboxStatus.PENDING, retryCount = 3, nextRetryAt = 9999L)

        val result = dao.fetchPending(9999L)
        assertEquals(1, result.size)
        assertEquals(3, result[0].retryCount)
    }

    private fun outboxOp(nextRetryAt: Long) = OutboxOpEntity(
        opId           = UUID.randomUUID().toString(),
        entityId       = UUID.randomUUID().toString(),
        opType         = "CREATE",
        payloadJson    = "{}",
        idempotencyKey = UUID.randomUUID().toString(),
        nextRetryAt    = nextRetryAt,
        createdAt      = System.currentTimeMillis(),
    )
}
