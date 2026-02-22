package com.example.bookkeeping.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * WorkManager 任务调度器。
 *
 * - [enqueuePeriodic]：App 启动时注册 15 分钟周期任务（网络可用时触发）。
 *   使用 [ExistingPeriodicWorkPolicy.KEEP] 确保多次调用不重复入队。
 * - [enqueueOneShot]：写入操作完成后立即触发一次同步（缩短 PENDING 持续时间）。
 */
object SyncScheduler {

    private const val WORK_NAME_PERIODIC = "bookkeeping-sync-periodic"

    fun enqueuePeriodic(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val work = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            work,
        )
    }

    fun enqueueOneShot(context: Context) {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueue(req)
    }
}
