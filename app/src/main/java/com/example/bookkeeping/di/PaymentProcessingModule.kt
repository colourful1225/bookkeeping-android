package com.example.bookkeeping.di

import android.content.Context
import com.example.bookkeeping.data.local.dao.TransactionDao
import com.example.bookkeeping.notification.AccessibilityPerformanceOptimizer
import com.example.bookkeeping.notification.ConflictDetector
import com.example.bookkeeping.notification.EnhancedAmountExtractor
import com.example.bookkeeping.notification.MerchantCategoryLearner
import com.example.bookkeeping.notification.ObfuscationStrategy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 自动记账系统依赖注入配置。
 *
 * 集成 5 个核心改进模块：
 * 1. EnhancedAmountExtractor - 多格式金额提取
 * 2. MerchantCategoryLearner - 商户分类学习库
 * 3. ConflictDetector - 交易冲突检测
 * 4. ObfuscationStrategy - 隐蔽性优化
 * 5. AccessibilityPerformanceOptimizer - 无障碍性能优化
 */
@Module
@InstallIn(SingletonComponent::class)
object PaymentProcessingModule {

    @Singleton
    @Provides
    fun provideEnhancedAmountExtractor(): EnhancedAmountExtractor =
        EnhancedAmountExtractor

    @Singleton
    @Provides
    fun provideMerchantCategoryLearner(): MerchantCategoryLearner =
        MerchantCategoryLearner()

    @Singleton
    @Provides
    fun provideConflictDetector(
        transactionDao: TransactionDao,
    ): ConflictDetector =
        ConflictDetector(transactionDao)

    @Singleton
    @Provides
    fun provideObfuscationStrategy(): ObfuscationStrategy =
        ObfuscationStrategy()

    @Singleton
    @Provides
    fun provideAccessibilityPerformanceOptimizer(): AccessibilityPerformanceOptimizer =
        AccessibilityPerformanceOptimizer()
}
