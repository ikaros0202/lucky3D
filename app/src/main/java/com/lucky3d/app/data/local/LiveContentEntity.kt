package com.lucky3d.app.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "trial_numbers")
data class TrialNumberEntity(
    @PrimaryKey
    val issue: String,
    val number: String,
    val source: String,
    val sourcePageUrl: String,
    val sourceLocalDate: String,
    val fetchedAtEpochMillis: Long,
)

@Entity(tableName = "caibao_documents")
data class CaibaoDocumentEntity(
    @PrimaryKey
    val issue: String,
    val edition: String,
    val title: String,
    val sourcePageUrl: String,
    val imageUrl: String,
    val localFileName: String,
    val sha256: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val cachedLocalDate: String,
    val fetchedAtEpochMillis: Long,
)

@Entity(tableName = "live_content_refresh_metadata")
data class LiveContentRefreshMetadataEntity(
    @PrimaryKey
    val contentType: String,
    val attemptLocalDate: String?,
    val autoAttemptCount: Int,
    val lastAttemptEpochMillis: Long?,
    val lastSuccessLocalDate: String?,
    val lastSuccessEpochMillis: Long?,
    val nextAllowedAutoAttemptEpochMillis: Long?,
    val lastFailureType: String?,
)
