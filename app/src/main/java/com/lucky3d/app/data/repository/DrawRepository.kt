package com.lucky3d.app.data.repository

import com.lucky3d.app.data.local.DrawDao
import com.lucky3d.app.data.local.DrawEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class DrawRepository @Inject constructor(
    drawDao: DrawDao,
    private val syncCoordinator: SyncCoordinator,
) {
    val latestDraw: Flow<DrawEntity?> = drawDao.observeLatest()

    suspend fun refresh(): SyncResult = syncCoordinator.sync(SyncTrigger.MANUAL)

    suspend fun syncOnForeground(): SyncResult = syncCoordinator.sync(SyncTrigger.AUTO)
}
