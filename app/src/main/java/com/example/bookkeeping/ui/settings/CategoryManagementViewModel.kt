package com.example.bookkeeping.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookkeeping.data.local.dao.CategoryDao
import com.example.bookkeeping.data.local.entity.CategoryEntity
import com.example.bookkeeping.data.local.entity.CategoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryDao: CategoryDao,
) : ViewModel() {

    data class UiState(
        val expenseCategories: List<CategoryEntity> = emptyList(),
        val incomeCategories: List<CategoryEntity> = emptyList(),
    )

    val uiState: StateFlow<UiState> =
        combine(
            categoryDao.observeByType(CategoryType.EXPENSE),
            categoryDao.observeByType(CategoryType.INCOME),
        ) { expense, income ->
            UiState(expenseCategories = expense, incomeCategories = income)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun addCategory(type: String, name: String, icon: String?) {
        viewModelScope.launch {
            val entity = CategoryEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                icon = icon?.trim()?.takeIf { it.isNotEmpty() },
                type = type,
                isDefault = false,
            )
            categoryDao.insert(entity)
        }
    }

    fun updateCategory(category: CategoryEntity, name: String, icon: String?) {
        viewModelScope.launch {
            val updated = category.copy(
                name = name.trim(),
                icon = icon?.trim()?.takeIf { it.isNotEmpty() },
            )
            categoryDao.update(updated)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        if (category.isDefault) return
        viewModelScope.launch {
            categoryDao.deleteById(category.id)
        }
    }
}
