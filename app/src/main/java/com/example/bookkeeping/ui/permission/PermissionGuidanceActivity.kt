package com.example.bookkeeping.ui.permission

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookkeeping.R
import com.example.bookkeeping.notification.PaymentNotificationListenerService
import com.example.bookkeeping.notification.accessibility.PaymentAccessibilityService
import com.example.bookkeeping.ui.theme.BookkeepingTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 权限一键启用引导页面。
 *
 * 职责：
 * 1. 检测所需权限状态（通知监听、无障碍服务）
 * 2. 引导用户顺序启用权限
 * 3. 自动检测权限变化，更新进度
 * 4. 完成后自动返回/跳回主页
 *
 * 使用场景：
 * - 用户点击"一键启用"时从 AutoImportScreen 跳转至此
 * - 逐步教学、即时反馈权限状态
 */
@AndroidEntryPoint
class PermissionGuidanceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookkeepingTheme {
                PermissionGuidanceScreen(
                    context = this,
                    onBack = { finish() },
                    onAllPermissionsGranted = { finish() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("PermissionGuidanceActivity", "onResume: 检查权限状态是否发生变化")
        // 用户从系统设置返回后，自动检测权限
        // 由于 Compose 中使用了 remember 存储状态，需要触发重新组合
        // 这里仅记录日志，实际状态刷新由 Composable 中的 onResume 触发
    }
}

/**
 * 权限引导屏幕组件。
 *
 * 显示权限检查列表，让用户逐个启用权限。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionGuidanceScreen(
    context: android.content.Context,
    onBack: () -> Unit,
    onAllPermissionsGranted: () -> Unit,
) {
    var notificationGranted by remember { mutableStateOf(isNotificationListenerGranted(context)) }
    var accessibilityGranted by remember { mutableStateOf(isAccessibilityServiceGranted(context)) }

    val allGranted = notificationGranted && accessibilityGranted
    val progressPercent = if (allGranted) 100f else if (notificationGranted || accessibilityGranted) 50f else 0f

    // ── 权限监听：每次返回或焦点变化时刷新状态 ────────────────────────────
    LaunchedEffect(Unit) {
        while (true) {
            // 每 2 秒检测一次权限状态（用户可能在系统设置中）
            kotlinx.coroutines.delay(2000)
            val newNotificationGranted = isNotificationListenerGranted(context)
            val newAccessibilityGranted = isAccessibilityServiceGranted(context)
            
            if (newNotificationGranted != notificationGranted) {
                Log.d("PermissionGuidance", "通知权限状态变化: $notificationGranted -> $newNotificationGranted")
                notificationGranted = newNotificationGranted
            }
            if (newAccessibilityGranted != accessibilityGranted) {
                Log.d("PermissionGuidance", "无障碍权限状态变化: $accessibilityGranted -> $newAccessibilityGranted")
                accessibilityGranted = newAccessibilityGranted
            }

            // 权限全部启用时自动返回
            if (newNotificationGranted && newAccessibilityGranted && !allGranted) {
                Log.d("PermissionGuidance", "权限已全部启用，3 秒后自动返回")
                kotlinx.coroutines.delay(3000)
                onAllPermissionsGranted()
                break
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("一键启用权限") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── 进度条 ──────────────────────────────────────────────────
            Text("权限启用进度", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            LinearProgressIndicator(
                progress = { progressPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "${progressPercent.toInt()}%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── 权限 1: 通知监听 ─────────────────────────────────────────
            PermissionCard(
                title = "通知监听权限",
                description = "获取支付/转账通知，自动记账",
                granted = notificationGranted,
                onRequest = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    // 返回时刷新状态
                    notificationGranted = isNotificationListenerGranted(context)
                },
            )

            // ── 权限 2: 无障碍服务 ───────────────────────────────────────
            PermissionCard(
                title = "无障碍服务权限",
                description = "提高数据捕获覆盖率，支持更多应用",
                granted = accessibilityGranted,
                onRequest = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    // 返回时刷新状态
                    accessibilityGranted = isAccessibilityServiceGranted(context)
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── 完成提示 ──────────────────────────────────────────────────
            if (allGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "✓ 所有权限已启用！",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "自动记账功能已就绪，立即开始享受便捷记账",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "提示",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "请按照下方步骤启用权限。系统设置打开后，请返回本页面继续",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 返回按钮 ──────────────────────────────────────────────────
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = allGranted,
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(if (allGranted) "返回设置" else "权限未完成")
            }
        }
    }
}

/**
 * 单个权限卡片。
 */
@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Checkbox(checked = granted, onCheckedChange = null)
            }

            // 启用按钮或已启用提示
            if (!granted) {
                Button(
                    onClick = onRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("启用权限", fontSize = 12.sp)
                }
            } else {
                Text(
                    "✓ 已启用",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ── 权限检测工具函数 ────────────────────────────────────────────

private fun isNotificationListenerGranted(context: android.content.Context): Boolean {
    val cn = ComponentName(context, PaymentNotificationListenerService::class.java)
    val flat = Settings.Secure.getString(
        context.contentResolver, "enabled_notification_listeners"
    ) ?: return false
    return flat.contains(cn.flattenToString())
}

private fun isAccessibilityServiceGranted(context: android.content.Context): Boolean {
    return try {
        val cn = ComponentName(context, PaymentAccessibilityService::class.java)
        val flat = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        flat.contains(cn.flattenToString())
    } catch (e: Exception) {
        false
    }
}
