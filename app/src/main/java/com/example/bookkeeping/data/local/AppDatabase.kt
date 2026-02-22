package com.example.bookkeeping.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.bookkeeping.data.local.dao.CategoryDao
import com.example.bookkeeping.data.local.dao.OutboxDao
import com.example.bookkeeping.data.local.dao.ReportDao
import com.example.bookkeeping.data.local.dao.SyncStateDao
import com.example.bookkeeping.data.local.dao.TransactionDao
import com.example.bookkeeping.data.local.entity.CategoryEntity
import com.example.bookkeeping.data.local.entity.OutboxOpEntity
import com.example.bookkeeping.data.local.entity.SyncStateEntity
import com.example.bookkeeping.data.local.entity.TransactionEntity

/**
 * Room 数据库单例定义。
 *
 * - exportSchema = true：将表结构导出到 /schemas 目录，纳入版本控制，方便后续 Migration 审查。
 * - 通过 Hilt [DatabaseModule] 以单例方式注入，不可直接实例化。
 */
@Database(
    entities = [
        TransactionEntity::class,
        OutboxOpEntity::class,
        SyncStateEntity::class,
        CategoryEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun outboxDao(): OutboxDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun reportDao(): ReportDao
    abstract fun categoryDao(): CategoryDao
}
