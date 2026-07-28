package com.lucky3d.app.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SchemeDao {
    @Upsert
    suspend fun upsertTemplate(template: TemplateEntity)

    @Query("SELECT * FROM templates WHERE id = :id LIMIT 1")
    suspend fun templateById(id: String): TemplateEntity?

    @Query("SELECT * FROM templates ORDER BY updatedAtEpochMillis DESC")
    fun observeTemplates(): Flow<List<TemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertScheme(scheme: SchemeEntity)

    @Update
    suspend fun updateScheme(scheme: SchemeEntity)

    @Query("SELECT * FROM schemes WHERE id = :id LIMIT 1")
    suspend fun schemeById(id: String): SchemeEntity?

    @Query("SELECT * FROM schemes WHERE issue = :issue ORDER BY createdAtEpochMillis DESC")
    suspend fun schemesForIssue(issue: String): List<SchemeEntity>

    @Query("SELECT * FROM schemes ORDER BY updatedAtEpochMillis DESC")
    fun observeSchemes(): Flow<List<SchemeEntity>>

    @Query("UPDATE schemes SET isDrawn = 1, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun markSchemeDrawn(id: String, updatedAt: Long = 0L)

    @Upsert
    suspend fun upsertReplay(replay: ReplayEntity)

    @Query("SELECT * FROM replays WHERE schemeId = :schemeId LIMIT 1")
    suspend fun replayBySchemeId(schemeId: String): ReplayEntity?

    @Query("SELECT * FROM replays ORDER BY calculatedAtEpochMillis DESC")
    fun observeReplays(): Flow<List<ReplayEntity>>

    @Transaction
    suspend fun saveReplayAndMarkDrawn(
        replay: ReplayEntity,
        updatedAtEpochMillis: Long,
    ) {
        upsertReplay(replay)
        markSchemeDrawn(replay.schemeId, updatedAtEpochMillis)
    }

    @Transaction
    suspend fun saveDraftOrCopy(
        desired: SchemeEntity,
        copyId: String,
        copiedAtEpochMillis: Long,
    ): SchemeEntity {
        val existing = schemeById(desired.id)
        if (existing == null) {
            insertScheme(desired)
            return desired
        }
        if (existing.isDrawn) {
            require(copyId != existing.id) { "A drawn scheme copy needs a new id" }
            val copy = desired.copy(
                id = copyId,
                isDrawn = false,
                copiedFromSchemeId = existing.id,
                createdAtEpochMillis = copiedAtEpochMillis,
                updatedAtEpochMillis = copiedAtEpochMillis,
            )
            insertScheme(copy)
            return copy
        }
        val updated = desired.copy(
            createdAtEpochMillis = existing.createdAtEpochMillis,
            updatedAtEpochMillis = copiedAtEpochMillis,
        )
        updateScheme(updated)
        return updated
    }
}
