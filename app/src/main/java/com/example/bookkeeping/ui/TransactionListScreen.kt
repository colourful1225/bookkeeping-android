package com.example.bookkeeping.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.bookkeeping.ui.util.localizedCategoryName
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bookkeeping.R
import com.example.bookkeeping.data.local.entity.TransactionEntity
import com.example.bookkeeping.ui.transaction.TransactionSearchViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel = hiltViewModel(),
    searchViewModel: TransactionSearchViewModel = hiltViewModel(),
    onAddClick: () -> Unit = {},
    onEditClick: (transactionId: String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val searchResults by searchViewModel.transactions.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = { if (!isSearchActive) Text(stringResource(R.string.page_title_transactions)) },
                    actions = {
                        if (!isSearchActive) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_icon_desc))
                            }
                        } else {
                            IconButton(onClick = {
                                isSearchActive = false
                                searchViewModel.updateSearchQuery("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_search_icon_desc))
                            }
                        }
                    }
                )
                if (isSearchActive) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchViewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .focusRequester(searchFocusRequester),
                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                        singleLine = true,
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_transaction_icon_desc))
            }
        },
    ) { padding ->
        val displayTransactions = if (searchQuery.isNotEmpty()) searchResults else uiState.transactions

        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            else -> TransactionListContent(
                transactions = displayTransactions,
                categoryMap = uiState.categoryMap,
                onEditClick = onEditClick,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun TransactionListContent(
    transactions: List<TransactionEntity>,
    categoryMap: Map<String, com.example.bookkeeping.data.local.entity.CategoryEntity>,
    onEditClick: (transactionId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val todayKey = rememberDateKey(System.currentTimeMillis())
    val monthKey = rememberMonthKey(System.currentTimeMillis())

    val todayExpense = transactions
        .filter { it.type == "EXPENSE" && rememberDateKey(it.occurredAt) == todayKey }
        .sumOf { it.amount }
    val todayIncome = transactions
        .filter { it.type == "INCOME" && rememberDateKey(it.occurredAt) == todayKey }
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
                todayIncome = todayIncome,
                monthExpense = monthExpense,
                monthIncome = monthIncome,
            )
        }

        if (sortedKeys.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.no_records))
                }
            }
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
                TransactionRow(tx, categoryMap, onEditClick = { onEditClick(tx.id) })
            }
        }
    }
}

@Composable
private fun getExpenseColor(): Color {
    return if (isSystemInDarkTheme()) {
        Color(0xFFFF6B6B)
    } else {
        Color(0xFFD32F2F)
    }
}

@Composable
private fun getIncomeColor(): Color {
    return if (isSystemInDarkTheme()) {
        Color(0xFF66BB6A)
    } else {
        Color(0xFF2E7D32)
    }
}

@Composable
private fun SummaryHeader(todayExpense: Long, todayIncome: Long, monthExpense: Long, monthIncome: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // 今日支出
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    stringResource(R.string.summary_today_expense),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    formatYuan(todayExpense),
                    style = MaterialTheme.typography.titleSmall,
                    color = getExpenseColor(),
                )
            }
        }
        // 本月收支（一个卡片内）
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        stringResource(R.string.summary_month_income),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        formatYuan(monthIncome),
                        style = MaterialTheme.typography.titleSmall,
                        color = getIncomeColor(),
                    )
                }
                HorizontalDivider(
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        stringResource(R.string.summary_month_expense),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        formatYuan(monthExpense),
                        style = MaterialTheme.typography.titleSmall,
                        color = getExpenseColor(),
                    )
                }
            }
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
            stringResource(
                R.string.summary_day_totals,
                formatYuan(dayExpense),
                formatYuan(dayIncome),
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = MaterialTheme.typography.labelSmall.fontSize,
        )
    }
}

@Composable
private fun TransactionRow(
    tx: TransactionEntity,
    categoryMap: Map<String, com.example.bookkeeping.data.local.entity.CategoryEntity>,
    onEditClick: () -> Unit = {},
) {
    val category = categoryMap[tx.categoryId]
    val categoryLabel = if (category != null) {
        localizedCategoryName(category.id, category.name)
    } else {
        tx.categoryId
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick),
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
                val autoImportLabel = autoImportBadgeLabel(tx.note)
                Text(
                    categoryLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        rememberTime(tx.occurredAt),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    )
                    autoImportLabel?.let {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = it,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
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
        "PENDING"  -> stringResource(R.string.sync_status_pending)
        "SYNCED"   -> stringResource(R.string.sync_status_synced)
        "FAILED"   -> stringResource(R.string.sync_status_failed)
        "CONFLICT" -> stringResource(R.string.sync_status_conflict)
        else       -> status
    }
    Text(label)
}

private fun formatYuan(amount: Long): String = String.format(Locale.getDefault(), "%.2f", amount / 100.0)

private fun rememberDateKey(timeMillis: Long): String {
    val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

private fun rememberMonthKey(timeMillis: Long): String {
    val sdf = SimpleDateFormat("yyyy.MM", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

@Composable
private fun rememberDayOfWeek(timeMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY    -> stringResource(R.string.weekday_mon)
        Calendar.TUESDAY   -> stringResource(R.string.weekday_tue)
        Calendar.WEDNESDAY -> stringResource(R.string.weekday_wed)
        Calendar.THURSDAY  -> stringResource(R.string.weekday_thu)
        Calendar.FRIDAY    -> stringResource(R.string.weekday_fri)
        Calendar.SATURDAY  -> stringResource(R.string.weekday_sat)
        Calendar.SUNDAY    -> stringResource(R.string.weekday_sun)
        else -> ""
    }
}

private fun rememberTime(timeMillis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

@Composable
private fun autoImportBadgeLabel(note: String?): String? {
    if (note.isNullOrBlank()) return null
    return when {
        note.startsWith("[自动]微信支付") -> stringResource(R.string.wechat_auto_import)
        note.startsWith("[自动]支付宝") -> stringResource(R.string.alipay_auto_import)
        else -> null
    }
}

