package com.lucky3d.app.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.LiveContentRepository
import com.lucky3d.app.domain.livecontent.LiveRefreshTrigger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class LifecycleSyncObserver internal constructor(
    private val repository: DrawRepository,
    private val liveContentRepository: LiveContentRepository,
    private val scope: CoroutineScope,
) : DefaultLifecycleObserver {
    @Inject
    constructor(
        repository: DrawRepository,
        liveContentRepository: LiveContentRepository,
    ) : this(
        repository,
        liveContentRepository,
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    override fun onStart(owner: LifecycleOwner) {
        launchIsolated { repository.syncOnForeground() }
        launchIsolated {
            liveContentRepository.refreshTrial(LiveRefreshTrigger.AUTO_FOREGROUND)
        }
        launchIsolated { liveContentRepository.cleanCaibaoCache() }
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
}
