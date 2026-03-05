package com.example.bookkeeping.ui.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_NAME = "budget_settings"
        private const val KEY_BUDGET_BY_CATEGORY = "budget_by_category"
        private const val KEY_BUDGET_ALERT_ENABLED = "budget_alert_enabled"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getBudgetMap(): Map<String, Int> {
        val raw = prefs.getString(KEY_BUDGET_BY_CATEGORY, "{}") ?: "{}"
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    put(key, json.optInt(key, 0).coerceAtLeast(0))
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun setBudget(categoryId: String, budget: Int) {
        val current = JSONObject(prefs.getString(KEY_BUDGET_BY_CATEGORY, "{}") ?: "{}")
        current.put(categoryId, budget.coerceAtLeast(0))
        prefs.edit().putString(KEY_BUDGET_BY_CATEGORY, current.toString()).apply()
    }

    var budgetAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_BUDGET_ALERT_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_BUDGET_ALERT_ENABLED, value).apply()
        }
}
