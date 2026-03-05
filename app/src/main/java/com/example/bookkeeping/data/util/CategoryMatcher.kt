package com.example.bookkeeping.data.util

import com.example.bookkeeping.data.local.dao.CategoryDao
import com.example.bookkeeping.data.local.entity.CategoryEntity

/**
 * CSV 导入时的分类匹配器
 * 
 * 根据 CSV 中的分类名称自动匹配数据库中的分类 ID
 */
object CategoryMatcher {
    
    /**
     * 匹配分类名称到分类 ID
     * @param categoryName CSV 中的分类名称
     * @param categories 可用的分类列表
     * @return 匹配到的分类 ID，未找到则返回默认分类 "others"
     */
    suspend fun matchCategoryName(
        categoryName: String,
        categories: List<CategoryEntity>,
    ): String {
        if (categoryName.isBlank()) return "others"
        
        val trimmed = categoryName.trim()
        
        // 精确匹配
        categories.firstOrNull { it.name == trimmed }?.let { return it.id }
        
        // 模糊匹配 - 前缀匹配
        categories.firstOrNull { 
            it.name.startsWith(trimmed, ignoreCase = true) 
        }?.let { return it.id }
        
        // 模糊匹配 - 包含匹配
        categories.firstOrNull { 
            it.name.contains(trimmed, ignoreCase = true) 
        }?.let { return it.id }
        
        // 模糊匹配 - 相似度匹配（编辑距离）
        return categories.minByOrNull { 
            levenshteinDistance(trimmed.lowercase(), it.name.lowercase()) 
        }?.id ?: "others"
    }
    
    /**
     * 计算编辑距离（Levenshtein Distance）用于模糊匹配
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        return when {
            s1.isEmpty() -> s2.length
            s2.isEmpty() -> s1.length
            else -> {
                val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
                for (i in 0..s1.length) dp[i][0] = i
                for (j in 0..s2.length) dp[0][j] = j
                
                for (i in 1..s1.length) {
                    for (j in 1..s2.length) {
                        val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                        dp[i][j] = minOf(
                            dp[i - 1][j] + 1,      // 删除
                            dp[i][j - 1] + 1,      // 插入
                            dp[i - 1][j - 1] + cost // 替换
                        )
                    }
                }
                dp[s1.length][s2.length]
            }
        }
    }
}
