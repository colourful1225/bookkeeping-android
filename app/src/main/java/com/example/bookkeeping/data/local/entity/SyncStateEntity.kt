package com.example.bookkeeping.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 全局同步状态快照表（单行，key = "global"）。
 *
 * - [lastSyncAt]：最后一次成功同步的时间戳（ms）。
 * - [lastError]：最近一次同步失败的摘要，可空。
 */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val key: String = "global",
    val lastSyncAt: Long = 0L,
    val lastError: String? = null,
)
