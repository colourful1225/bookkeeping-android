package com.example.bookkeeping.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookkeeping.data.local.entity.CategoryEntity
import com.example.bookkeeping.data.local.entity.CategoryType
import com.example.bookkeeping.data.local.dao.CategoryDao
import com.example.bookkeeping.domain.usecase.AddExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    )

    private val _formState = MutableStateFlow(FormState())
    val formState: StateFlow<FormState> = _formState.asStateFlow()

    init {
        loadCategories()
    }

    /**
     * 加载所有可用分类。
     */
    private fun loadCategories(type: String = CategoryType.EXPENSE) {
        viewModelScope.launch {
            categoryDao.observeByType(type).collect { categories ->
                _formState.value = _formState.value.copy(
                    categories = categories,
                    categoryId = categories.firstOrNull()?.id ?: "others"
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
                    error = e.message ?: "添加失败",
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
            return "请输入金额"
        }

        return try {
            val amount = state.amount.trimEnd('.').toDouble()
            when {
                amount <= 0 -> "金额必须大于 0"
                else -> null
            }
        } catch (e: NumberFormatException) {
            "金额格式无效"
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
