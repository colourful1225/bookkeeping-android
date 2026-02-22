package com.example.bookkeeping.domain.usecase

import com.example.bookkeeping.data.local.entity.TransactionEntity
import com.example.bookkeeping.data.repo.ITransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/**
 * AddExpenseUseCase 参数校验的纯 JVM 单元测试。
 * 使用 FakeTransactionRepository 隔离数据库依赖。
 */
class AddExpenseUseCaseTest {

    private val fakeRepo = FakeTransactionRepository()
    private val useCase  = AddExpenseUseCase(fakeRepo)

    @Test
    fun 正常金额_成功写入() = runTest {
        useCase(amount = 500L, categoryId = "food", note = "午饭")
        assertEquals(1, fakeRepo.addedExpenses.size)
        assertEquals(500L, fakeRepo.addedExpenses[0].amount)
    }

    @Test
    fun 金额为0_抛出IllegalArgument() = runTest {
        try {
            useCase(amount = 0L, categoryId = "food")
            fail("应该抛出 IllegalArgumentException")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun 金额为负数_抛出IllegalArgument() = runTest {
        try {
            useCase(amount = -100L, categoryId = "food")
            fail("应该抛出 IllegalArgumentException")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun 分类ID为空白_抛出IllegalArgument() = runTest {
        try {
            useCase(amount = 100L, categoryId = "   ")
            fail("应该抛出 IllegalArgumentException")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun 备注可为空() = runTest {
        useCase(amount = 100L, categoryId = "misc", note = null)
        assertEquals(null, fakeRepo.addedExpenses[0].note)
    }
}

// ── 测试替身 ──────────────────────────────────────────────
private data class FakeEntry(val amount: Long, val categoryId: String, val note: String?)

private class FakeTransactionRepository : ITransactionRepository {
    val addedExpenses = mutableListOf<FakeEntry>()

    override fun observeTransactions(): Flow<List<TransactionEntity>> = emptyFlow()

    override suspend fun addExpense(amount: Long, categoryId: String, note: String?) {
        addedExpenses.add(FakeEntry(amount, categoryId, note))
    }
}
