package com.example.bookkeeping.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookkeeping.data.remote.CsvImportResult
import com.example.bookkeeping.domain.usecase.ExportCsvUseCase
import com.example.bookkeeping.domain.usecase.ImportCsvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream
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
) : ViewModel() {

    data class UiState(
        val isImporting: Boolean = false,
        val importResult: CsvImportResult? = null,
        val importError: String? = null,
        val isExporting: Boolean = false,
        val exportSuccess: Boolean = false,
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
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importError = "导入失败: ${e.message ?: "未知错误"}",
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

    /**
     * 导出 CSV 文件。
     *
     * @param outputStream 输出流
     */
    fun exportCsvFile(outputStream: OutputStream) {
        _uiState.value = _uiState.value.copy(
            isExporting = true,
            exportSuccess = false,
            exportError = null,
        )

        viewModelScope.launch {
            try {
                exportCsvUseCase(outputStream)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = true,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportError = "导出失败: ${e.message ?: "未知错误"}",
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
            exportError = null,
        )
    }
}
