package com.example.bookkeeping.data.repo

import com.example.bookkeeping.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 交易仓库接口。
 *
 * UseCase / ViewModel 只依赖此接口，方便单元测试替换 Fake 实现，
 * 真实实现为 [TransactionRepository]。
 */
interface ITransactionRepository {
    fun observeTransactions(): Flow<List<TransactionEntity>>
    suspend fun addExpense(amount: Long, categoryId: String, note: String?)
}
