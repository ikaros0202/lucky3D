package com.lucky3d.app.data.local

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val playType: String,
    val conditionsJson: String,
    val conditionsSchemaVersion: Int,
    val observationWindow: Int,
    val ruleVersion: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "schemes",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["issue"]),
        Index(value = ["templateId"]),
        Index(value = ["copiedFromSchemeId"]),
    ],
)
data class SchemeEntity(
    @PrimaryKey
    val id: String,
    val issue: String,
    val title: String,
    val observationWindow: Int,
    val templateId: String?,
    val playType: String,
    val conditionsJson: String,
    val conditionsSchemaVersion: Int,
    val candidateNumbersJson: String,
    val betCount: Int,
    val multiplier: Int,
    val amountYuan: Int,
    val note: String,
    val ruleVersion: Int,
    val isDrawn: Boolean,
    val copiedFromSchemeId: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "replays",
    foreignKeys = [
        ForeignKey(
            entity = SchemeEntity::class,
            parentColumns = ["id"],
            childColumns = ["schemeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["issue"]),
    ],
)
data class ReplayEntity(
    @PrimaryKey
    val schemeId: String,
    val issue: String,
    val schemeFingerprint: String,
    val officialFingerprint: String,
    val winningNumber: String,
    val covered: Boolean,
    val matchedCandidate: String?,
    val ruleVersion: Int,
    val revision: Int,
    val calculatedAtEpochMillis: Long,
)
