package com.example.bookkeeping.di

import android.content.Context
import androidx.room.Room
import com.example.bookkeeping.data.local.AppDatabase
import com.example.bookkeeping.data.local.dao.CategoryDao
import com.example.bookkeeping.data.local.dao.OutboxDao
import com.example.bookkeeping.data.local.dao.ReportDao
import com.example.bookkeeping.data.local.dao.SyncStateDao
import com.example.bookkeeping.data.local.dao.TransactionDao
import com.example.bookkeeping.data.local.entity.DefaultCategories
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "bookkeeping.db")
            .fallbackToDestructiveMigration()   // TODO: 生产环境替换为显式 Migration
            .build()
        
        // 初始化默认分类
        runBlocking {
            DatabaseInitializer.initializeDefaultCategories(db)
        }
        
        return db
    }

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideOutboxDao(db: AppDatabase): OutboxDao = db.outboxDao()

    @Provides
    fun provideSyncStateDao(db: AppDatabase): SyncStateDao = db.syncStateDao()

    @Provides
    fun provideReportDao(db: AppDatabase): ReportDao = db.reportDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
}

/**
 * 数据库初始化工具。
 */
object DatabaseInitializer {
    /**
     * 初始化默认分类。
     * 若分类表为空，则插入所有预置分类。
     */
    suspend fun initializeDefaultCategories(db: AppDatabase) {
        try {
            val count = db.categoryDao().count()
            if (count == 0) {
                db.categoryDao().insertAll(DefaultCategories.ALL)
            }
        } catch (e: Exception) {
            // 如果表不存在（新建），捕获异常继续
            android.util.Log.w("DatabaseInit", "Failed to init categories: ${e.message}")
        }
    }
}
