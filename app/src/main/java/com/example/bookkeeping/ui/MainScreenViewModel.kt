package com.example.bookkeeping.ui

import androidx.lifecycle.ViewModel
import com.example.bookkeeping.notification.PaymentAutoImporter
import com.example.bookkeeping.notification.model.ConflictAlert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * MainScreen 的 ViewModel。
 *
 * 职责：
 * 1. 通过 DI 获取 PaymentAutoImporter Singleton。
 * 2. 代理 conflictAlertFlow，使 MainScreen 能监听冲突事件。
 * 3. 提供处理用户冲突对话框选择的方法。
 */
@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val paymentAutoImporter: PaymentAutoImporter,
) : ViewModel() {

    /** 冲突信息流（代理自 PaymentAutoImporter） */
    val conflictAlertFlow: StateFlow<ConflictAlert?> = paymentAutoImporter.conflictAlertFlow

    /** 用户选择"保留现有交易" */
    fun onConflictKeepExisting() {
        paymentAutoImporter.confirmKeepExisting()
    }

    /** 用户选择"覆盖现有交易" */
    fun onConflictOverwrite(existingTxId: String) {
        paymentAutoImporter.confirmOverwrite(existingTxId)
    }

    /** 用户选择"取消导入" */
    fun onConflictCancel() {
        paymentAutoImporter.confirmCancel()
    }
}
