package com.example.bookkeeping.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AppLanguage(val tag: String) {
    ZH("zh"),
    EN("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: ZH
    }
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.ZH,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
)

@Singleton
class AppSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME_MODE = "theme_mode"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun updateLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.tag).apply()
        _settings.value = _settings.value.copy(language = language)
    }

    fun updateThemeMode(themeMode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
        _settings.value = _settings.value.copy(themeMode = themeMode)
    }

    fun toLocaleListCompat(language: AppLanguage): LocaleListCompat = when (language) {
        AppLanguage.ZH -> LocaleListCompat.forLanguageTags("zh")
        AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
    }

    private fun loadSettings(): AppSettings {
        val language = AppLanguage.fromTag(prefs.getString(KEY_LANGUAGE, AppLanguage.ZH.tag))
        val themeMode = runCatching {
            AppThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
        }.getOrDefault(AppThemeMode.SYSTEM)
        return AppSettings(language = language, themeMode = themeMode)
    }
}
