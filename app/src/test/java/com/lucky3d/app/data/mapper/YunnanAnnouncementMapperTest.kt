package com.lucky3d.app.data.mapper

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.core.model.YunnanAnnouncement
import com.lucky3d.app.core.model.YunnanPlayAnnouncement
import com.lucky3d.app.core.model.YunnanPlayType
import com.lucky3d.app.domain.attributes.DrawNumber
import org.junit.Test

class YunnanAnnouncementMapperTest {
    @Test
    fun `mapping preserves leading zero and independent province fields`() {
        val original = YunnanAnnouncement(
            issue = "2024291",
            drawDate = "2024-10-31",
            number = DrawNumber.parse("006"),
            salesAmountYuan = 11_888_140,
            winningTotalYuan = 6_167_545,
            prizePoolBalanceFen = 162_507_548,
            plays = listOf(
                YunnanPlayAnnouncement(YunnanPlayType.SINGLE, 4_617, 1_040),
                YunnanPlayAnnouncement(YunnanPlayType.GROUP3, 0, 346),
                YunnanPlayAnnouncement(YunnanPlayType.GROUP6, 7_834, 173),
            ),
            redemptionDeadline = "2024-12-30",
            sourceUpdatedAt = "2026-06-22 13:26:25",
            fetchedAtEpochMillis = 1L,
            fingerprint = "fingerprint",
        )

        val restored = original.toEntity().toAnnouncement()

        assertThat(restored).isEqualTo(original)
        assertThat(restored.number.value).isEqualTo("006")
        assertThat(restored.prizePoolBalanceFen).isEqualTo(162_507_548L)
    }
}
