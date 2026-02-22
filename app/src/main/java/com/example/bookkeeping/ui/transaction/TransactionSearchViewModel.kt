package com.example.bookkeeping.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookkeeping.data.local.entity.TransactionEntity
import com.example.bookkeeping.domain.model.SearchFilter
import com.example.bookkeeping.domain.usecase.ObserveTransactionsUseCase
import com.example.bookkeeping.domain.usecase.SearchTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 交易列表搜索和过滤 ViewModel
 */
@HiltViewModel
class TransactionSearchViewModel @Inject constructor(
    private val observeTransactionsUseCase: ObserveTransactionsUseCase,
    private val searchTransactionsUseCase: SearchTransactionsUseCase,
) : ViewModel() {
    
    // 搜索条件
    private val _searchFilter = MutableStateFlow(SearchFilter())
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()
    
    // 搜索关键词
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // 高级过滤展开状态
    private val _isAdvancedFilterExpanded = MutableStateFlow(false)
    val isAdvancedFilterExpanded: StateFlow<Boolean> = _isAdvancedFilterExpanded.asStateFlow()
    
    // 搜索结果
    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionEntity>> = _searchFilter
        .flatMapLatest { filter ->
            if (filter.hasFilters()) {
                searchTransactionsUseCase.execute(filter)
            } else {
                observeTransactionsUseCase()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )
    
    /**
     * 更新搜索条件
     */
    fun updateFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }
    
    /**
     * 更新搜索关键词
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        // 同时更新过滤条件中的查询字段
        _searchFilter.value = _searchFilter.value.copy(query = query.takeIf { it.isNotBlank() })
    }
    
    /**
     * 切换高级过滤面板
     */
    fun toggleAdvancedFilter() {
        _isAdvancedFilterExpanded.value = !_isAdvancedFilterExpanded.value
    }
    
    /**
     * 清除所有过滤条件
     */
    fun clearAllFilters() {
        _searchFilter.value = SearchFilter()
        _searchQuery.value = ""
        _isAdvancedFilterExpanded.value = false
    }
    
    /**
     * 按交易类型过滤
     */
    fun filterByType(type: String?) {
        _searchFilter.value = _searchFilter.value.copy(type = type)
    }
    
    /**
     * 按日期范围过滤
     */
    fun filterByDateRange(startDate: java.time.LocalDate?, endDate: java.time.LocalDate?) {
        _searchFilter.value = _searchFilter.value.copy(
            startDate = startDate,
            endDate = endDate,
        )
    }
    
    /**
     * 按金额范围过滤
     */
    fun filterByAmountRange(minAmount: Long?, maxAmount: Long?) {
        _searchFilter.value = _searchFilter.value.copy(
            minAmount = minAmount,
            maxAmount = maxAmount,
        )
    }
}
