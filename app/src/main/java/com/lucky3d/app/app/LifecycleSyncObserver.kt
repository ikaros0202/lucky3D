package com.lucky3d.app.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.lucky3d.app.data.repository.LiveContentRepository
import com.lucky3d.app.data.repository.OfficialDataSyncCoordinator
import com.lucky3d.app.data.repository.OfficialDataSyncTrigger
import com.lucky3d.app.domain.livecontent.LiveRefreshTrigger
import com.lucky3d.app.domain.livecontent.LiveContentRefreshResult
import com.lucky3d.app.domain.livecontent.SkipReason
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class LifecycleSyncObserver internal constructor(
    private val officialDataSyncCoordinator: OfficialDataSyncCoordinator,
    private val liveContentRepository: LiveContentRepository,
    private val scope: CoroutineScope,
    private val clock: Clock,
) : DefaultLifecycleObserver {
    @Inject
    constructor(
        officialDataSyncCoordinator: OfficialDataSyncCoordinator,
        liveContentRepository: LiveContentRepository,
        clock: Clock,
    ) : this(
        officialDataSyncCoordinator,
        liveContentRepository,
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
        clock,
    )

    private var trialScheduleJob: Job? = null

    override fun onStart(owner: LifecycleOwner) {
        launchIsolated {
            officialDataSyncCoordinator.sync(OfficialDataSyncTrigger.AUTO)
        }
        if (trialScheduleJob?.isActive != true) {
            trialScheduleJob = scope.launch {
                try {
                    liveContentRepository.importBundledTrialSeed()
                    runTrialSchedule()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    // Foreground trial maintenance is best effort.
                }
            }
        }
        launchIsolated { liveContentRepository.cleanCaibaoCache() }
    }

    override fun onStop(owner: LifecycleOwner) {
        trialScheduleJob?.cancel()
        trialScheduleJob = null
    }

    private suspend fun runTrialSchedule() {
        while (currentCoroutineContext().isActive) {
            val now = clock.instant().atZone(BEIJING)
            if (now.toLocalTime() < TRIAL_REFRESH_TIME) {
                val release = now.toLocalDate().atTime(TRIAL_REFRESH_TIME).atZone(BEIJING)
                delay(
                    minOf(
                        Duration.between(now, release).toMillis().coerceAtLeast(1L),
                        millisUntilNextMidnight(now),
                    ),
                )
                continue
            }
            val result = liveContentRepository.refreshTrial(LiveRefreshTrigger.AUTO_FOREGROUND)
            val waitMillis = when (result) {
                LiveContentRefreshResult.Success -> millisUntilNextMidnight(now)
                is LiveContentRefreshResult.Failed -> TRIAL_RETRY_MILLIS
                is LiveContentRefreshResult.Skipped -> when (result.reason) {
                    SkipReason.ALREADY_SUCCEEDED_TODAY,
                    SkipReason.DAILY_AUTO_LIMIT,
                    -> millisUntilNextMidnight(now)
                    SkipReason.COOLDOWN -> TRIAL_RETRY_MILLIS
                    SkipReason.BEFORE_RELEASE_WINDOW -> 1_000L
                    SkipReason.TRIGGER_NOT_APPLICABLE -> return
                }
            }
            delay(minOf(waitMillis, millisUntilNextMidnight(now)).coerceAtLeast(1L))
        }
    }

    private fun millisUntilNextMidnight(now: java.time.ZonedDateTime): Long {
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay(BEIJING)
        return Duration.between(now, midnight).toMillis().coerceAtLeast(1L)
    }

    private fun launchIsolated(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // Each foreground maintenance action is best effort and isolated from its siblings.
            }
        }
    }

    private companion object {
        val BEIJING: ZoneId = ZoneId.of("Asia/Shanghai")
        val TRIAL_REFRESH_TIME: LocalTime = LocalTime.of(16, 30)
        const val TRIAL_RETRY_MILLIS = 30 * 60 * 1000L
    }
}
