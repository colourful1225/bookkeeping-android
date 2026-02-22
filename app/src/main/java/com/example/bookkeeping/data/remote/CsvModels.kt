package com.example.bookkeeping.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * CSV 导入行数据模型。
 *
 * 支持通用 CSV 格式，包含以下字段：
 * - [amount]：金额（必需）
 * - [category]：分类名称（可选，默认为"其他"）
 * - [date]：日期（可选，格式 "yyyy-MM-dd HH:mm:ss" 或 "yyyy-MM-dd"）
 * - [note]：备注（可选）
 */
@JsonClass(generateAdapter = false)
data class CsvImportRow(
    @Json(name = "amount")
    val amount: String? = null,
    
    @Json(name = "category")
    val category: String? = null,
    
    @Json(name = "date")
    val date: String? = null,
    
    @Json(name = "note")
    val note: String? = null,
)

/**
 * CSV 导入结果。
 */
data class CsvImportResult(
    val successCount: Int,
    val failureCount: Int,
    val errors: List<String>,
    val importedRowCount: Int,
)
