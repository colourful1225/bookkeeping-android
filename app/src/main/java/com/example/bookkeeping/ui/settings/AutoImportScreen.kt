package com.example.bookkeeping.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.bookkeeping.R
import com.example.bookkeeping.notification.ImportMode

/**
 * 自动记账功能设置页面。
 *
 * 包含：
 * 1. 微信/支付宝通知监听开关（NotificationListenerService）
 * 2. **读取模式选择**：仅通知 / 通知+无障碍服务综合读取
 * 3. 无障碍服务授权入口（选择综合模式时显示）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoImportScreen(
    viewModel: AutoImportViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    val notifSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.refresh() }

    val accessibilityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auto_import_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.button_back),
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
            // ── 一键启用按钮（快速路径） ──────────────────────────────
            if (!uiState.notificationPermissionGranted || !uiState.accessibilityGranted) {
                QuickEnableButton(
                    permissionGuideIntent = viewModel.getPermissionGuidanceIntent(),
                )
            }

            // ── 功能说明 ──────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.section_explain), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.auto_import_explain_text),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // ── 通知监听开关 ──────────────────────────────────────────────
            SectionTitle(stringResource(R.string.auto_import_section_wechat_alipay))
            AutoImportToggleCard(
                label             = stringResource(R.string.auto_import_notification_label),
                description       = stringResource(R.string.auto_import_notification_desc),
                checked           = uiState.notificationEnabled,
                onCheckedChange   = { viewModel.toggleNotificationImport(it) },
                permissionGranted = uiState.notificationPermissionGranted,
                onGrantPermission = {
                    notifSettingsLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
            )

            // ── 读取模式 ──────────────────────────────────────────────────
            SectionTitle(stringResource(R.string.auto_import_section_import_mode))
            ImportModeCard(
                selectedMode      = uiState.importMode,
                onModeSelected    = { viewModel.setImportMode(it) },
                accessibilityGranted = uiState.accessibilityGranted,
                onGrantAccessibility = {
                    accessibilityLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
            )

            SectionTitle(stringResource(R.string.auto_import_section_accessibility_debug))
            TraceToggleCard(
                checked = uiState.accessibilityTraceEnabled,
                onCheckedChange = { viewModel.toggleAccessibilityTrace(it) },
            )

            // ── 隐私声明 ──────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.privacy_notice),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── 读取模式选择卡片 ──────────────────────────────────────────────────────────

/**
 * 读取模式选择卡片（RadioButton 组）。
 *
 * - 仅通知：权限需求最小
 * - 通知+无障碍服务：信息更完整，双源去重
 */
@Composable
private fun ImportModeCard(
    selectedMode: ImportMode,
    onModeSelected: (ImportMode) -> Unit,
    accessibilityGranted: Boolean,
    onGrantAccessibility: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 模式 1：仅通知
            ModeRadioRow(
                label       = stringResource(R.string.label_notification_only),
                description = stringResource(R.string.desc_notification_only),
                selected    = selectedMode == ImportMode.NOTIFICATION_ONLY,
                onClick     = { onModeSelected(ImportMode.NOTIFICATION_ONLY) },
            )

            // 模式 2：通知 + 无障碍服务
            ModeRadioRow(
                label       = stringResource(R.string.label_notification_and_accessibility),
                description = stringResource(R.string.desc_notification_and_accessibility),
                selected    = selectedMode == ImportMode.NOTIFICATION_AND_ACCESSIBILITY,
                onClick     = { onModeSelected(ImportMode.NOTIFICATION_AND_ACCESSIBILITY) },
            )

            // 无障碍授权提示（仅 NOTIFICATION_AND_ACCESSIBILITY 模式下显示）
            if (selectedMode == ImportMode.NOTIFICATION_AND_ACCESSIBILITY) {
                Spacer(modifier = Modifier.height(4.dp))
                if (!accessibilityGranted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.warn_accessibility_required),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onGrantAccessibility) { Text(stringResource(R.string.button_open)) }
                    }
                    Text(
                        stringResource(R.string.tip_accessibility),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        stringResource(R.string.msg_accessibility_granted),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeRadioRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── 基础组件 ──────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
}

@Composable
private fun AutoImportToggleCard(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    permissionGranted: Boolean,
    onGrantPermission: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, fontWeight = FontWeight.Medium)
                    Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }

            if (!permissionGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.warn_permission_not_granted), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onGrantPermission) {
                        Text(stringResource(R.string.auto_import_grant_permission))
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "✓ ${stringResource(R.string.auto_import_permission_granted)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun TraceToggleCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.label_accessibility_trace), fontWeight = FontWeight.Medium)
                    Text(
                        stringResource(R.string.desc_accessibility_trace),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.tip_trace_debug),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 一键启用权限按钮。
 *
 * 提供快速跳转到权限引导页面的入口，简化 4 步为 1 步。
 */
@Composable
private fun QuickEnableButton(permissionGuideIntent: Intent) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "权限启用进度: 差一步就可以开始",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { context.startActivity(permissionGuideIntent) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("一键启用权限", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

