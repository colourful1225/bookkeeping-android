package com.example.bookkeeping.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookkeeping.data.remote.CsvImportResult
import com.example.bookkeeping.domain.usecase.ExportCsvUseCase
import com.example.bookkeeping.domain.usecase.ExportDebugLogUseCase
import com.example.bookkeeping.domain.usecase.ImportCsvUseCase
import com.example.bookkeeping.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置屏幕的 ViewModel。
 *
 * 管理 CSV 导入/导出状态。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val importCsvUseCase: ImportCsvUseCase,
    private val exportCsvUseCase: ExportCsvUseCase,
    private val exportDebugLogUseCase: ExportDebugLogUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    data class UiState(
        val isImporting: Boolean = false,
        val importResult: CsvImportResult? = null,
        val importError: String? = null,
        val isExporting: Boolean = false,
        val exportSuccess: Boolean = false,
        val exportSuccessSource: String? = null,
        val exportError: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * 导入 CSV 文件。
     *
     * @param uri 文件 URI
     */
    fun importCsvFile(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            isImporting = true,
            importError = null,
            importResult = null,
        )

        viewModelScope.launch {
            try {
                val result = importCsvUseCase(uri)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importResult = result,
                )
            } catch (e: Exception) {
                val detail = e.message ?: context.getString(R.string.error_unknown)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importError = context.getString(R.string.error_import_failed, detail),
                )
            }
        }
    }

    /**
     * 清除导入结果。
     */
    fun clearImportResult() {
        _uiState.value = _uiState.value.copy(
            importResult = null,
            importError = null,
        )
    }

    fun exportCsvFile(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            isExporting = true,
            exportSuccess = false,
            exportSuccessSource = null,
            exportError = null,
        )

        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    exportCsvUseCase(outputStream)
                } ?: error("openOutputStream returned null")
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = true,
                    exportSuccessSource = context.getString(R.string.export_success_data),
                )
            } catch (e: Exception) {
                val detail = e.message ?: context.getString(R.string.error_unknown)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportError = context.getString(R.string.error_export_failed, detail),
                )
            }
        }
    }

    fun exportDebugLogFile(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            isExporting = true,
            exportSuccess = false,
            exportSuccessSource = null,
            exportError = null,
        )

        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    exportDebugLogUseCase(outputStream)
                } ?: error("openOutputStream returned null")
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = true,
                    exportSuccessSource = context.getString(R.string.export_success_log),
                )
            } catch (e: Exception) {
                val detail = e.message ?: context.getString(R.string.error_unknown)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportError = context.getString(R.string.error_export_log_failed, detail),
                )
            }
        }
    }

    /**
     * 清除导出结果。
     */
    fun clearExportResult() {
        _uiState.value = _uiState.value.copy(
            exportSuccess = false,
            exportSuccessSource = null,
            exportError = null,
        )
    }
}
