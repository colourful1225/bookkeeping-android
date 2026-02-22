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
import com.example.bookkeeping.data.util.CsvParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

            // 解析 CSV
            val rows = CsvParser.parse(inputStream)
            inputStream.close()

            // 获取所有分类映射
            val categoryMap = db.categoryDao().getAll().associateBy { it.name }

            val now = System.currentTimeMillis()

            // 逐行处理
            rows.forEachIndexed { index, row ->
                try {
                    val data = CsvParser.mapRowToImportData(row)
                    val tx = parseRowToTransaction(
                        data = data,
                        categoryMap = categoryMap,
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
                } catch (e: Exception) {
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
        } catch (e: Exception) {
            errors.add("导入失败: ${e.message ?: "未知错误"}")
            CsvImportResult(
                successCount = 0,
                failureCount = 1,
                errors = errors,
                importedRowCount = 0,
            )
        }
    }

    /**
     * 解析 CSV 行为 TransactionEntity。
     *
     * @param data 映射后的行数据
     * @param categoryMap 分类映射表
     * @param now 当前时间
     * @param rowIndex 行号（用于错误信息）
     */
    private fun parseRowToTransaction(
        data: Map<String, String?>,
        categoryMap: Map<String, com.example.bookkeeping.data.local.entity.CategoryEntity>,
        now: Long,
        rowIndex: Int,
    ): TransactionEntity {
        // 解析金额
        val amountStr = data["amount"]?.trim()
            ?: throw IllegalArgumentException("金额字段缺失或为空")
        
        // 支持 "100", "100.50" 等格式，转换为分（长整数）
        val amountDouble = amountStr.toDoubleOrNull()
            ?: throw IllegalArgumentException("金额 '$amountStr' 格式无效")
        
        val amount = (amountDouble * 100).toLong()
        if (amount <= 0) {
            throw IllegalArgumentException("金额必须大于 0，当前值: $amountStr")
        }

        // 解析分类
        val categoryName = data["category"]?.trim() ?: "others"
        val categoryId = categoryMap[categoryName]?.id
            ?: run {
                // 如果分类不存在，尝试模糊匹配
                categoryMap.entries.find { 
                    it.value.name.contains(categoryName, ignoreCase = true) 
                }?.value?.id ?: "others"
            }

        // 解析日期
        val occurredAt = parseDate(data["date"]) ?: now

        // 备注
        val note = data["note"]?.trim()?.takeIf { it.isNotEmpty() }

        return TransactionEntity(
            id = UUID.randomUUID().toString(),
            amount = amount,
            type = "EXPENSE",
            categoryId = categoryId,
            note = note,
            occurredAt = occurredAt,
            updatedAt = now,
        )
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
