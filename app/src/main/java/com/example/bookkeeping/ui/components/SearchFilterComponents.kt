package com.example.bookkeeping.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.bookkeeping.R
import com.example.bookkeeping.domain.model.SearchFilter
import java.time.LocalDate

/**
 * 搜索栏组件
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(R.string.search_desc),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.search_placeholder_memo)) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.clear_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 过滤条件显示和编辑组件
 */
@Composable
fun FilterChipRow(
    filter: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 交易类型
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "EXPENSE" to stringResource(R.string.label_expense_type),
                "INCOME" to stringResource(R.string.label_income_type),
            ).forEach { (type, label) ->
                FilterChip(
                    selected = filter.type == type,
                    onClick = {
                        onFilterChange(
                            filter.copy(
                                type = if (filter.type == type) null else type
                            )
                        )
                    },
                    label = { Text(label) },
                )
            }
        }
        
        // 日期范围
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (filter.startDate != null) {
                FilterChip(
                    selected = true,
                    onClick = {
                        onFilterChange(filter.copy(startDate = null))
                    },
                    label = { Text(stringResource(R.string.label_start_date, filter.startDate ?: "")) },
                )
            }
            if (filter.endDate != null) {
                FilterChip(
                    selected = true,
                    onClick = {
                        onFilterChange(filter.copy(endDate = null))
                    },
                    label = { Text(stringResource(R.string.label_end_date, filter.endDate ?: "")) },
                )
            }
        }
    }
}

/**
 * 高级过滤对话框（展开式）
 */
@Composable
fun AdvancedFilterPanel(
    filter: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 交易类型选择
        Text(
            stringResource(R.string.label_transaction_type),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "EXPENSE" to stringResource(R.string.label_expense_type),
                "INCOME" to stringResource(R.string.label_income_type),
                null to stringResource(R.string.label_all),
            ).forEach { (type, label) ->
                FilterChip(
                    selected = filter.type == type,
                    onClick = {
                        onFilterChange(filter.copy(type = type))
                    },
                    label = { Text(label) },
                )
            }
        }
        
        // 日期范围
        Text(
            stringResource(R.string.label_date_range),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        // 日期选择器（待实现）
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    filter.startDate?.toString()
                        ?: stringResource(R.string.label_start_date_placeholder),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        // 日期选择器（待实现）
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    filter.endDate?.toString()
                        ?: stringResource(R.string.label_end_date_placeholder),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        
        // 金额范围（简化版，实际应使用 RangeSlider）
        Text(
            stringResource(R.string.label_amount_range),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val minAmountText = filter.minAmount?.let { it / 100.0 }?.toString()
                ?: stringResource(R.string.label_no_limit)
            Text(
                stringResource(R.string.label_min_amount, minAmountText),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            val maxAmountText = filter.maxAmount?.let { it / 100.0 }?.toString()
                ?: stringResource(R.string.label_no_limit)
            Text(
                stringResource(R.string.label_max_amount, maxAmountText),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
