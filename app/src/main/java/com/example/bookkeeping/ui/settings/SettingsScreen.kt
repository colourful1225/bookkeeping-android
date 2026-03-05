package com.example.bookkeeping.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bookkeeping.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onCategoryManage: () -> Unit = {},
    onBudgetManage: () -> Unit = {},
    onAutoImport: () -> Unit = {},
    onGeneralSettings: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    // 导入文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importCsvFile(uri)
        }
    }

    // 导出文件选择器
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsvFile(uri)
        }
    }

    val exportLogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportDebugLogFile(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontFamily = systemDefaultFontFamily()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle(stringResource(R.string.section_auto_import))
            SettingsCard {
                SettingsItem(
                    title = stringResource(R.string.setting_auto_import),
                    subtitle = stringResource(R.string.setting_auto_import_desc),
                    onClick = onAutoImport
                )
                DividerLine()
                SettingsItem(title = stringResource(R.string.setting_category_management), onClick = onCategoryManage)
                DividerLine()
                SettingsItem(title = stringResource(R.string.setting_budget_management), onClick = onBudgetManage)
            }

            SectionTitle(stringResource(R.string.section_general))
            SettingsCard {
                SettingsItem(
                    title = stringResource(R.string.general_title),
                    subtitle = "${stringResource(R.string.general_language_title)} / ${stringResource(R.string.general_theme_title)}",
                    onClick = onGeneralSettings,
                )
            }

            SectionTitle(stringResource(R.string.section_import_data))
            SettingsCard {
                SettingsItem(
                    title = stringResource(R.string.setting_import_data),
                    subtitle = stringResource(R.string.setting_import_data_desc),
                    onClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "text/csv",
                                "text/*",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                            )
                        )
                    },
                )
                DividerLine()
                SettingsItem(
                    title = stringResource(R.string.setting_export_data),
                    subtitle = stringResource(R.string.setting_export_data_desc),
                    onClick = { exportLauncher.launch("bookkeeping_export_${System.currentTimeMillis()}.csv") }
                )
                DividerLine()
                SettingsItem(
                    title = stringResource(R.string.setting_export_log),
                    subtitle = stringResource(R.string.setting_export_log_desc),
                    onClick = { exportLogLauncher.launch("bookkeeping_debug_${System.currentTimeMillis()}.log") },
                )
            }

            if (uiState.isImporting || uiState.isExporting) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            uiState.importResult?.let { result ->
                ImportResultCard(result)
            }

            if (uiState.exportSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Text(
                        "${uiState.exportSuccessSource ?: stringResource(R.string.export_success_default)}${stringResource(R.string.export_success_suffix)}",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = systemDefaultFontFamily(),
                    )
                }
            }

            uiState.importError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = systemDefaultFontFamily(),
                    )
                }
            }

            uiState.exportError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = systemDefaultFontFamily(),
                    )
                }
            }

            SectionTitle(stringResource(R.string.section_about))
            SettingsCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.app_name_version),
                        fontFamily = systemDefaultFontFamily(),
                    )
                    Text(
                        stringResource(R.string.app_description),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = systemDefaultFontFamily(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = systemDefaultFontFamily())
}

private fun systemDefaultFontFamily(): FontFamily {
    return FontFamily.Default  // 使用系统默认字体，自动缩放中文支持
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        content()
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(title, color = textColor, fontFamily = systemDefaultFontFamily())
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = systemDefaultFontFamily(),
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = textColor,
        )
    }
}

@Composable
private fun DividerLine() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun ImportResultCard(result: com.example.bookkeeping.data.remote.CsvImportResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.import_result_title), fontWeight = FontWeight.Bold, fontFamily = systemDefaultFontFamily())
            
            Text(
                stringResource(R.string.import_result_success, result.successCount),
                fontSize = 14.sp,
                color = Color(0xFF4CAF50),
                fontFamily = systemDefaultFontFamily(),
            )

            if (result.failureCount > 0) {
                Text(
                    stringResource(R.string.import_result_failure, result.failureCount),
                    fontSize = 14.sp,
                    color = Color(0xFFF44336),
                    fontFamily = systemDefaultFontFamily(),
                )
            }

            Text(
                stringResource(R.string.import_result_total, result.importedRowCount),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = systemDefaultFontFamily(),
            )

            if (result.errors.isNotEmpty()) {
                Text(stringResource(R.string.import_result_errors), fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = systemDefaultFontFamily())
                result.errors.take(3).forEach { error ->
                    Text(
                        "• $error",
                        fontSize = 11.sp,
                        color = Color(0xFFF44336),
                        fontFamily = systemDefaultFontFamily(),
                    )
                }
                if (result.errors.size > 3) {
                    Text(
                        stringResource(R.string.import_result_more_errors, result.errors.size - 3),
                        fontSize = 11.sp,
                        fontFamily = systemDefaultFontFamily(),
                    )
                }
            }
        }
    }
}
