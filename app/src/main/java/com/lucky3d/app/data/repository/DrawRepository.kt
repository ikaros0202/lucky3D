package com.lucky3d.app.data.repository

import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.data.local.DrawDao
import com.lucky3d.app.data.local.SyncMetadataDao
import com.lucky3d.app.data.local.SyncMetadataEntity
import com.lucky3d.app.data.mapper.toRecord
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class DrawSyncMetadata(
    val lastSuccessEpochMillis: Long? = null,
    val latestIssue: String? = null,
    val lastFailureType: String? = null,
    val correctedIssues: Set<String> = emptySet(),
)

interface DrawRepository {
    val latestDraw: Flow<DrawRecord?>
    val allDrawsAscending: Flow<List<DrawRecord>>
    val syncMetadata: Flow<DrawSyncMetadata?>

    fun observeRecent(limit: Int): Flow<List<DrawRecord>>

    fun observe(query: DrawQuery): Flow<List<DrawRecord>>

    suspend fun refresh(): SyncResult

    suspend fun syncOnForeground(): SyncResult
}

@Singleton
class DefaultDrawRepository @Inject constructor(
    private val drawDao: DrawDao,
    syncMetadataDao: SyncMetadataDao,
    private val syncCoordinator: SyncCoordinator,
) : DrawRepository {
    override val latestDraw: Flow<DrawRecord?> = drawDao.observeLatest().map { it?.toRecord() }
    override val allDrawsAscending: Flow<List<DrawRecord>> =
        drawDao.observeAllAscending().map { entities -> entities.map { it.toRecord() } }
    override val syncMetadata: Flow<DrawSyncMetadata?> =
        syncMetadataDao.observe().map { it?.toDrawSyncMetadata() }

    override fun observeRecent(limit: Int): Flow<List<DrawRecord>> {
        require(limit > 0) { "Recent draw limit must be positive" }
        return drawDao.observeRecent(limit).map { entities -> entities.map { it.toRecord() } }
    }

    override fun observe(query: DrawQuery): Flow<List<DrawRecord>> = when (query) {
        is DrawQuery.Recent -> observeRecent(query.limit)
        is DrawQuery.Issue -> drawDao.observeByIssue(query.issue)
            .map { entity -> listOfNotNull(entity?.toRecord()) }
        is DrawQuery.Year -> drawDao.observeByYear(query.year)
            .map { entities -> entities.map { it.toRecord() } }
        is DrawQuery.DateRange -> drawDao.observeByDateRange(query.startDate, query.endDate)
            .map { entities -> entities.map { it.toRecord() } }
    }

    override suspend fun refresh(): SyncResult = syncCoordinator.sync(SyncTrigger.MANUAL)

    override suspend fun syncOnForeground(): SyncResult = syncCoordinator.sync(SyncTrigger.AUTO)
}

internal fun SyncMetadataEntity.toDrawSyncMetadata(): DrawSyncMetadata = DrawSyncMetadata(
    lastSuccessEpochMillis = lastSuccessEpochMillis,
    latestIssue = latestIssue,
    lastFailureType = lastFailureType,
    correctedIssues = runCatching {
        kotlinx.serialization.json.Json.decodeFromString<List<String>>(correctedIssuesJson).toSet()
    }.getOrDefault(emptySet()),
)

sealed interface DrawQuery {
    data class Recent(val limit: Int) : DrawQuery {
        init {
            require(limit > 0)
        }
    }

    data class Issue(val issue: String) : DrawQuery {
        init {
            require(issue.matches(Regex("""\d{7}""")))
        }
    }

    data class Year(val year: String) : DrawQuery {
        init {
            require(year.matches(Regex("""\d{4}""")))
        }
    }

    data class DateRange(
        val startDate: String,
        val endDate: String,
    ) : DrawQuery {
        init {
            require(startDate.matches(ISO_DATE_REGEX))
            require(endDate.matches(ISO_DATE_REGEX))
            require(startDate <= endDate)
        }
    }

    private companion object {
        val ISO_DATE_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")
    }
}
