package com.example.bookkeeping.data.remote.dto

import com.squareup.moshi.Json

/** 服务端返回的响应体，包含生成的服务端 ID。 */
data class UpsertTransactionResponse(
    @Json(name = "server_id") val serverId: String,
)
