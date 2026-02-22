package com.example.bookkeeping.domain.usecase

import com.example.bookkeeping.data.repo.ITransactionRepository
import javax.inject.Inject

/**
 * 新增支出 UseCase。
 *
 * 职责：
 * - 校验金额（> 0）
 * - 委托 Repository 完成事务双写
 * - 写入完成后触发 one-shot 同步
 */
class AddExpenseUseCase @Inject constructor(
    private val repository: ITransactionRepository,
) {
    /**
     * @param amount     金额，单位：分（必须 > 0）
     * @param categoryId 分类 ID
     * @param note       备注（可空）
     * @param photoUri   凭证照片 URI（可空）
     * @throws IllegalArgumentException 若金额 <= 0
     */
    suspend operator fun invoke(
        amount: Long,
        categoryId: String,
        note: String? = null,
        photoUri: String? = null,
    ) {
        require(amount > 0) { "金额必须大于 0，当前值：$amount" }
        require(categoryId.isNotBlank()) { "分类 ID 不能为空" }

        repository.addExpense(
            amount     = amount,
            categoryId = categoryId,
            note       = note,
            photoUri   = photoUri,
        )
    }
}
