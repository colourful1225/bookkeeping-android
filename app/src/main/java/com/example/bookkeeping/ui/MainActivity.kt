package com.example.bookkeeping.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.lifecycleScope
import com.example.bookkeeping.ui.settings.AppSettingsManager
import com.example.bookkeeping.ui.settings.AppThemeMode
import com.example.bookkeeping.ui.theme.BookkeepingTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var appSettingsManager: AppSettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialSettings = appSettingsManager.settings.value
        AppCompatDelegate.setApplicationLocales(
            appSettingsManager.toLocaleListCompat(initialSettings.language)
        )
        val initialNightMode = when (initialSettings.themeMode) {
            AppThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(initialNightMode)

        lifecycleScope.launch {
            appSettingsManager.settings
                .map { it.language }
                .distinctUntilChanged()
                .collect { language ->
                    val newLocales = appSettingsManager.toLocaleListCompat(language)
                    if (AppCompatDelegate.getApplicationLocales() != newLocales) {
                        AppCompatDelegate.setApplicationLocales(newLocales)
                    }
                }
        }

        lifecycleScope.launch {
            appSettingsManager.settings
                .map { it.themeMode }
                .distinctUntilChanged()
                .collect { themeMode ->
                    val nightMode = when (themeMode) {
                        AppThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        AppThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                        AppThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    }
                    if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
                        AppCompatDelegate.setDefaultNightMode(nightMode)
                    }
                }
        }

        setContent {
            // 根据系统或用户设置决定是否使用暗黑模式
            val darkTheme = isSystemInDarkTheme()
            BookkeepingTheme(darkTheme = darkTheme) {
                MainScreen()
            }
        }
    }
}
