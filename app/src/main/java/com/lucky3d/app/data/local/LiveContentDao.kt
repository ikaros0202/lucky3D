package com.lucky3d.app.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveContentDao {
    @Query("SELECT * FROM trial_numbers ORDER BY sourceLocalDate DESC, fetchedAtEpochMillis DESC LIMIT 1")
    fun observeLatestTrial(): Flow<TrialNumberEntity?>

    @Query("SELECT * FROM trial_numbers ORDER BY sourceLocalDate DESC, fetchedAtEpochMillis DESC")
    fun observeAllTrials(): Flow<List<TrialNumberEntity>>

    @Query("SELECT * FROM trial_numbers ORDER BY sourceLocalDate DESC, fetchedAtEpochMillis DESC")
    suspend fun allTrials(): List<TrialNumberEntity>

    @Query("SELECT * FROM caibao_documents ORDER BY cachedLocalDate DESC, fetchedAtEpochMillis DESC LIMIT 1")
    fun observeLatestCaibao(): Flow<CaibaoDocumentEntity?>

    @Query("SELECT * FROM caibao_documents ORDER BY cachedLocalDate DESC, fetchedAtEpochMillis DESC")
    fun observeAllCaibao(): Flow<List<CaibaoDocumentEntity>>

    @Query("SELECT * FROM live_content_refresh_metadata WHERE contentType = :contentType LIMIT 1")
    fun observeRefreshMetadata(contentType: String): Flow<LiveContentRefreshMetadataEntity?>

    @Query("SELECT * FROM trial_numbers ORDER BY sourceLocalDate DESC, fetchedAtEpochMillis DESC LIMIT 1")
    suspend fun latestTrial(): TrialNumberEntity?

    @Query("SELECT * FROM caibao_documents ORDER BY cachedLocalDate DESC, fetchedAtEpochMillis DESC LIMIT 1")
    suspend fun latestCaibao(): CaibaoDocumentEntity?

    @Query("SELECT * FROM caibao_documents ORDER BY cachedLocalDate DESC, fetchedAtEpochMillis DESC")
    suspend fun allCaibao(): List<CaibaoDocumentEntity>

    @Query("SELECT * FROM live_content_refresh_metadata WHERE contentType = :contentType LIMIT 1")
    suspend fun refreshMetadata(contentType: String): LiveContentRefreshMetadataEntity?

    @Upsert
    suspend fun upsertTrial(trial: TrialNumberEntity)

    @Transaction
    suspend fun upsertTrials(trials: List<TrialNumberEntity>) {
        trials.forEach { upsertTrial(it) }
    }

    @Upsert
    suspend fun upsertCaibao(document: CaibaoDocumentEntity)

    @Upsert
    suspend fun upsertRefreshMetadata(metadata: LiveContentRefreshMetadataEntity)

    @Query("DELETE FROM trial_numbers")
    suspend fun deleteTrials()

    @Query("SELECT * FROM caibao_documents WHERE cachedLocalDate < :cutoffDate")
    suspend fun caibaoOlderThan(cutoffDate: String): List<CaibaoDocumentEntity>

    @Query("DELETE FROM caibao_documents WHERE issue IN (:issues)")
    suspend fun deleteCaibaoByIssues(issues: List<String>)

    @Transaction
    suspend fun upsertTrialAndMetadata(
        trial: TrialNumberEntity,
        metadata: LiveContentRefreshMetadataEntity,
    ) {
        upsertTrial(trial)
        upsertRefreshMetadata(metadata)
    }

    @Transaction
    suspend fun upsertTrialsAndMetadata(
        trials: List<TrialNumberEntity>,
        metadata: LiveContentRefreshMetadataEntity,
    ) {
        trials.forEach { upsertTrial(it) }
        upsertRefreshMetadata(metadata)
    }

    @Transaction
    suspend fun upsertCaibaoAndMetadata(
        document: CaibaoDocumentEntity,
        metadata: LiveContentRefreshMetadataEntity,
    ) {
        upsertCaibao(document)
        upsertRefreshMetadata(metadata)
    }

    @Transaction
    suspend fun recordRefreshMetadata(metadata: LiveContentRefreshMetadataEntity) {
        upsertRefreshMetadata(metadata)
    }
}
