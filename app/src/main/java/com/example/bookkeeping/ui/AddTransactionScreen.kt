package com.example.bookkeeping.ui

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bookkeeping.R
import com.example.bookkeeping.data.local.entity.CategoryEntity
import com.example.bookkeeping.data.local.entity.CategoryType
import com.example.bookkeeping.ui.components.CalculatorKeypad
import com.example.bookkeeping.ui.components.NoteAndPhotoSection
import com.example.bookkeeping.ui.util.localizedCategoryName
import com.example.bookkeeping.ui.util.PhotoManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.TimePicker
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel = hiltViewModel(),
    transactionId: String = "",
    onBack: () -> Unit = {},
    onSuccess: () -> Unit = {},
) {
    val formState by viewModel.formState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var isKeypadVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            val savedUri = PhotoManager.savePhotoFromUri(context, it)
            viewModel.updatePhotoUri(savedUri?.toString())
        }
    }

    // 编辑模式：加载交易
    LaunchedEffect(transactionId) {
        if (transactionId.isNotEmpty()) {
            viewModel.loadTransaction(transactionId)
        } else {
            viewModel.clearAll()
        }
    }

    val isEditMode = transactionId.isNotEmpty()
    val screenTitle = if (isEditMode) {
        stringResource(R.string.screen_title_edit_transaction)
    } else {
        stringResource(R.string.screen_title_add_transaction)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.button_back),
                        )
                    }
                },
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .padding(bottom = 240.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AmountHeader(
                    amount = formState.amount,
                    selectedDate = formState.selectedDate,
                    onDateClick = { showDatePicker = true },
                )

                TypeSelector(
                    selectedType = formState.type,
                    onTypeSelected = { viewModel.updateType(it) },
                )

                CategoryGridSection(
                    categories = formState.categories,
                    selectedCategoryId = formState.categoryId,
                    onCategorySelected = { viewModel.updateCategory(it) },
                )

                NoteAndPhotoSection(
                    note = formState.note,
                    onNoteChange = { viewModel.updateNote(it) },
                    photoUri = formState.photoUri,
                    onPhotoSelect = { photoPickerLauncher.launch("image/*") },
                    onPhotoRemove = {
                        formState.photoUri?.let { PhotoManager.deletePhoto(Uri.parse(it)) }
                        viewModel.clearPhoto()
                    },
                )

                formState.error?.let { error ->
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (isKeypadVisible) {
                    CalculatorKeypad(
                        amount = formState.amount,
                        onDigit = { viewModel.appendDigit(it) },
                        onDot = { viewModel.appendDot() },
                        onBackspace = { viewModel.backspace() },
                        onClear = { viewModel.clearAll() },
                        onOperator = { viewModel.handleOperator(it) },
                        onEqual = { viewModel.calculateResult() },
                        onSubmit = {
                            viewModel.submitForm(onSuccess = onSuccess)
                        },
                        onHide = { isKeypadVisible = false },
                        enabled = !formState.isSubmitting,
                        calculatorExpression = formState.calculatorExpression,
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { isKeypadVisible = true }) {
                            Text(stringResource(R.string.button_show_keyboard))
                        }
                    }
                }
            }
        }

        if (showDatePicker) {
            WheelDateTimePickerDialog(
                selectedDate = formState.selectedDate,
                onDateSelected = {
                    viewModel.updateDate(it)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false },
            )
        }
    }
}

@Composable
private fun AmountHeader(
    amount: String,
    selectedDate: Long,
    onDateClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "¥${if (amount.isBlank()) "0.00" else amount}",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        DateChip(
            selectedDate = selectedDate,
            onClick = onDateClick,
        )
    }
}

