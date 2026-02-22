package com.example.bookkeeping.di

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 将 Hilt 注入的 [HiltWorkerFactory] 注册到 WorkManager。
 *
 * [Configuration] 由 [BookkeepingApp.getWorkManagerConfiguration] 使用，
 * 覆盖默认初始化，使 WorkManager 使用携带依赖注入的 WorkerFactory。
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Singleton
    @Provides
    fun provideWorkManagerConfiguration(
        workerFactory: HiltWorkerFactory,
    ): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
