package com.example.bookkeeping.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 交易主数据表。
 *
 * - [id]：本地生成的 UUID，主键。
 * - [serverId]：同步成功后由服务端返回，可空。
 * - [amount]：金额，单位：分（Long），避免浮点精度问题。
 * - [type]：INCOME / EXPENSE / TRANSFER。
 * - [syncStatus]：PENDING / SYNCED / FAILED / CONFLICT。
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val serverId: String? = null,
    val amount: Long,
    val type: String,
    val categoryId: String,
    val note: String? = null,
    val photoUri: String? = null,  // 凭证照片 URI (content:// 格式)
    val occurredAt: Long,
    val updatedAt: Long,
    val syncStatus: String = SyncStatus.PENDING,
)

object SyncStatus {
    const val PENDING  = "PENDING"
    const val SYNCED   = "SYNCED"
    const val FAILED   = "FAILED"
    const val CONFLICT = "CONFLICT"
}
