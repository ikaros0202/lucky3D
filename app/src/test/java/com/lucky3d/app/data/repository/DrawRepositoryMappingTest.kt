package com.lucky3d.app.data.repository

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.data.local.SyncMetadataEntity
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
}
