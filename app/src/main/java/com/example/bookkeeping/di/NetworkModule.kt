package com.example.bookkeeping.di

import com.example.bookkeeping.BuildConfig
import com.example.bookkeeping.data.remote.BookkeepingApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * 服务端 Base URL 由 BuildConfig 注入：
     * - debug 包：BuildConfig.BASE_URL = "https://test-api.example.com"
     * - release 包：BuildConfig.BASE_URL = "https://api.example.com"
     * 如需修改测试地址，编辑 app/build.gradle.kts 中 debug.buildConfigField 即可。
     */

    @Singleton
    @Provides
    fun provideMoshi(): Moshi =
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        val logLevel = if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.NONE

        return OkHttpClient.Builder()
            .apply {
                // Mock 模式：拦截所有请求返回本地伪造响应，无需真实后端
                if (BuildConfig.USE_MOCK_API) {
                    addInterceptor(com.example.bookkeeping.debug.FakeApiInterceptor())
                }
            }
            .addInterceptor(HttpLoggingInterceptor().apply { setLevel(logLevel) })
            .build()
    }

    @Singleton
    @Provides
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Singleton
    @Provides
    fun provideBookkeepingApi(retrofit: Retrofit): BookkeepingApi =
        retrofit.create(BookkeepingApi::class.java)
}
