package com.example.bookkeeping.notification

import android.util.Log
import com.example.bookkeeping.data.local.dao.CategoryDao
import com.example.bookkeeping.data.local.entity.CategoryType
import com.example.bookkeeping.data.repo.ITransactionRepository
import com.example.bookkeeping.data.util.CategoryMatcher
import com.example.bookkeeping.notification.model.ConflictAlert
import com.example.bookkeeping.notification.model.ParsedPayment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 支付自动导入协调器。
 *
 * 职责：
 * 1. 接收来自通知监听或短信接收器的 [ParsedPayment]。
 * 2. 通过 [CategoryMatcher] 推断分类 ID。
 * 3. 调用 [ITransactionRepository.addExpense] 完成事务双写。
 * 4. 内嵌短期去重缓存，防止同一通知重复导入。
 */
@Singleton
class PaymentAutoImporter @Inject constructor(
    private val repository: ITransactionRepository,
    private val categoryDao: CategoryDao,
    private val reconciliation: ReconciliationEngine,
    private val conflictDetector: ConflictDetector,
    private val merchantLearner: MerchantCategoryLearner,
    private val obfuscation: ObfuscationStrategy,
) {
    companion object {
        private const val TAG = "PaymentAutoImporter"

        /** 去重窗口：最近 N 条原始文本的哈希，避免重复通知导致重复记录 */
        private const val DEDUP_CACHE_SIZE = 50
    }

    /** 应用级协程作用域，不绑定任何 Activity/Fragment 生命周期 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 去重队列（并发安全）。
     * 使用 ArrayDeque 模拟有容量上限的 FIFO 缓冲：超出容量时淘汰最旧哈希。
     */
    private val dedupQueue: ArrayDeque<Int> = ArrayDeque(DEDUP_CACHE_SIZE)
    private val dedupLock = Any()

    /** ▶ 冲突信息 StateFlow：UI 层可通过 collect 监听冲突事件 */
    private val _conflictAlertFlow = MutableStateFlow<ConflictAlert?>(null)
    val conflictAlertFlow: StateFlow<ConflictAlert?> = _conflictAlertFlow.asStateFlow()

    /** 待处理的支付信息缓存（当检测到冲突时保存，用于 UI 层确认后处理） */
    private var pendingPayment: ParsedPayment? = null

    /** UI 冲突回调：检测到冲突时调用此回调，让 UI 显示对话框 */
    private var onConflictDetected: ((ConflictAlert) -> Unit)? = null

    /**
     * 设置冲突检测回调。
     * @param callback 当检测到冲突时调用此回调，UI 层接收后显示对话框
     */
    fun setConflictCallback(callback: (ConflictAlert) -> Unit) {
        onConflictDetected = callback
    }

    /**
     * 处理用户在冲突对话框中选择"保留"（不覆盖）。
     * 此时丢弃自动记账，保留用户手动记录。
     */
    fun confirmKeepExisting() {
        Log.i(TAG, "用户选择保留现有交易")
        _conflictAlertFlow.value = null
        pendingPayment = null
    }

    /**
     * 处理用户在冲突对话框中选择"覆盖"。
     * 删除旧交易，导入新的自动记账。
     */
    fun confirmOverwrite(existingTxId: String) {
        val payment = pendingPayment ?: return
        Log.i(TAG, "用户选择覆盖，准备删除旧交易并导入新记录")
        _conflictAlertFlow.value = null
        
        scope.launch {
            try {
                val existingId = existingTxId.toLongOrNull() ?: return@launch
                repository.deleteTransaction(existingId)
                Log.i(TAG, "已删除旧交易: ID=$existingId")
                
                // 重新导入新支付
                importInternal(payment)
                Log.i(TAG, "覆盖导入成功")
                pendingPayment = null
            } catch (e: Exception) {
                Log.e(TAG, "覆盖导入失败: ${e.message}", e)
            }
        }
    }

    /**
     * 处理用户在冲突对话框中选择"取消"。
     * 放弃导入，丢弃自动记账。
     */
    fun confirmCancel() {
        Log.i(TAG, "用户取消导入")
        _conflictAlertFlow.value = null
        pendingPayment = null
    }

    /**
     * 异步导入支付记录。
     *
     * 在后台协程中执行，不阻塞调用方（Service / BroadcastReceiver）。
     *
     * @param payment 解析后的支付信息
     */
    fun importAsync(payment: ParsedPayment) {
        val hash = payment.rawText.hashCode()
        val isDuplicate = synchronized(dedupLock) {
            if (dedupQueue.contains(hash)) {
                true
            } else {
                if (dedupQueue.size >= DEDUP_CACHE_SIZE) dedupQueue.removeFirst()
                dedupQueue.addLast(hash)
                false
            }
        }
        if (isDuplicate) {
            Log.d(TAG, "跳过重复通知: ${payment.source} ¥${payment.amountFen / 100.0}")
            return
        }

        // 跨源对账：同一笔支付可能被通知和无障碍服务双重捕获
        val reconciled = reconciliation.reconcile(payment)
        if (reconciled == null) {
            Log.d(TAG, "对账去重（跨源重复）: ${payment.source} ¥${payment.amountFen / 100.0}")
            return
        }

        scope.launch {
            try {
                // ▶ 冲突检测：获取详情而不是仅返回 Boolean
                val categories = categoryDao.getByType(CategoryType.EXPENSE)
                val baseCategoryId = if (reconciled.merchantName != null) {
                    CategoryMatcher.matchCategoryName(reconciled.merchantName, categories)
                } else {
                    "others"
                }
                val learnedCategoryId = merchantLearner.queryLearned(reconciled.merchantName)
                val categoryId = learnedCategoryId.takeIf { !it.isNullOrBlank() } ?: baseCategoryId
                val autoNote = buildNote(reconciled, categoryId)

                val conflictAlert = conflictDetector.detectConflictWithDetails(
                    autoAmountFen = reconciled.amountFen,
                    autoCategoryId = categoryId,
                    autoOccurredAt = reconciled.occurredAt,
                    autoNote = autoNote,
                    autoSource = reconciled.source.toString(),
                    type = "EXPENSE"
                )

                if (conflictAlert != null) {
                    // ▶ 检测到冲突，缓存支付信息并通知 UI
                    Log.w(TAG, "检测到冲突，等待用户确认: ${reconciled.source} ¥${reconciled.amountFen / 100.0}")
                    // 缓存待处理的支付对象（用于 UI 层确认后处理）
                    pendingPayment = reconciled
                    // 更新 StateFlow，以便 UI 层通过 collect 接收冲突信息
                    _conflictAlertFlow.value = conflictAlert
                    onConflictDetected?.invoke(conflictAlert)
                    // 注意：此后不再自动导入，等待 UI 层用户决策
                    return@launch
                }

                // 无冲突，直接导入
                importInternal(reconciled)
            } catch (e: Exception) {
                Log.e(TAG, "自动导入失败: ${e.message}", e)
                synchronized(dedupLock) { dedupQueue.remove(hash) }
            }
        }
    }

    /**
     * 强制导入（用户在冲突对话框中选择"覆盖"后调用此方法）。
     *
     * @param payment 解析后的支付信息
     */
    fun forceImportWithConflict(payment: ParsedPayment) {
        scope.launch {
            try {
                importInternal(payment)
                Log.i(TAG, "用户确认覆盖，已导入: ${payment.source} ¥${payment.amountFen / 100.0}")
            } catch (e: Exception) {
                Log.e(TAG, "强制导入失败: ${e.message}", e)
            }
        }
    }

    /**
     * 处理用户在冲突对话框中选择"覆盖"的场景。
     *
     * @param conflictAlert 冲突详情（包含旧交易 ID）
     * @param payment 新的支付信息
     */
    suspend fun overwriteTransaction(conflictAlert: ConflictAlert, payment: ParsedPayment) {
        withContext(Dispatchers.IO) {
            // 删除旧记录
            val existingId = conflictAlert.existingTxId.toLongOrNull() ?: return@withContext
            repository.deleteTransaction(existingId)
            Log.i(TAG, "已删除旧交易: ID=$existingId 金额=¥${conflictAlert.existingAmount / 100.0}")

            // 导入新记录
            importInternal(payment)
        }
    }

    /** 内部同步导入逻辑，在 IO 协程中执行 */
    private suspend fun importInternal(payment: ParsedPayment) {
        val categories = categoryDao.getByType(CategoryType.EXPENSE)
        
        // ▶ 分类学习：优先查询学习库，再用传统匹配
        val baseCategoryId = if (payment.merchantName != null) {
            CategoryMatcher.matchCategoryName(payment.merchantName, categories)
        } else {
            "others"
        }
        
        // 查询是否有用户纠正历史
        val learnedCategoryId = merchantLearner.queryLearned(payment.merchantName)
        val categoryId = learnedCategoryId.takeIf { !it.isNullOrBlank() } ?: baseCategoryId

        repository.addExpense(
            amount     = payment.amountFen,
            categoryId = categoryId,
            note       = buildNote(payment, categoryId),
            photoUri   = null,
        )

        Log.i(
            TAG,
            "自动导入成功: [${payment.source}] 商户=${payment.merchantName ?: "-"} " +
                "金额=¥${payment.amountFen / 100.0} 分类=$categoryId",
        )
    }

    /** 构建备注，包含来源标识与商户名（难以感知优化） */
    private fun buildNote(payment: ParsedPayment, categoryId: String): String {
        // ▶ 难以感知：使用 ObfuscationStrategy 隐蔽标签与商户名
        val baseNote = buildString {
            if (!payment.merchantName.isNullOrBlank()) {
                append(payment.merchantName)
            } else {
                append("自动记账")
            }
        }
        
        return obfuscation.obfuscateNote(baseNote)
    }
}
