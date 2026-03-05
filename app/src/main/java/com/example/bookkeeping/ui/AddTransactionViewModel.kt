package com.example.bookkeeping.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookkeeping.data.local.entity.CategoryEntity
import com.example.bookkeeping.data.local.entity.CategoryType
import com.example.bookkeeping.data.local.dao.CategoryDao
import com.example.bookkeeping.data.local.dao.TransactionDao
import com.example.bookkeeping.domain.usecase.AddExpenseUseCase
import com.example.bookkeeping.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 新增交易屏幕的 ViewModel。
 *
 * 管理表单状态：金额、分类、日期、备注。
 */
@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addExpenseUseCase: AddExpenseUseCase,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    data class FormState(
        val amount: String = "",
        val type: String = CategoryType.EXPENSE,
        val categoryId: String = "",
        val selectedDate: Long = System.currentTimeMillis(),
        val note: String = "",
        val photoUri: String? = null,  // 凭证照片 URI (content:// 格式)
        val categories: List<CategoryEntity> = emptyList(),
        val isSubmitting: Boolean = false,
        val error: String? = null,
        val success: Boolean = false,
        val calculatorExpression: String = "",  // 计算表达式 (如 "123 + 456")
    )

    private val _formState = MutableStateFlow(FormState())
    val formState: StateFlow<FormState> = _formState.asStateFlow()

    init {
        loadCategories()
    }

    /**
     * 加载所有可用分类（按使用频率排序）。
     */
    private fun loadCategories(type: String = CategoryType.EXPENSE) {
        viewModelScope.launch {
            categoryDao.observeByType(type).collect { categories ->
                // 获取分类使用频率
                val frequencies = try {
                    transactionDao.getCategoryFrequency(type).associate { 
                        it.categoryId to it.frequency 
                    }
                } catch (e: Exception) {
                    emptyMap()
                }
                
                // 按频率排序，频率相同的保持原有顺序
                val sortedCategories = categories.sortedByDescending { 
                    frequencies[it.id] ?: 0 
                }
                
                _formState.value = _formState.value.copy(
                    categories = sortedCategories,
                    categoryId = sortedCategories.firstOrNull()?.id ?: "others"
                )
            }
        }
    }

    /**
     * 更新金额输入。
     */
    fun updateAmount(amount: String) {
        _formState.value = _formState.value.copy(amount = normalizeAmountInput(amount))
    }

    /**
     * 追加数字输入。
     */
    fun appendDigit(digit: Char) {
        val current = _formState.value.amount
        val next = if (current == "0") {
            digit.toString()
        } else {
            current + digit
        }
        _formState.value = _formState.value.copy(amount = normalizeAmountInput(next))
    }

    /**
     * 追加小数点。
     */
    fun appendDot() {
        val current = _formState.value.amount
        if (current.contains('.')) return
        val next = if (current.isBlank()) "0." else "$current."
        _formState.value = _formState.value.copy(amount = next)
    }

    /**
     * 删除最后一位。
     */
    fun backspace() {
        val current = _formState.value.amount
        val next = if (current.isNotEmpty()) current.dropLast(1) else ""
        _formState.value = _formState.value.copy(amount = next)
    }

    /**
     * 清空金额。
     */
    fun clearAmount() {
        _formState.value = _formState.value.copy(amount = "")
    }

    /**
     * 切换交易类型（支出/收入）。
     */
    fun updateType(type: String) {
        if (_formState.value.type == type) return
        _formState.value = _formState.value.copy(type = type)
        loadCategories(type)
    }

    /**
     * 更新选中分类。
     */
    fun updateCategory(categoryId: String) {
        _formState.value = _formState.value.copy(categoryId = categoryId)
    }

    /**
     * 更新选中日期。
     */
    fun updateDate(dateMillis: Long) {
        _formState.value = _formState.value.copy(selectedDate = dateMillis)
    }

    /**
     * 更新备注。
     */
    fun updateNote(note: String) {
        _formState.value = _formState.value.copy(note = note)
    }

    /**
     * 更新照片 URI。
     */
    fun updatePhotoUri(uri: String?) {
        _formState.value = _formState.value.copy(photoUri = uri)
    }

    /**
     * 清除照片。
     */
    fun clearPhoto() {
        _formState.value = _formState.value.copy(photoUri = null)
    }

    /**
     * 打开照片选择器（用于UI触发图片选择）。
     */
    fun openPhotoSelector() {
        // 这个方法由UI层调用后，触发 Activity 的 Intent
        // 实际的照片 URI 会通过 updatePhotoUri 更新
    }

    /**
     * 处理计算器运算符。
     */
    fun handleOperator(operator: Char) {
        val state = _formState.value
        val expr = state.calculatorExpression + state.amount + " $operator "
        _formState.value = state.copy(
            calculatorExpression = expr,
            amount = "",  // 清空当前输入，等待下一个数字
        )
    }

    /**
     * 计算表达式结果。
     */
    fun calculateResult() {
        val state = _formState.value
        if (state.calculatorExpression.isBlank() || state.amount.isBlank()) return

        try {
            val expr = (state.calculatorExpression + state.amount).replace("×", "*").replace("÷", "/")
            val result = eval(expr)
            _formState.value = state.copy(
                amount = result.toString(),
                calculatorExpression = "",  // 清空表达式
            )
        } catch (e: Exception) {
            val detail = e.message ?: context.getString(R.string.error_unknown)
            _formState.value = state.copy(
                error = context.getString(R.string.error_calculation_failed, detail)
            )
        }
    }

    /**
     * 清除所有（包括表达式）。
     */
    fun clearAll() {
        _formState.value = _formState.value.copy(
            amount = "",
            calculatorExpression = "",
        )
    }

    /**
     * 加载现有交易进行编辑
     */
    fun loadTransaction(transactionId: String) {
        viewModelScope.launch {
            try {
                val transaction = transactionDao.findById(transactionId)
                if (transaction != null) {
                    _formState.value = _formState.value.copy(
                        amount = String.format(java.util.Locale.US, "%.2f", transaction.amount / 100.0),
                        type = transaction.type,
                        categoryId = transaction.categoryId,
                        selectedDate = transaction.occurredAt,
                        note = transaction.note ?: "",
                        photoUri = transaction.photoUri,
                    )
                    loadCategories(transaction.type)
                }
            } catch (e: Exception) {
                val detail = e.message ?: context.getString(R.string.error_unknown)
                _formState.value = _formState.value.copy(
                    error = context.getString(R.string.error_load_transaction_failed, detail)
                )
            }
        }
    }
    private fun eval(expression: String): Double {
        // 这是一个简化的实现，生产环境可使用 JavaScript 引擎或其他库
        return try {
            val parts = expression.split(Regex("(?=[-+*/])|(?<=[--+*/])"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            var result = parts[0].toDouble()
            var i = 1
            while (i < parts.size) {
                if (i >= parts.size - 1) break
                val op = parts[i]
                val num = parts[i + 1].toDouble()
                result = when (op) {
                    "+" -> result + num
                    "-" -> result - num
                    "*" -> result * num
                    "/" -> if (num != 0.0) result / num else throw Exception(
                        context.getString(R.string.error_divide_by_zero)
                    )
                    else -> result
                }
                i += 2
            }
            result
        } catch (e: Exception) {
            throw Exception(context.getString(R.string.error_expression_parse))
        }
    }

    /**
     * 提交表单，新增交易。
     */
    fun submitForm(onSuccess: () -> Unit = {}) {
        val state = _formState.value
        
        // 验证
        val error = validateForm(state)
        if (error != null) {
            _formState.value = state.copy(error = error)
            return
        }

        _formState.value = state.copy(isSubmitting = true, error = null)

        viewModelScope.launch {
            try {
                val amountValue = state.amount.trimEnd('.').toDouble()
                val amount = (amountValue * 100).toLong()
                addExpenseUseCase(
                    amount = amount,
                    categoryId = state.categoryId,
                    note = state.note.takeIf { it.isNotBlank() },
                    photoUri = state.photoUri,
                )
                
                _formState.value = _formState.value.copy(
                    success = true,
                    isSubmitting = false,
                )
                onSuccess()
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    error = e.message ?: context.getString(R.string.error_add_failed),
                    isSubmitting = false,
                )
            }
        }
    }

    /**
     * 重置表单状态。
     */
    fun resetForm() {
        val currentType = _formState.value.type
        _formState.value = FormState(
            type = currentType,
            categories = _formState.value.categories,
        )
        loadCategories(currentType)
    }

    /**
     * 验证表单。
     */
    private fun validateForm(state: FormState): String? {
        if (state.amount.isBlank()) {
            return context.getString(R.string.error_amount_required)
        }

        return try {
            val amount = state.amount.trimEnd('.').toDouble()
            when {
                amount <= 0 -> context.getString(R.string.error_amount_positive)
                else -> null
            }
        } catch (e: NumberFormatException) {
            context.getString(R.string.error_amount_invalid)
        }
    }

    private fun normalizeAmountInput(value: String): String {
        if (value.isBlank()) return ""
        val clean = value.filter { it.isDigit() || it == '.' }
        if (clean.count { it == '.' } > 1) return clean.substringBeforeLast('.')
        val parts = clean.split('.', limit = 2)
        if (parts.size == 1) return parts[0]
        val decimals = parts[1].take(2)
        return "${parts[0]}.${decimals}"
    }
}
