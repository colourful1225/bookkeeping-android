package com.example.bookkeeping.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookkeeping.domain.model.ReportData
import com.example.bookkeeping.domain.model.ReportPeriodFactory
import com.example.bookkeeping.domain.model.ReportPeriodType
import com.example.bookkeeping.domain.usecase.GetReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** 报表页 UI 状态。 */
sealed interface ReportUiState {
    data object Loading : ReportUiState
    data class Success(val data: ReportData) : ReportUiState
    data class Error(val message: String) : ReportUiState
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val useCase: GetReportUseCase,
) : ViewModel() {

    private val _periodType = MutableStateFlow(ReportPeriodType.MONTH)
    val periodType: StateFlow<ReportPeriodType> = _periodType.asStateFlow()

    private val _referenceDate = MutableStateFlow(LocalDate.now())

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Loading)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        loadReport(_periodType.value, _referenceDate.value)
    }

    fun selectPeriod(type: ReportPeriodType) {
        if (_periodType.value == type) return
        _periodType.value = type
        loadReport(type, _referenceDate.value)
    }

    fun refresh() = loadReport(_periodType.value, _referenceDate.value)

    fun shiftPeriod(delta: Int) {
        val current = _referenceDate.value
        val next = when (_periodType.value) {
            ReportPeriodType.WEEK -> current.plusWeeks(delta.toLong())
            ReportPeriodType.MONTH -> current.plusMonths(delta.toLong())
            ReportPeriodType.YEAR -> current.plusYears(delta.toLong())
        }
        _referenceDate.value = next
        loadReport(_periodType.value, next)
    }

    private fun loadReport(type: ReportPeriodType, referenceDate: LocalDate) {
        _uiState.value = ReportUiState.Loading
        viewModelScope.launch {
            runCatching {
                val period = when (type) {
                    ReportPeriodType.WEEK  -> ReportPeriodFactory.week(referenceDate)
                    ReportPeriodType.MONTH -> ReportPeriodFactory.month(referenceDate)
                    ReportPeriodType.YEAR  -> ReportPeriodFactory.year(referenceDate)
                }
                useCase(period)
            }.onSuccess { data ->
                _uiState.value = ReportUiState.Success(data)
            }.onFailure { e ->
                _uiState.value = ReportUiState.Error(e.message ?: "未知错误")
            }
        }
    }
}

