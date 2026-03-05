package com.example.bookkeeping.notification

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自动导入功能的开关与模式管理器（基于 SharedPreferences）。
 *
 * ## 读取模式（[importMode]）
 * | 模式                              | 说明                                         |
 * |-----------------------------------|----------------------------------------------|
 * | [ImportMode.NOTIFICATION_ONLY]             | 仅读取通知（默认）                 |
 * | [ImportMode.NOTIFICATION_AND_ACCESSIBILITY]| 通知 + 无障碍服务读取微信/支付宝支付成功页   |
 *
 * 无论哪种模式，通知监听（NotificationListenerService）始终可单独开关。
 *
 * 注意：开关仅控制本应用逻辑，底层系统授权需用户在系统设置中单独授予。
 */
@Singleton
class AutoImportSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_NAME               = "auto_import_settings"
        private const val KEY_NOTIFICATION_ENABLED  = "notification_listener_enabled"
        private const val KEY_IMPORT_MODE           = "import_mode"
        private const val KEY_A11Y_TRACE_ENABLED    = "a11y_trace_enabled"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** 通知监听开关（默认 false，须用户主动开启） */
    var isNotificationListenerEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, value).apply() }

    /**
     * 自动导入模式（默认 [ImportMode.NOTIFICATION_ONLY]）。
     *
     * 切换为 [ImportMode.NOTIFICATION_AND_ACCESSIBILITY] 时，系统无障碍服务也需用户手动授权。
     */
    var importMode: ImportMode
        get() = ImportMode.valueOf(
            prefs.getString(KEY_IMPORT_MODE, ImportMode.NOTIFICATION_ONLY.name)
                ?: ImportMode.NOTIFICATION_ONLY.name
        )
        set(value) { prefs.edit().putString(KEY_IMPORT_MODE, value.name).apply() }

    /**
     * 无障碍服务逻辑开关。
      * 等同于 [importMode] == [ImportMode.NOTIFICATION_AND_ACCESSIBILITY]。
     */
    val isAccessibilityEnabled: Boolean
          get() = importMode == ImportMode.NOTIFICATION_AND_ACCESSIBILITY

    /** 无障碍 → 入账链路调试日志开关（默认 false） */
    var isAccessibilityTraceEnabled: Boolean
        get() = prefs.getBoolean(KEY_A11Y_TRACE_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_A11Y_TRACE_ENABLED, value).apply() }
}

/**
 * 自动记账读取模式。
 */
enum class ImportMode {
    /** 仅通过通知捕获消费记录 */
    NOTIFICATION_ONLY,

    /**
     * 通知 + 无障碍服务综合读取：
     * - 支付通知作为基础来源
     * - 无障碍服务读取微信/支付宝支付成功页，获取完整商户和金额信息
     * - 两者结果经 ReconciliationEngine 对账去重
     */
    NOTIFICATION_AND_ACCESSIBILITY,
}

