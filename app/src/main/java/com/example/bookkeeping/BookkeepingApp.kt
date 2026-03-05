package com.example.bookkeeping

import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.bookkeeping.notification.PermissionCheckWorker
import com.example.bookkeeping.sync.SyncScheduler
import javax.inject.Inject
import java.util.concurrent.TimeUnit

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
        
        // 创建通知频道
        createNotificationChannels()
        
        // 启动同步任务
        SyncScheduler.enqueuePeriodic(this)
        
        // 启动权限监控任务
        startPermissionMonitoring()
    }

    /**
     * 创建应用所需的通知频道。
     * 注意：minSdkVersion 已是 26 (>= O)，无需版本检查。
     */
    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // 权限监控通知频道
        val permissionChannel = NotificationChannel(
            "permission_monitoring",
            "权限监控",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "权限丢失提醒"
        }
        notificationManager.createNotificationChannel(permissionChannel)
    }

    /**
     * 启动后台权限监控任务。
     * - 15分钟检测一次（Doze 模式下 30分钟）
     * - 如果权限丢失，向用户发送通知
     */
    private fun startPermissionMonitoring() {
        val checkInterval = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 系统更严格，用更低频的检测
            30  // 分钟
        } else {
            15  // 分钟
        }

        val permissionCheckWork = PeriodicWorkRequestBuilder<PermissionCheckWorker>(
            checkInterval.toLong(), TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "permission_check_work",
            ExistingPeriodicWorkPolicy.KEEP,
            permissionCheckWork
        )
    }
}
