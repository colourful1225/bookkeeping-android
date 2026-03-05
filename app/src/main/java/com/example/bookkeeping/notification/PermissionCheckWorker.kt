package com.example.bookkeeping.notification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.bookkeeping.ui.permission.PermissionGuidanceActivity

/**
 * 后台工作线程：定期检测权限状态（15分钟一次）。
 *
 * 如果检测到权限丢失，向用户发送通知提示重新启用。
 */
class PermissionCheckWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val TAG = "PermissionCheckWorker"
        private const val NOTIFICATION_ID = 2001
        private const val NOTIFICATION_CHANNEL_ID = "permission_monitoring"
    }

    override fun doWork(): Result {
        return try {
            Log.d(TAG, "检查权限状态...")
            
            val notificationGranted = isNotificationListenerGranted()
            val accessibilityGranted = isAccessibilityServiceGranted()

            if (!notificationGranted || !accessibilityGranted) {
                Log.w(TAG, "权限丢失检测: 通知=$notificationGranted, 无障碍=$accessibilityGranted")
                showPermissionLostNotification(notificationGranted, accessibilityGranted)
            } else {
                Log.d(TAG, "权限状态正常")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "权限检查异常: ${e.message}", e)
            Result.retry()  // 重试
        }
    }

    /**
     * 检测通知监听权限。
     */
    private fun isNotificationListenerGranted(): Boolean {
        return try {
            val cn = android.content.ComponentName(
                applicationContext,
                PaymentNotificationListenerService::class.java
            )
            val flat = Settings.Secure.getString(
                applicationContext.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            flat.contains(cn.flattenToString())
        } catch (e: Exception) {
            Log.w(TAG, "通知权限检查异常: ${e.message}")
            false
        }
    }

    /**
     * 检测无障碍服务权限。
     */
    private fun isAccessibilityServiceGranted(): Boolean {
        return try {
            val cn = android.content.ComponentName(
                applicationContext,
                com.example.bookkeeping.notification.accessibility.PaymentAccessibilityService::class.java
            )
            val flat = Settings.Secure.getString(
                applicationContext.contentResolver,
                "enabled_accessibility_services"
            ) ?: return false
            flat.contains(cn.flattenToString())
        } catch (e: Exception) {
            Log.w(TAG, "无障碍权限检查异常: ${e.message}")
            false
        }
    }

    /**
     * 显示权限丢失通知。
     */
    private fun showPermissionLostNotification(
        notificationGranted: Boolean,
        accessibilityGranted: Boolean
    ) {
        val missingPermissions = listOf(
            if (!notificationGranted) "通知监听" else null,
            if (!accessibilityGranted) "无障碍服务" else null
        ).filterNotNull()

        val title = "权限已禁用"
        val message = "缺失: ${missingPermissions.joinToString("、")}，点击重新启用"

        val intent = Intent(applicationContext, PermissionGuidanceActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)  // 使用系统内置警告图标
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        // Android 13+ 需要检查 POST_NOTIFICATIONS 权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
            } else {
                Log.w(TAG, "缺少 POST_NOTIFICATIONS 权限，无法发送通知")
            }
        } else {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        }

        Log.d(TAG, "已发送权限丢失通知: $message")
    }
}