@Composable
private fun TypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TypeChip(
            label = stringResource(R.string.label_expense_type),
            selected = selectedType == CategoryType.EXPENSE,
            onClick = { onTypeSelected(CategoryType.EXPENSE) },
            modifier = Modifier.weight(1f),
        )
        TypeChip(
            label = stringResource(R.string.label_income_type),
            selected = selectedType == CategoryType.INCOME,
            onClick = { onTypeSelected(CategoryType.INCOME) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun CategoryGridSection(
    categories: List<CategoryEntity>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
) {
    Text(stringResource(R.string.label_category), fontWeight = FontWeight.Bold)
    CategoryGrid(
        categories = categories,
        selectedCategoryId = selectedCategoryId,
        onCategorySelected = onCategorySelected,
    )
}

@Composable
private fun CategoryGrid(
    categories: List<CategoryEntity>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
) {
    val rows = categories.chunked(5)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { category ->
                    CategoryItem(
                        category = category,
                        isSelected = category.id == selectedCategoryId,
                        onClick = { onCategorySelected(category.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(5 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(category.icon ?: "📌", fontSize = 22.sp)
            Text(
                localizedCategoryName(category.id, category.name),
                fontSize = 11.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DateChip(
    selectedDate: Long,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Default.DateRange,
            contentDescription = stringResource(R.string.label_select_date),
        )
        Text(formatHeaderDate(selectedDate), fontSize = 13.sp)
    }
}

@Composable
private fun Keypad(
    amount: String,
    onDigit: (Char) -> Unit,
    onDot: () -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean,
) {
    val buttons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫"),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    KeypadKey(
                        label = key,
                        onClick = {
                            when (key) {
                                "." -> onDot()
                                "⌫" -> onBackspace()
                                else -> onDigit(key.first())
                            }
                        },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeypadKey(
                label = stringResource(R.string.keypad_done),
                onClick = onSubmit,
                enabled = enabled && amount.isNotBlank(),
                modifier = Modifier.weight(1f),
                isPrimary = true,
            )
        }
    }
}

@Composable
private fun KeypadKey(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
) {
    val background = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Card(
        modifier = modifier
            .height(56.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                color = if (enabled) contentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontSize = if (isPrimary) 16.sp else 18.sp,
                fontWeight = if (isPrimary) FontWeight.Medium else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WheelDateTimePickerDialog(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val calendar = remember(selectedDate) { Calendar.getInstance().apply { timeInMillis = selectedDate } }
    var year by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var day by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_select_time)) },
        text = {
            AndroidView(
                factory = { context ->
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        val datePicker = DatePicker(context).apply {
                            init(year, month, day) { _, y, m, d ->
                                year = y
                                month = m
                                day = d
                            }
                        }
                        val timePicker = TimePicker(context).apply {
                            setIs24HourView(true)
                            hour = hour
                            minute = minute
                            setOnTimeChangedListener { _, h, min ->
                                hour = h
                                minute = min
                            }
                        }
                        addView(datePicker)
                        addView(timePicker)
                    }
                },
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(year, month, day, hour, minute, 0)
                    }
                    onDateSelected(cal.timeInMillis)
                },
            ) {
                Text(stringResource(R.string.button_confirm))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.button_cancel)) }
        },
    )
}

fun formatDateForDisplay(timeMillis: Long): String {
    val formatter = DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT,
        Locale.getDefault(),
    )
    return formatter.format(Date(timeMillis))
}

@Composable
private fun formatHeaderDate(timeMillis: Long): String {
    val now = System.currentTimeMillis()
    return if (isSameDay(now, timeMillis)) {
        val timeFormat = SimpleDateFormat(
            stringResource(R.string.format_header_time),
            Locale.getDefault(),
        )
        stringResource(
            R.string.format_header_today,
            timeFormat.format(Date(timeMillis)),
        )
    } else {
        val sdf = SimpleDateFormat(
            stringResource(R.string.format_header_datetime),
            Locale.getDefault(),
        )
        sdf.format(Date(timeMillis))
    }
}

private fun isSameDay(a: Long, b: Long): Boolean {
    val calA = Calendar.getInstance().apply { timeInMillis = a }
    val calB = Calendar.getInstance().apply { timeInMillis = b }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR)
        && calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
}
