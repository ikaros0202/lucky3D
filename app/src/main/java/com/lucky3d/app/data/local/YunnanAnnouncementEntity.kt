package com.lucky3d.app.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Room representation of the province-scoped announcement contract. */
@Entity(
    tableName = "yunnan_announcements",
    indices = [Index(value = ["drawDate"])],
)
data class YunnanAnnouncementEntity(
    @PrimaryKey
    val issue: String,
    val drawDate: String,
    val winningNumber: String,
    val salesAmountYuan: Long,
    val winningTotalYuan: Long,
    val prizePoolBalanceFen: Long? = null,
    val playsJson: String,
    val redemptionDeadline: String?,
    val sourceUpdatedAt: String,
    val fetchedAtEpochMillis: Long,
    val fingerprint: String,
)
