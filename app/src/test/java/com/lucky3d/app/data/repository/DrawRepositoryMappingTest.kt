package com.lucky3d.app.data.repository

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.data.local.DrawEntity
import com.lucky3d.app.data.local.SyncMetadataEntity
import com.lucky3d.app.data.mapper.toEntity
import com.lucky3d.app.data.mapper.toRecord
import com.lucky3d.app.data.remote.OfficialDraw
import com.lucky3d.app.domain.attributes.DrawNumber
import java.time.LocalDate
import org.junit.Test

class DrawRepositoryMappingTest {
    @Test
    fun `room sync metadata maps to repository boundary model`() {
        val entity = SyncMetadataEntity(
            lastSuccessEpochMillis = 123L,
            latestIssue = "2026198",
            lastFailureType = "NETWORK",
            correctedIssuesJson = """["2026197","2026198"]""",
        )

        assertThat(entity.toDrawSyncMetadata()).isEqualTo(
            DrawSyncMetadata(
                lastSuccessEpochMillis = 123L,
                latestIssue = "2026198",
                lastFailureType = "NETWORK",
                correctedIssues = setOf("2026197", "2026198"),
            ),
        )
    }

    @Test
    fun `official draw mapping preserves sales amount and fingerprint`() {
        val official = OfficialDraw(
            issue = "2026198",
            drawDate = LocalDate.of(2026, 7, 27),
            number = DrawNumber.parse("685"),
            detailUrl = "https://www.cwl.gov.cn/c/2026/07/27/662242.shtml",
            fingerprint = "DRAW-FINGERPRINT",
            salesAmountYuan = 123_456_789L,
        )

        val entity = official.toEntity()
        assertThat(entity).isEqualTo(
            DrawEntity(
                issue = "2026198",
                drawDate = "2026-07-27",
                hundreds = 6,
                tens = 8,
                ones = 5,
                officialDetailUrl = "https://www.cwl.gov.cn/c/2026/07/27/662242.shtml",
                officialFingerprint = "DRAW-FINGERPRINT",
                salesAmountYuan = 123_456_789L,
            ),
        )

        val record = entity.toRecord()
        assertThat(record.salesAmountYuan).isEqualTo(123_456_789L)
        assertThat(record.officialFingerprint).isEqualTo("DRAW-FINGERPRINT")
    }

    @Test
    fun `mapping preserves missing sales amount as null`() {
        val entity = DrawEntity(
            issue = "2026197",
            drawDate = "2026-07-26",
            hundreds = 2,
            tens = 3,
            ones = 2,
            officialDetailUrl = "https://www.cwl.gov.cn/c/2026/07/26/662035.shtml",
            officialFingerprint = "DRAW-FINGERPRINT",
        )

        assertThat(entity.toRecord().salesAmountYuan).isNull()
    }
}
