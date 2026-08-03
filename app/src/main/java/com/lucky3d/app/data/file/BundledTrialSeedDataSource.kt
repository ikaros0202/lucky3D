package com.lucky3d.app.data.file

import android.content.Context
import com.lucky3d.app.core.model.TrialNumber
import com.lucky3d.app.core.model.TrialSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

fun interface TrialSeedAssetReader {
    suspend fun read(): String
}

fun interface TrialSeedDataSource {
    suspend fun load(): BundledTrialSeedResult
}

enum class BundledTrialSeedFailure {
    FILE_IO,
    INVALID_PAYLOAD,
}

sealed interface BundledTrialSeedResult {
    data class Success(val records: List<TrialNumber>) : BundledTrialSeedResult
    data class Failure(val failure: BundledTrialSeedFailure) : BundledTrialSeedResult
}

@Singleton
class BundledTrialSeedDataSource internal constructor(
    private val assetReader: TrialSeedAssetReader,
    private val ioDispatcher: CoroutineDispatcher,
) : TrialSeedDataSource {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(
        assetReader = TrialSeedAssetReader {
            context.assets.open(ASSET_PATH).bufferedReader(Charsets.UTF_8).use { it.readText() }
        },
        ioDispatcher = Dispatchers.IO,
    )

    override suspend fun load(): BundledTrialSeedResult = withContext(ioDispatcher) {
        val payload = try {
            assetReader.read()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            return@withContext BundledTrialSeedResult.Failure(BundledTrialSeedFailure.FILE_IO)
        }
        try {
            val document = JSON.decodeFromString<TrialSeedDocument>(payload)
            val records = validate(document)
                ?: return@withContext BundledTrialSeedResult.Failure(
                    BundledTrialSeedFailure.INVALID_PAYLOAD,
                )
            BundledTrialSeedResult.Success(records)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: SerializationException) {
            BundledTrialSeedResult.Failure(BundledTrialSeedFailure.INVALID_PAYLOAD)
        } catch (_: IllegalArgumentException) {
            BundledTrialSeedResult.Failure(BundledTrialSeedFailure.INVALID_PAYLOAD)
        }
    }

    private fun validate(document: TrialSeedDocument): List<TrialNumber>? {
        if (document.schemaVersion != SCHEMA_VERSION || document.source != SOURCE) return null
        val generatedAt = runCatching { Instant.parse(document.generatedAt) }.getOrNull() ?: return null
        if (document.records.isEmpty()) return null
        val unique = linkedMapOf<String, TrialNumber>()
        document.records.forEach { raw ->
            if (!ISSUE.matches(raw.issue) || !NUMBER.matches(raw.number)) return null
            val sourceDate = runCatching { LocalDate.parse(raw.sourceDate) }.getOrNull() ?: return null
            if (raw.issue.take(4) != sourceDate.year.toString()) return null
            if (!approvedAnnualUrl(raw.sourcePageUrl, sourceDate.year)) return null
            if (unique.containsKey(raw.issue)) return null
            unique[raw.issue] = TrialNumber(
                issue = raw.issue,
                number = raw.number,
                source = TrialSource.CAIBA_55125,
                sourcePageUrl = raw.sourcePageUrl,
                sourceLocalDate = sourceDate,
                fetchedAtEpochMillis = generatedAt.toEpochMilli(),
            )
        }
        val records = unique.values.sortedBy(TrialNumber::issue)
        if (records.first().issue != FIRST_ISSUE) return null
        val byYear = records.groupBy { it.issue.take(4).toInt() }
        if (byYear.keys != APPROVED_YEARS) return null
        byYear.forEach { (year, yearRecords) ->
            val expectedLast = if (year == 2025) 351 else yearRecords.last().issue.takeLast(3).toInt()
            val expected = (1..expectedLast).map { "$year${it.toString().padStart(3, '0')}" }
            if (yearRecords.map(TrialNumber::issue) != expected) return null
        }
        return records
    }

    private fun approvedAnnualUrl(value: String, year: Int): Boolean {
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        return uri.scheme == "https" &&
            uri.host == APPROVED_HOST &&
            uri.rawQuery == null &&
            uri.rawFragment == null &&
            uri.rawPath == "/3d/3dsjhcx-$year.htm"
    }

    @Serializable
    private data class TrialSeedDocument(
        val generatedAt: String,
        val records: List<TrialSeedRecord>,
        val schemaVersion: Int,
        val source: String,
    )

    @Serializable
    private data class TrialSeedRecord(
        val issue: String,
        val number: String,
        val sourceDate: String,
        val sourcePageUrl: String,
    )

    private companion object {
        const val ASSET_PATH = "trial/caiba-55125-trial-seed.json"
        const val SCHEMA_VERSION = 1
        const val SOURCE = "CAIBA_55125"
        const val APPROVED_HOST = "www.55125.cn"
        const val FIRST_ISSUE = "2025001"
        val APPROVED_YEARS = setOf(2025, 2026)
        val ISSUE = Regex("20\\d{5}")
        val NUMBER = Regex("\\d{3}")
        val JSON = Json {
            ignoreUnknownKeys = false
            explicitNulls = false
        }
    }
}
