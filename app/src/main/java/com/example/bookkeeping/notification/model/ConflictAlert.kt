package com.example.bookkeeping.notification.model

/**
 * 交易冲突提醒信息。
 *
 * 用于在 UI 层展示冲突对话框，让用户选择保留/覆盖。
 */
data class ConflictAlert(
    /** 冲突的现有交易 ID */
    val existingTxId: String,

    /** 冲突的现有交易金额（分） */
    val existingAmount: Long,

    /** 冲突的现有交易备注 */
    val existingNote: String,

    /** 冲突的现有交易分类 */
    val existingCategory: String,

    /** 冲突的现有交易发生时间 */
    val existingOccurredAt: Long,

    /** 即将入账的自动记账金额（分） */
    val autoAmount: Long,

    /** 即将入账的自动记账备注 */
    val autoNote: String,

    /** 即将入账的自动记账分类 */
    val autoCategory: String,

    /** 来源：微信/支付宝/短信 */
    val source: String,
)
