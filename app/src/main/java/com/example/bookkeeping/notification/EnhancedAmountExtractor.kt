package com.example.bookkeeping.notification

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 增强的金额提取器：支持多格式、繁体、特殊符号。
 *
 * ## 支持格式
 * - `¥28.50` / `￥28.50`（常规）
 * - `¥1,234.56` / `¥1234.56`（千位分隔符）
 * - `人民币 28 元 50 分`（拆分）
 * - `¥28` / `¥28.5`（无分位或单分位）
 * - `壹仟贰佰叁拾肆元` （繁体，6位以内）
 */
object EnhancedAmountExtractor {

    /**
     * 从文本中提取金额（分）。
     * @return 成功返回金额（分），失败返回 null
     */
    fun extract(text: String): Long? {
        // 1. 尝试常规 ¥ 后跟数字格式
        extractYuanFormat(text)?.let { return it }

        // 2. 尝试"人民币 X 元 Y 分"格式
        extractRmbFormat(text)?.let { return it }

        // 3. 尝试繁体数字格式
        extractChineseFormat(text)?.let { return it }

        // 4. 尝试"X 元"独立格式（需支付关键词附近）
        extractYuanStandaloneFormat(text)?.let { return it }

        return null
    }

    // ── 常规格式 ────────────────────────────────────
    private fun extractYuanFormat(text: String): Long? {
        // 匹配 ¥/￥ 后跟可选千位分隔符 + 金额
        val pattern = Regex("""[¥￥]\s*([\d,]{1,15}(?:\.\d{1,2})?)""")
        return pattern.find(text)?.let { match ->
            val amountStr = match.groupValues[1].replace(",", "")
            stringToFen(amountStr)
        }
    }

    // ── "人民币 28 元 50 分" 格式 ────────────
    private fun extractRmbFormat(text: String): Long? {
        val pattern = Regex("""人民币\s*([\d,]{1,15}(?:\.\d{1,2})?)\s*元(?:\s*(\d{1,2})\s*分)?""")
        return pattern.find(text)?.let { match ->
            val yuanStr = match.groupValues[1].replace(",", "")
            val fen = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            val yuan = stringToFen(yuanStr) ?: return null
            yuan + fen
        }
    }

    // ── 繁体数字格式 ────────────────────────
    private fun extractChineseFormat(text: String): Long? {
        // 简单支持：壹仟贰佰叁拾肆元
        val chineseDigits = mapOf(
            '零' to 0, '一' to 1, '二' to 2, '三' to 3, '四' to 4,
            '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9,
            '壹' to 1, '贰' to 2, '叁' to 3, '肆' to 4,
            '伍' to 5, '陆' to 6, '柒' to 7, '捌' to 8, '玖' to 9
        )
        val pattern = Regex("""([壹贰叁肆伍陆柒捌玖零一二三四五六七八九]{1,10})元""")
        return pattern.find(text)?.let { match ->
            val chinese = match.groupValues[1]
            chineseToFen(chinese, chineseDigits)
        }
    }

    // ── "X 元"独立格式（支付语境内）────────────
    private fun extractYuanStandaloneFormat(text: String): Long? {
        val payContext = listOf("支付", "付款", "消费", "收款", "交易", "账单")
        if (payContext.none { text.contains(it) }) return null

        val pattern = Regex("""(^|[^\d])([\d,]{1,12}(?:\.\d{1,2})?)\s*元""")
        return pattern.find(text)?.let { match ->
            stringToFen(match.groupValues[2].replace(",", ""))
        }
    }

    // ── 转换工具 ────────────────────────────
    private fun stringToFen(amountStr: String): Long? {
        return try {
            BigDecimal(amountStr)
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        } catch (_: Exception) {
            null
        }
    }

    private fun chineseToFen(chinese: String, digitMap: Map<Char, Int>): Long? {
        var result = 0L
        var currentNum = 0
        var unitMultiplier = 1

        for (c in chinese.reversed()) {
            when {
                c in digitMap -> currentNum = digitMap[c]!! * unitMultiplier
                c == '十' || c == '拾' -> {
                    unitMultiplier = 10
                    if (currentNum == 0) currentNum = 10
                }
                c == '百' || c == '佰' -> {
                    unitMultiplier = 100
                    if (currentNum == 0) currentNum = 100
                }
                c == '千' || c == '仟' -> {
                    unitMultiplier = 1000
                    if (currentNum == 0) currentNum = 1000
                }
                c == '万' || c == '萬' -> {
                    result = result + currentNum * 10000
                    currentNum = 0
                    unitMultiplier = 1
                }
            }
            result += currentNum
            currentNum = 0
            unitMultiplier = 1
        }
        return if (result > 0) result * 100 else null
    }
}
