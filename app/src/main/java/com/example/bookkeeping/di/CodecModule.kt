package com.example.bookkeeping.di

import com.example.bookkeeping.data.repo.JsonCodec
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 将 Moshi 包装为 [JsonCodec]，供 Repository 和 SyncMapper 序列化/反序列化使用。
 */
@Module
@InstallIn(SingletonComponent::class)
object CodecModule {

    @Singleton
    @Provides
    fun provideJsonCodec(moshi: Moshi): JsonCodec =
        object : JsonCodec {
            override fun toJson(any: Any): String {
                @Suppress("UNCHECKED_CAST")
                val adapter = moshi.adapter(any::class.java as Class<Any>)
                return adapter.toJson(any)
            }

            override fun <T> fromJson(json: String, clazz: Class<T>): T =
                moshi.adapter(clazz).fromJson(json)
                    ?: error("Failed to deserialize JSON to ${clazz.simpleName}")
        }
}
