package com.example.bookkeeping.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookkeeping.data.local.dao.CategoryDao
import com.example.bookkeeping.data.local.dao.TransactionDao
import com.example.bookkeeping.data.local.entity.CategoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class BudgetCategoryUi(
    val categoryId: String,
    val icon: String,
    val name: String,
    val budget: Int,
    val spent: Int,
)

data class BudgetManagementUiState(
    val budgetAlertEnabled: Boolean = true,
    val items: List<BudgetCategoryUi> = emptyList(),
)

@HiltViewModel
class BudgetManagementViewModel @Inject constructor(
    categoryDao: CategoryDao,
    transactionDao: TransactionDao,
    private val budgetSettingsManager: BudgetSettingsManager,
) : ViewModel() {

    private val refreshTick = MutableStateFlow(0)

    val uiState: StateFlow<BudgetManagementUiState> = combine(
        categoryDao.observeByType(CategoryType.EXPENSE),
        transactionDao.observeAll(),
        refreshTick,
    ) { categories, transactions, _ ->
        val budgetMap = budgetSettingsManager.getBudgetMap()
        val startOfMonth = LocalDate.now()
            .withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val spentByCategory = transactions
            .asSequence()
            .filter { it.type == CategoryType.EXPENSE && it.occurredAt >= startOfMonth }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        BudgetManagementUiState(
            budgetAlertEnabled = budgetSettingsManager.budgetAlertEnabled,
            items = categories.map { category ->
                BudgetCategoryUi(
                    categoryId = category.id,
                    icon = category.icon ?: "📌",
                    name = category.name,
                    budget = budgetMap[category.id] ?: 0,
                    spent = ((spentByCategory[category.id] ?: 0L) / 100L).toInt(),
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetManagementUiState())

    fun updateCategoryBudget(categoryId: String, budget: Int) {
        viewModelScope.launch {
            budgetSettingsManager.setBudget(categoryId, budget)
            refreshTick.value = refreshTick.value + 1
        }
    }

    fun updateBudgetAlertEnabled(enabled: Boolean) {
        viewModelScope.launch {
            budgetSettingsManager.budgetAlertEnabled = enabled
            refreshTick.value = refreshTick.value + 1
        }
    }
}
