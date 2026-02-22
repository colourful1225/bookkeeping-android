package com.example.bookkeeping.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bookkeeping.data.local.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel = hiltViewModel(),
    onAddClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("记账") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "新增支出")
            }
        },
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            uiState.transactions.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text("暂无记录，点击 + 新增") }

            else -> TransactionListContent(
                transactions = uiState.transactions,
                categoryMap = uiState.categoryMap,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun TransactionListContent(
    transactions: List<TransactionEntity>,
    categoryMap: Map<String, com.example.bookkeeping.data.local.entity.CategoryEntity>,
    modifier: Modifier = Modifier,
) {
    val todayKey = rememberDateKey(System.currentTimeMillis())
    val monthKey = rememberMonthKey(System.currentTimeMillis())

    val todayExpense = transactions
        .filter { it.type == "EXPENSE" && rememberDateKey(it.occurredAt) == todayKey }
        .sumOf { it.amount }
    val monthExpense = transactions
        .filter { it.type == "EXPENSE" && rememberMonthKey(it.occurredAt) == monthKey }
        .sumOf { it.amount }
    val monthIncome = transactions
        .filter { it.type == "INCOME" && rememberMonthKey(it.occurredAt) == monthKey }
        .sumOf { it.amount }

    val grouped = transactions.groupBy { rememberDateKey(it.occurredAt) }
    val sortedKeys = grouped.keys.sortedDescending()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SummaryHeader(
                todayExpense = todayExpense,
                monthExpense = monthExpense,
                monthIncome = monthIncome,
            )
        }

        sortedKeys.forEach { dateKey ->
            val dayList = grouped[dateKey].orEmpty().sortedByDescending { it.occurredAt }
            val dayExpense = dayList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val dayIncome = dayList.filter { it.type == "INCOME" }.sumOf { it.amount }

            item {
                DateGroupHeader(
                    dateKey = dateKey,
                    dayOfWeek = rememberDayOfWeek(dayList.firstOrNull()?.occurredAt ?: 0L),
                    dayExpense = dayExpense,
                    dayIncome = dayIncome,
                )
            }

            items(dayList, key = { it.id }) { tx ->
                TransactionRow(tx, categoryMap)
            }
        }
    }
}

@Composable
private fun SummaryHeader(todayExpense: Long, monthExpense: Long, monthIncome: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("今日支出（元）", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatYuan(todayExpense),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryMiniCard(
                    label = "本月支出（元）",
                    amount = monthExpense,
                    modifier = Modifier.weight(1f),
                )
                SummaryMiniCard(
                    label = "本月收入（元）",
                    amount = monthIncome,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryMiniCard(label: String, amount: Long, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = MaterialTheme.typography.labelSmall.fontSize)
            Text(formatYuan(amount), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun DateGroupHeader(dateKey: String, dayOfWeek: String, dayExpense: Long, dayIncome: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$dateKey $dayOfWeek", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text(
            "支${formatYuan(dayExpense)} 收${formatYuan(dayIncome)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = MaterialTheme.typography.labelSmall.fontSize,
        )
    }
}

@Composable
private fun TransactionRow(
    tx: TransactionEntity,
    categoryMap: Map<String, com.example.bookkeeping.data.local.entity.CategoryEntity>,
) {
    val category = categoryMap[tx.categoryId]
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(category?.icon ?: "📌", fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    category?.name ?: tx.categoryId,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        rememberTime(tx.occurredAt),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    )
                    tx.note?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        )
                    }
                }
            }
            val isIncome = tx.type == "INCOME"
            val amountText = if (isIncome) "+${formatYuan(tx.amount)}" else "-${formatYuan(tx.amount)}"
            val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            Text(amountText, color = amountColor, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SyncStatusBadge(status: String) {
    val label = when (status) {
        "PENDING"  -> "⏳ 待同步"
        "SYNCED"   -> "✅ 已同步"
        "FAILED"   -> "❌ 失败"
        "CONFLICT" -> "⚠️ 冲突"
        else       -> status
    }
    Text(label)
}

private fun formatYuan(amount: Long): String = String.format(Locale.CHINA, "%.2f", amount / 100.0)

private fun rememberDateKey(timeMillis: Long): String {
    val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.CHINA)
    return sdf.format(Date(timeMillis))
}

private fun rememberMonthKey(timeMillis: Long): String {
    val sdf = SimpleDateFormat("yyyy.MM", Locale.CHINA)
    return sdf.format(Date(timeMillis))
}

private fun rememberDayOfWeek(timeMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY    -> "周一"
        Calendar.TUESDAY   -> "周二"
        Calendar.WEDNESDAY -> "周三"
        Calendar.THURSDAY  -> "周四"
        Calendar.FRIDAY    -> "周五"
        Calendar.SATURDAY  -> "周六"
        Calendar.SUNDAY    -> "周日"
        else -> ""
    }
}

private fun rememberTime(timeMillis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.CHINA)
    return sdf.format(Date(timeMillis))
}
