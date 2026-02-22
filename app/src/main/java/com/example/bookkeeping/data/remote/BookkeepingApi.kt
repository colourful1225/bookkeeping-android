package com.example.bookkeeping.data.remote

import com.example.bookkeeping.data.remote.dto.UpsertTransactionRequest
import com.example.bookkeeping.data.remote.dto.UpsertTransactionResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * 记账服务端 Retrofit 接口。
 *
 * 服务端约束：
 * - 对相同 [Idempotency-Key] 保证幂等，不重复入账。
 * - 已存在的 id 以本次请求数据覆盖（upsert 语义）。
 * - 冲突时返回可识别错误码（HTTP 409），客户端映射为 CONFLICT。
 */
interface BookkeepingApi {

    @POST("/v1/transactions/upsert")
    suspend fun upsertTransaction(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: UpsertTransactionRequest,
    ): UpsertTransactionResponse
}
