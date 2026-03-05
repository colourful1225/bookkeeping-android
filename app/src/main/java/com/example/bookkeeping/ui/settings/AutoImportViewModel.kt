package com.example.bookkeeping.ui.settings

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import com.example.bookkeeping.notification.AutoImportSettingsManager
import com.example.bookkeeping.notification.ImportMode
import com.example.bookkeeping.notification.PaymentNotificationListenerService
import com.example.bookkeeping.notification.accessibility.PaymentAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AutoImportUiState(
    // ── 通知监听 ────────────────────────────────────────────────
    val notificationEnabled: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    // ── 读取模式 ────────────────────────────────────────────────
    val importMode: ImportMode = ImportMode.NOTIFICATION_ONLY,
    // ── 无障碍服务 ──────────────────────────────────────────────
    val accessibilityGranted: Boolean = false,
    // ── 无障碍链路调试日志 ───────────────────────────────────────
    val accessibilityTraceEnabled: Boolean = false,
)

/**
 * 自动导入设置 ViewModel。
 *
 * 管理以下状态：
 * - 通知监听开关 + 系统授权检测
 * - 读取模式（仅通知 / 通知+无障碍服务）
 * - 无障碍服务系统授权检测
 */
@HiltViewModel
class AutoImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: AutoImportSettingsManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AutoImportUiState())
    val uiState: StateFlow<AutoImportUiState> = _uiState.asStateFlow()

    init { refresh() }

    /** 刷新所有权限与开关状态（从系统设置页返回时调用） */
    fun refresh() {
        _uiState.update {
            it.copy(
                notificationEnabled           = settings.isNotificationListenerEnabled,
                notificationPermissionGranted = isNotificationListenerGranted(),
                importMode                    = settings.importMode,
                accessibilityGranted          = isAccessibilityGranted(),
                accessibilityTraceEnabled     = settings.isAccessibilityTraceEnabled,
            )
        }
    }

    fun toggleNotificationImport(enabled: Boolean) {
        settings.isNotificationListenerEnabled = enabled
        _uiState.update { it.copy(notificationEnabled = enabled) }
    }

    fun setImportMode(mode: ImportMode) {
        settings.importMode = mode
        _uiState.update { it.copy(importMode = mode) }
    }

    fun toggleAccessibilityTrace(enabled: Boolean) {
        settings.isAccessibilityTraceEnabled = enabled
        _uiState.update { it.copy(accessibilityTraceEnabled = enabled) }
    }

    // ── 权限检测 ──────────────────────────────────────────────────────────

    private fun isNotificationListenerGranted(): Boolean {
        val cn = ComponentName(context, PaymentNotificationListenerService::class.java)
        val flat = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return flat.contains(cn.flattenToString())
    }

    internal fun isAccessibilityGranted(): Boolean {
        val cn = ComponentName(context, PaymentAccessibilityService::class.java)
        val flat = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return flat.contains(cn.flattenToString())
    }

    /**
     * 获取权限启用引导页面的 Intent。
     * 用于"一键启用"按钮快速跳转。
     */
    fun getPermissionGuidanceIntent(): android.content.Intent {
        return android.content.Intent(
            context,
            com.example.bookkeeping.ui.permission.PermissionGuidanceActivity::class.java
        ).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * 检查是否所有权限都已启用。
     */
    fun areAllPermissionsGranted(): Boolean {
        return isNotificationListenerGranted() && isAccessibilityGranted()
    }
}
