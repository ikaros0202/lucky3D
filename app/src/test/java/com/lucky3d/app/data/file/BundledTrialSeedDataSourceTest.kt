package com.lucky3d.app.data.file

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.core.model.TrialSource
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BundledTrialSeedDataSourceTest {
    @Test
    fun `approved asset contains complete 2025 and current 2026 ranges`() = runTest {
        val result = source(assetText()).load()

        assertThat(result).isInstanceOf(BundledTrialSeedResult.Success::class.java)
        val records = (result as BundledTrialSeedResult.Success).records
        assertThat(records).hasSize(555)
        assertThat(records.first().issue).isEqualTo("2025001")
        assertThat(records.last().issue).isEqualTo("2026204")
        assertThat(records.map { it.issue }.distinct()).hasSize(555)
        assertThat(records.map { it.source }.distinct()).containsExactly(TrialSource.CAIBA_55125)
        assertThat(records.all { it.number.matches(Regex("\\d{3}")) }).isTrue()
    }

    @Test
    fun `invalid source duplicate date and number reject entire asset`() = runTest {
        val valid = assetText()
        val firstRecord = valid.substringAfter("    {").substringBefore("    },") + "    },\n"
        val invalidPayloads = listOf(
            valid.replace("https://www.55125.cn/3d/", "https://example.com/3d/"),
            valid.replace("  ],\n  \"schemaVersion\"", "    $firstRecord  ],\n  \"schemaVersion\""),
            valid.replace("2025-01-01", "2025-02-31"),
            valid.replace("\"number\": \"287\"", "\"number\": \"87\""),
        )

        invalidPayloads.forEach { payload ->
            assertThat(source(payload).load()).isEqualTo(
                BundledTrialSeedResult.Failure(BundledTrialSeedFailure.INVALID_PAYLOAD),
            )
        }
    }

    @Test
    fun `unknown fields and malformed json are rejected`() = runTest {
        val withUnknown = assetText().replace(
            "\"schemaVersion\": 1",
            "\"schemaVersion\": 1,\n  \"unexpected\": true",
        )

        assertThat(source(withUnknown).load()).isEqualTo(
            BundledTrialSeedResult.Failure(BundledTrialSeedFailure.INVALID_PAYLOAD),
        )
        assertThat(source("{").load()).isEqualTo(
            BundledTrialSeedResult.Failure(BundledTrialSeedFailure.INVALID_PAYLOAD),
        )
    }

    private fun source(payload: String) = BundledTrialSeedDataSource(
        assetReader = TrialSeedAssetReader { payload },
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun assetText(): String {
        val candidates = listOf(
            File("src/main/assets/trial/caiba-55125-trial-seed.json"),
            File("app/src/main/assets/trial/caiba-55125-trial-seed.json"),
        )
        return checkNotNull(candidates.firstOrNull(File::isFile)) {
            "Bundled trial seed asset not found from ${File(".").absolutePath}"
        }.readText(Charsets.UTF_8)
    }
}
