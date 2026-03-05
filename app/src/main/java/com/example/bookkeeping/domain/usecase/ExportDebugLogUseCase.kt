package com.example.bookkeeping.domain.usecase

import android.os.Build
import com.example.bookkeeping.BuildConfig
import com.example.bookkeeping.data.local.dao.OutboxDao
import com.example.bookkeeping.data.local.dao.TransactionDao
import com.example.bookkeeping.notification.AutoImportSettingsManager
import com.example.bookkeeping.ui.settings.AppSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ExportDebugLogUseCase @Inject constructor(
    private val transactionDao: TransactionDao,
    private val outboxDao: OutboxDao,
    private val autoImportSettingsManager: AutoImportSettingsManager,
    private val appSettingsManager: AppSettingsManager,
) {
    suspend operator fun invoke(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val transactions = transactionDao.getAll()
        val pendingOutboxCount = outboxDao.pendingCount()
        val appSettings = appSettingsManager.settings.value

        outputStream.bufferedWriter().use { writer ->
            writer.appendLine("# Bookkeeping Debug Log")
            writer.appendLine("generatedAt=${sdf.format(Date(now))}")
            writer.appendLine("appVersion=${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})")
            writer.appendLine("buildType=${BuildConfig.BUILD_TYPE}")
            writer.appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
            writer.appendLine("android=${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            writer.appendLine("language=${appSettings.language.tag}")
            writer.appendLine("theme=${appSettings.themeMode.name}")
            writer.appendLine("autoImport.notificationEnabled=${autoImportSettingsManager.isNotificationListenerEnabled}")
            writer.appendLine("autoImport.importMode=${autoImportSettingsManager.importMode.name}")
            writer.appendLine("autoImport.accessibilityEnabled=${autoImportSettingsManager.isAccessibilityEnabled}")
            writer.appendLine("transactions.count=${transactions.size}")
            writer.appendLine("outbox.pendingCount=$pendingOutboxCount")
            writer.appendLine()
            writer.appendLine("## latest_transactions")
            transactions.take(30).forEach { tx ->
                writer.appendLine(
                    "${tx.id},${tx.type},${tx.amount},${tx.categoryId},${sdf.format(Date(tx.occurredAt))},${tx.syncStatus},${tx.note.orEmpty()}"
                )
            }
        }
    }
}
