package com.example.bookkeeping.data.remote.dto

import com.squareup.moshi.Json

/** 创建/更新交易的请求体。与 [TransactionEntity] 解耦，仅包含服务端关心的字段。 */
data class UpsertTransactionRequest(
    @Json(name = "id")          val id: String,
    @Json(name = "amount")      val amount: Long,
    @Json(name = "type")        val type: String,
    @Json(name = "category_id") val categoryId: String,
    @Json(name = "note")        val note: String?,
    @Json(name = "occurred_at") val occurredAt: Long,
)
