package com.example.bookkeeping.domain.usecase

import com.example.bookkeeping.data.local.dao.TransactionDao
import com.example.bookkeeping.data.local.entity.TransactionEntity
import com.example.bookkeeping.domain.model.SearchFilter
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 高级搜索过滤用例
 * 
 * 支持多条件组合搜索：
 * - 交易类型（支出/收入）
 * - 分类
 * - 日期范围
 * - 金额范围
 * - 备注关键词
 */
class SearchTransactionsUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
) {
    
    /**
     * 执行搜索查询
     * @param filter 搜索条件
     * @return 匹配条件的交易记录流
     */
    fun execute(filter: SearchFilter): Flow<List<TransactionEntity>> {
        val params = filter.toQueryParams()
        
        return transactionDao.searchTransactions(
            type = params.type,
            categoryId = params.categoryId,
            startDate = params.startDate,
            endDate = params.endDate,
            minAmount = params.minAmount,
            maxAmount = params.maxAmount,
            query = params.query,
        )
    }
    
    /**
     * 执行空查询（返回所有交易）
     */
    fun executeAll(): Flow<List<TransactionEntity>> {
        return transactionDao.observeAll()
    }
}
