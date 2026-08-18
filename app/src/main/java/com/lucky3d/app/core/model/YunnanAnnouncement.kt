package com.lucky3d.app.core.model

import com.lucky3d.app.domain.attributes.DrawNumber

/**
 * Province-scoped 3D announcement data. It intentionally has no relationship
 * to the national draw/sales contract.
 */
data class YunnanAnnouncement(
    val issue: String,
    val drawDate: String,
    val number: DrawNumber,
    val salesAmountYuan: Long,
    val winningTotalYuan: Long,
    val prizePoolBalanceFen: Long? = null,
    val plays: List<YunnanPlayAnnouncement>,
    val redemptionDeadline: String?,
    val sourceUpdatedAt: String,
    val fetchedAtEpochMillis: Long,
    val fingerprint: String,
) {
    init {
        require(prizePoolBalanceFen == null || prizePoolBalanceFen >= 0L) {
            "prizePoolBalanceFen must not be negative"
        }
    }

    /** True only when a supported play has a positive count and unit amount. */
    val hasPayout: Boolean
        get() = plays.any { it.hasPayout }

    /**
     * A compact, deterministic summary for the home/detail surfaces. A missing
     * or zero payout is deliberately represented by null, never by a guessed
     * amount.
     */
    val payoutSummary: String?
        get() = plays
            .filter { it.hasPayout }
            .joinToString(separator = "；") { play ->
                "${play.playType.displayName}${play.payoutCount}注 × ${play.payoutPerBetYuan}元/注"
            }
            .ifBlank { null }
}

enum class YunnanPlayType(val displayName: String) {
    SINGLE("单选"),
    GROUP3("组选3"),
    GROUP6("组选6"),
}

data class YunnanPlayAnnouncement(
    val playType: YunnanPlayType,
    val winningCount: Long,
    val prizePerBetYuan: Long,
    val payoutCount: Long? = null,
    val payoutPerBetYuan: Long? = null,
) {
    init {
        require(winningCount >= 0L) { "winningCount must not be negative" }
        require(prizePerBetYuan > 0L) { "prizePerBetYuan must be positive" }
        require((payoutCount == null) == (payoutPerBetYuan == null)) {
            "payout count and amount must be supplied together"
        }
        require(payoutCount == null || payoutCount >= 0L) {
            "payoutCount must not be negative"
        }
        require(payoutPerBetYuan == null || payoutPerBetYuan > 0L) {
            "payoutPerBetYuan must be positive"
        }
    }

    val hasPayout: Boolean
        get() = payoutCount != null &&
            payoutPerBetYuan != null &&
            payoutCount > 0L &&
            payoutPerBetYuan > 0L
}
