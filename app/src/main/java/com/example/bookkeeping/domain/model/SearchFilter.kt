package com.example.bookkeeping.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 交易搜索过滤条件
 */
data class SearchFilter(
    // 交易类型（INCOME/EXPENSE/TRANSFER）
    val type: String? = null,
    
    // 分类 ID
    val categoryId: String? = null,
    
    // 日期范围（开始）
    val startDate: LocalDate? = null,
    
    // 日期范围（结束）
    val endDate: LocalDate? = null,
    
    // 金额范围（最小，单位：分）
    val minAmount: Long? = null,
    
    // 金额范围（最大，单位：分）
    val maxAmount: Long? = null,
    
    // 备注关键词（模糊搜索）
    val query: String? = null,
) {
    
    /**
     * 转换为数据库查询参数
     */
    fun toQueryParams(): SearchQueryParams {
        return SearchQueryParams(
            type = type,
            categoryId = categoryId,
            startDate = startDate?.let { 
                it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() 
            },
            endDate = endDate?.let { 
                it.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1 
            },
            minAmount = minAmount,
            maxAmount = maxAmount,
            query = query?.takeIf { it.isNotBlank() },
        )
    }
    
    /**
     * 判断是否有有效的过滤条件
     */
    fun hasFilters(): Boolean {
        return type != null || categoryId != null || startDate != null 
            || endDate != null || minAmount != null || maxAmount != null 
            || !query.isNullOrBlank()
    }
}

/**
 * 数据库查询参数（已转换为 Long 类型的时间戳）
 */
data class SearchQueryParams(
    val type: String?,
    val categoryId: String?,
    val startDate: Long?,
    val endDate: Long?,
    val minAmount: Long?,
    val maxAmount: Long?,
    val query: String?,
)
