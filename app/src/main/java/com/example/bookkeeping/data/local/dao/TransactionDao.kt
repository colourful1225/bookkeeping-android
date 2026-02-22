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

    /**
     * 高级搜索过滤查询
     * @param type 交易类型（INCOME/EXPENSE，null 表示不限）
     * @param categoryId 分类 ID（null 表示不限）
     * @param startDate 起始日期时间戳（null 表示不限）
     * @param endDate 结束日期时间戳（null 表示不限）
     * @param minAmount 最小金额（分，null 表示不限）
     * @param maxAmount 最大金额（分，null 表示不限）
     * @param query 备注关键词搜索（模糊匹配，null 表示不限）
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE (:type IS NULL OR type = :type)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:startDate IS NULL OR occurredAt >= :startDate)
          AND (:endDate IS NULL OR occurredAt <= :endDate)
          AND (:minAmount IS NULL OR amount >= :minAmount)
          AND (:maxAmount IS NULL OR amount <= :maxAmount)
          AND (:query IS NULL OR note LIKE '%' || :query || '%')
        ORDER BY occurredAt DESC
        """
    )
    fun searchTransactions(
        type: String?,
        categoryId: String?,
        startDate: Long?,
        endDate: Long?,
        minAmount: Long?,
        maxAmount: Long?,
        query: String?,
    ): Flow<List<TransactionEntity>>

    /**
     * 按日期范围和类型统计
     */
    @Query(
        """
        SELECT COUNT(*) as count, SUM(amount) as total
        FROM transactions
        WHERE type = :type
          AND occurredAt >= :startDate
          AND occurredAt <= :endDate
        """
    )
    suspend fun getSummary(
        type: String,
        startDate: Long,
        endDate: Long,
    ): TransactionSummary?
}

/**
 * 统计数据模型
 */
data class TransactionSummary(
    val count: Int,
    val total: Long,
)
