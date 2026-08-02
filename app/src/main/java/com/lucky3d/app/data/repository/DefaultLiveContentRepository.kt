package com.lucky3d.app.data.repository

import com.lucky3d.app.core.model.CaibaoDocument
import com.lucky3d.app.core.model.LiveContentType
import com.lucky3d.app.core.model.TrialNumber
import com.lucky3d.app.core.model.TrialSource
import com.lucky3d.app.data.file.CaibaoFileException
import com.lucky3d.app.data.file.CaibaoFileStore
import com.lucky3d.app.data.local.CaibaoDocumentEntity
import com.lucky3d.app.data.local.DrawDao
import com.lucky3d.app.data.local.LiveContentDao
import com.lucky3d.app.data.local.LiveContentRefreshMetadataEntity
import com.lucky3d.app.data.local.TrialNumberEntity
import com.lucky3d.app.data.remote.CaibaoDataSource
import com.lucky3d.app.data.remote.CaibaoDescriptorResult
import com.lucky3d.app.data.remote.CaibaoImageResult
import com.lucky3d.app.data.remote.CaibaoRemoteDescriptor
import com.lucky3d.app.data.remote.LiveContentRemoteFailure
import com.lucky3d.app.data.remote.TrialDataSource
import com.lucky3d.app.data.remote.TrialRemoteResult
import com.lucky3d.app.data.remote.TrialRemoteHistoryResult
import com.lucky3d.app.data.remote.TrialRemoteRecord
import com.lucky3d.app.domain.livecontent.LiveContentFailure
import com.lucky3d.app.domain.livecontent.LiveContentRefreshMetadata
import com.lucky3d.app.domain.livecontent.LiveContentRefreshResult
import com.lucky3d.app.domain.livecontent.LiveContentRefreshState
import com.lucky3d.app.domain.livecontent.LiveRefreshTrigger
import com.lucky3d.app.domain.livecontent.RefreshContext
import com.lucky3d.app.domain.livecontent.RefreshDecision
import com.lucky3d.app.domain.livecontent.RefreshPolicy
import java.net.URI
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal interface LiveContentStore {
    fun observeTrial(): Flow<TrialNumber?>
    fun observeTrials(): Flow<List<TrialNumber>> =
        observeTrial().map { it?.let(::listOf).orEmpty() }
    fun observeCaibao(): Flow<CaibaoDocument?>
    fun observeCaibaos(): Flow<List<CaibaoDocument>> =
        observeCaibao().map { it?.let(::listOf).orEmpty() }
    fun observeMetadata(contentType: LiveContentType): Flow<LiveContentRefreshMetadata?>
    suspend fun metadata(contentType: LiveContentType): LiveContentRefreshMetadata?
    suspend fun latestCaibao(): CaibaoDocument?
    suspend fun allCaibao(): List<CaibaoDocument>
    suspend fun latestOfficialIssue(): String?
    suspend fun commitTrial(trial: TrialNumber, metadata: LiveContentRefreshMetadata)
    suspend fun commitTrials(trials: List<TrialNumber>, metadata: LiveContentRefreshMetadata) {
        trials.forEach { commitTrial(it, metadata) }
    }
    suspend fun commitCaibao(document: CaibaoDocument, metadata: LiveContentRefreshMetadata)
    suspend fun recordMetadata(metadata: LiveContentRefreshMetadata)
    suspend fun deleteCaibao(issue: String)
}

