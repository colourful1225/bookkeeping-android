package com.example.bookkeeping.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookkeeping.data.local.dao.CategoryDao
import com.example.bookkeeping.data.local.entity.CategoryEntity
import com.example.bookkeeping.data.local.entity.TransactionEntity
import com.example.bookkeeping.domain.usecase.AddExpenseUseCase
import com.example.bookkeeping.domain.usecase.ObserveTransactionsUseCase
import com.example.bookkeeping.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionListUiState(
    val isLoading: Boolean = true,
    val transactions: List<TransactionEntity> = emptyList(),
    val categoryMap: Map<String, CategoryEntity> = emptyMap(),
    val errorMessage: String? = null,
)

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    observeTransactions: ObserveTransactionsUseCase,
    categoryDao: CategoryDao,
    private val addExpense: AddExpenseUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val uiState: StateFlow<TransactionListUiState> =
        combine(
            observeTransactions(),
            categoryDao.observeAll(),
        ) { txs, categories ->
            TransactionListUiState(
                isLoading = false,
                transactions = txs,
                categoryMap = categories.associateBy { it.id },
            )
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionListUiState())

    fun addSampleExpense() {
        viewModelScope.launch {
            try {
                addExpense(amount = 1000L, categoryId = "food", note = "示例支出")
                SyncScheduler.enqueueOneShot(context)
            } catch (e: IllegalArgumentException) {
                // TODO: 将 e.message 反映到 UiState.errorMessage
            }
        }
    }
}
