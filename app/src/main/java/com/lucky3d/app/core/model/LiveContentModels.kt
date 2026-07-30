package com.lucky3d.app.core.model

import java.time.LocalDate

enum class LiveContentType {
    TRIAL_NUMBER,
    CAIBAO,
}

data class TrialNumber(
    val issue: String,
    val number: String,
    val sourcePageUrl: String,
    val sourceLocalDate: LocalDate,
    val fetchedAtEpochMillis: Long,
)

data class CaibaoDocument(
    val issue: String,
    val edition: String,
    val sourcePageUrl: String,
    val imageUrl: String,
    val localFileName: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val sourceLocalDate: LocalDate,
    val fetchedAtEpochMillis: Long,
)
