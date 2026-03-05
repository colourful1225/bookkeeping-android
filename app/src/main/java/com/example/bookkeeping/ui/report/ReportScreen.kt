package com.example.bookkeeping.ui.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bookkeeping.R
import com.example.bookkeeping.domain.model.CategoryItem
import com.example.bookkeeping.domain.model.ReportData
import com.example.bookkeeping.domain.model.ReportPeriodType
import com.example.bookkeeping.domain.model.TrendPoint
import com.example.bookkeeping.ui.util.localizedCategoryName
import com.example.bookkeeping.ui.util.localizedReportPeriodLabel
import java.text.NumberFormat
import java.util.Locale
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

// -------------------------------------------------------------------------- Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel = hiltViewModel()) {
    val uiState   by viewModel.uiState.collectAsState()
    val period    by viewModel.periodType.collectAsState()
    var focusType by remember { mutableStateOf(ReportFocusType.EXPENSE) }

    LaunchedEffect(Unit) {
        viewModel.resetToCurrentPeriod()
    }

    val headerLabel = when (val state = uiState) {
        is ReportUiState.Success -> localizedReportPeriodLabel(state.data.period)
        else -> periodLabel(period)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.page_title_report)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            PeriodSelector(selected = period, onSelect = viewModel::selectPeriod)
            Spacer(Modifier.height(12.dp))
            PeriodHeader(
                label = headerLabel,
                focusType = focusType,
                onFocusChange = { focusType = it },
                onPrev = { viewModel.shiftPeriod(-1) },
                onNext = { viewModel.shiftPeriod(1) },
            )
            Spacer(Modifier.height(12.dp))

            when (val state = uiState) {
                is ReportUiState.Loading -> {
                    Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ReportUiState.Error -> {
                    Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is ReportUiState.Success -> ReportContent(data = state.data, focusType = focusType)
            }
        }
    }
}

// -------------------------------------------------------------------------- Period Selector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(selected: ReportPeriodType, onSelect: (ReportPeriodType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == ReportPeriodType.WEEK,
            onClick = { onSelect(ReportPeriodType.WEEK) },
            label = { Text(stringResource(R.string.label_week)) },
        )
        FilterChip(
            selected = selected == ReportPeriodType.MONTH,
            onClick = { onSelect(ReportPeriodType.MONTH) },
            label = { Text(stringResource(R.string.label_month)) },
        )
        FilterChip(
            selected = selected == ReportPeriodType.YEAR,
            onClick = { onSelect(ReportPeriodType.YEAR) },
            label = { Text(stringResource(R.string.label_year)) },
        )
    }
}

// -------------------------------------------------------------------------- Full Report Content

@Composable
private fun ReportContent(data: ReportData, focusType: ReportFocusType) {
    val localizedPeriodLabel = localizedReportPeriodLabel(data.period)
    // 报表标签（如"2026年2月"）
    Text(
        text  = localizedPeriodLabel,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(12.dp))

    // 汇总卡片
    SummaryOverviewCard(data = data)
    Spacer(Modifier.height(16.dp))

    // 趋势图
    if (data.trend.isNotEmpty()) {
        SectionTitle(stringResource(R.string.report_section_trend, localizedPeriodLabel))
        TrendLineChart(points = data.trend, focusType = focusType)
        Spacer(Modifier.height(16.dp))
    }

    // 最近趋势
    if (data.recentPeriodTrend.size >= 2) {
        SectionTitle(
            stringResource(R.string.report_section_recent_trend, data.recentPeriodTrend.size)
        )
        RecentTrendBarChart(points = data.recentPeriodTrend, focusType = focusType)
        Spacer(Modifier.height(16.dp))
    }

    if (focusType == ReportFocusType.EXPENSE && data.expenseCategories.isNotEmpty()) {
        SectionTitle(stringResource(R.string.report_section_expense_breakdown))
        // 按金额排序分类
        val sortedExpenseCategories = data.expenseCategories.sortedByDescending { it.amount }
        CategoryList(items = sortedExpenseCategories, barColor = Color(0xFFEF5350))
        Spacer(Modifier.height(16.dp))
    }

    if (focusType == ReportFocusType.INCOME && data.incomeCategories.isNotEmpty()) {
        SectionTitle(stringResource(R.string.report_section_income_breakdown))
        // 按金额排序分类
        val sortedIncomeCategories = data.incomeCategories.sortedByDescending { it.amount }
        CategoryList(items = sortedIncomeCategories, barColor = Color(0xFF66BB6A))
        Spacer(Modifier.height(16.dp))
    }

    // 与上期对比
    ComparisonCard(data = data)
}

private enum class ReportFocusType { EXPENSE, INCOME }

