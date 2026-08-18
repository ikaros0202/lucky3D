package com.lucky3d.app.data.repository

import com.lucky3d.app.data.local.DrawEntity
import com.lucky3d.app.data.local.SyncMetadataEntity
import com.lucky3d.app.data.mapper.toEntity
import com.lucky3d.app.data.remote.OfficialDataResult
import com.lucky3d.app.data.remote.OfficialDraw
import com.lucky3d.app.data.remote.OfficialDrawDataSource
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex

enum class SyncTrigger { AUTO, MANUAL }

sealed interface SyncResult {
    data class Updated(
        val added: Int,
        val corrected: Int,
        val unchanged: Int,
        val latestIssue: String?,
    ) : SyncResult

    data class Failed(val failureType: String) : SyncResult
    data object Throttled : SyncResult
    data object AlreadyRunning : SyncResult
}

fun interface TimeProvider {
    fun nowEpochMillis(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}

interface SyncStore {
    suspend fun latest(): DrawEntity?
    suspend fun byIssues(issues: Set<String>): List<DrawEntity>
    suspend fun metadata(): SyncMetadataEntity?
    suspend fun commit(draws: List<DrawEntity>, metadata: SyncMetadataEntity)
    suspend fun recordFailure(metadata: SyncMetadataEntity)
}

interface ReplayRefresher {
    suspend fun refresh(issues: Set<String>)
}

@Singleton
class SyncCoordinator @Inject constructor(
    private val remote: OfficialDrawDataSource,
    private val store: SyncStore,
    private val replayRefresher: ReplayRefresher,
    private val timeProvider: TimeProvider,
) {
    private val mutex = Mutex()
    private val running = AtomicBoolean(false)

    suspend fun sync(trigger: SyncTrigger): SyncResult {
        if (!running.compareAndSet(false, true)) return SyncResult.AlreadyRunning
        if (!mutex.tryLock()) {
            running.set(false)
            return SyncResult.AlreadyRunning
        }
        try {
            val now = timeProvider.nowEpochMillis()
            val previousMetadata = store.metadata()
            if (
                trigger == SyncTrigger.AUTO &&
                previousMetadata?.lastSuccessEpochMillis?.let { now - it < AUTO_THROTTLE_MILLIS } == true
            ) {
                return SyncResult.Throttled
            }

            val recent = remote.fetchRecent(RECENT_LIMIT)
            val recentPage = when (recent) {
                is OfficialDataResult.Success -> recent.page
                else -> return fail(now, previousMetadata, recent.failureName())
            }
            val localLatest = store.latest()
            val allRemote = if (needsRangeBackfill(localLatest, recentPage.draws)) {
                when (
                    val backfill = fetchCompleteRange(
                        issueStart = nextIssue(localLatest!!.issue),
                        issueEnd = recentPage.draws.maxOf { it.issue },
                    )
                ) {
                    is RangeResult.Success -> backfill.draws
                    is RangeResult.Failure -> return fail(now, previousMetadata, backfill.reason)
                }
            } else {
                recentPage.draws
            }

            if (allRemote.groupingBy { it.issue }.eachCount().any { it.value > 1 }) {
                return fail(now, previousMetadata, "INVALID_PAYLOAD")
            }
            val existing = store.byIssues(allRemote.mapTo(mutableSetOf()) { it.issue })
                .associateBy { it.issue }
            val added = mutableListOf<DrawEntity>()
            val corrected = mutableListOf<DrawEntity>()
            val metadataUpdates = mutableListOf<DrawEntity>()
            var unchanged = 0
            allRemote.forEach { official ->
                val entity = official.toEntity()
                val local = existing[official.issue]
                when {
                    local == null -> added += entity
                    local.hasSameDrawFieldsAs(entity) -> {
                        unchanged += 1
                        if (local != entity) metadataUpdates += entity
                    }
                    else -> corrected += entity
                }
            }

            val changed = added + corrected + metadataUpdates
            val latestIssue = sequenceOf(
                localLatest?.issue,
                allRemote.maxOfOrNull { it.issue },
            ).filterNotNull().maxOrNull()
            val correctedIssues = corrected.mapTo(sortedSetOf()) { it.issue }
            val successMetadata = SyncMetadataEntity(
                lastAttemptEpochMillis = now,
                lastSuccessEpochMillis = now,
                latestIssue = latestIssue,
                lastFailureType = null,
                correctedIssuesJson = if (correctedIssues.isEmpty()) {
                    "[]"
                } else {
                    correctedIssues.joinToString(
                        prefix = "[\"",
                        separator = "\",\"",
                        postfix = "\"]",
                    )
                },
            )
            store.commit(changed, successMetadata)

            val replayIssues = (added + corrected).mapTo(sortedSetOf()) { it.issue }
            if (replayIssues.isNotEmpty()) replayRefresher.refresh(replayIssues)
            return SyncResult.Updated(
                added = added.size,
                corrected = corrected.size,
                unchanged = unchanged,
                latestIssue = latestIssue,
            )
        } finally {
            mutex.unlock()
            running.set(false)
        }
    }

    private suspend fun fetchCompleteRange(
        issueStart: String,
        issueEnd: String,
    ): RangeResult {
        val first = remote.fetchRange(issueStart, issueEnd, pageNumber = 1, pageSize = PAGE_SIZE)
        val firstPage = (first as? OfficialDataResult.Success)?.page
            ?: return RangeResult.Failure(first.failureName())
        val total = firstPage.total ?: return RangeResult.Failure("INVALID_PAYLOAD")
        if (total <= 0) return RangeResult.Failure("EMPTY_RESPONSE")
        val pageCount = (total + PAGE_SIZE - 1) / PAGE_SIZE
        val draws = firstPage.draws.toMutableList()
        for (pageNumber in 2..pageCount) {
            when (val result = remote.fetchRange(issueStart, issueEnd, pageNumber, PAGE_SIZE)) {
                is OfficialDataResult.Success -> draws += result.page.draws
                else -> return RangeResult.Failure(result.failureName())
            }
        }
        if (draws.size != total || draws.any { it.issue !in issueStart..issueEnd }) {
            return RangeResult.Failure("INCOMPLETE_RANGE")
        }
        if (draws.map { it.issue }.distinct().size != draws.size) {
            return RangeResult.Failure("INVALID_PAYLOAD")
        }
        return RangeResult.Success(draws)
    }

    private suspend fun fail(
        now: Long,
        previous: SyncMetadataEntity?,
        type: String,
    ): SyncResult {
        store.recordFailure(
            (previous ?: SyncMetadataEntity()).copy(
                lastAttemptEpochMillis = now,
                lastFailureType = type,
            ),
        )
        return SyncResult.Failed(type)
    }

    private fun needsRangeBackfill(
        localLatest: DrawEntity?,
        remoteDraws: List<OfficialDraw>,
    ): Boolean =
        localLatest != null &&
            remoteDraws.isNotEmpty() &&
            localLatest.issue < remoteDraws.minOf { it.issue }

    private fun nextIssue(issue: String): String =
        (issue.toLong() + 1L).toString().padStart(7, '0')

    private sealed interface RangeResult {
        data class Success(val draws: List<OfficialDraw>) : RangeResult
        data class Failure(val reason: String) : RangeResult
    }

    private companion object {
        const val RECENT_LIMIT = 100
        const val PAGE_SIZE = 100
        const val AUTO_THROTTLE_MILLIS = 5 * 60 * 1000L
    }
}

private fun DrawEntity.hasSameDrawFieldsAs(other: DrawEntity): Boolean =
    issue == other.issue &&
        drawDate == other.drawDate &&
        hundreds == other.hundreds &&
        tens == other.tens &&
        ones == other.ones &&
        officialDetailUrl == other.officialDetailUrl

private fun OfficialDataResult.failureName(): String = when (this) {
    OfficialDataResult.EmptyResponse -> "EMPTY_RESPONSE"
    is OfficialDataResult.HttpFailure -> "HTTP_$statusCode"
    is OfficialDataResult.InvalidPayload -> "INVALID_PAYLOAD"
    is OfficialDataResult.NetworkFailure -> "NETWORK"
    is OfficialDataResult.Success -> error("Success is not a failure")
}
