package com.example.bookkeeping.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bookkeeping.data.local.AppDatabase
import com.example.bookkeeping.data.local.entity.OutboxStatus
import com.example.bookkeeping.data.local.entity.SyncStateEntity
import com.example.bookkeeping.data.local.entity.SyncStatus
import com.example.bookkeeping.data.remote.BookkeepingApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 离线补传 Worker。
 *
 * 执行逻辑：
 * 1. 分页拉取到期的 PENDING outbox 记录（每次最多 20 条）。
 * 2. 标记为 PROCESSING，调用服务端 upsert。
 * 3. 成功：原子地回写 SYNCED 并删除 outbox。
 * 4. 失败：指数退避更新 nextRetryAt；超过 [MAX_RETRY] 次标记 DEAD。
 *
 * 依赖注入通过 [HiltWorker] + [AssistedInject] 实现，须在 [DatabaseModule] 中注册 WorkerFactory。
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val db: AppDatabase,
    private val api: BookkeepingApi,
    private val mapper: SyncMapper,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val now     = System.currentTimeMillis()
        val pending = db.outboxDao().fetchPending(now, limit = 20)

        var lastError: String? = null

        for (op in pending) {
            try {
                // 标记执行中，防止并发 Worker 重复消费
                db.outboxDao().updateStatus(op.opId, OutboxStatus.PROCESSING, op.retryCount, op.nextRetryAt)

                val tx   = mapper.payloadToTransaction(op.payloadJson)
                val resp = api.upsertTransaction(op.idempotencyKey, mapper.toRequest(tx))

                db.withTransaction {
                    db.transactionDao().updateSyncResult(
                        id        = tx.id,
                        status    = SyncStatus.SYNCED,
                        serverId  = resp.serverId,
                        updatedAt = System.currentTimeMillis(),
                    )
                    db.outboxDao().delete(op.opId)
                }
            } catch (e: retrofit2.HttpException) {
                // 4xx（业务不可恢复）直接 DEAD，不重试
                if (e.code() in 400..499) {
                    db.outboxDao().updateStatus(op.opId, OutboxStatus.DEAD, op.retryCount, op.nextRetryAt)
                    lastError = "HTTP ${e.code()} on opId=${op.opId}"
                } else {
                    applyBackoff(op.opId, op.retryCount)
                    lastError = e.message()
                }
            } catch (e: Exception) {
                applyBackoff(op.opId, op.retryCount)
                lastError = e.message
            }
        }

        // 更新全局同步状态（最后一次错误摘要）
        val state = SyncStateEntity(
            lastSyncAt = System.currentTimeMillis(),
            lastError  = lastError,
        )
        db.syncStateDao().upsert(state)

        Result.success()
    }

    private suspend fun applyBackoff(opId: String, currentRetry: Int) {
        val retry      = currentRetry + 1
        val backoffMs  = (1 shl retry.coerceAtMost(6)) * 1_000L  // 最大 64 s
        val nextStatus = if (retry >= MAX_RETRY) OutboxStatus.DEAD else OutboxStatus.PENDING
        db.outboxDao().updateStatus(
            opId        = opId,
            status      = nextStatus,
            retryCount  = retry,
            nextRetryAt = System.currentTimeMillis() + backoffMs,
        )
    }

    companion object {
        const val MAX_RETRY = 10
    }
}
