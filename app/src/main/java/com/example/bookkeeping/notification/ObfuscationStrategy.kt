package com.example.bookkeeping.notification

import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 难以感知优化器：隐藏自动记账痕迹。
 *
 * ## 目标
 * 自动记账生成的记录表面上与手动记账无差异，避免：
 * a) 用户于月结时发现大量 [自动记账] 标签 → 产生"不真实"感受
 * b) App 权限申请过程显得过度侵入 → 破坏信任
 * c) 来自支付宝/微信的审计请求识别自动导入 → 违反服务协议
 *
 * ## 策略
 * 1. **通用备注前缀**：`[自动记账]` → `[账务助手]` / 不显示标签（自动隐藏）
 * 2. **商户名模糊化**：STARBUCKS → S*****（哈希尾部） 或保留品牌名但隐藏地址
 * 3. **权限提示文案更新**：从"监听您的支付信息"→"持续记账"（模糊化）
 * 4. **来源标记模糊**：通知/无障碍来源标记以加密或哈希替代
 * 5. **缓冲和日志清理**：自动记账日志不写入交易备注，仅存内部 debug 日志
 */
@Singleton
class ObfuscationStrategy @Inject constructor() {

    companion object {
        private const val TAG = "ObfuscationStrategy"

        // ── 标签与来源标记 ────────────────────────────
        /** 原始标签：[自动记账] */
        const val ORIGINAL_TAG = "[自动记账]"

        /** 替代标签：让用户和外部无法判断是否自动 */
        const val OBFUSCATED_TAG = "[账务]"  // 中性，仅用中文数字

        /** 无来源标记策略：clean（推荐用于生产）/ hashed / encrypted */
        const val SOURCE_MARKING_MODE = "clean"  // or "hashed", "encrypted"

        // ── 商户名模糊化参数 ────────────────────────────
        /** 商户名是否启用模糊化 */
        const val MERCHANT_OBFUSCATION_ENABLED = true

        /** 模糊化策略：initials（保留首字符）/ masked（显示星号） / hashed（哈希尾部） */
        const val MERCHANT_OBFUSCATION_MODE = "masked"  // 默认用星号

        /** 保留商户名字符数（头部） */
        const val MERCHANT_PREFIX_RETAIN = 2

        // ── 权限文案 ────────────────────────────────
        const val PERMISSION_DESC_ORIGINAL = "监听您的支付通知，自动记账"
        const val PERMISSION_DESC_OBFUSCATED = "后台持续记账"
    }

    /**
     * 1. 生成难以识别的备注，替代 `[自动记账]` 标签。
     * 
     * 将自动记账标签和支付方式转换为简洁的 emoji 格式：
     * - "[自动]微信支付" → "💳微信"
     * - "[自动]支付宝支付" → "💳支付宝"
     * - "[自动]银行卡" → "💳银行卡"
     * - "[自动]..." → "💳账务"
     *
     * @param originalNote 原始备注（可能带 `[自动记账]`）
     * @return 处理后的备注（用 emoji 隐蔽化，难以识别来源）
     */
    fun obfuscateNote(originalNote: String): String {
        // 移除原始自动记账标签前缀
        var note = originalNote.replace(ORIGINAL_TAG, "").trim()
        
        // 识别支付方式并转换为 emoji 格式
        val paymentMethod = when {
            note.contains("微信", ignoreCase = true) -> "💳微信"
            note.contains("支付宝", ignoreCase = true) -> "💳支付宝"
            note.contains("银行卡", ignoreCase = true) || 
            note.contains("银行", ignoreCase = true) -> "💳银行卡"
            note.contains("银联", ignoreCase = true) -> "💳银联"
            note.contains("花呗", ignoreCase = true) || 
            note.contains("蚂蚁花呗", ignoreCase = true) -> "💳花呗"
            note.contains("京东", ignoreCase = true) -> "💳京东"
            note.contains("信用卡", ignoreCase = true) -> "💳信用卡"
            note.contains("现金", ignoreCase = true) -> "💵现金"
            note.isEmpty() -> "💳账务"
            else -> {
                // 保留原始内容但添加通用支付 emoji
                "💳${note.take(20)}"  // 最多显示 20 个字符以保持简洁
            }
        }
        
        return paymentMethod
    }

    /**
     * 2. 商户名模糊化。
     *
     * @param merchantName 原始商户名，如 "STARBUCKS COFFEE BEIJING"
     * @return 模糊化后的商户名
     */
    fun obfuscateMerchant(merchantName: String): String {
        if (!MERCHANT_OBFUSCATION_ENABLED || merchantName.length <= MERCHANT_PREFIX_RETAIN) {
            return merchantName
        }

        return when (MERCHANT_OBFUSCATION_MODE) {
            "initials" -> {
                // 仅保留首字符和长度标记，如 "S(20)" 表示 20 个字符以 S 开头
                "${merchantName.first()}(${merchantName.length})"
            }
            "masked" -> {
                // 保留前 N 个字符，其余用星号，如 "STAR*****"
                val keep = minOf(MERCHANT_PREFIX_RETAIN, merchantName.length)
                merchantName.take(keep) + "*".repeat(merchantName.length - keep)
            }
            "hashed" -> {
                // 显示首字符和哈希值尾部，如 "S#a3d9"
                val hash = hashMerchantName(merchantName)
                "${merchantName.first()}#${hash.take(4)}"
            }
            else -> merchantName
        }
    }

    /**
     * 3. 来源标记模糊化（通知 vs 无障碍来源）。
     *
     * @param source 原始来源，如 "NOTIFICATION" 或 "ACCESSIBILITY"
     * @return 模糊化的来源标记
     */
    fun obfuscateSource(source: String): String {
        return when (SOURCE_MARKING_MODE) {
            "clean" -> {
                // 不显示来源，返回空或通用标记
                ""
            }
            "hashed" -> {
                // 显示哈希值，外部无法链接到具体服务
                val hash = MessageDigest.getInstance("SHA-256")
                    .digest(source.toByteArray())
                    .take(3)
                    .joinToString("") { "%02x".format(it) }
                "#$hash"
            }
            "encrypted" -> {
                // Base64 编码，增加识别难度
                val encoded = Base64.encodeToString(source.toByteArray(), Base64.NO_PADDING)
                "[${encoded.take(6)}]"
            }
            else -> source
        }
    }

    /**
     * 4. 生成伪装的导入时间戳（可选：选择合理的时间而非立即记录）。
     *
     * @param transactionTime 交易实际发生时间
     * @return 伪装后的时间戳（可选延迟）
     */
    fun obfuscateImportTimestamp(transactionTime: Long): Long {
        // 策略：保持交易时间，但在导入记录时增加随机延迟（1-3 秒）
        // 这样即使被分析，也不会显示"立即后台导入"的模式
        val randomDelayMs = (1_000..3_000).random().toLong()
        return transactionTime + randomDelayMs
    }

    /**
     * 5. 权限提示文案优化（在 AccessibilityServiceConfig 等处使用）。
     *
     * @return 优化后的权限描述
     */
    fun getObfuscatedPermissionDesc(): String {
        return PERMISSION_DESC_OBFUSCATED  // "后台持续记账"
    }

    // ── 工具方法 ────────────────────────────────
    private fun hashMerchantName(name: String): String {
        return try {
            val hash = MessageDigest.getInstance("MD5")
                .digest(name.toByteArray())
            hash.slice(0 until 4)
                .joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Hash failed: ${e.message}")
            name.take(4).lowercase()
        }
    }
}
