package com.lucky3d.app.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawDao {
    @Upsert
    suspend fun upsertAll(draws: List<DrawEntity>)

    @Query("SELECT COUNT(*) FROM draws")
    suspend fun count(): Int

    @Query("SELECT * FROM draws ORDER BY issue DESC LIMIT 1")
    suspend fun latest(): DrawEntity?

    @Query("SELECT * FROM draws WHERE issue = :issue LIMIT 1")
    suspend fun byIssue(issue: String): DrawEntity?

    @Query("SELECT * FROM draws WHERE issue IN (:issues)")
    suspend fun byIssues(issues: Set<String>): List<DrawEntity>

    @Query("SELECT * FROM draws WHERE issue BETWEEN :startIssue AND :endIssue ORDER BY issue ASC")
    suspend fun range(startIssue: String, endIssue: String): List<DrawEntity>

    @Query("SELECT * FROM draws ORDER BY issue DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<DrawEntity>

    @Query("SELECT * FROM draws ORDER BY issue DESC LIMIT 1")
    fun observeLatest(): Flow<DrawEntity?>

    @Upsert
    suspend fun upsertSyncMetadata(metadata: SyncMetadataEntity)

    @Query("SELECT * FROM sync_metadata WHERE id = 1 LIMIT 1")
    suspend fun syncMetadata(): SyncMetadataEntity?

    @Transaction
    suspend fun commitValidatedSync(
        draws: List<DrawEntity>,
        metadata: SyncMetadataEntity,
    ) {
        if (draws.isNotEmpty()) upsertAll(draws)
        upsertSyncMetadata(metadata)
    }
}

@Dao
interface SyncMetadataDao {
    @Upsert
    suspend fun upsert(metadata: SyncMetadataEntity)

    @Query("SELECT * FROM sync_metadata WHERE id = 1 LIMIT 1")
    suspend fun get(): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE id = 1 LIMIT 1")
    fun observe(): Flow<SyncMetadataEntity?>
}
