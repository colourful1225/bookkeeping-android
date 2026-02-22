package com.example.bookkeeping.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bookkeeping.data.local.entity.CategoryEntity
import com.example.bookkeeping.data.local.entity.CategoryType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryManagementScreen(
    viewModel: CategoryManagementViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var editorState by remember { mutableStateOf<EditorState?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记账分类管理") },
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SectionTitle("支出分类", "点击编辑，长按删除自定义分类")
            CategoryGrid(
                items = uiState.expenseCategories,
                onAddClick = { editorState = EditorState(type = CategoryType.EXPENSE, category = null) },
                onEditClick = { editorState = EditorState(type = CategoryType.EXPENSE, category = it) },
                onDeleteClick = { viewModel.deleteCategory(it) },
            )

            SectionTitle("收入分类", "点击编辑，长按删除自定义分类")
            CategoryGrid(
                items = uiState.incomeCategories,
                onAddClick = { editorState = EditorState(type = CategoryType.INCOME, category = null) },
                onEditClick = { editorState = EditorState(type = CategoryType.INCOME, category = it) },
                onDeleteClick = { viewModel.deleteCategory(it) },
            )
        }
    }

    editorState?.let { state ->
        CategoryEditorDialog(
            state = state,
            onDismiss = { editorState = null },
            onSave = { name, icon ->
                if (state.category == null) {
                    viewModel.addCategory(state.type, name, icon)
                } else {
                    viewModel.updateCategory(state.category, name, icon)
                }
                editorState = null
            },
            onDelete = {
                state.category?.let { viewModel.deleteCategory(it) }
                editorState = null
            },
        )
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (subtitle != null) {
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    items: List<CategoryEntity>,
    onAddClick: () -> Unit,
    onEditClick: (CategoryEntity) -> Unit,
    onDeleteClick: (CategoryEntity) -> Unit,
) {
    val fullList = items + CategoryEntity(
        id = "add",
        name = "添加",
        icon = null,
        type = items.firstOrNull()?.type ?: CategoryType.EXPENSE,
        isDefault = false,
    )
    val rows = fullList.chunked(5)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { item ->
                    if (item.id == "add") {
                        AddCategoryCard(onClick = onAddClick, modifier = Modifier.weight(1f))
                    } else {
                        CategoryCard(
                            item = item,
                            modifier = Modifier.weight(1f),
                            onEditClick = { onEditClick(item) },
                            onDeleteClick = { onDeleteClick(item) },
                        )
                    }
                }
                repeat(5 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryCard(
    item: CategoryEntity,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .height(86.dp)
            .combinedClickable(
                onClick = onEditClick,
                onLongClick = { if (!item.isDefault) onDeleteClick() },
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(item.icon ?: "📌", fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(item.name, fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddCategoryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(86.dp)
            .combinedClickable(onClick = onClick, onLongClick = null),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text("添加", fontSize = 11.sp)
        }
    }
}

private data class EditorState(
    val type: String,
    val category: CategoryEntity?,
)

@Composable
private fun CategoryEditorDialog(
    state: EditorState,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(state) { mutableStateOf(state.category?.name ?: "") }
    var icon by remember(state) { mutableStateOf(state.category?.icon ?: "") }
    val isEdit = state.category != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "编辑分类" else "新增分类") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("图标（可选）") },
                    singleLine = true,
                )
                Text(
                    if (state.type == CategoryType.EXPENSE) "类型：支出" else "类型：收入",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, icon) },
                enabled = name.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEdit && state.category?.isDefault == false) {
                    TextButton(onClick = onDelete) { Text("删除") }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}
