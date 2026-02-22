package com.example.bookkeeping.ui.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bookkeeping.domain.model.CategoryItem
import com.example.bookkeeping.domain.model.ReportData
import com.example.bookkeeping.domain.model.ReportPeriodType
import com.example.bookkeeping.domain.model.TrendPoint
import java.text.NumberFormat
import java.util.Locale
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

// -------------------------------------------------------------------------- Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel = hiltViewModel()) {
    val uiState   by viewModel.uiState.collectAsState()
    val period    by viewModel.periodType.collectAsState()
    var focusType by remember { mutableStateOf(ReportFocusType.EXPENSE) }
    val headerLabel = when (val state = uiState) {
        is ReportUiState.Success -> state.data.period.label
        else -> periodLabel(period)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("报表") })
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

@Composable
private fun PeriodSelector(selected: ReportPeriodType, onSelect: (ReportPeriodType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            ReportPeriodType.WEEK  to "周",
            ReportPeriodType.MONTH to "月",
            ReportPeriodType.YEAR  to "年",
        ).forEach { (type, label) ->
            FilterChip(
                selected = selected == type,
                onClick  = { onSelect(type) },
                label    = { Text(label) },
            )
        }
    }
}

// -------------------------------------------------------------------------- Full Report Content

@Composable
private fun ReportContent(data: ReportData, focusType: ReportFocusType) {
    // 报表标签（如"2026年2月"）
    Text(
        text  = data.period.label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(12.dp))

    // 汇总卡片
    SummaryOverviewCard(data = data)
    Spacer(Modifier.height(16.dp))

    // 趋势图
    if (data.trend.isNotEmpty()) {
        SectionTitle("${data.period.label}趋势")
        TrendLineChart(points = data.trend, focusType = focusType)
        Spacer(Modifier.height(16.dp))
    }

    if (focusType == ReportFocusType.EXPENSE && data.expenseCategories.isNotEmpty()) {
        SectionTitle("支出构成")
        PieChart(items = data.expenseCategories)
        Spacer(Modifier.height(12.dp))
        CategoryList(items = data.expenseCategories, barColor = Color(0xFFEF5350))
        Spacer(Modifier.height(16.dp))
    }

    if (focusType == ReportFocusType.INCOME && data.incomeCategories.isNotEmpty()) {
        SectionTitle("收入构成")
        CategoryList(items = data.incomeCategories, barColor = Color(0xFF66BB6A))
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
                contentDescription = "上一期",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onPrev),
            )
            Text(label, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "下一期",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onNext),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = focusType == ReportFocusType.EXPENSE,
                onClick = { onFocusChange(ReportFocusType.EXPENSE) },
                label = { Text("支出") },
            )
            FilterChip(
                selected = focusType == ReportFocusType.INCOME,
                onClick = { onFocusChange(ReportFocusType.INCOME) },
                label = { Text("收入") },
            )
        }
    }
}

private fun periodLabel(period: ReportPeriodType): String = when (period) {
    ReportPeriodType.WEEK -> "本周"
    ReportPeriodType.MONTH -> "本月"
    ReportPeriodType.YEAR -> "本年"
}

@Composable
private fun PieChart(items: List<CategoryItem>) {
    val total = items.sumOf { it.amount }.coerceAtLeast(1L)
    val colors = listOf(
        Color(0xFF42A5F5),
        Color(0xFF66BB6A),
        Color(0xFFFFA726),
        Color(0xFFAB47BC),
        Color(0xFF26C6DA),
        Color(0xFFEF5350),
    )
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                var startAngle = -90f
                items.forEachIndexed { index, item ->
                    val sweep = (item.amount.toFloat() / total.toFloat()) * 360f
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                    )
                    startAngle += sweep
                }
            }
        }
    }
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
                    label = "${shortPeriodLabel(data.period)}支出（元）",
                    value = formatYuan(data.current.totalExpense),
                    valueColor = Color(0xFF1E88E5),
                )
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = "日均支出（元）",
                    value = formatYuan(avgExpense),
                    valueColor = Color(0xFF1E88E5),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = "比上期支出（元）",
                    value = signedAmount(expenseDelta),
                    valueColor = if (expenseDelta <= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                )
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = "收支结余（元）",
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

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
                val totalWidth = size.width
                val totalHeight = size.height
                val n = points.size
                val step = if (n > 1) totalWidth / (n - 1) else totalWidth

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

                offsets.forEach { point ->
                    drawCircle(color = lineColor, radius = 6f, center = point)
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(points.first().label, style = MaterialTheme.typography.labelSmall)
                if (points.size > 2) {
                    Text(points[points.size / 2].label, style = MaterialTheme.typography.labelSmall)
                }
                Text(points.last().label, style = MaterialTheme.typography.labelSmall)
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
                Text(item.categoryName, style = MaterialTheme.typography.bodySmall)
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
        ReportPeriodType.WEEK  -> "上周"
        ReportPeriodType.MONTH -> "上月"
        ReportPeriodType.YEAR  -> "上年"
    }
    SectionTitle("对比$prevLabel")
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ComparisonItem("收入", data.incomeDelta)
            ComparisonItem("支出", data.expenseDelta)
            ComparisonItem("结余", data.balanceDelta)
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
    val fmt = NumberFormat.getNumberInstance(Locale.CHINA).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }
    return "¥${fmt.format(cents / 100.0)}"
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

private fun shortPeriodLabel(period: com.example.bookkeeping.domain.model.ReportPeriod): String {
    return when (period.type) {
        ReportPeriodType.WEEK -> "本周"
        ReportPeriodType.MONTH -> "本月"
        ReportPeriodType.YEAR -> "本年"
    }
}
