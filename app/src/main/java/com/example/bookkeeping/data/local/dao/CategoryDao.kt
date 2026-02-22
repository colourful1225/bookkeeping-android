package com.example.bookkeeping.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.bookkeeping.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 分类数据访问对象。
 */
@Dao
interface CategoryDao {
    /**
     * 获取所有分类（按名称升序）。
     */
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    /**
     * 按类型获取分类（EXPENSE / INCOME）。
     */
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun observeByType(type: String): Flow<List<CategoryEntity>>

    /**
     * 同步获取所有分类。
     */
    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAll(): List<CategoryEntity>

    /**
     * 按类型获取所有分类。
     */
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    suspend fun getByType(type: String): List<CategoryEntity>

    /**
     * 根据 ID 查询分类。
     */
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    /**
     * 插入分类（存在时忽略）。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity)

    /**
     * 批量插入分类。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    /**
     * 更新分类。
     */
    @Update
    suspend fun update(category: CategoryEntity)

    /**
     * 删除分类。
     */
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * 统计分类数量。
     */
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
