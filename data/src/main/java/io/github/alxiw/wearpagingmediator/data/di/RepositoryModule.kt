package io.github.alxiw.wearpagingmediator.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.alxiw.wearpagingmediator.data.ReposRepository
import io.github.alxiw.wearpagingmediator.domain.Repository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    internal abstract fun bindRepository(repository: ReposRepository): Repository
}
