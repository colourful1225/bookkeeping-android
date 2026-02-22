package com.example.bookkeeping

import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import android.app.Application
import com.example.bookkeeping.sync.SyncScheduler
import javax.inject.Inject

/**
 * Application 入口。
 *
 * - 通过 [HiltAndroidApp] 开启 Hilt 组件树。
 * - 实现 [Configuration.Provider] 使 WorkManager 使用 Hilt 注入的 WorkerFactory。
 * - 启动时注册周期同步任务（[SyncScheduler.enqueuePeriodic]）。
 */
@HiltAndroidApp
class BookkeepingApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerConfiguration: Configuration

    override val workManagerConfiguration: Configuration
        get() = workerConfiguration

    override fun onCreate() {
        super.onCreate()
        SyncScheduler.enqueuePeriodic(this)
    }
}