@Composable
private fun PeriodHeader(
    label: String,
    focusType: ReportFocusType,
    onFocusChange: (ReportFocusType) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.label_prev_period),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onPrev),
            )
            Text(label, fontWeight = FontWeight.SemiBold)
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.label_next_period),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onNext),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = focusType == ReportFocusType.EXPENSE,
                onClick = { onFocusChange(ReportFocusType.EXPENSE) },
                label = { Text(stringResource(R.string.label_expense_type)) },
            )
            FilterChip(
                selected = focusType == ReportFocusType.INCOME,
                onClick = { onFocusChange(ReportFocusType.INCOME) },
                label = { Text(stringResource(R.string.label_income_type)) },
            )
        }
    }
}

@Composable
private fun periodLabel(period: ReportPeriodType): String = when (period) {
    ReportPeriodType.WEEK -> stringResource(R.string.report_period_week)
    ReportPeriodType.MONTH -> stringResource(R.string.report_period_month)
    ReportPeriodType.YEAR -> stringResource(R.string.report_period_year)
}

// -------------------------------------------------------------------------- Summary Overview Card

@Composable
private fun SummaryOverviewCard(data: ReportData) {
    val days = daysInPeriod(data.period.currentStart, data.period.currentEnd)
    val avgExpense = if (days > 0) data.current.totalExpense / days else 0L
    val expenseDelta = data.current.totalExpense - data.previous.totalExpense
    val balance = data.current.balance

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(
                        R.string.chart_label_expense,
                        shortPeriodLabel(data.period),
                    ),
                    value = formatYuan(data.current.totalExpense),
                    valueColor = Color(0xFF1E88E5),
                )
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.chart_label_daily_avg),
                    value = formatYuan(avgExpense),
                    valueColor = Color(0xFF1E88E5),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.chart_label_period_compare),
                    value = signedAmount(expenseDelta),
                    valueColor = if (expenseDelta <= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                )
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.chart_label_balance),
                    value = signedAmount(balance),
                    valueColor = if (balance >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color,
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(value, fontWeight = FontWeight.Bold, color = valueColor, fontSize = 18.sp)
    }
}

// -------------------------------------------------------------------------- Trend Line Chart

