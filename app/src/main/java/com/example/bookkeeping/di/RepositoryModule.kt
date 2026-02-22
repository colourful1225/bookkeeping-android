package com.example.bookkeeping.di

import com.example.bookkeeping.data.repo.ITransactionRepository
import com.example.bookkeeping.data.repo.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 将 [ITransactionRepository] 接口绑定到 [TransactionRepository] 实现。
 * UseCase 只注入接口，测试时可替换为 Fake 而不影响生产代码。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepository,
    ): ITransactionRepository
}
