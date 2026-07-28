package com.lucky3d.app.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "draws",
    indices = [
        Index(value = ["drawDate"]),
    ],
)
data class DrawEntity(
    @PrimaryKey
    val issue: String,
    val drawDate: String,
    val hundreds: Int,
    val tens: Int,
    val ones: Int,
    val officialDetailUrl: String,
    val officialFingerprint: String,
)

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val lastAttemptEpochMillis: Long? = null,
    val lastSuccessEpochMillis: Long? = null,
    val latestIssue: String? = null,
    val lastFailureType: String? = null,
    val correctedIssuesJson: String = "[]",
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