@Composable
private fun TrendLineChart(points: List<TrendPoint>, focusType: ReportFocusType) {
    val lineColor = if (focusType == ReportFocusType.EXPENSE) Color(0xFF1E88E5) else Color(0xFF43A047)
    val maxVal = points.maxOf { if (focusType == ReportFocusType.EXPENSE) it.expense else it.income }
        .takeIf { it > 0L } ?: 1L
    val chartHeight = 160.dp
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            // 选中点的数值提示
            val selIdx = selectedIndex
            if (selIdx != null && selIdx in points.indices) {
                val pt = points[selIdx]
                val value = if (focusType == ReportFocusType.EXPENSE) pt.expense else pt.income
                Text(
                    text = "${pt.label}: ${formatYuan(value)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = lineColor,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(4.dp))
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                // Y 轴标签
                Column(
                    modifier = Modifier.width(36.dp).height(chartHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatYuanShort(maxVal), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth())
                    Text(formatYuanShort(maxVal / 2), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth())
                    Text("0", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth())
                }

                // 折线图 Canvas
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(chartHeight)
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                val n = points.size
                                if (n == 0) return@detectTapGestures
                                val step = if (n > 1) size.width.toFloat() / (n - 1) else 0f
                                val idx = ((offset.x / step + 0.5f).toInt()).coerceIn(0, n - 1)
                                selectedIndex = if (selectedIndex == idx) null else idx
                            }
                        },
                ) {
                    val totalWidth = size.width
                    val totalHeight = size.height
                    val n = points.size
                    val step = if (n > 1) totalWidth / (n - 1) else totalWidth

                    // Y 轴辅助线
                    for (fraction in listOf(0.0f, 0.5f, 1.0f)) {
                        val y = totalHeight * (1f - fraction)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(0f, y),
                            end = Offset(totalWidth, y),
                            strokeWidth = 1f,
                        )
                    }

                    val offsets = points.mapIndexed { index, point ->
                        val value = if (focusType == ReportFocusType.EXPENSE) point.expense else point.income
                        val y = totalHeight - (value.toFloat() / maxVal * totalHeight)
                        Offset(step * index, y)
                    }

                    for (i in 0 until offsets.size - 1) {
                        drawLine(
                            color = lineColor,
                            start = offsets[i],
                            end = offsets[i + 1],
                            strokeWidth = 4f,
                        )
                    }

                    offsets.forEachIndexed { idx, pt ->
                        val isSelected = selectedIndex == idx
                        drawCircle(color = lineColor, radius = if (isSelected) 10f else 6f, center = pt)
                        if (isSelected) {
                            drawCircle(color = Color.White, radius = 5f, center = pt)
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth().padding(start = 36.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(points.first().label, style = MaterialTheme.typography.labelSmall)
                if (points.size > 2) {
                    Text(points[points.size / 2].label, style = MaterialTheme.typography.labelSmall)
                }
                Text(points.last().label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// -------------------------------------------------------------------------- Recent Trend Bar Chart

@Composable
private fun RecentTrendBarChart(points: List<TrendPoint>, focusType: ReportFocusType) {
    val barColor = if (focusType == ReportFocusType.EXPENSE) Color(0xFF1E88E5) else Color(0xFF43A047)
    val maxVal = points.maxOf { if (focusType == ReportFocusType.EXPENSE) it.expense else it.income }
        .takeIf { it > 0L } ?: 1L
    val chartHeight = 120.dp

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Y 轴标签
                Column(
                    modifier = Modifier.width(36.dp).height(chartHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatYuanShort(maxVal), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth())
                    Text(formatYuanShort(maxVal / 2), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth())
                    Text("0", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth())
                }

                Canvas(modifier = Modifier.weight(1f).height(chartHeight)) {
                    val totalWidth = size.width
                    val totalHeight = size.height
                    val n = points.size
                    val barWidth = totalWidth / (n * 2f)
                    val gap = barWidth

                    // 辅助线
                    for (fraction in listOf(0.5f, 1.0f)) {
                        val y = totalHeight * (1f - fraction)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(0f, y),
                            end = Offset(totalWidth, y),
                            strokeWidth = 1f,
                        )
                    }

                    points.forEachIndexed { idx, point ->
                        val value = if (focusType == ReportFocusType.EXPENSE) point.expense else point.income
                        val barHeight = (value.toFloat() / maxVal * totalHeight).coerceAtLeast(2f)
                        val x = gap / 2 + idx * (barWidth + gap)
                        drawRect(
                            color = barColor,
                            topLeft = Offset(x, totalHeight - barHeight),
                            size = Size(barWidth, barHeight),
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                points.forEach { pt ->
                    Text(pt.label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------- Category List

@Composable
private fun CategoryList(items: List<CategoryItem>, barColor: Color) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                CategoryRow(item = item, barColor = barColor)
            }
        }
    }
}

@Composable
private fun CategoryRow(item: CategoryItem, barColor: Color) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!item.categoryIcon.isNullOrEmpty()) {
                    Text(item.categoryIcon, fontSize = 16.sp)
                }
                Text(
                    localizedCategoryName(item.categoryId, item.categoryName),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "%.1f%%".format(item.percentage),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    formatYuan(item.amount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        // 进度条
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor.copy(alpha = 0.15f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(item.percentage / 100f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

// -------------------------------------------------------------------------- Comparison Card

@Composable
private fun ComparisonCard(data: ReportData) {
    val prevLabel = when (data.period.type) {
        ReportPeriodType.WEEK  -> stringResource(R.string.report_prev_week)
        ReportPeriodType.MONTH -> stringResource(R.string.report_prev_month)
        ReportPeriodType.YEAR  -> stringResource(R.string.report_prev_year)
    }
    SectionTitle(stringResource(R.string.report_section_compare, prevLabel))
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ComparisonItem(stringResource(R.string.label_income_type), data.incomeDelta)
            ComparisonItem(stringResource(R.string.label_expense_type), data.expenseDelta)
            ComparisonItem(stringResource(R.string.label_balance_plain), data.balanceDelta)
        }
    }
}

@Composable
private fun ComparisonItem(label: String, delta: Long) {
    val isPositive = delta >= 0
    val sign       = if (isPositive) "+" else "-"
    val color      = if (isPositive) Color(0xFF66BB6A) else Color(0xFFEF5350)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text  = "$sign${formatYuan(abs(delta))}",
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}

// -------------------------------------------------------------------------- Helpers

@Composable
private fun SectionTitle(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

/** 分转元，保留两位小数。 */
private fun formatYuan(cents: Long): String {
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }
    return "¥${fmt.format(cents / 100.0)}"
}

/** 分转元简短格式（Y 轴标签用）。 */
private fun formatYuanShort(cents: Long): String {
    val yuan = cents / 100.0
    return when {
        yuan >= 10000 -> "%.1fw".format(yuan / 10000)
        yuan >= 1000  -> "%.0f".format(yuan)
        else          -> "%.0f".format(yuan)
    }
}

private fun signedAmount(cents: Long): String {
    val sign = if (cents >= 0) "+" else "-"
    return "$sign${formatYuan(abs(cents)).removePrefix("¥")}".let { "¥$it" }
}

private fun daysInPeriod(startMillis: Long, endMillis: Long): Long {
    val zone = ZoneId.systemDefault()
    val startDate = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
    val endDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(startDate, endDate)
    return if (days <= 0) 1 else days
}

@Composable
private fun shortPeriodLabel(period: com.example.bookkeeping.domain.model.ReportPeriod): String {
    return when (period.type) {
        ReportPeriodType.WEEK -> stringResource(R.string.report_period_week)
        ReportPeriodType.MONTH -> stringResource(R.string.report_period_month)
        ReportPeriodType.YEAR -> stringResource(R.string.report_period_year)
    }
}
