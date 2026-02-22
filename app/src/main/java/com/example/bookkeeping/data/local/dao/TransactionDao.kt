package com.example.bookkeeping.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bookkeeping.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /** 新增或整体替换（幂等写入，重复 id 以最新为准）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tx: TransactionEntity)

    /** 批量插入（用于 CSV 导入）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    /** 按发生时间倒序，持续观测本地全量交易列表。 */
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    /** 获取全部交易记录（用于导出 CSV）。 */
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    suspend fun getAll(): List<TransactionEntity>

    /** 同步成功后回写服务端 ID 与状态。 */
    @Query(
        """
        UPDATE transactions
        SET syncStatus = :status,
            serverId   = :serverId,
            updatedAt  = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateSyncResult(
        id: String,
        status: String,
        serverId: String?,
        updatedAt: Long,
    )

    /** 单条 id 查询（供 ViewModel 详情页使用）。 */
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TransactionEntity?
}
