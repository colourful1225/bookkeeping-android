package com.example.bookkeeping.domain.usecase

import com.example.bookkeeping.data.local.entity.TransactionEntity
import com.example.bookkeeping.data.repo.ITransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 观测交易列表 UseCase。
 *
 * 对 UI 隐藏 Repository 细节；后续可在这里加排序、过滤、分页等逻辑，
 * 不影响 ViewModel。
 */
class ObserveTransactionsUseCase @Inject constructor(
    private val repository: ITransactionRepository,
) {
    operator fun invoke(): Flow<List<TransactionEntity>> =
        repository.observeTransactions()
}
