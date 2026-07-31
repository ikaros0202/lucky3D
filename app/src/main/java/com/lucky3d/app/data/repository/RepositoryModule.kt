package com.lucky3d.app.data.repository

import android.content.Context
import com.lucky3d.app.data.file.CaibaoFileStore
import com.lucky3d.app.data.remote.OfficialDrawDataSource
import com.lucky3d.app.data.remote.OfficialFc3dDataSource
import com.lucky3d.app.data.remote.CaibaoDataSource
import com.lucky3d.app.data.remote.CjcpTrialDataSource
import com.lucky3d.app.data.remote.Cz89CaibaoDataSource
import com.lucky3d.app.data.remote.TrialDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.time.Clock
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
    fun provideTrialDataSource(client: OkHttpClient): TrialDataSource = CjcpTrialDataSource(client)

    @Provides
    @Singleton
    fun provideCaibaoDataSource(client: OkHttpClient): CaibaoDataSource = Cz89CaibaoDataSource(client)

    @Provides
    @Singleton
    fun provideCaibaoFileStore(@ApplicationContext context: Context): CaibaoFileStore =
        CaibaoFileStore(File(context.filesDir, "live-content/caibao"))

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideDrawRepository(repository: DefaultDrawRepository): DrawRepository = repository

    @Provides
    @Singleton
    fun provideLiveContentRepository(
        repository: DefaultLiveContentRepository,
    ): LiveContentRepository = repository

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
