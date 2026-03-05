package com.example.bookkeeping.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 支付通知监听服务（微信支付 & 支付宝）。
 *
 * ## 前置条件
 * 用户需在 系统设置 → 通知 → 通知使用权 中手动授权本应用。
 * 在 [AutoImportSettingsManager.isEnabled] 为 false 时，服务处于绑定状态但不处理通知。
 *
 * ## 安全说明
 * - 仅监听 [PaymentNotificationParser.WECHAT_PACKAGE] 和 [PaymentNotificationParser.ALIPAY_PACKAGE]。
 * - 不读取通知中的私聊内容（仅触发支付关键词白名单匹配者）。
 */
@AndroidEntryPoint
class PaymentNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "PayNotifListener"
    }

    @Inject
    lateinit var importer: PaymentAutoImporter

    @Inject
    lateinit var settings: AutoImportSettingsManager

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 功能未开启时直接返回，最小化系统资源占用
        if (!settings.isNotificationListenerEnabled) return

        val pkg = sbn.packageName ?: return
        if (pkg != PaymentNotificationParser.WECHAT_PACKAGE &&
            pkg != PaymentNotificationParser.ALIPAY_PACKAGE
        ) return

        val extras = sbn.notification?.extras ?: return
        val title   = extras.getCharSequence("android.title")?.toString()
        val text    = extras.getCharSequence("android.text")?.toString()
        val bigText = extras.getCharSequence("android.bigText")?.toString()

        Log.d(TAG, "收到通知: pkg=$pkg title=$title text=$text")

        val payment = PaymentNotificationParser.parse(
            packageName = pkg,
            title       = title,
            text        = text,
            bigText     = bigText,
            postedAt    = sbn.postTime,
        ) ?: return

        importer.importAsync(payment)
    }
}
