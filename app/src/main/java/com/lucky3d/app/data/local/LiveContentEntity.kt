package com.lucky3d.app.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "trial_numbers")
data class TrialNumberEntity(
    @PrimaryKey
    val issue: String,
    val number: String,
    val sourcePageUrl: String,
    val sourceLocalDate: String,
    val fetchedAtEpochMillis: Long,
)

@Entity(tableName = "caibao_documents")
data class CaibaoDocumentEntity(
    @PrimaryKey
    val issue: String,
    val edition: String,
    val sourcePageUrl: String,
    val imageUrl: String,
    val localFileName: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val sourceLocalDate: String,
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
    val lastFailureType: String?,
)
