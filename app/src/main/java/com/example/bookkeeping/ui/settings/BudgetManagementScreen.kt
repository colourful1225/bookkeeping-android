package com.example.bookkeeping.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bookkeeping.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetManagementScreen(
    viewModel: BudgetManagementViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingBudget by remember { mutableStateOf<BudgetCategoryUi?>(null) }

    val totalBudget = uiState.items.sumOf { it.budget }
    val totalSpent = uiState.items.sumOf { it.spent }
    val remaining = totalBudget - totalSpent

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.page_title_budget)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.button_back))
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
            SectionTitle(stringResource(R.string.label_budget_overview))
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
                        Text(stringResource(R.string.label_total_budget), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥ $totalBudget", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.label_spent), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥ $totalSpent", fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.label_remaining), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥ $remaining", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            SectionTitle(stringResource(R.string.section_title_reminder))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.label_budget_alert), fontWeight = FontWeight.Medium)
                        Text(
                            stringResource(R.string.desc_budget_alert),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = uiState.budgetAlertEnabled,
                        onCheckedChange = { viewModel.updateBudgetAlertEnabled(it) },
                    )
                }
            }

            SectionTitle(stringResource(R.string.label_category_budgets))
            uiState.items.forEach { budget ->
                BudgetCategoryCard(
                    item = budget,
                    onClick = { editingBudget = budget },
                )
            }
        }

        editingBudget?.let { budget ->
            EditBudgetDialog(
                budget = budget,
                onDismiss = { editingBudget = null },
                onConfirm = { newBudgetValue ->
                    viewModel.updateCategoryBudget(budget.categoryId, newBudgetValue)
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
private fun BudgetCategoryCard(item: BudgetCategoryUi, onClick: () -> Unit) {
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

@Composable
private fun EditBudgetDialog(
    budget: BudgetCategoryUi,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var inputValue by remember { mutableStateOf(budget.budget.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_edit_budget, budget.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.label_current_spent, budget.spent), fontSize = 14.sp)
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.label_budget_amount)) },
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
                Text(stringResource(R.string.button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        },
    )
}
