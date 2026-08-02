package com.lucky3d.app.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): Lucky3dDatabase =
        Room.databaseBuilder(context, Lucky3dDatabase::class.java, Lucky3dDatabase.DATABASE_NAME)
            .createFromAsset(Lucky3dDatabase.ASSET_PATH)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides
    fun provideDrawDao(database: Lucky3dDatabase): DrawDao = database.drawDao()

    @Provides
    fun provideSyncMetadataDao(database: Lucky3dDatabase): SyncMetadataDao =
        database.syncMetadataDao()

    @Provides
    fun provideSchemeDao(database: Lucky3dDatabase): SchemeDao = database.schemeDao()

    @Provides
    fun provideLiveContentDao(database: Lucky3dDatabase): LiveContentDao = database.liveContentDao()

}
