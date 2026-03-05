package com.example.bookkeeping.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ────────────────────────────────────────────────────────────────────────────
// 自定义色彩定义 - 品牌主色
// ────────────────────────────────────────────────────────────────────────────

// 主色系（蓝绿色）
private val PrimaryLight = Color(0xFF006B63)
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFF76F8E8)
private val OnPrimaryContainerLight = Color(0xFF002020)

// 主色系（暗黑版）
private val PrimaryDark = Color(0xFF52DBD4)
private val OnPrimaryDark = Color(0xFF003735)
private val PrimaryContainerDark = Color(0xFF005047)
private val OnPrimaryContainerDark = Color(0xFF76F8E8)

// 次色系（紫色/强调）
private val SecondaryLight = Color(0xFF496B73)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFCCF1F8)
private val OnSecondaryContainerLight = Color(0xFF001F27)

private val SecondaryDark = Color(0xFFB0D5DC)
private val OnSecondaryDark = Color(0xFF00363E)
private val SecondaryContainerDark = Color(0xFF314D55)
private val OnSecondaryContainerDark = Color(0xFFCCF1F8)

// 第三色系（深棕 - 用于中立强调）
private val TertiaryLight = Color(0xFF5D6B48)
private val OnTertiaryLight = Color(0xFFFFFFFF)
private val TertiaryContainerLight = Color(0xFFDFF6D1)
private val OnTertiaryContainerLight = Color(0xFF1B2411)

private val TertiaryDark = Color(0xFFC3DAB7)
private val OnTertiaryDark = Color(0xFF2F371E)
private val TertiaryContainerDark = Color(0xFF465030)
private val OnTertiaryContainerDark = Color(0xFFDFF6D1)

// ────────────────────────────────────────────────────────────────────────────
// 背景与表面色
// ────────────────────────────────────────────────────────────────────────────

// 明亮主题背景
private val BackgroundLight = Color(0xFFFAFDFC)
private val SurfaceLight = Color(0xFFFAFDFC)
private val SurfaceVariantLight = Color(0xFFE0F2F0)
private val OutlineLight = Color(0xFF70787E)
private val OutlineVariantLight = Color(0xFFB0B8BC)

// 暗黑主题背景（优化对比度和舒适度）
private val BackgroundDark = Color(0xFF0F1514)
private val SurfaceDark = Color(0xFF141D1C)
private val SurfaceVariantDark = Color(0xFF1F3432)
private val OutlineDark = Color(0xFF89928C)
private val OutlineVariantDark = Color(0xFF5E6B67)

// 支出色（红色）- 用于支出金额
private val ExpenseColorLight = Color(0xFFD32F2F)
private val ExpenseColorDark = Color(0xFFFF6B6B)

// 收入色（绿色）- 用于收入金额
private val IncomeColorLight = Color(0xFF2E7D32)
private val IncomeColorDark = Color(0xFF66BB6A)

// 警告/错误色（用于导入失败、错误提示）
private val ErrorLight = Color(0xFFB3261E)
private val ErrorDark = Color(0xFFF2B8B5)

// ────────────────────────────────────────────────────────────────────────────
// Material 3 明亮主题配置
// ────────────────────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    
    background = BackgroundLight,
    onBackground = Color(0xFF0F1514),
    surface = SurfaceLight,
    onSurface = Color(0xFF0F1514),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF3F4945),
    
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    
    error = ErrorLight,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

// ────────────────────────────────────────────────────────────────────────────
// Material 3 暗黑主题配置（专业优化版本）
// ────────────────────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    
    background = BackgroundDark,
    onBackground = Color(0xFFE0E7E6),
    surface = SurfaceDark,
    onSurface = Color(0xFFE0E7E6),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFB0B8BC),
    
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    
    error = ErrorDark,
    onError = Color(0xFF601510),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF2B8B5),
)

// ────────────────────────────────────────────────────────────────────────────
// 主题应用入口
// ────────────────────────────────────────────────────────────────────────────

/**
 * 记账本应用主题。
 * 
 * 支持明亮/暗黑/系统三种模式，根据用户设置自动适配。
 * - 暗黑模式优化了深色背景下的舒适度和对比度
 * - 支出色采用红色，收入色采用绿色，便于识别
 */
@Composable
fun BookkeepingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BookkeepingTypography,
        content = content,
    )
}

// ────────────────────────────────────────────────────────────────────────────
// 应用级别的色彩常量（便于直接引用）
// ────────────────────────────────────────────────────────────────────────────

object AppColors {
    // 支出指示色
    val expenseLight = ExpenseColorLight
    val expenseDark = ExpenseColorDark
    
    // 收入指示色
    val incomeLight = IncomeColorLight
    val incomeDark = IncomeColorDark
}
