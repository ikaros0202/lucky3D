package com.lucky3d.app.data.repository

import com.lucky3d.app.data.local.DrawDao
import com.lucky3d.app.data.local.DrawEntity
import com.lucky3d.app.data.local.SyncMetadataEntity
import javax.inject.Inject

class RoomSyncStore @Inject constructor(
    private val drawDao: DrawDao,
) : SyncStore {
    override suspend fun latest(): DrawEntity? = drawDao.latest()

    override suspend fun byIssues(issues: Set<String>): List<DrawEntity> =
        if (issues.isEmpty()) emptyList() else drawDao.byIssues(issues)

    override suspend fun metadata(): SyncMetadataEntity? = drawDao.syncMetadata()

    override suspend fun commit(
        draws: List<DrawEntity>,
        metadata: SyncMetadataEntity,
    ) = drawDao.commitValidatedSync(draws, metadata)

    override suspend fun recordFailure(metadata: SyncMetadataEntity) =
        drawDao.upsertSyncMetadata(metadata)
}
