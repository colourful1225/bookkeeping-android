package com.example.bookkeeping.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bookkeeping.data.local.entity.OutboxOpEntity

@Dao
interface OutboxDao {

    /**
     * 插入一条 outbox 操作。
     * 使用 ABORT 策略以确保相同 opId 不被静默覆盖，调用方负责保证唯一性。
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(op: OutboxOpEntity)

    /**
     * 批量插入 outbox 操作（用于 CSV 导入）。
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(ops: List<OutboxOpEntity>)

    /**
     * 拉取 [limit] 条到期的 PENDING 记录，按 createdAt 升序（先入先出）。
     * 仅返回 nextRetryAt <= now 的记录，确保退避窗口生效。
     */
    @Query(
        """
        SELECT * FROM outbox_ops
        WHERE  status       = 'PENDING'
          AND  nextRetryAt <= :now
        ORDER BY createdAt ASC
        LIMIT  :limit
        """
    )
    suspend fun fetchPending(now: Long, limit: Int = 20): List<OutboxOpEntity>

    /** 更新操作状态与重试元数据。 */
    @Query(
        """
        UPDATE outbox_ops
        SET status       = :status,
            retryCount   = :retryCount,
            nextRetryAt  = :nextRetryAt
        WHERE opId = :opId
        """
    )
    suspend fun updateStatus(
        opId: String,
        status: String,
        retryCount: Int,
        nextRetryAt: Long,
    )

    /** 同步成功后删除对应 outbox 记录。 */
    @Query("DELETE FROM outbox_ops WHERE opId = :opId")
    suspend fun delete(opId: String)

    /** 查询当前 PENDING 数量（供 UI 角标/提示使用）。 */
    @Query("SELECT COUNT(*) FROM outbox_ops WHERE status = 'PENDING'")
    suspend fun pendingCount(): Int
}
