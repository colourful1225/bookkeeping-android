package com.example.bookkeeping.data.repo

import androidx.room.withTransaction
import com.example.bookkeeping.data.local.AppDatabase
import com.example.bookkeeping.data.local.entity.OutboxOpEntity
import com.example.bookkeeping.data.local.entity.OpType
import com.example.bookkeeping.data.local.entity.TransactionEntity
import com.example.bookkeeping.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 交易数据仓库。
 *
 * 核心职责：
 * 1. 对外暴露本地 Flow，UI 仅订阅本地数据。
 * 2. 写入时使用单事务同时写 `transactions` + `outbox_ops`，保证不一致不可见。
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val db: AppDatabase,
    private val json: JsonCodec,
) : ITransactionRepository {
    /** 以时间倒序持续观测本地全量交易列表。 */
    override fun observeTransactions(): Flow<List<TransactionEntity>> =
        db.transactionDao().observeAll()

    /**
     * 离线新增支出。
     *
     * 事务保证：若任意写入失败，两表均回滚，不会出现"有交易但无 outbox"的状态。
     */
    override suspend fun addExpense(amount: Long, categoryId: String, note: String?, photoUri: String?) {
        val now   = System.currentTimeMillis()
        val txId  = UUID.randomUUID().toString()

        val tx = TransactionEntity(
            id          = txId,
            amount      = amount,
            type        = "EXPENSE",
            categoryId  = categoryId,
            note        = note,
            photoUri    = photoUri,
            occurredAt  = now,
            updatedAt   = now,
            syncStatus  = SyncStatus.PENDING,
        )

        val op = OutboxOpEntity(
            opId             = UUID.randomUUID().toString(),
            entityId         = txId,
            opType           = OpType.CREATE,
            payloadJson      = json.toJson(tx),
            idempotencyKey   = "tx-create-$txId",
            createdAt        = now,
        )

        db.withTransaction {
            db.transactionDao().upsert(tx)
            db.outboxDao().insert(op)
        }
    }
}

/** Moshi/Gson 封装：由 DI 提供具体实现。 */
interface JsonCodec {
    fun toJson(any: Any): String
    fun <T> fromJson(json: String, clazz: Class<T>): T
}
