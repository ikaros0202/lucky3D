package com.lucky3d.app.data.repository

import com.lucky3d.app.data.remote.OfficialDrawDataSource
import com.lucky3d.app.data.remote.OfficialFc3dDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

    @Provides
    @Singleton
    fun provideOfficialDrawDataSource(client: OkHttpClient): OfficialDrawDataSource =
        OfficialFc3dDataSource(client)

    @Provides
    @Singleton
    fun provideDrawRepository(repository: DefaultDrawRepository): DrawRepository = repository

    @Provides
    @Singleton
    fun provideSchemeRepository(repository: DefaultSchemeRepository): SchemeRepository = repository

    @Provides
    @Singleton
    fun provideSyncStore(store: RoomSyncStore): SyncStore = store

    @Provides
    @Singleton
    fun provideReplayRefresher(refresher: RoomReplayRefresher): ReplayRefresher = refresher

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider
}
