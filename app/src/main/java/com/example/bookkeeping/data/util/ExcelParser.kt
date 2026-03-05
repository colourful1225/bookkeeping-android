package com.example.bookkeeping.data.util

import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.InputStream

/**
 * Excel 文件解析工具（.xlsx）。
 *
 * 读取首个工作表，使用首行作为表头。
 */
object ExcelParser {
    fun parse(inputStream: InputStream): List<Map<String, String>> {
        val result = mutableListOf<Map<String, String>>()

        ReadableWorkbook(inputStream).use { workbook ->
            val sheet = workbook.firstSheet
            sheet.openStream().use { rows ->
                val iterator = rows.iterator()
                if (!iterator.hasNext()) return result

                val headerRow = iterator.next()
                val columnCount = headerRow.getCellCount()
                val headers = (0 until columnCount)
                    .map { index ->
                        headerRow.getCell(index)?.text?.trim().orEmpty()
                    }

                while (iterator.hasNext()) {
                    val row = iterator.next()
                    if (row == null) continue
                    val rowMap = mutableMapOf<String, String>()
                    for (i in 0 until columnCount) {
                        val header = headers.getOrNull(i).orEmpty()
                        if (header.isBlank()) continue
                        val value = row.getCell(i)?.text?.trim().orEmpty()
                        if (value.isNotBlank()) {
                            rowMap[header] = value
                        }
                    }
                    if (rowMap.isNotEmpty()) {
                        result.add(rowMap)
                    }
                }
            }
        }

        return result
    }
}
