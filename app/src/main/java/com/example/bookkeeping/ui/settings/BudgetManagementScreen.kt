package com.example.bookkeeping.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetManagementScreen(
    onBack: () -> Unit = {},
) {
    // 保存分类预算状态
    var budgets by remember {
        mutableStateOf(
            listOf(
                BudgetItem("🍔", "餐饮", 1000, 580),
                BudgetItem("🚖", "出行", 800, 200),
                BudgetItem("🛒", "购物", 1500, 650),
                BudgetItem("🏠", "居住", 2000, 0),
            )
        )
    }
    
    var editingBudget by remember { mutableStateOf<BudgetItem?>(null) }
    
    val totalBudget = budgets.sumOf { it.budget }
    val totalSpent = budgets.sumOf { it.spent }
    val remaining = totalBudget - totalSpent

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预算管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle("预算概览")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("本月总预算", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥ $totalBudget", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("已支出", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥ $totalSpent", fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("剩余", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥ $remaining", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            SectionTitle("分类预算（点击编辑）")
            budgets.forEach { budget ->
                BudgetCategoryCard(
                    item = budget,
                    onClick = { editingBudget = budget },
                )
            }

            SectionTitle("关于")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("预算管理", fontWeight = FontWeight.SemiBold)
                    Text(
                        "为每个分类设置预算，帮助你合理规划消费，避免月光。点击分类预算卡片可编辑。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        
        // 编辑对话框
        editingBudget?.let { budget ->
            EditBudgetDialog(
                budget = budget,
                onDismiss = { editingBudget = null },
                onConfirm = { newBudgetValue ->
                    budgets = budgets.map { 
                        if (it.name == budget.name) it.copy(budget = newBudgetValue) else it
                    }
                    editingBudget = null
                },
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
}

@Composable
private fun BudgetCategoryCard(item: BudgetItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.icon, fontSize = 24.sp)
                    Text(item.name, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("¥ ${item.spent} / ${item.budget}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    val percentage = if (item.budget > 0) (item.spent * 100f / item.budget).toInt() else 0
                    Text(
                        "$percentage%",
                        fontSize = 12.sp,
                        color = if (percentage > 80) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // 进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFE0E0E0))
            ) {
                val progress = if (item.budget > 0) (item.spent.toFloat() / item.budget).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (progress > 0.8f) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}

data class BudgetItem(
    val icon: String,
    val name: String,
    val budget: Int,
    val spent: Int,
)

@Composable
private fun EditBudgetDialog(
    budget: BudgetItem,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var inputValue by remember { mutableStateOf(budget.budget.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑${budget.name}预算") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("当前已支出：¥${budget.spent}", fontSize = 14.sp)
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it.filter { c -> c.isDigit() } },
                    label = { Text("预算金额（元）") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    inputValue.toIntOrNull()?.let(onConfirm)
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
