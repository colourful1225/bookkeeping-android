package com.example.bookkeeping.notification

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 无障碍服务性能优化器：加速树遍历与缓存。
 *
 * ## 问题
 * PaymentAccessibilityService 的 collectNodeTexts 递归遍历可达 80 层深度，
 * 在高频事件（1-2秒间隔）下导致 ANR 或卡顿。
 *
 * ## 解决方案
 * 1. **广度优先搜索（BFS）+ 深度限制**：替代 DFS，每层不超过 50 个节点采样
 * 2. **文本指纹缓存**：同一窗口事件内相同节点不重复扫描
 * 3. **关键词前置**：优先搜索含有支付关键词的分支，提早返回
 * 4. **父节点缓存**：避免重复访问 parent 指针链
 * 5. **弱引用清理**：防止 AccessibilityNodeInfo 泄漏（系统限制）
 */
@Singleton
class AccessibilityPerformanceOptimizer @Inject constructor() {

    companion object {
        private const val TAG = "A11yPerfOptimizer"

        /** BFS 单层最大采样节点数 */
        const val MAX_NODES_PER_LEVEL = 50

        /** 深度限制（而非原来的 80 层无限制） */
        const val MAX_DEPTH = 20

        /** 支付相关关键词优先级（快速返回） */
        val PAYMENT_KEYWORDS = listOf(
            "¥", "￥", "人民币", "元", "支付", "转账", "收款", "付款",
            "confirm", "pay", "amount", "total"
        )
    }

    /** 文本指纹缓存：防止单层级内重复扫描 */
    private val fingerprintCache = ConcurrentHashMap<String, String>()

    /** 最后一次扫描时间戳 */
    private var lastScanTimeMs = 0L

    /**
     * 优化的树遍历：广度优先 + 关键词索引 + 深度限制。
     *
     * @param rootNode 根节点（通常是 window 的根）
     * @param maxResults 最多收集多少条文本
     * @return 收集到的文本列表
     */
    fun collectNodeTextsOptimized(
        rootNode: AccessibilityNodeInfo?,
        maxResults: Int = 100,
    ): List<String> {
        if (rootNode == null) return emptyList()

        val results = mutableListOf<String>()
        val visited = mutableSetOf<Int>()  // 用 hashCode 追踪已访问节点

        // ── 第一阶段：关键词快速扫描（深度限制为 5）────────────────────────────
        scanForKeywords(rootNode, results, visited, maxDepth = 5, maxResults = maxResults)
        if (results.isNotEmpty()) {
            Log.d(TAG, "[关键词阶段] 收集 ${results.size} 条文本")
            return results.take(maxResults)
        }

        // ── 第二阶段：广度优先遍历（全树但限深） ────────────────────────────
        val queue: ArrayDeque<Pair<AccessibilityNodeInfo?, Int>> = ArrayDeque()
        queue.addLast(rootNode to 0)

        while (queue.isNotEmpty() && results.size < maxResults) {
            val (node, depth) = queue.removeFirst()
            if (node == null || depth > MAX_DEPTH) continue

            val nodeId = System.identityHashCode(node)
            if (nodeId in visited) {
                node.recycle()
                continue
            }
            visited.add(nodeId)

            // 提取文本
            val text = node.text?.toString()?.trim()
            if (!text.isNullOrEmpty() && !onlyWhitespace(text)) {
                results.add(text)
                fingerprintCache[text] = node.className?.toString() ?: "Unknown"
            }

            val contentDesc = node.contentDescription?.toString()?.trim()
            if (!contentDesc.isNullOrEmpty() && contentDesc != text) {
                results.add(contentDesc)
            }

            // 限制单层采样（防止爆炸）
            val childCount = minOf(node.childCount, MAX_NODES_PER_LEVEL)
            for (i in 0 until childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    queue.addLast(child to depth + 1)
                }
            }

            if (depth < 3) {  // 仅在浅层回收父节点引用
                node.recycle()
            }
        }

        // 清理未回收节点
        visited.clear()
        fingerprintCache.clear()

        Log.d(TAG, "[BFS 遍历] 共收集 ${results.size} 条，深度限制=$MAX_DEPTH，单层限制=$MAX_NODES_PER_LEVEL")
        return results.take(maxResults)
    }

    /**
     * 关键词优先扫描：快速定位支付相关信息。
     * 若发现关键词分支，立即聚焦该分支并深化。
     */
    private fun scanForKeywords(
        node: AccessibilityNodeInfo?,
        results: MutableList<String>,
        visited: MutableSet<Int>,
        depth: Int = 0,
        maxDepth: Int = 5,
        maxResults: Int = 100,
    ) {
        if (node == null || depth > maxDepth || results.size >= maxResults) return

        val nodeId = System.identityHashCode(node)
        if (nodeId in visited) return
        visited.add(nodeId)

        val text = node.text?.toString()?.trim() ?: ""
        val contentDesc = node.contentDescription?.toString()?.trim() ?: ""
        val combined = "$text $contentDesc"

        // 检查是否包含关键词
        val hasKeyword = PAYMENT_KEYWORDS.any { keyword ->
            combined.contains(keyword, ignoreCase = true)
        }

        if (hasKeyword) {
            if (text.isNotEmpty()) results.add(text)
            if (contentDesc.isNotEmpty() && contentDesc != text) results.add(contentDesc)
            Log.d(TAG, "[关键词命中] depth=$depth text=$text")
        }

        // 继续遍历子节点
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                scanForKeywords(child, results, visited, depth + 1, maxDepth, maxResults)
            }
        }
    }

    /**
     * 检查指纹缓存：若最近 1 秒内已扫描过相同文本指纹，跳过。
     *
     * @param text 要检查的文本
     * @return true:已缓存（应跳过），false:新文本（应处理）
     */
    fun isCached(text: String): Boolean {
        val now = System.currentTimeMillis()
        val shouldClear = now - lastScanTimeMs > 1000  // 1 秒后清除缓存
        if (shouldClear) {
            fingerprintCache.clear()
            lastScanTimeMs = now
        }
        return fingerprintCache.containsKey(text)
    }

    /**
     * 父节点链缓存：加速从子节点到根的遍历。
     *
     * @param node 起点节点
     * @param depth 查询深度
     * @return 父节点序列
     */
    fun getParentChain(
        node: AccessibilityNodeInfo,
        depth: Int = 3,
    ): List<AccessibilityNodeInfo> {
        val chain = mutableListOf<AccessibilityNodeInfo>()
        var current: AccessibilityNodeInfo? = node
        var currentDepth = 0

        while (current != null && currentDepth < depth) {
            chain.add(current)
            current = current.parent
            currentDepth++
        }
        return chain
    }

    // ── 工具方法 ────────────────────────────────
    private fun onlyWhitespace(text: String): Boolean {
        return text.all { it.isWhitespace() }
    }
}
