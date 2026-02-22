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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
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
            contentDescription = "搜索",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("搜索备注...") },
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
                    contentDescription = "清空",
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
            listOf("EXPENSE" to "支出", "INCOME" to "收入").forEach { (type, label) ->
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
                    label = { Text("起：${filter.startDate}") },
                )
            }
            if (filter.endDate != null) {
                FilterChip(
                    selected = true,
                    onClick = {
                        onFilterChange(filter.copy(endDate = null))
                    },
                    label = { Text("止：${filter.endDate}") },
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
            "交易类型",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("EXPENSE" to "支出", "INCOME" to "收入", null to "全部").forEach { (type, label) ->
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
            "日期范围",
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
                    filter.startDate?.toString() ?: "开始日期",
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
                    filter.endDate?.toString() ?: "结束日期",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        
        // 金额范围（简化版，实际应使用 RangeSlider）
        Text(
            "金额范围 (元)",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "最小：${filter.minAmount?.let { it / 100.0 } ?: "不限"}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                "最大：${filter.maxAmount?.let { it / 100.0 } ?: "不限"}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
