package com.lucky3d.app.core.model

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.domain.attributes.DrawNumber
import kotlin.test.assertFailsWith
import org.junit.Test

class YunnanAnnouncementTest {
    @Test
    fun `payout summary only exists when both payout count and price are positive`() {
        val announcement = announcement(
            plays = listOf(
                YunnanPlayAnnouncement(
                    playType = YunnanPlayType.SINGLE,
                    winningCount = 12_155,
                    prizePerBetYuan = 1_040,
                    payoutCount = 11_029,
                    payoutPerBetYuan = 246,
                ),
                YunnanPlayAnnouncement(
                    playType = YunnanPlayType.GROUP3,
                    winningCount = 0,
                    prizePerBetYuan = 346,
                    payoutCount = 0,
                    payoutPerBetYuan = 60,
                ),
            ),
        )

        assertThat(announcement.hasPayout).isTrue()
        assertThat(announcement.payoutSummary).contains("单选")
        assertThat(announcement.payoutSummary).contains("11029")
    }

    @Test
    fun `no positive payout has empty summary`() {
        val announcement = announcement(
            plays = listOf(
                YunnanPlayAnnouncement(YunnanPlayType.SINGLE, 4617, 1040),
                YunnanPlayAnnouncement(YunnanPlayType.GROUP3, 0, 346),
                YunnanPlayAnnouncement(YunnanPlayType.GROUP6, 7834, 173),
            ),
        )

        assertThat(announcement.hasPayout).isFalse()
        assertThat(announcement.payoutSummary).isNull()
    }

    @Test
    fun `negative prize pool balance is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            announcement(
                plays = listOf(
                    YunnanPlayAnnouncement(YunnanPlayType.SINGLE, 4617, 1040),
                ),
                prizePoolBalanceFen = -1L,
            )
        }
    }

    private fun announcement(
        plays: List<YunnanPlayAnnouncement>,
        prizePoolBalanceFen: Long? = null,
    ) = YunnanAnnouncement(
        issue = "2024291",
        drawDate = "2024-10-31",
        number = DrawNumber.parse("426"),
        salesAmountYuan = 11_888_140,
        winningTotalYuan = 6_167_545,
        prizePoolBalanceFen = prizePoolBalanceFen,
        plays = plays,
        redemptionDeadline = "2024-12-30",
        sourceUpdatedAt = "2026-06-22 13:26:25",
        fetchedAtEpochMillis = 1L,
        fingerprint = "fingerprint",
    )
}