@Singleton
class DefaultLiveContentRepository internal constructor(
    private val store: LiveContentStore,
    private val trialDataSource: TrialDataSource,
    private val caibaoDataSource: CaibaoDataSource,
    private val fileStore: CaibaoFileStore,
    private val clock: Clock,
    private val repositoryScope: CoroutineScope,
) : LiveContentRepository {
    @Inject
    constructor(
        liveContentDao: LiveContentDao,
        drawDao: DrawDao,
        trialDataSource: TrialDataSource,
        caibaoDataSource: CaibaoDataSource,
        fileStore: CaibaoFileStore,
        clock: Clock,
    ) : this(
        store = RoomLiveContentStore(liveContentDao, drawDao),
        trialDataSource = trialDataSource,
        caibaoDataSource = caibaoDataSource,
        fileStore = fileStore,
        clock = clock,
        repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    private val trialRuntimeState = MutableStateFlow<LiveContentRefreshState?>(null)
    private val caibaoRuntimeState = MutableStateFlow<LiveContentRefreshState?>(null)
    private val trialInFlight = InFlightRefresh()
    private val caibaoInFlight = InFlightRefresh()
    private val caibaoOperationMutex = Mutex()

    override val trialNumber: Flow<TrialNumber?> = store.observeTrial()
    override val trialNumbers: Flow<List<TrialNumber>> = store.observeTrials()
    override val caibaoDocument: Flow<CaibaoDocument?> = store.observeCaibao().map { document ->
        document?.takeIf {
            it.cachedLocalDate >= clock.instant().atZone(BEIJING).toLocalDate()
                .minusDays(CAIBAO_CACHE_DAYS - 1)
        }
    }
    override val caibaoDocuments: Flow<List<CaibaoDocument>> = store.observeCaibaos().map { documents ->
        val cutoff = clock.instant().atZone(BEIJING).toLocalDate().minusDays(CAIBAO_CACHE_DAYS - 1)
        documents.filter { it.cachedLocalDate >= cutoff }
    }
    override val trialRefreshState: Flow<LiveContentRefreshState> =
        refreshState(store.observeMetadata(LiveContentType.TRIAL_NUMBER), trialRuntimeState)
    override val caibaoRefreshState: Flow<LiveContentRefreshState> =
        caibaoRefreshState(
            refreshMetadata = store.observeMetadata(LiveContentType.CAIBAO),
            cleanupMetadata = store.observeMetadata(LiveContentType.CAIBAO_CLEANUP),
            runtime = caibaoRuntimeState,
        )

    override suspend fun refreshTrial(trigger: LiveRefreshTrigger): LiveContentRefreshResult =
        sharedRefresh(trialInFlight) { performTrialRefresh(trigger) }

    override suspend fun refreshTrialHistory(trigger: LiveRefreshTrigger): LiveContentRefreshResult =
        sharedRefresh(trialInFlight) { performTrialHistoryRefresh(trigger) }

    override suspend fun refreshCaibao(trigger: LiveRefreshTrigger): LiveContentRefreshResult =
        sharedRefresh(caibaoInFlight) {
            caibaoOperationMutex.withLock { performCaibaoRefresh(trigger, requestedIssue = null) }
        }

    override suspend fun refreshCaibaoIssue(issue: String): LiveContentRefreshResult =
        sharedRefresh(caibaoInFlight) {
            caibaoOperationMutex.withLock {
                performCaibaoRefresh(LiveRefreshTrigger.MANUAL, requestedIssue = issue)
            }
        }

    override suspend fun readCaibaoImage(document: CaibaoDocument): CaibaoImageReadResult =
        caibaoOperationMutex.withLock {
            try {
                CaibaoImageReadResult.Loaded(
                    fileStore.readValidated(
                        fileName = document.localFileName,
                        expectedSha256 = document.sha256,
                        expectedMimeType = document.mimeType,
                        expectedWidth = document.width,
                        expectedHeight = document.height,
                    ),
                )
            } catch (failure: CaibaoFileException) {
                unavailableCaibaoImage(document, failure.failure)
            } catch (exception: Exception) {
                exception.rethrowCancellation()
                unavailableCaibaoImage(document, LiveContentFailure.FILE_IO)
            }
        }

    override suspend fun invalidateCaibaoImage(document: CaibaoDocument) {
        caibaoOperationMutex.withLock {
            unavailableCaibaoImage(document, LiveContentFailure.INVALID_IMAGE)
        }
    }

    override suspend fun cleanCaibaoCache() {
        caibaoOperationMutex.withLock {
            val failure = cleanCaibaoCacheInternal()
            if (failure == null) {
                clearCleanupFailureAfterSuccessfulCleanup()
            } else {
                recordCleanupFailure(failure)
            }
        }
    }

    private suspend fun unavailableCaibaoImage(
        document: CaibaoDocument,
        readFailure: LiveContentFailure,
    ): CaibaoImageReadResult.Unavailable {
        val current = try {
            store.latestCaibao()
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            caibaoRuntimeState.value =
                LiveContentRefreshState.Failed(LiveContentFailure.DATABASE)
            return CaibaoImageReadResult.Unavailable(LiveContentFailure.DATABASE)
        }
        if (current != document) {
            return CaibaoImageReadResult.Unavailable(readFailure)
        }
        var cleanupFailure: LiveContentFailure? = null
        val stagedDeletion = try {
            fileStore.stageDelete(document.localFileName)
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            cleanupFailure = LiveContentFailure.FILE_IO
            null
        }
        try {
            store.deleteCaibao(document.issue)
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            var failure = LiveContentFailure.DATABASE
            if (stagedDeletion != null) {
                try {
                    fileStore.rollbackDelete(stagedDeletion)
                } catch (rollbackException: Exception) {
                    rollbackException.rethrowCancellation()
                    failure = LiveContentFailure.FILE_IO
                }
            }
            caibaoRuntimeState.value = LiveContentRefreshState.Failed(failure)
            return CaibaoImageReadResult.Unavailable(failure)
        }
        if (stagedDeletion != null) {
            try {
                fileStore.commitDelete(stagedDeletion)
            } catch (exception: Exception) {
                exception.rethrowCancellation()
                cleanupFailure = LiveContentFailure.FILE_IO
            }
        }
        val failure = cleanupFailure ?: readFailure
        caibaoRuntimeState.value = LiveContentRefreshState.Failed(failure)
        return CaibaoImageReadResult.Unavailable(failure)
    }

    private suspend fun performTrialRefresh(
        trigger: LiveRefreshTrigger,
    ): LiveContentRefreshResult {
        val now = clock.instant()
        val previous = try {
            store.metadata(LiveContentType.TRIAL_NUMBER)
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            trialRuntimeState.value = LiveContentRefreshState.Failed(LiveContentFailure.DATABASE)
            return LiveContentRefreshResult.Failed(LiveContentFailure.DATABASE)
        }
        when (
            val decision = RefreshPolicy.decideTrial(
                trigger,
                RefreshContext(now, BEIJING, previous),
            )
        ) {
            is RefreshDecision.Skip -> return LiveContentRefreshResult.Skipped(decision.reason)
            RefreshDecision.Fetch -> Unit
        }
        trialRuntimeState.value = LiveContentRefreshState.Refreshing
        val officialIssue = try {
            store.latestOfficialIssue()
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            return failTrial(previous, trigger, now.toEpochMilli(), LiveContentFailure.DATABASE, false)
        }
        val remote = try {
            trialDataSource.fetchLatest()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            TrialRemoteResult.Failure(LiveContentRemoteFailure.Network)
        }
        val record = when (remote) {
            is TrialRemoteResult.Failure -> {
                return failTrial(
                    previous,
                    trigger,
                    now.toEpochMilli(),
                    mapHtmlFailure(remote.failure),
                    true,
                )
            }
            is TrialRemoteResult.Success -> remote.record
        }
        val validationFailure = when {
            !ISSUE_PATTERN.matches(record.issue) -> LiveContentFailure.INVALID_ISSUE
            !NUMBER_PATTERN.matches(record.number) -> LiveContentFailure.INVALID_NUMBER
            officialIssue != null && record.issue <= officialIssue -> LiveContentFailure.INVALID_ISSUE
            else -> null
        }
        if (validationFailure != null) {
            return failTrial(previous, trigger, now.toEpochMilli(), validationFailure, true)
        }
        val metadata = successMetadata(
            LiveContentType.TRIAL_NUMBER,
            previous,
            trigger,
            now.toEpochMilli(),
        )
        val trial = TrialNumber(
            issue = record.issue,
            number = record.number,
            source = TrialSource.CJCP_SIMULATED,
            sourcePageUrl = TRIAL_SOURCE_PAGE,
            sourceLocalDate = now.atZone(BEIJING).toLocalDate(),
            fetchedAtEpochMillis = now.toEpochMilli(),
        )
        return try {
            store.commitTrial(trial, metadata)
            trialRuntimeState.value = null
            LiveContentRefreshResult.Success
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            failTrial(previous, trigger, now.toEpochMilli(), LiveContentFailure.DATABASE, true)
        }
    }

    private suspend fun performTrialHistoryRefresh(
        trigger: LiveRefreshTrigger,
    ): LiveContentRefreshResult {
        val now = clock.instant()
        val previous = try {
            store.metadata(LiveContentType.TRIAL_NUMBER)
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            trialRuntimeState.value = LiveContentRefreshState.Failed(LiveContentFailure.DATABASE)
            return LiveContentRefreshResult.Failed(LiveContentFailure.DATABASE)
        }
        trialRuntimeState.value = LiveContentRefreshState.Refreshing
        val records = mutableListOf<TrialRemoteRecord>()
        for (page in 1..5) {
            val result = try {
                trialDataSource.fetchHistoryPage(page)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                TrialRemoteHistoryResult.Failure(page, LiveContentRemoteFailure.Network)
            }
            when (result) {
                is TrialRemoteHistoryResult.Failure -> return failTrial(
                    previous, trigger, now.toEpochMilli(), mapHtmlFailure(result.failure), true,
                )
                is TrialRemoteHistoryResult.Success -> records += result.records
            }
        }
        val unique = linkedMapOf<String, TrialRemoteRecord>()
        records.forEach { record ->
            if (!ISSUE_PATTERN.matches(record.issue) || !NUMBER_PATTERN.matches(record.number)) {
                return failTrial(previous, trigger, now.toEpochMilli(), LiveContentFailure.INVALID_HTML, true)
            }
            if (unique.put(record.issue, record) != null) {
                return failTrial(previous, trigger, now.toEpochMilli(), LiveContentFailure.INVALID_HTML, true)
            }
        }
        if (unique.isEmpty()) {
            return failTrial(previous, trigger, now.toEpochMilli(), LiveContentFailure.INVALID_HTML, true)
        }
        val metadata = successMetadata(LiveContentType.TRIAL_NUMBER, previous, trigger, now.toEpochMilli())
        val trials = unique.values.map { record ->
            TrialNumber(
                issue = record.issue,
                number = record.number,
                source = TrialSource.CJCP_SIMULATED,
                sourcePageUrl = TRIAL_SOURCE_PAGE,
                sourceLocalDate = now.atZone(BEIJING).toLocalDate(),
                fetchedAtEpochMillis = now.toEpochMilli(),
            )
        }
        return try {
            store.commitTrials(trials, metadata)
            trialRuntimeState.value = null
            LiveContentRefreshResult.Success
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            failTrial(previous, trigger, now.toEpochMilli(), LiveContentFailure.DATABASE, true)
        }
    }

    private suspend fun performCaibaoRefresh(
        trigger: LiveRefreshTrigger,
        requestedIssue: String?,
    ): LiveContentRefreshResult {
        val now = clock.instant()
        val previous = try {
            store.metadata(LiveContentType.CAIBAO)
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            caibaoRuntimeState.value = LiveContentRefreshState.Failed(LiveContentFailure.DATABASE)
            return LiveContentRefreshResult.Failed(LiveContentFailure.DATABASE)
        }
        when (
            val decision = RefreshPolicy.decideCaibao(
                trigger,
                RefreshContext(now, BEIJING, previous),
            )
        ) {
            is RefreshDecision.Skip -> return LiveContentRefreshResult.Skipped(decision.reason)
            RefreshDecision.Fetch -> Unit
        }
        caibaoRuntimeState.value = LiveContentRefreshState.Refreshing
        val latest = try {
            store.latestCaibao()
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            return failCaibao(previous, trigger, now.toEpochMilli(), LiveContentFailure.DATABASE, false)
        }
        val descriptorResult = try {
            if (requestedIssue == null) {
                caibaoDataSource.fetchLatestDescriptor()
            } else {
                caibaoDataSource.fetchDescriptor(requestedIssue)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            CaibaoDescriptorResult.Failure(LiveContentRemoteFailure.Network)
        }
        val descriptor = when (descriptorResult) {
            is CaibaoDescriptorResult.Failure -> {
                return failCaibao(
                    previous,
                    trigger,
                    now.toEpochMilli(),
                    mapDescriptorFailure(descriptorResult.failure),
                    true,
                )
            }
            is CaibaoDescriptorResult.Success -> descriptorResult.descriptor
        }
        val descriptorFailure = validateDescriptor(descriptor)
        if (descriptorFailure != null) {
            return failCaibao(previous, trigger, now.toEpochMilli(), descriptorFailure, true)
        }
        if (requestedIssue != null && descriptor.issue != requestedIssue) {
            return failCaibao(
                previous,
                trigger,
                now.toEpochMilli(),
                LiveContentFailure.INVALID_ISSUE,
                true,
            )
        }
        if (requestedIssue == null && latest != null && descriptor.issue < latest.issue) {
            return failCaibao(
                previous,
                trigger,
                now.toEpochMilli(),
                LiveContentFailure.INVALID_ISSUE,
                true,
            )
        }
        val successMetadata = successMetadata(
            LiveContentType.CAIBAO,
            previous,
            trigger,
            now.toEpochMilli(),
        )
        if (latest?.issue == descriptor.issue) {
            return try {
                val cleanupFailure = cleanCaibaoCacheInternal()
                if (cleanupFailure != null) {
                    recordCleanupFailure(cleanupFailure)
                    return LiveContentRefreshResult.Failed(cleanupFailure)
                }
                clearCleanupFailureAfterSuccessfulCleanup()
                store.recordMetadata(successMetadata)
                caibaoRuntimeState.value = null
                LiveContentRefreshResult.Success
            } catch (exception: Exception) {
                exception.rethrowCancellation()
                failCaibao(previous, trigger, now.toEpochMilli(), LiveContentFailure.DATABASE, true)
            }
        }
        val imageResult = try {
            caibaoDataSource.fetchImage(descriptor.imageUrl)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            CaibaoImageResult.Failure(LiveContentRemoteFailure.Network)
        }
        val image = when (imageResult) {
            is CaibaoImageResult.Failure -> {
                return failCaibao(
                    previous,
                    trigger,
                    now.toEpochMilli(),
                    mapImageFailure(imageResult.failure),
                    true,
                )
            }
            is CaibaoImageResult.Success -> imageResult
        }
        val staged = try {
            fileStore.stageAndValidate(descriptor.issue, image.bytes, image.mimeType)
        } catch (failure: CaibaoFileException) {
            return failCaibao(previous, trigger, now.toEpochMilli(), failure.failure, true)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            return failCaibao(
                previous,
                trigger,
                now.toEpochMilli(),
                LiveContentFailure.FILE_IO,
                true,
            )
        }
        val stored = try {
            fileStore.commit(staged)
        } catch (failure: CaibaoFileException) {
            return failCaibao(previous, trigger, now.toEpochMilli(), failure.failure, true)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            return failCaibao(
                previous,
                trigger,
                now.toEpochMilli(),
                LiveContentFailure.FILE_IO,
                true,
            )
        }
        val document = CaibaoDocument(
            issue = descriptor.issue,
            edition = CAIBAO_EDITION,
            title = descriptor.title,
            sourcePageUrl = CAIBAO_SOURCE_PAGE,
            imageUrl = descriptor.imageUrl,
            localFileName = stored.fileName,
            sha256 = stored.sha256,
            mimeType = stored.mimeType,
            width = stored.width,
            height = stored.height,
            cachedLocalDate = now.atZone(BEIJING).toLocalDate(),
            fetchedAtEpochMillis = now.toEpochMilli(),
        )
        try {
            store.commitCaibao(document, successMetadata)
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            val compensated = try {
                fileStore.rollback(stored.file)
                true
            } catch (rollbackException: Exception) {
                rollbackException.rethrowCancellation()
                false
            }
            val failure = if (compensated) LiveContentFailure.DATABASE else LiveContentFailure.FILE_IO
            return failCaibao(previous, trigger, now.toEpochMilli(), failure, true)
        }
        val cleanupFailure = cleanCaibaoCacheInternal()
        if (cleanupFailure != null) {
            recordCleanupFailure(cleanupFailure)
            return LiveContentRefreshResult.Failed(cleanupFailure)
        }
        clearCleanupFailureAfterSuccessfulCleanup()
        return LiveContentRefreshResult.Success
    }

    private suspend fun failTrial(
        previous: LiveContentRefreshMetadata?,
        trigger: LiveRefreshTrigger,
        nowEpochMillis: Long,
        failure: LiveContentFailure,
        attempted: Boolean,
    ): LiveContentRefreshResult = recordFailure(
        type = LiveContentType.TRIAL_NUMBER,
        previous = previous,
        trigger = trigger,
        nowEpochMillis = nowEpochMillis,
        failure = failure,
        attempted = attempted,
        cooldownMillis = TRIAL_COOLDOWN_MILLIS,
        runtime = trialRuntimeState,
    )

    private suspend fun failCaibao(
        previous: LiveContentRefreshMetadata?,
        trigger: LiveRefreshTrigger,
        nowEpochMillis: Long,
        failure: LiveContentFailure,
        attempted: Boolean,
    ): LiveContentRefreshResult = recordFailure(
        type = LiveContentType.CAIBAO,
        previous = previous,
        trigger = trigger,
        nowEpochMillis = nowEpochMillis,
        failure = failure,
        attempted = attempted,
        cooldownMillis = CAIBAO_COOLDOWN_MILLIS,
        runtime = caibaoRuntimeState,
    )

    private suspend fun recordFailure(
        type: LiveContentType,
        previous: LiveContentRefreshMetadata?,
        trigger: LiveRefreshTrigger,
        nowEpochMillis: Long,
        failure: LiveContentFailure,
        attempted: Boolean,
        cooldownMillis: Long,
        runtime: MutableStateFlow<LiveContentRefreshState?>,
    ): LiveContentRefreshResult {
        val base = if (attempted) {
            attemptedMetadata(type, previous, trigger, nowEpochMillis)
        } else {
            previous ?: emptyMetadata(type)
        }
        val failed = base.copy(
            nextAllowedAutoAttemptEpochMillis = nowEpochMillis + cooldownMillis,
            lastFailure = failure,
        )
        return try {
            store.recordMetadata(failed)
            runtime.value = null
            LiveContentRefreshResult.Failed(failure)
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            runtime.value = LiveContentRefreshState.Failed(LiveContentFailure.DATABASE)
            LiveContentRefreshResult.Failed(LiveContentFailure.DATABASE)
        }
    }

    private suspend fun cleanCaibaoCacheInternal(): LiveContentFailure? {
        val documents = try {
            store.allCaibao()
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            return LiveContentFailure.DATABASE
        }
        try {
            fileStore.removeTemporaryAndOrphanFiles(documents.mapTo(mutableSetOf()) { it.localFileName })
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            return LiveContentFailure.FILE_IO
        }
        val cutoff = clock.instant().atZone(BEIJING).toLocalDate()
            .minusDays(CAIBAO_CACHE_DAYS - 1)
        var failure: LiveContentFailure? = null
        documents.filter { it.cachedLocalDate < cutoff }.forEach { document ->
            val stagedDeletion = try {
                fileStore.stageDelete(document.localFileName)
            } catch (exception: Exception) {
                exception.rethrowCancellation()
                failure = LiveContentFailure.FILE_IO
                return@forEach
            }
            try {
                store.deleteCaibao(document.issue)
            } catch (exception: Exception) {
                exception.rethrowCancellation()
                failure = try {
                    fileStore.rollbackDelete(stagedDeletion)
                    LiveContentFailure.DATABASE
                } catch (rollbackException: Exception) {
                    rollbackException.rethrowCancellation()
                    LiveContentFailure.FILE_IO
                }
                return@forEach
            }
            try {
                fileStore.commitDelete(stagedDeletion)
            } catch (exception: Exception) {
                exception.rethrowCancellation()
                failure = LiveContentFailure.FILE_IO
            }
        }
        return failure
    }

    private suspend fun recordCleanupFailure(failure: LiveContentFailure) {
        val previous = try {
            store.metadata(LiveContentType.CAIBAO_CLEANUP)
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            caibaoRuntimeState.value = LiveContentRefreshState.Failed(LiveContentFailure.DATABASE)
            return
        }
        val failed =
            (previous ?: emptyMetadata(LiveContentType.CAIBAO_CLEANUP)).copy(lastFailure = failure)
        try {
            store.recordMetadata(failed)
            caibaoRuntimeState.value = null
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            caibaoRuntimeState.value = LiveContentRefreshState.Failed(LiveContentFailure.DATABASE)
        }
    }

    private suspend fun clearCleanupFailureAfterSuccessfulCleanup() {
        val current = try {
            store.metadata(LiveContentType.CAIBAO_CLEANUP)
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            caibaoRuntimeState.value = LiveContentRefreshState.Failed(LiveContentFailure.DATABASE)
            return
        }
        if (current?.lastFailure == null) {
            caibaoRuntimeState.value = null
            return
        }
        try {
            store.recordMetadata(current.copy(lastFailure = null))
            caibaoRuntimeState.value = null
        } catch (exception: Exception) {
            exception.rethrowCancellation()
            caibaoRuntimeState.value = LiveContentRefreshState.Failed(LiveContentFailure.DATABASE)
        }
    }

    private suspend fun sharedRefresh(
        slot: InFlightRefresh,
        operation: suspend () -> LiveContentRefreshResult,
    ): LiveContentRefreshResult {
        val deferred = slot.mutex.withLock {
            slot.current?.takeUnless { it.isCompleted } ?: repositoryScope.async {
                operation()
            }.also { created ->
                slot.current = created
                created.invokeOnCompletion {
                    repositoryScope.launch {
                        slot.mutex.withLock {
                            if (slot.current === created) slot.current = null
                        }
                    }
                }
            }
        }
        return deferred.await()
    }

    private fun refreshState(
        metadata: Flow<LiveContentRefreshMetadata?>,
        runtime: Flow<LiveContentRefreshState?>,
    ): Flow<LiveContentRefreshState> = combine(metadata, runtime) { stored, transient ->
        transient ?: stored?.lastFailure?.let(LiveContentRefreshState::Failed)
        ?: LiveContentRefreshState.Idle
    }.distinctUntilChanged()

    private fun caibaoRefreshState(
        refreshMetadata: Flow<LiveContentRefreshMetadata?>,
        cleanupMetadata: Flow<LiveContentRefreshMetadata?>,
        runtime: Flow<LiveContentRefreshState?>,
    ): Flow<LiveContentRefreshState> =
        combine(refreshMetadata, cleanupMetadata, runtime) { refresh, cleanup, transient ->
            transient
                ?: refresh?.lastFailure?.let(LiveContentRefreshState::Failed)
                ?: cleanup?.lastFailure?.let(LiveContentRefreshState::Failed)
                ?: LiveContentRefreshState.Idle
        }.distinctUntilChanged()

    private fun successMetadata(
        type: LiveContentType,
        previous: LiveContentRefreshMetadata?,
        trigger: LiveRefreshTrigger,
        nowEpochMillis: Long,
    ): LiveContentRefreshMetadata = attemptedMetadata(
        type,
        previous,
        trigger,
        nowEpochMillis,
    ).copy(
        lastSuccessLocalDate = java.time.Instant.ofEpochMilli(nowEpochMillis)
            .atZone(BEIJING)
            .toLocalDate(),
        lastSuccessEpochMillis = nowEpochMillis,
        nextAllowedAutoAttemptEpochMillis = null,
        lastFailure = null,
    )

    private fun attemptedMetadata(
        type: LiveContentType,
        previous: LiveContentRefreshMetadata?,
        trigger: LiveRefreshTrigger,
        nowEpochMillis: Long,
    ): LiveContentRefreshMetadata {
        val today = java.time.Instant.ofEpochMilli(nowEpochMillis).atZone(BEIJING).toLocalDate()
        val previousCount = if (previous?.attemptLocalDate == today) previous.autoAttemptCount else 0
        val automatic = trigger != LiveRefreshTrigger.MANUAL
        return (previous ?: emptyMetadata(type)).copy(
            contentType = type,
            attemptLocalDate = today,
            autoAttemptCount = previousCount + if (automatic) 1 else 0,
            lastAttemptEpochMillis = nowEpochMillis,
        )
    }

    private fun validateDescriptor(descriptor: CaibaoRemoteDescriptor): LiveContentFailure? {
        if (!ISSUE_PATTERN.matches(descriptor.issue)) return LiveContentFailure.INVALID_ISSUE
        if (
            descriptor.edition != CAIBAO_EDITION ||
            !descriptor.title.contains(CAIBAO_TITLE) ||
            descriptor.sourcePageUrl != CAIBAO_SOURCE_PAGE
        ) {
            return LiveContentFailure.INVALID_HTML
        }
        val imageUri = runCatching { URI(descriptor.imageUrl) }.getOrNull()
            ?: return LiveContentFailure.UNAPPROVED_IMAGE_HOST
        if (
            imageUri.scheme != "https" ||
            imageUri.host != CAIBAO_IMAGE_HOST ||
            (imageUri.port != -1 && imageUri.port != 443) ||
            imageUri.userInfo != null ||
            imageUri.rawQuery != null ||
            imageUri.rawFragment != null ||
            imageUri.rawPath != "/ftp/app/${descriptor.issue}/A11.jpg" &&
            imageUri.rawPath != "/ftp/yuwang/${descriptor.issue}/A11.jpg"
        ) {
            return LiveContentFailure.UNAPPROVED_IMAGE_HOST
        }
        return null
    }

    private class InFlightRefresh {
        val mutex = Mutex()
        var current: Deferred<LiveContentRefreshResult>? = null
    }

    private companion object {
        val BEIJING: ZoneId = ZoneId.of("Asia/Shanghai")
        const val TRIAL_SOURCE_PAGE = "https://m.cjcp.cn/kjhsjh/3dls/"
        const val CAIBAO_SOURCE_PAGE = "https://m.cz89.com/tuku/A11.htm"
        const val CAIBAO_IMAGE_HOST = "tuku.cz89.com"
        const val CAIBAO_EDITION = "A11"
        const val CAIBAO_CACHE_DAYS = 30L
        const val CAIBAO_TITLE = "彩吧彩报第三版"
        const val TRIAL_COOLDOWN_MILLIS = 30 * 60 * 1000L
        const val CAIBAO_COOLDOWN_MILLIS = 2 * 60 * 60 * 1000L
        val ISSUE_PATTERN = Regex("""20\d{5}""")
        val NUMBER_PATTERN = Regex("""\d{3}""")
    }
}

private class RoomLiveContentStore(
    private val liveContentDao: LiveContentDao,
    private val drawDao: DrawDao,
) : LiveContentStore {
    override fun observeTrial(): Flow<TrialNumber?> =
        liveContentDao.observeLatestTrial().map { it?.toDomain() }

    override fun observeTrials(): Flow<List<TrialNumber>> =
        liveContentDao.observeAllTrials().map { rows -> rows.map(TrialNumberEntity::toDomain) }

    override fun observeCaibao(): Flow<CaibaoDocument?> =
        liveContentDao.observeLatestCaibao().map { it?.toDomain() }

    override fun observeCaibaos(): Flow<List<CaibaoDocument>> =
        liveContentDao.observeAllCaibao().map { rows -> rows.map(CaibaoDocumentEntity::toDomain) }

    override fun observeMetadata(contentType: LiveContentType): Flow<LiveContentRefreshMetadata?> =
        liveContentDao.observeRefreshMetadata(contentType.name).map { it?.toDomain() }

    override suspend fun metadata(contentType: LiveContentType): LiveContentRefreshMetadata? =
        liveContentDao.refreshMetadata(contentType.name)?.toDomain()

    override suspend fun latestCaibao(): CaibaoDocument? =
        liveContentDao.latestCaibao()?.toDomain()

    override suspend fun allCaibao(): List<CaibaoDocument> =
        liveContentDao.allCaibao().map(CaibaoDocumentEntity::toDomain)

    override suspend fun latestOfficialIssue(): String? = drawDao.latest()?.issue

    override suspend fun commitTrial(
        trial: TrialNumber,
        metadata: LiveContentRefreshMetadata,
    ) {
        liveContentDao.upsertTrialAndMetadata(trial.toEntity(), metadata.toEntity())
    }

    override suspend fun commitTrials(
        trials: List<TrialNumber>,
        metadata: LiveContentRefreshMetadata,
    ) {
        liveContentDao.upsertTrialsAndMetadata(
            trials = trials.map(TrialNumber::toEntity),
            metadata = metadata.toEntity(),
        )
    }

    override suspend fun commitCaibao(
        document: CaibaoDocument,
        metadata: LiveContentRefreshMetadata,
    ) {
        liveContentDao.upsertCaibaoAndMetadata(document.toEntity(), metadata.toEntity())
    }

    override suspend fun recordMetadata(metadata: LiveContentRefreshMetadata) {
        liveContentDao.recordRefreshMetadata(metadata.toEntity())
    }

    override suspend fun deleteCaibao(issue: String) {
        liveContentDao.deleteCaibaoByIssues(listOf(issue))
    }
}

private fun TrialNumberEntity.toDomain() = TrialNumber(
    issue = issue,
    number = number,
    source = TrialSource.valueOf(source),
    sourcePageUrl = sourcePageUrl,
    sourceLocalDate = LocalDate.parse(sourceLocalDate),
    fetchedAtEpochMillis = fetchedAtEpochMillis,
)

private fun TrialNumber.toEntity() = TrialNumberEntity(
    issue = issue,
    number = number,
    source = source.name,
    sourcePageUrl = sourcePageUrl,
    sourceLocalDate = sourceLocalDate.toString(),
    fetchedAtEpochMillis = fetchedAtEpochMillis,
)

private fun CaibaoDocumentEntity.toDomain() = CaibaoDocument(
    issue = issue,
    edition = edition,
    title = title,
    sourcePageUrl = sourcePageUrl,
    imageUrl = imageUrl,
    localFileName = localFileName,
    sha256 = sha256,
    mimeType = mimeType,
    width = width,
    height = height,
    cachedLocalDate = LocalDate.parse(cachedLocalDate),
    fetchedAtEpochMillis = fetchedAtEpochMillis,
)

private fun CaibaoDocument.toEntity() = CaibaoDocumentEntity(
    issue = issue,
    edition = edition,
    title = title,
    sourcePageUrl = sourcePageUrl,
    imageUrl = imageUrl,
    localFileName = localFileName,
    sha256 = sha256,
    mimeType = mimeType,
    width = width,
    height = height,
    cachedLocalDate = cachedLocalDate.toString(),
    fetchedAtEpochMillis = fetchedAtEpochMillis,
)

private fun LiveContentRefreshMetadataEntity.toDomain() = LiveContentRefreshMetadata(
    contentType = LiveContentType.valueOf(contentType),
    attemptLocalDate = attemptLocalDate?.let(LocalDate::parse),
    autoAttemptCount = autoAttemptCount,
    lastAttemptEpochMillis = lastAttemptEpochMillis,
    lastSuccessLocalDate = lastSuccessLocalDate?.let(LocalDate::parse),
    lastSuccessEpochMillis = lastSuccessEpochMillis,
    nextAllowedAutoAttemptEpochMillis = nextAllowedAutoAttemptEpochMillis,
    lastFailure = lastFailureType?.let(LiveContentFailure::valueOf),
)

private fun LiveContentRefreshMetadata.toEntity() = LiveContentRefreshMetadataEntity(
    contentType = contentType.name,
    attemptLocalDate = attemptLocalDate?.toString(),
    autoAttemptCount = autoAttemptCount,
    lastAttemptEpochMillis = lastAttemptEpochMillis,
    lastSuccessLocalDate = lastSuccessLocalDate?.toString(),
    lastSuccessEpochMillis = lastSuccessEpochMillis,
    nextAllowedAutoAttemptEpochMillis = nextAllowedAutoAttemptEpochMillis,
    lastFailureType = lastFailure?.name,
)

private fun emptyMetadata(type: LiveContentType) = LiveContentRefreshMetadata(
    contentType = type,
    attemptLocalDate = null,
    autoAttemptCount = 0,
    lastAttemptEpochMillis = null,
    lastSuccessLocalDate = null,
    lastSuccessEpochMillis = null,
    nextAllowedAutoAttemptEpochMillis = null,
    lastFailure = null,
)

private fun mapHtmlFailure(failure: LiveContentRemoteFailure): LiveContentFailure = when (failure) {
    LiveContentRemoteFailure.Network -> LiveContentFailure.NETWORK
    LiveContentRemoteFailure.Http -> LiveContentFailure.HTTP
    LiveContentRemoteFailure.TooLarge,
    LiveContentRemoteFailure.InvalidContentType,
    LiveContentRemoteFailure.InvalidSource,
    LiveContentRemoteFailure.InvalidPayload,
    -> LiveContentFailure.INVALID_HTML
}

private fun mapDescriptorFailure(failure: LiveContentRemoteFailure): LiveContentFailure = when (failure) {
    LiveContentRemoteFailure.Network -> LiveContentFailure.NETWORK
    LiveContentRemoteFailure.Http -> LiveContentFailure.HTTP
    LiveContentRemoteFailure.InvalidSource -> LiveContentFailure.UNAPPROVED_IMAGE_HOST
    LiveContentRemoteFailure.TooLarge,
    LiveContentRemoteFailure.InvalidContentType,
    LiveContentRemoteFailure.InvalidPayload,
    -> LiveContentFailure.INVALID_HTML
}

private fun mapImageFailure(failure: LiveContentRemoteFailure): LiveContentFailure = when (failure) {
    LiveContentRemoteFailure.Network -> LiveContentFailure.NETWORK
    LiveContentRemoteFailure.Http -> LiveContentFailure.HTTP
    LiveContentRemoteFailure.TooLarge -> LiveContentFailure.IMAGE_TOO_LARGE
    LiveContentRemoteFailure.InvalidSource -> LiveContentFailure.UNAPPROVED_IMAGE_HOST
    LiveContentRemoteFailure.InvalidContentType,
    LiveContentRemoteFailure.InvalidPayload,
    -> LiveContentFailure.INVALID_IMAGE
}

private fun Exception.rethrowCancellation() {
    if (this is CancellationException) throw this
}
