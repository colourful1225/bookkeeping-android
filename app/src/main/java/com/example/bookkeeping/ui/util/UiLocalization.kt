package com.example.bookkeeping.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.bookkeeping.R
import com.example.bookkeeping.domain.model.ReportPeriod
import com.example.bookkeeping.domain.model.ReportPeriodType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.util.Locale

@Composable
fun localizedCategoryName(categoryId: String, fallbackName: String): String {
    val resourceId = categoryNameResId(categoryId)
    return if (resourceId != null) stringResource(resourceId) else fallbackName
}

@Composable
fun localizedReportPeriodLabel(period: ReportPeriod): String {
    val locale = Locale.getDefault()
    val date = Instant.ofEpochMilli(period.currentStart)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    return when (period.type) {
        ReportPeriodType.WEEK -> {
            val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            val weekYear = date.get(IsoFields.WEEK_BASED_YEAR)
            if (locale.language.startsWith("zh")) {
                "${weekYear}年第${week}周"
            } else {
                "Week $week, $weekYear"
            }
        }

        ReportPeriodType.MONTH -> {
            if (locale.language.startsWith("zh")) {
                "${date.year}年${date.monthValue}月"
            } else {
                date.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            }
        }

        ReportPeriodType.YEAR -> {
            if (locale.language.startsWith("zh")) {
                "${date.year}年"
            } else {
                date.year.toString()
            }
        }
    }
}

@StringRes
private fun categoryNameResId(categoryId: String): Int? = when (categoryId) {
    "pay_for" -> R.string.category_default_pay_for
    "shopping" -> R.string.category_default_shopping
    "dining" -> R.string.category_default_dining
    "transportation" -> R.string.category_default_transportation
    "housing" -> R.string.category_default_housing
    "entertainment" -> R.string.category_default_entertainment
    "communication" -> R.string.category_default_communication
    "utilities" -> R.string.category_default_utilities
    "beauty" -> R.string.category_default_beauty
    "pet" -> R.string.category_default_pet
    "social" -> R.string.category_default_social
    "family" -> R.string.category_default_family
    "repayment" -> R.string.category_default_repayment
    "lend_out" -> R.string.category_default_lend_out
    "invest" -> R.string.category_default_invest
    "travel" -> R.string.category_default_travel
    "red_packet" -> R.string.category_default_red_packet
    "medical" -> R.string.category_default_medical
    "education" -> R.string.category_default_education
    "consume" -> R.string.category_default_consume
    "transfer" -> R.string.category_default_transfer
    "others" -> R.string.category_default_others
    "salary" -> R.string.category_default_salary
    "investment" -> R.string.category_default_investment
    "refund" -> R.string.category_default_refund
    "income_others" -> R.string.category_default_income_others
    "income_debt" -> R.string.category_default_income_debt
    "borrow_in" -> R.string.category_default_borrow_in
    "income_red_packet" -> R.string.category_default_income_red_packet
    "income_transfer" -> R.string.category_default_income_transfer
    else -> null
}