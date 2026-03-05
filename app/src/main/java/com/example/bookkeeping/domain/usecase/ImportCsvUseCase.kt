package com.example.bookkeeping.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.bookkeeping.data.local.AppDatabase
import com.example.bookkeeping.data.local.entity.OutboxOpEntity
import com.example.bookkeeping.data.local.entity.OutboxStatus
import com.example.bookkeeping.data.local.entity.OpType
import com.example.bookkeeping.data.local.entity.TransactionEntity
import com.example.bookkeeping.data.remote.CsvImportResult
import com.example.bookkeeping.data.util.CategoryMatcher
import com.example.bookkeeping.data.util.CsvParser
import com.example.bookkeeping.data.util.ExcelParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.provider.OpenableColumns
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * CSV 文件导入 UseCase。
 *
 * 职责：
 * - 读取 CSV 文件
 * - 解析行数据并验证
 * - 映射到 TransactionEntity
 * - 批量入库
 */
class ImportCsvUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
) {
    /**
     * 执行 CSV 导入。
     *
     * @param uri 文件 URI
     * @return 导入结果（成功数、失败数、错误列表）
     */
    suspend operator fun invoke(uri: Uri): CsvImportResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        val transactions = mutableListOf<TransactionEntity>()
        val outboxOps = mutableListOf<OutboxOpEntity>()

        try {
            // 打开文件流
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("无法打开文件: $uri")

            if (isLegacyExcelFile(uri)) {
                throw IllegalArgumentException("暂不支持 .xls，请另存为 .xlsx 后导入")
            }

            // 解析 CSV/Excel
            val rows = inputStream.use { stream ->
                if (isExcelFile(uri)) {
                    ExcelParser.parse(stream)
                } else {
                    CsvParser.parse(stream)
                }
            }

            // 获取所有分类（用于匹配）
            val categories = db.categoryDao().getAll()

            val now = System.currentTimeMillis()

            // 逐行处理
            rows.forEachIndexed { index, row ->
                try {
                    val data = CsvParser.mapRowToImportData(row)
                    val tx = parseRowToTransaction(
                        data = data,
                        categories = categories,
                        now = now,
                        rowIndex = index + 2, // +2 因为 1 是头行，计数从 1 开始
                    )
                    
                    transactions.add(tx)

                    // 创建对应的 outbox 记录
                    outboxOps.add(
                        OutboxOpEntity(
                            opId = UUID.randomUUID().toString(),
                            entityId = tx.id,
                            opType = OpType.CREATE,
                            payloadJson = "", // 可选，用于存储原始数据快照
                            idempotencyKey = "tx-create-${tx.id}",
                            retryCount = 0,
                            nextRetryAt = now,
                            status = OutboxStatus.PENDING,
                            createdAt = now,
                        )
                    )
                } catch (e: Throwable) {
                    errors.add("行 ${index + 2}: ${e.message ?: "未知错误"}")
                }
            }

            // 批量入库（事务）
            if (transactions.isNotEmpty()) {
                db.withTransaction {
                    db.transactionDao().insertAll(transactions)
                    db.outboxDao().insertAll(outboxOps)
                }
            }

            CsvImportResult(
                successCount = transactions.size,
                failureCount = errors.size,
                errors = errors,
                importedRowCount = rows.size,
            )
        } catch (e: Throwable) {
            errors.add("导入失败: ${e.message ?: "未知错误"}")
            CsvImportResult(
                successCount = 0,
                failureCount = 1,
                errors = errors,
                importedRowCount = 0,
            )
        }
    }

    private fun isExcelFile(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") {
            return true
        }

        val displayName = try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (_: Exception) {
            null
        }

        return displayName?.lowercase(Locale.ROOT)?.endsWith(".xlsx") == true
    }

    private fun isLegacyExcelFile(uri: Uri): Boolean {
        val displayName = try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (_: Exception) {
            null
        }
        return displayName?.lowercase(Locale.ROOT)?.endsWith(".xls") == true
    }

    /**
     * 解析 CSV 行为 TransactionEntity。
     *
     * @param data 映射后的行数据
     * @param categoryMap 分类映射表
     * @param now 当前时间
     * @param rowIndex 行号（用于错误信息）
     */
    private suspend fun parseRowToTransaction(
        data: Map<String, String?>,
        categories: List<com.example.bookkeeping.data.local.entity.CategoryEntity>,
        now: Long,
        rowIndex: Int,
    ): TransactionEntity {
        // 解析金额
        val amountStr = data["amount"]?.trim()
            ?: throw IllegalArgumentException("金额字段缺失或为空")
        
        val normalizedAmount = amountStr
            .replace("¥", "")
            .replace(",", "")
            .trim()

        // 支持 "100", "100.50" 等格式，转换为分（长整数）
        val amountDouble = normalizedAmount.toDoubleOrNull()
            ?: throw IllegalArgumentException("金额 '$amountStr' 格式无效")
        
        val amount = (amountDouble * 100).toLong()
        if (amount <= 0) {
            throw IllegalArgumentException("金额必须大于 0，当前值: $amountStr")
        }

        // 备注
        val note = data["note"]?.trim()?.takeIf { it.isNotEmpty() }

        // 解析分类（优先分类字段，其次子类，再用备注做匹配提示）
        val categoryIdFromFile = data["categoryId"]?.trim().orEmpty()
        val categoryName = data["category"]?.trim().orEmpty()
        val subcategoryName = data["subcategory"]?.trim().orEmpty()
        val categoryId = when {
            categoryIdFromFile.isNotBlank() && categories.any { it.id == categoryIdFromFile } -> categoryIdFromFile
            else -> {
                val categoryHint = when {
                    categoryName.isNotBlank() -> categoryName
                    subcategoryName.isNotBlank() -> subcategoryName
                    else -> note.orEmpty()
                }
                CategoryMatcher.matchCategoryName(categoryHint, categories)
            }
        }

        // 解析日期
        val occurredAt = parseDate(data["date"]) ?: now

        // 解析收支类型
        val type = parseType(data["type"]) ?: "EXPENSE"

        return TransactionEntity(
            id = UUID.randomUUID().toString(),
            amount = amount,
            type = type,
            categoryId = categoryId,
            note = note,
            occurredAt = occurredAt,
            updatedAt = now,
        )
    }

    private fun parseType(rawType: String?): String? {
        if (rawType.isNullOrBlank()) return null
        val normalized = rawType.trim().lowercase(Locale.CHINA)
        return when {
            normalized.contains("收入") || normalized == "income" -> "INCOME"
            normalized.contains("支出") || normalized == "expense" -> "EXPENSE"
            else -> null
        }
    }

    /**
     * 解析日期字符串。
     *
     * 支持格式：
     * - "yyyy-MM-dd HH:mm:ss"
     * - "yyyy-MM-dd"
     */
    private fun parseDate(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null

        return try {
            val formats = listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy/MM/dd",
            )

            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.CHINA)
                    return sdf.parse(dateStr.trim())?.time
                } catch (e: Exception) {
                    // 继续尝试下一个格式
                }
            }

            null
        } catch (e: Exception) {
            null
        }
    }
}
