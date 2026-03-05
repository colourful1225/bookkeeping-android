package com.example.bookkeeping.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val appSettingsManager: AppSettingsManager,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = appSettingsManager.settings

    fun updateLanguage(language: AppLanguage) {
        appSettingsManager.updateLanguage(language)
    }

    fun updateThemeMode(themeMode: AppThemeMode) {
        appSettingsManager.updateThemeMode(themeMode)
    }
}
