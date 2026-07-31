package com.lucky3d.app.core.model

import java.time.LocalDate

enum class LiveContentType {
    TRIAL_NUMBER,
    CAIBAO,
}

enum class TrialSource {
    CJCP_SIMULATED,
}

data class TrialNumber(
    val issue: String,
    val number: String,
    val source: TrialSource,
    val sourcePageUrl: String,
    val sourceLocalDate: LocalDate,
    val fetchedAtEpochMillis: Long,
)

data class CaibaoDocument(
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
    val cachedLocalDate: LocalDate,
    val fetchedAtEpochMillis: Long,
)
