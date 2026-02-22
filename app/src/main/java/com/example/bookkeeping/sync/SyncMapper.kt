package com.example.bookkeeping.sync

import com.example.bookkeeping.data.local.entity.TransactionEntity
import com.example.bookkeeping.data.remote.dto.UpsertTransactionRequest
import com.example.bookkeeping.data.repo.JsonCodec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync 层的序列化/反序列化适配器。
 *
 * - [payloadToTransaction]：将 outbox 记录中存储的 JSON 快照还原为本地实体。
 * - [toRequest]：将本地实体映射为远端 API DTO。
 */
@Singleton
class SyncMapper @Inject constructor(
    private val json: JsonCodec,
) {
    fun payloadToTransaction(payload: String): TransactionEntity =
        json.fromJson(payload, TransactionEntity::class.java)

    fun toRequest(tx: TransactionEntity): UpsertTransactionRequest =
        UpsertTransactionRequest(
            id         = tx.id,
            amount     = tx.amount,
            type       = tx.type,
            categoryId = tx.categoryId,
            note       = tx.note,
            occurredAt = tx.occurredAt,
        )
}
