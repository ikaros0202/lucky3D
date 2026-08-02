package com.lucky3d.app.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [
        DrawEntity::class,
        SyncMetadataEntity::class,
        TemplateEntity::class,
        SchemeEntity::class,
        ReplayEntity::class,
        TrialNumberEntity::class,
        CaibaoDocumentEntity::class,
        LiveContentRefreshMetadataEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class Lucky3dDatabase : RoomDatabase() {
    abstract fun drawDao(): DrawDao

    abstract fun syncMetadataDao(): SyncMetadataDao

    abstract fun schemeDao(): SchemeDao

    abstract fun liveContentDao(): LiveContentDao

    companion object {
        const val DATABASE_NAME = "lucky3d.db"
        const val ASSET_PATH = "database/lucky3d.db"
    }
}
