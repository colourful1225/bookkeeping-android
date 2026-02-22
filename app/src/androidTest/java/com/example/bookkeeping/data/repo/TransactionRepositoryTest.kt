package com.example.bookkeeping.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.bookkeeping.data.local.AppDatabase
import com.example.bookkeeping.data.local.entity.SyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TransactionRepository 仪器化测试（in-memory Room）。
 *
 * 重点验证：
 * - [TransactionRepository.addExpense] 的事务双写：
 *   transactions + outbox_ops 同时写入，且状态初始为 PENDING。
 * - 写入后 Flow 立即可见（本地优先）。
 */
@RunWith(AndroidJUnit4::class)
class TransactionRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: TransactionRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db   = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repo = TransactionRepository(db, FakeJsonCodec())
    }

    @After
    fun teardown() = db.close()

    @Test
    fun addExpense_写入后列表立即可见且状态为PENDING() = runTest {
        repo.addExpense(amount = 500L, categoryId = "food", note = "午饭")

        repo.observeTransactions().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(500L, list[0].amount)
            assertEquals(SyncStatus.PENDING, list[0].syncStatus)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addExpense_同时写入outbox记录() = runTest {
        repo.addExpense(amount = 1000L, categoryId = "transport", note = null)

        val pending = db.outboxDao().fetchPending(System.currentTimeMillis() + 1_000)
        assertEquals(1, pending.size)
        assertEquals("CREATE", pending[0].opType)
        assertNotNull(pending[0].idempotencyKey)
    }

    @Test
    fun addExpense_多次写入不互相干扰() = runTest {
        repeat(3) { i ->
            repo.addExpense(amount = (i + 1) * 100L, categoryId = "misc", note = "item $i")
        }

        val pending = db.outboxDao().fetchPending(System.currentTimeMillis() + 1_000)
        assertEquals(3, pending.size)
    }
}

/** 测试用 JsonCodec：直接序列化字段名，不依赖 Moshi 配置。 */
private class FakeJsonCodec : JsonCodec {
    override fun toJson(any: Any): String = any.toString()
    override fun <T> fromJson(json: String, clazz: Class<T>): T =
        throw UnsupportedOperationException("FakeJsonCodec 不支持反序列化")
}
