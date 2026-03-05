package com.example.bookkeeping.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.TextButton
import com.example.bookkeeping.notification.model.ConflictAlert
import java.text.SimpleDateFormat
import java.util.*

/**
 * 交易冲突确认对话框。
 *
 * 当检测到手动+自动同时记账时显示，允许用户选择：
 * - 保留：保存已有记录，丢弃自动记账
 * - 覆盖：用自动记账的信息替换
 * - 取消：不处理
 */
@Composable
fun ConflictAlertDialog(
    conflictAlert: ConflictAlert?,
    onKeep: () -> Unit,  // 保留已有
    onOverwrite: () -> Unit,  // 覆盖为自动记账
    onCancel: () -> Unit,  // 取消
) {
    if (conflictAlert == null) return

    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val existingTime = dateFormat.format(conflictAlert.existingOccurredAt)
    val autoTime = dateFormat.format(System.currentTimeMillis())

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text("⚠️ 检测到交易冲突")
        },
        text = {
            Text(
                buildString {
                    append("同笔交易被记录两次，请选择保留方式：\n\n")
                    append("【已有记录】(${existingTime})\n")
                    append("  金额：¥${conflictAlert.existingAmount / 100.0}\n")
                    append("  分类：${conflictAlert.existingCategory}\n")
                    append("  备注：${conflictAlert.existingNote}\n\n")
                    append("【自动记账】(来源：${conflictAlert.source})\n")
                    append("  金额：¥${conflictAlert.autoAmount / 100.0}\n")
                    append("  分类：${conflictAlert.autoCategory}\n")
                    append("  备注：${conflictAlert.autoNote}")
                }
            )
        },
        confirmButton = {
            Button(onClick = onOverwrite) {
                Text("覆盖（用自动记账）")
            }
        },
        dismissButton = {
            TextButton(onClick = onKeep) {
                Text("保留（已有记录）")
            }
        }
    )
}
