package com.example.bookkeeping.debug

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.UUID

/**
 * Debug-only OkHttp 拦截器：拦截所有请求并返回本地伪造响应。
 *
 * 使用场景：无真实后端时，在 debug 包中模拟同步成功，
 * 验证"PENDING → SYNCED + outbox 清除"的完整链路。
 *
 * 开关：[com.example.bookkeeping.BuildConfig.USE_MOCK_API]
 */
class FakeApiInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path    = request.url.encodedPath

        // 模拟网络延迟
        Thread.sleep(300)

        return when {
            path.endsWith("/v1/transactions/upsert") -> mockUpsertResponse(chain)
            else -> chain.proceed(request)  // 其他请求正常透传
        }
    }

    private fun mockUpsertResponse(chain: Interceptor.Chain): Response {
        val serverId = "srv-${UUID.randomUUID()}"
        val body     = """{"server_id":"$serverId"}"""
            .toResponseBody("application/json".toMediaType())

        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body)
            .build()
    }
}
