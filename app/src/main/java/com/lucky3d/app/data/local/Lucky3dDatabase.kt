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
    ],
    version = 3,
    exportSchema = true,
)
abstract class Lucky3dDatabase : RoomDatabase() {
    abstract fun drawDao(): DrawDao

    abstract fun syncMetadataDao(): SyncMetadataDao

    abstract fun schemeDao(): SchemeDao

    companion object {
        const val DATABASE_NAME = "lucky3d.db"
        const val ASSET_PATH = "database/lucky3d.db"
    }
}
