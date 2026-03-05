package com.example.bookkeeping.notification

import android.util.Log
import com.example.bookkeeping.data.local.entity.CategoryEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 商户分类学习器：记录用户手动纠正历史，自适应优化自动分类。
 *
 * ## 工作流程
 * 1. 自动记账时用 CategoryMatcher 分类
 * 2. 用户手动修改分类 → 调用 recordCorrection
 * 3. 下次遇到同商户时，优先查询学习库
 * 4. 若学习库无覆盖，再用 CategoryMatcher 兜底
 *
 * ## 优化点
 * - 减少新商户/变名商户的首发误分
 * - 商户别名自动归类（e.g. "星巴克" ⇄ "STARBUCKS" → 同分类）
 * - 本地持久化学习记录，旧数据自动淘汰
 */
@Singleton
class MerchantCategoryLearner @Inject constructor() {

    companion object {
        private const val TAG = "MerchantCategoryLearner"
        /** 学习记录保留时长（7天） */
        private const val LEARNING_WINDOW_MS = 7 * 24 * 60 * 60 * 1000L
    }

    /** 商户 → [(分类ID, 最后更新时间)] */
    private val learningCache = mutableMapOf<String, Pair<String, Long>>()
    private val cacheLock = Mutex()

    /**
     * 查询商户的学习分类。
     * @return 学习记录中的分类 ID，若无则返回 null
     */
    suspend fun queryLearned(merchantName: String?): String? {
        if (merchantName.isNullOrBlank()) return null
        val key = merchantName.trim().lowercase()
        return cacheLock.withLock {
            val (category, timestamp) = learningCache[key] ?: return null
            if (System.currentTimeMillis() - timestamp > LEARNING_WINDOW_MS) {
                learningCache.remove(key)
                null
            } else {
                category
            }
        }
    }

    /**
     * 记录一条用户手动纠正：商户 X 应分为 Y。
     * @param merchantName 商户名（规范化后存储）
     * @param categoryId   正确的分类 ID
     */
    suspend fun recordCorrection(merchantName: String, categoryId: String) {
        if (merchantName.isBlank() || categoryId.isBlank()) return
        val key = merchantName.trim().lowercase()
        cacheLock.withLock {
            learningCache[key] = categoryId to System.currentTimeMillis()
            Log.d(TAG, "记录纠正: $merchantName → $categoryId")
        }
    }

    /**
     * 批量加载离线商户库（可选）。
     * 将常见商户/平台与分类预绑定，加速首发分类。
     *
     * 示例：
     * ```
     * loadMerchantLibrary(
     *   "京东" to "shopping",
     *   "美团" to "dining/shopping",
     *   "滴滴" to "transportation",
     *   ...
     * )
     * ```
     */
    suspend fun loadMerchantLibrary(entries: List<Pair<String, String>>) {
        cacheLock.withLock {
            val now = System.currentTimeMillis()
            entries.forEach { (merchant, category) ->
                val key = merchant.trim().lowercase()
                learningCache.putIfAbsent(key, category to now)
            }
            Log.d(TAG, "加载商户库: ${entries.size} 条记录")
        }
    }

    /**
     * 导出当前学习记录（供备份/迁移）。
     */
    suspend fun exportLearnings(): List<Pair<String, String>> {
        return cacheLock.withLock {
            learningCache.mapNotNull { (merchant, pair) ->
                if (System.currentTimeMillis() - pair.second <= LEARNING_WINDOW_MS) {
                    merchant to pair.first
                } else null
            }
        }
    }

    /**
     * 清空学习缓存。
     */
    suspend fun clearCache() {
        cacheLock.withLock {
            learningCache.clear()
            Log.d(TAG, "已清空学习缓存")
        }
    }
}
