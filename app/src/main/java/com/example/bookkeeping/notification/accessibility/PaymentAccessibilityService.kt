package com.example.bookkeeping.notification.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.bookkeeping.notification.AccessibilityPerformanceOptimizer
import com.example.bookkeeping.notification.AutoImportSettingsManager
import com.example.bookkeeping.notification.PaymentAutoImporter
import com.example.bookkeeping.notification.PaymentNotificationParser
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 微信/支付宝支付成功页无障碍读取服务。
 *
 * ## 工作原理
 * 1. 监听 [AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED]（新页面opened）。
 * 2. 判断当前包名是否为微信或支付宝。
 * 3. 遍历 AccessibilityNodeInfo 树，收集所有文本节点。
 * 4. 交给 [WeChatAccessibilityParser] / [AlipayAccessibilityParser] 尝试解析。
 * 5. 解析成功后调用 [PaymentAutoImporter.importAsync]，由 [ReconciliationEngine] 负责去重。
 *
 * ## 触发条件（用户必须完成）
 * - 在系统设置 → 无障碍 → 安装的应用 → 记账本 中启用本服务。
 * - [AutoImportSettingsManager.importMode] 为 [ImportMode.NOTIFICATION_AND_ACCESSIBILITY]。
 *
 * ## 隐私保证
 * - 仅处理 [PaymentNotificationParser.WECHAT_PACKAGE] 和 [PaymentNotificationParser.ALIPAY_PACKAGE] 的事件。
 * - 仅提取文本，不拦截或模拟任何用户操作。
 * - 提取到的信息只在本地处理，不上传。
 */
@AndroidEntryPoint
class PaymentAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "PayA11yService"

        /**
         * 同一窗口包名的最小处理间隔（毫秒）。
         * 防止页面内容反复变化导致多次解析同一支付。
         */
        private const val PARSE_COOLDOWN_MS = 8_000L
    }

    @Inject
    lateinit var importer: PaymentAutoImporter

    @Inject
    lateinit var settings: AutoImportSettingsManager
    
    @Inject
    lateinit var perfOptimizer: AccessibilityPerformanceOptimizer

    /** 记录最近一次对各包名的处理时间，实现冷却限速 */
    private val lastParseTime = mutableMapOf<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        trace("服务已连接 mode=${settings.importMode} accessibilityEnabled=${settings.isAccessibilityEnabled}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 只在无障碍模式开启时处理
        if (!settings.isAccessibilityEnabled) {
            trace("忽略事件：模式未开启 eventType=${event.eventType} package=${event.packageName}")
            return
        }

        val pkg = event.packageName?.toString() ?: return
        if (pkg != PaymentNotificationParser.WECHAT_PACKAGE &&
            pkg != PaymentNotificationParser.ALIPAY_PACKAGE
        ) {
            trace("忽略事件：非目标应用 package=$pkg eventType=${event.eventType}")
            return
        }

        // 仅处理页面切换事件，减少对目标应用的干扰
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            trace("忽略事件：事件类型不匹配 package=$pkg eventType=${event.eventType}")
            return
        }

        trace("命中事件：package=$pkg eventType=${event.eventType}")

        val now = System.currentTimeMillis()
        val last = lastParseTime[pkg] ?: 0L
        if (now - last < PARSE_COOLDOWN_MS) {
            trace("忽略事件：冷却中 package=$pkg delta=${now - last}ms")
            return
        }
        lastParseTime[pkg] = now

        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            trace("解析中断：rootInActiveWindow 为空 package=$pkg")
            return
        }
        val texts = collectNodeTexts(rootNode)
        rootNode.recycle()

        if (texts.isEmpty()) {
            trace("解析中断：节点文本为空 package=$pkg")
            return
        }

        Log.d(TAG, "[$pkg] 收集到 ${texts.size} 个节点文本，尝试解析")

        val payment = when (pkg) {
            PaymentNotificationParser.WECHAT_PACKAGE ->
                WeChatAccessibilityParser.parse(texts, pkg, now)
            PaymentNotificationParser.ALIPAY_PACKAGE ->
                AlipayAccessibilityParser.parse(texts, now)
            else -> null
        }
        if (payment == null) {
            trace("解析失败：未命中支付成功页模式 package=$pkg nodes=${texts.size}")
            return
        }

        Log.d(TAG, "[$pkg] 无障碍解析成功: ¥${payment.amountFen / 100.0} ${payment.merchantName}")
        trace(
            "准备入账：source=${payment.source} amountFen=${payment.amountFen} " +
                "merchant=${payment.merchantName ?: "-"}",
        )
        importer.importAsync(payment)
        trace("已提交入账队列：source=${payment.source} amountFen=${payment.amountFen}")
    }

    override fun onInterrupt() {
        Log.w(TAG, "无障碍服务被中断")
        trace("服务中断")
    }

    // ── 节点树文本收集 ────────────────────────────────────────────────────

    /**
     * 优化的文本收集：使用 BFS + 深度限制 + 关键词优先，替代原来的 DFS 递归。
     *
     * @return 收集到的文本列表（最多 100 条）
     */
    private fun collectNodeTexts(root: AccessibilityNodeInfo): List<String> {
        // ▶ 无障碍性能优化：BFS + 深度限制（由原来的无限深度 → 20 层）
        return perfOptimizer.collectNodeTextsOptimized(
            rootNode = root,
            maxResults = 100
        )
    }

    private fun trace(message: String) {
        if (!settings.isAccessibilityTraceEnabled) return
        Log.i(TAG, "[TRACE] $message")
    }
}
