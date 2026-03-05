package com.example.bookkeeping.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookkeeping.data.local.entity.TransactionEntity
import com.example.bookkeeping.domain.model.SearchFilter
import com.example.bookkeeping.domain.usecase.ObserveTransactionsUseCase
import com.example.bookkeeping.domain.usecase.SearchTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject

/**
 * 交易列表搜索和过滤 ViewModel
 */
@HiltViewModel
class TransactionSearchViewModel @Inject constructor(
    private val observeTransactionsUseCase: ObserveTransactionsUseCase,
    private val searchTransactionsUseCase: SearchTransactionsUseCase,
) : ViewModel() {

    // 搜索关键词
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 搜索条件（保留用于高级搜索兼容）
    private val _searchFilter = MutableStateFlow(SearchFilter())
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    // 全部交易流
    private val allTransactions = observeTransactionsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 搜索结果：客户端关键词过滤（同时匹配备注和金额）
    val transactions: StateFlow<List<TransactionEntity>> = combine(
        _searchQuery,
        allTransactions,
    ) { query, all ->
        if (query.isBlank()) {
            all
        } else {
            all.filter { tx ->
                val amountStr = String.format(Locale.US, "%.2f", tx.amount / 100.0)
                val amountStrCN = String.format(java.util.Locale.CHINA, "%.2f", tx.amount / 100.0)
                (tx.note?.contains(query, ignoreCase = true) == true)
                    || amountStr.contains(query)
                    || amountStrCN.contains(query)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    /**
     * 更新搜索关键词（实时触发过滤）
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * 更新搜索条件（保留用于高级搜索兼容）
     */
    fun updateFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }

    /**
     * 清除所有过滤条件
     */
    fun clearAllFilters() {
        _searchFilter.value = SearchFilter()
        _searchQuery.value = ""
    }
}
