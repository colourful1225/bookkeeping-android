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
 * CSV 格式：amount,category,date,note
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
            // 写入 CSV 头
            writer.write("amount,category,date,note\n")
            
            // 写入每行数据
            transactions.forEach { tx ->
                val amountYuan = "%.2f".format(tx.amount / 100.0)
                val categoryName = categoryMap[tx.categoryId]?.name ?: tx.categoryId
                val dateStr = dateFormatter.format(Instant.ofEpochMilli(tx.occurredAt))
                val note = tx.note ?: ""
                
                // CSV 转义：包含逗号、引号、换行的字段需加引号，引号需转义
                val escapedCategory = escapeField(categoryName)
                val escapedNote = escapeField(note)
                
                writer.write("$amountYuan,$escapedCategory,$dateStr,$escapedNote\n")
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
