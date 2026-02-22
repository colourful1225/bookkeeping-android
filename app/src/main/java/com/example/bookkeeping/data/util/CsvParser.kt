package com.example.bookkeeping.data.util

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * CSV 文件解析工具。
 *
 * 支持简单的 CSV 格式（带头行），自动识别常见列名。
 */
object CsvParser {
    /**
     * 解析 CSV 输入流。
     *
     * @param inputStream CSV 文件流
     * @param charset 字符编码（默认 UTF-8）
     * @return 行列表，每行为 Map<列名, 值>
     */
    fun parse(inputStream: InputStream, charset: String = "UTF-8"): List<Map<String, String>> {
        val result = mutableListOf<Map<String, String>>()
        val reader = BufferedReader(InputStreamReader(inputStream, charset))

        reader.use { bf ->
            // 读取头行
            val headerLine = bf.readLine() ?: return result
            val headers = parseCsvLine(headerLine)

            // 读取数据行
            bf.forEachLine { line ->
                if (line.isNotBlank()) {
                    val values = parseCsvLine(line)
                    val row = mutableMapOf<String, String>()
                    
                    headers.forEachIndexed { index, header ->
                        if (index < values.size) {
                            row[header] = values[index]
                        }
                    }
                    
                    if (row.isNotEmpty()) {
                        result.add(row)
                    }
                }
            }
        }

        return result
    }

    /**
     * 解析单行 CSV。
     *
     * 支持引号和逗号转义。
     */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' && (i == 0 || line[i - 1] != '\\') -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    fields.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> {
                    current.append(char)
                }
            }
            i++
        }

        fields.add(current.toString().trim())
        return fields.map { it.removeSurrounding("\"") }
    }

    /**
     * 根据 CSV 行数据构建导入对象。
     *
     * 映射规则（不区分大小写）：
     * - amount, 金额, price → amount
     * - category, 分类, type → category
     * - date, 日期, time → date
     * - note, 备注, memo, description → note
     */
    fun mapRowToImportData(row: Map<String, String>): Map<String, String?> {
        val result = mutableMapOf<String, String?>()

        // 映射金额
        result["amount"] = findValue(row, listOf("amount", "金额", "price", "金钱", "消费金额"))

        // 映射分类
        result["category"] = findValue(row, listOf("category", "分类", "type", "类别", "消费分类"))

        // 映射日期
        result["date"] = findValue(row, listOf("date", "日期", "time", "时间", "发生日期", "交易日期"))

        // 映射备注
        result["note"] = findValue(row, listOf("note", "备注", "memo", "description", "说明", "摘要"))

        return result
    }

    /**
     * 在 Map 中查找键（不区分大小写）。
     */
    private fun findValue(
        row: Map<String, String>,
        candidates: List<String>,
    ): String? {
        val lowerRow = row.mapKeys { (key, _) -> key.lowercase() }
        
        for (candidate in candidates) {
            val value = lowerRow[candidate.lowercase()]
            if (!value.isNullOrBlank()) {
                return value
            }
        }

        return null
    }
}
