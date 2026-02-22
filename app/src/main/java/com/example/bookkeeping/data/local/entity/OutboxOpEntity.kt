package com.example.bookkeeping.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Outbox 操作队列表。
 *
 * 每一条业务写入都对应一条 outbox 记录，两者同事务提交。
 * Worker 批量消费此表，实现离线补传与幂等重试。
 *
 * - [opId]：操作唯一主键（UUID）。
 * - [entityId]：关联的 [TransactionEntity.id]。
 * - [opType]：CREATE / UPDATE / DELETE。
 * - [payloadJson]：操作时的数据快照（序列化 JSON）。
 * - [idempotencyKey]：幂等键，服务端基于此防重复入账。
 * - [status]：PENDING / PROCESSING / DONE / DEAD。
 * - [retryCount]、[nextRetryAt]：指数退避控制。
 */
@Entity(
    tableName = "outbox_ops",
    indices = [Index(value = ["status", "nextRetryAt", "createdAt"])],
)
data class OutboxOpEntity(
    @PrimaryKey val opId: String,
    val entityId: String,
    val opType: String,
    val payloadJson: String,
    val idempotencyKey: String,
    val retryCount: Int = 0,
    val nextRetryAt: Long = 0L,
    val status: String = OutboxStatus.PENDING,
    val createdAt: Long,
)

object OutboxStatus {
    const val PENDING    = "PENDING"
    const val PROCESSING = "PROCESSING"
    const val DONE       = "DONE"
    const val DEAD       = "DEAD"
}

object OpType {
    const val CREATE = "CREATE"
    const val UPDATE = "UPDATE"
    const val DELETE = "DELETE"
}
