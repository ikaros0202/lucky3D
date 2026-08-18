package com.lucky3d.app.data.repository

import com.lucky3d.app.data.remote.YunnanAnnouncementDataResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class OfficialDataSyncTrigger { AUTO, MANUAL }

enum class AnnouncementRefreshState { IDLE, REFRESHING, READY, ERROR, DRAW_ERROR }

data class OfficialDataSyncResult(
    val drawResult: SyncResult,
    val announcementResult: YunnanAnnouncementResult,
)

/**
 * Serializes the two official data sources so provincial announcements can
 * only be validated after the canonical draw refresh has completed.
 */
@Singleton
class OfficialDataSyncCoordinator @Inject constructor(
    private val drawRepository: DrawRepository,
    private val yunnanRepository: YunnanAnnouncementRepository,
) {
    private val syncMutex = Mutex()
    private val mutableAnnouncementRefreshState =
        MutableStateFlow(AnnouncementRefreshState.IDLE)

    val announcementRefreshState: StateFlow<AnnouncementRefreshState> =
        mutableAnnouncementRefreshState.asStateFlow()

    suspend fun sync(trigger: OfficialDataSyncTrigger): OfficialDataSyncResult =
        syncMutex.withLock {
            val previousRefreshState = mutableAnnouncementRefreshState.value
            mutableAnnouncementRefreshState.value = AnnouncementRefreshState.REFRESHING
            try {
                val drawResult = try {
                    when (trigger) {
                        OfficialDataSyncTrigger.AUTO -> drawRepository.syncOnForeground()
                        OfficialDataSyncTrigger.MANUAL -> drawRepository.refresh()
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    SyncResult.Failed("UNEXPECTED")
                }
                val announcementResult = if (drawResult == SyncResult.AlreadyRunning) {
                    YunnanAnnouncementDataResult.NetworkFailure(
                        "Draw refresh is already running",
                    )
                } else {
                    refreshAnnouncement { yunnanRepository.refreshRecent() }
                }
                mutableAnnouncementRefreshState.value = resultState(
                    drawResult,
                    announcementResult,
                )
                OfficialDataSyncResult(drawResult, announcementResult)
            } catch (exception: CancellationException) {
                mutableAnnouncementRefreshState.value = previousRefreshState
                throw exception
            }
        }

    suspend fun syncIssue(issue: String): OfficialDataSyncResult = syncMutex.withLock {
        try {
            val drawResult = try {
                drawRepository.syncOnForeground()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                SyncResult.Failed("UNEXPECTED")
            }
            val announcementResult = if (drawResult == SyncResult.AlreadyRunning) {
                YunnanAnnouncementDataResult.NetworkFailure(
                    "Draw refresh is already running",
                )
            } else {
                refreshAnnouncement { yunnanRepository.refreshIssue(issue) }
            }
            OfficialDataSyncResult(drawResult, announcementResult)
        } catch (exception: CancellationException) {
            throw exception
        }
    }

    private suspend fun refreshAnnouncement(
        refresh: suspend () -> YunnanAnnouncementResult,
    ): YunnanAnnouncementResult = try {
        refresh()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        YunnanAnnouncementDataResult.NetworkFailure(
            exception.message ?: "Unexpected announcement refresh failure",
        )
    }

    private fun resultState(
        drawResult: SyncResult,
        announcementResult: YunnanAnnouncementResult,
    ): AnnouncementRefreshState {
        if (drawResult is SyncResult.Failed || drawResult == SyncResult.AlreadyRunning) {
            return AnnouncementRefreshState.DRAW_ERROR
        }
        return when (announcementResult) {
            is YunnanAnnouncementDataResult.Success -> {
                if (announcementResult.announcements.isNotEmpty()) {
                    AnnouncementRefreshState.READY
                } else {
                    AnnouncementRefreshState.ERROR
                }
            }
            YunnanAnnouncementDataResult.EmptyResponse,
            is YunnanAnnouncementDataResult.HttpFailure,
            is YunnanAnnouncementDataResult.InvalidPayload,
            is YunnanAnnouncementDataResult.NetworkFailure,
            -> AnnouncementRefreshState.ERROR
        }
    }
}
