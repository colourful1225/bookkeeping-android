package com.example.bookkeeping.domain.usecase

import com.example.bookkeeping.data.local.dao.CategoryDao
import com.example.bookkeeping.data.local.dao.TransactionDao
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 导出所有交易记录到 CSV 格式。
 *
 * CSV 格式：amount,type,categoryId,category,date,note,syncStatus
 */
class ExportCsvUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    suspend operator fun invoke(outputStream: OutputStream) {
        val transactions = transactionDao.getAll()
        val categories = categoryDao.getAll()
        val categoryMap = categories.associateBy { it.id }

        outputStream.bufferedWriter().use { writer ->
            writer.write("\uFEFF")
            writer.write("amount,type,categoryId,category,date,note,syncStatus\n")
            
            // 写入每行数据
            transactions.forEach { tx ->
                val amountYuan = String.format(java.util.Locale.US, "%.2f", tx.amount / 100.0)
                val type = tx.type
                val categoryId = tx.categoryId
                val categoryName = categoryMap[tx.categoryId]?.name ?: tx.categoryId
                val dateStr = dateFormatter.format(Instant.ofEpochMilli(tx.occurredAt))
                val note = tx.note ?: ""
                val syncStatus = tx.syncStatus
                
                // CSV 转义：包含逗号、引号、换行的字段需加引号，引号需转义
                val escapedType = escapeField(type)
                val escapedCategoryId = escapeField(categoryId)
                val escapedCategory = escapeField(categoryName)
                val escapedNote = escapeField(note)
                val escapedSyncStatus = escapeField(syncStatus)
                
                writer.write(
                    "$amountYuan,$escapedType,$escapedCategoryId,$escapedCategory,$dateStr,$escapedNote,$escapedSyncStatus\n"
                )
            }
        }
    }

    private fun escapeField(field: String): String {
        return if (field.contains(',') || field.contains('"') || field.contains('\n')) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }
}
