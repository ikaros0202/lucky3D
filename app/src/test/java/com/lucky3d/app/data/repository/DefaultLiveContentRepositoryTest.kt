package com.lucky3d.app.data.repository

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.core.model.CaibaoDocument
import com.lucky3d.app.core.model.LiveContentType
import com.lucky3d.app.core.model.TrialNumber
import com.lucky3d.app.core.model.TrialSource
import com.lucky3d.app.data.file.AtomicFileMover
import com.lucky3d.app.data.file.CaibaoFileStore
import com.lucky3d.app.data.file.ImageBounds
import com.lucky3d.app.data.file.ImageBoundsReader
import com.lucky3d.app.data.remote.CaibaoDataSource
import com.lucky3d.app.data.remote.CaibaoDescriptorResult
import com.lucky3d.app.data.remote.CaibaoImageResult
import com.lucky3d.app.data.remote.CaibaoRemoteDescriptor
import com.lucky3d.app.data.remote.LiveContentRemoteFailure
import com.lucky3d.app.data.remote.TrialDataSource
import com.lucky3d.app.data.remote.TrialRemoteRecord
import com.lucky3d.app.data.remote.TrialRemoteResult
import com.lucky3d.app.domain.livecontent.LiveContentFailure
import com.lucky3d.app.domain.livecontent.LiveContentRefreshMetadata
import com.lucky3d.app.domain.livecontent.LiveContentRefreshResult
import com.lucky3d.app.domain.livecontent.LiveContentRefreshState
import com.lucky3d.app.domain.livecontent.LiveRefreshTrigger
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLiveContentRepositoryTest {
    private lateinit var root: File

    @Before
    fun setUp() {
        root = Files.createTempDirectory("live-content-repository").toFile()
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun `content and persisted failure flows are mapped from the Room store`() = runTest {
        val store = FakeLiveContentStore(latestOfficialIssue = "2026200")
        val repository = repository(store = store, scope = backgroundScope)
        val trial = trial("2026201", "007")
        val caibao = caibao("2026201", "cached.jpg", LocalDate.of(2026, 7, 31))
        store.trial.value = trial
        store.caibaoDocuments[caibao.issue] = caibao
        store.updateCaibaoFlow()
        store.setMetadata(
            metadata(LiveContentType.TRIAL_NUMBER, failure = LiveContentFailure.NETWORK),
        )

        assertThat(repository.trialNumber.first()).isEqualTo(trial)
        assertThat(repository.caibaoDocument.first()).isEqualTo(caibao)
        assertThat(repository.trialRefreshState.first())
            .isEqualTo(LiveContentRefreshState.Failed(LiveContentFailure.NETWORK))
    }

    @Test
    fun `caibao flow hides a cache older than the three Beijing date window`() = runTest {
        val expired = caibao("2026198", "expired.jpg", LocalDate.of(2026, 7, 28))
        val store = FakeLiveContentStore().apply {
            caibaoDocuments[expired.issue] = expired
            updateCaibaoFlow()
        }
        val repository = repository(store = store, scope = backgroundScope)

        assertThat(repository.caibaoDocument.first()).isNull()
    }

    @Test
    fun `trial success commits fixed content and metadata atomically while preserving leading zero`() = runTest {
        val store = FakeLiveContentStore(latestOfficialIssue = "2026200")
        val remote = FakeTrialDataSource(TrialRemoteResult.Success(TrialRemoteRecord("2026201", "007")))
        val repository = repository(store, trialRemote = remote, scope = backgroundScope)

        val result = repository.refreshTrial(LiveRefreshTrigger.AUTO_FOREGROUND)

        assertThat(result).isEqualTo(LiveContentRefreshResult.Success)
        assertThat(store.trialCommits).isEqualTo(1)
        assertThat(store.trial.value).isEqualTo(
            TrialNumber(
                issue = "2026201",
                number = "007",
                source = TrialSource.CJCP_SIMULATED,
                sourcePageUrl = "https://m.cjcp.cn/kjhsjh/3dls/",
                sourceLocalDate = LocalDate.of(2026, 7, 31),
                fetchedAtEpochMillis = NOW.toEpochMilli(),
            ),
        )
        val saved = store.metadata(LiveContentType.TRIAL_NUMBER)
        assertThat(saved?.autoAttemptCount).isEqualTo(1)
        assertThat(saved?.lastSuccessLocalDate).isEqualTo(LocalDate.of(2026, 7, 31))
        assertThat(saved?.lastFailure).isNull()
        assertThat(saved?.nextAllowedAutoAttemptEpochMillis).isNull()
    }

    @Test
    fun `trial issue not strictly after latest official draw is rejected without replacing cache`() = runTest {
        val cached = trial("2026199", "123")
        val store = FakeLiveContentStore(latestOfficialIssue = "2026201").apply { trial.value = cached }
        val remote = FakeTrialDataSource(TrialRemoteResult.Success(TrialRemoteRecord("2026201", "456")))
        val repository = repository(store, trialRemote = remote, scope = backgroundScope)

        val result = repository.refreshTrial(LiveRefreshTrigger.MANUAL)

        assertThat(result)
            .isEqualTo(LiveContentRefreshResult.Failed(LiveContentFailure.INVALID_ISSUE))
        assertThat(store.trial.value).isEqualTo(cached)
        assertThat(store.trialCommits).isEqualTo(0)
        assertThat(store.metadata(LiveContentType.TRIAL_NUMBER)?.nextAllowedAutoAttemptEpochMillis)
            .isEqualTo(NOW.plusSeconds(30 * 60).toEpochMilli())
    }

    @Test
    fun `trial remote failures have stable mappings and persist automatic cooldown and count`() = runTest {
        val expectations = listOf(
            LiveContentRemoteFailure.Network to LiveContentFailure.NETWORK,
            LiveContentRemoteFailure.Http to LiveContentFailure.HTTP,
            LiveContentRemoteFailure.TooLarge to LiveContentFailure.INVALID_HTML,
            LiveContentRemoteFailure.InvalidContentType to LiveContentFailure.INVALID_HTML,
            LiveContentRemoteFailure.InvalidSource to LiveContentFailure.INVALID_HTML,
            LiveContentRemoteFailure.InvalidPayload to LiveContentFailure.INVALID_HTML,
        )

        expectations.forEach { (remoteFailure, expected) ->
            val store = FakeLiveContentStore(latestOfficialIssue = "2026200")
            val repository = repository(
                store,
                trialRemote = FakeTrialDataSource(TrialRemoteResult.Failure(remoteFailure)),
                scope = backgroundScope,
            )

            assertThat(repository.refreshTrial(LiveRefreshTrigger.AUTO_FOREGROUND))
                .isEqualTo(LiveContentRefreshResult.Failed(expected))
            val saved = store.metadata(LiveContentType.TRIAL_NUMBER)
            assertThat(saved?.attemptLocalDate).isEqualTo(LocalDate.of(2026, 7, 31))
            assertThat(saved?.autoAttemptCount).isEqualTo(1)
            assertThat(saved?.lastFailure).isEqualTo(expected)
            assertThat(saved?.nextAllowedAutoAttemptEpochMillis)
                .isEqualTo(NOW.plusSeconds(30 * 60).toEpochMilli())
        }
    }

    @Test
    fun `manual trial failure records the attempt without consuming an automatic attempt`() = runTest {
        val store = FakeLiveContentStore(latestOfficialIssue = "2026200").apply {
            setMetadata(
                this@DefaultLiveContentRepositoryTest
                    .metadata(LiveContentType.TRIAL_NUMBER)
                    .copy(autoAttemptCount = 2),
            )
        }
        val repository = repository(
            store,
            trialRemote = FakeTrialDataSource(
                TrialRemoteResult.Failure(LiveContentRemoteFailure.Network),
            ),
            scope = backgroundScope,
        )

        repository.refreshTrial(LiveRefreshTrigger.MANUAL)

        val saved = store.metadata(LiveContentType.TRIAL_NUMBER)
        assertThat(saved?.lastAttemptEpochMillis).isEqualTo(NOW.toEpochMilli())
        assertThat(saved?.autoAttemptCount).isEqualTo(2)
    }

    @Test
    fun `caibao descriptor remote failures have stable mappings`() = runTest {
        val expectations = listOf(
            LiveContentRemoteFailure.Network to LiveContentFailure.NETWORK,
            LiveContentRemoteFailure.Http to LiveContentFailure.HTTP,
            LiveContentRemoteFailure.TooLarge to LiveContentFailure.INVALID_HTML,
            LiveContentRemoteFailure.InvalidContentType to LiveContentFailure.INVALID_HTML,
            LiveContentRemoteFailure.InvalidSource to LiveContentFailure.UNAPPROVED_IMAGE_HOST,
            LiveContentRemoteFailure.InvalidPayload to LiveContentFailure.INVALID_HTML,
        )

        expectations.forEach { (remoteFailure, expected) ->
            val store = FakeLiveContentStore()
            val repository = repository(
                store,
                caibaoRemote = FailingCaibaoDataSource(descriptorFailure = remoteFailure),
                scope = backgroundScope,
            )

            assertThat(repository.refreshCaibao(LiveRefreshTrigger.MANUAL))
                .isEqualTo(LiveContentRefreshResult.Failed(expected))
            assertThat(store.metadata(LiveContentType.CAIBAO)?.lastFailure).isEqualTo(expected)
        }
    }

    @Test
    fun `caibao image remote failures have stable mappings`() = runTest {
        val expectations = listOf(
            LiveContentRemoteFailure.Network to LiveContentFailure.NETWORK,
            LiveContentRemoteFailure.Http to LiveContentFailure.HTTP,
            LiveContentRemoteFailure.TooLarge to LiveContentFailure.IMAGE_TOO_LARGE,
            LiveContentRemoteFailure.InvalidContentType to LiveContentFailure.INVALID_IMAGE,
            LiveContentRemoteFailure.InvalidSource to LiveContentFailure.UNAPPROVED_IMAGE_HOST,
            LiveContentRemoteFailure.InvalidPayload to LiveContentFailure.INVALID_IMAGE,
        )

        expectations.forEach { (remoteFailure, expected) ->
            val store = FakeLiveContentStore()
            val repository = repository(
                store,
                caibaoRemote = FailingCaibaoDataSource(imageFailure = remoteFailure),
                scope = backgroundScope,
            )

            assertThat(repository.refreshCaibao(LiveRefreshTrigger.MANUAL))
                .isEqualTo(LiveContentRefreshResult.Failed(expected))
            assertThat(store.metadata(LiveContentType.CAIBAO)?.lastFailure).isEqualTo(expected)
        }
    }

    @Test
    fun `caibao same issue succeeds without image download while older issue is rejected`() = runTest {
        val cached = caibao("2026201", "cached.jpg", LocalDate.of(2026, 7, 31))
        val sameStore = FakeLiveContentStore().apply {
            caibaoDocuments[cached.issue] = cached
            updateCaibaoFlow()
        }
        val sameRemote = FakeCaibaoDataSource(descriptor("2026201"))
        val sameRepository = repository(
            sameStore,
            caibaoRemote = sameRemote,
            scope = backgroundScope,
        )

        assertThat(sameRepository.refreshCaibao(LiveRefreshTrigger.MANUAL))
            .isEqualTo(LiveContentRefreshResult.Success)
        assertThat(sameRemote.imageCalls).isEqualTo(0)
        assertThat(sameStore.metadata(LiveContentType.CAIBAO)?.lastSuccessLocalDate)
            .isEqualTo(LocalDate.of(2026, 7, 31))

        val oldStore = FakeLiveContentStore().apply {
            caibaoDocuments[cached.issue] = cached
            updateCaibaoFlow()
        }
        val oldRepository = repository(
            oldStore,
            caibaoRemote = FakeCaibaoDataSource(descriptor("2026200")),
            scope = backgroundScope,
        )
        assertThat(oldRepository.refreshCaibao(LiveRefreshTrigger.MANUAL))
            .isEqualTo(LiveContentRefreshResult.Failed(LiveContentFailure.INVALID_ISSUE))
        assertThat(oldStore.caibaoDocuments.values).containsExactly(cached)
    }

    @Test
    fun `new caibao commits validated file before Room and stores fixed descriptor fields`() = runTest {
        val events = mutableListOf<String>()
        val store = FakeLiveContentStore(commitEvents = events, fileRoot = root)
        val remote = FakeCaibaoDataSource(descriptor("2026201"), events = events)
        val repository = repository(store, caibaoRemote = remote, scope = backgroundScope)

        val result = repository.refreshCaibao(LiveRefreshTrigger.CAIBAO_VISIBLE)

        assertThat(result).isEqualTo(LiveContentRefreshResult.Success)
        assertThat(events).containsExactly("descriptor", "image", "room").inOrder()
        val saved = store.caibaoDocuments.getValue("2026201")
        assertThat(saved.edition).isEqualTo("A11")
        assertThat(saved.title).contains("彩吧彩报第三版")
        assertThat(saved.sourcePageUrl).isEqualTo("https://m.cz89.com/tuku/A11.htm")
        assertThat(saved.localFileName).matches("2026201-A11-[0-9a-f]{12}\\.jpg")
        assertThat(File(root, saved.localFileName).exists()).isTrue()
        assertThat(saved.cachedLocalDate).isEqualTo(LocalDate.of(2026, 7, 31))
        assertThat(store.metadata(LiveContentType.CAIBAO)?.autoAttemptCount).isEqualTo(1)
    }

    @Test
    fun `Room failure compensates the new final file and preserves old caibao cache`() = runTest {
        val cached = caibao("2026200", "old.jpg", LocalDate.of(2026, 7, 30))
        File(root, cached.localFileName).writeText("old")
        val store = FakeLiveContentStore(fileRoot = root).apply {
            caibaoDocuments[cached.issue] = cached
            updateCaibaoFlow()
            failCaibaoCommit = true
        }
        val repository = repository(
            store,
            caibaoRemote = FakeCaibaoDataSource(descriptor("2026201")),
            scope = backgroundScope,
        )

        val result = repository.refreshCaibao(LiveRefreshTrigger.MANUAL)

        assertThat(result).isEqualTo(LiveContentRefreshResult.Failed(LiveContentFailure.DATABASE))
        assertThat(store.caibaoDocuments.values).containsExactly(cached)
        assertThat(File(root, cached.localFileName).exists()).isTrue()
        assertThat(root.listFiles().orEmpty().map { it.name }).containsExactly(cached.localFileName)
    }

    @Test
    fun `caibao image and file failures preserve old cache and persist stable failures`() = runTest {
        val cached = caibao("2026200", "old.jpg", LocalDate.of(2026, 7, 30))
        val imageFailureStore = FakeLiveContentStore().apply {
            caibaoDocuments[cached.issue] = cached
            updateCaibaoFlow()
        }
        val imageFailureRepository = repository(
            imageFailureStore,
            caibaoRemote = FakeCaibaoDataSource(
                descriptor("2026201"),
                image = CaibaoImageResult.Failure(LiveContentRemoteFailure.TooLarge),
            ),
            scope = backgroundScope,
        )

        assertThat(imageFailureRepository.refreshCaibao(LiveRefreshTrigger.MANUAL))
            .isEqualTo(LiveContentRefreshResult.Failed(LiveContentFailure.IMAGE_TOO_LARGE))
        assertThat(imageFailureStore.caibaoDocuments.values).containsExactly(cached)

        val invalidFileStore = FakeLiveContentStore().apply {
            caibaoDocuments[cached.issue] = cached
            updateCaibaoFlow()
        }
        val invalidFileRepository = repository(
            invalidFileStore,
            caibaoRemote = FakeCaibaoDataSource(
                descriptor("2026201"),
                image = CaibaoImageResult.Success(byteArrayOf(1, 2, 3), "image/jpeg"),
            ),
            fileStore = fileStore(bounds = null),
            scope = backgroundScope,
        )
        assertThat(invalidFileRepository.refreshCaibao(LiveRefreshTrigger.MANUAL))
            .isEqualTo(LiveContentRefreshResult.Failed(LiveContentFailure.INVALID_IMAGE))
        assertThat(invalidFileStore.caibaoDocuments.values).containsExactly(cached)
    }

    @Test
    fun `same type automatic and manual callers share one request and cancelling a waiter does not cancel it`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val remote = BlockingTrialDataSource(gate)
            val repository = repository(
                FakeLiveContentStore(latestOfficialIssue = "2026200"),
                trialRemote = remote,
                scope = backgroundScope,
            )

            val first = async { repository.refreshTrial(LiveRefreshTrigger.AUTO_FOREGROUND) }
            remote.started.await()
            val second = async { repository.refreshTrial(LiveRefreshTrigger.MANUAL) }
            val cancelledWaiter = async { repository.refreshTrial(LiveRefreshTrigger.MANUAL) }
            runCurrent()
            cancelledWaiter.cancelAndJoin()
            gate.complete(Unit)

            assertThat(first.await()).isEqualTo(LiveContentRefreshResult.Success)
            assertThat(second.await()).isEqualTo(LiveContentRefreshResult.Success)
            assertThat(remote.calls).isEqualTo(1)
        }

    @Test
    fun `trial and caibao refreshes do not block one another`() = runTest {
        val trialGate = CompletableDeferred<Unit>()
        val trialRemote = BlockingTrialDataSource(trialGate)
        val caibaoRemote = FakeCaibaoDataSource(descriptor("2026201"))
        val repository = repository(
            FakeLiveContentStore(latestOfficialIssue = "2026200", fileRoot = root),
            trialRemote = trialRemote,
            caibaoRemote = caibaoRemote,
            scope = backgroundScope,
        )

        val trialRefresh = async { repository.refreshTrial(LiveRefreshTrigger.MANUAL) }
        trialRemote.started.await()
        val caibaoResult = repository.refreshCaibao(LiveRefreshTrigger.MANUAL)

        assertThat(caibaoResult).isEqualTo(LiveContentRefreshResult.Success)
        assertThat(caibaoRemote.descriptorCalls).isEqualTo(1)
        trialGate.complete(Unit)
        assertThat(trialRefresh.await()).isEqualTo(LiveContentRefreshResult.Success)
    }

    @Test
    fun `cleanup keeps three Beijing dates and removes day four temporary and orphan files`() = runTest {
        val store = FakeLiveContentStore()
        listOf(
            caibao("2026201", "2026201-A11-000000000001.jpg", LocalDate.of(2026, 7, 31)),
            caibao("2026200", "2026200-A11-000000000002.jpg", LocalDate.of(2026, 7, 30)),
            caibao("2026199", "2026199-A11-000000000003.jpg", LocalDate.of(2026, 7, 29)),
            caibao("2026198", "2026198-A11-000000000004.jpg", LocalDate.of(2026, 7, 28)),
        ).forEach {
            store.caibaoDocuments[it.issue] = it
            File(root, it.localFileName).writeText(it.issue)
        }
        store.updateCaibaoFlow()
        File(root, "2026197-A11-000000000005.jpg").writeText("orphan")
        File(root, "download.tmp").writeText("temp")
        val repository = repository(store, scope = backgroundScope)

        repository.cleanCaibaoCache()

        assertThat(store.caibaoDocuments.keys)
            .containsExactly("2026201", "2026200", "2026199")
        assertThat(root.listFiles().orEmpty().map { it.name })
            .containsExactly(
                "2026201-A11-000000000001.jpg",
                "2026200-A11-000000000002.jpg",
                "2026199-A11-000000000003.jpg",
            )
    }

    @Test
    fun `cleanup retains metadata and records FILE IO when an expired file cannot be deleted`() =
        runTest {
            val cached = caibao(
                "2026198",
                "2026198-A11-000000000004.jpg",
                LocalDate.of(2026, 7, 28),
            )
            val undeletable = File(root, cached.localFileName).apply {
                mkdir()
                File(this, "child").writeText("keep")
            }
            val store = FakeLiveContentStore().apply {
                caibaoDocuments[cached.issue] = cached
                updateCaibaoFlow()
            }
            val repository = repository(store, scope = backgroundScope)

            repository.cleanCaibaoCache()

            assertThat(undeletable.exists()).isTrue()
            assertThat(store.caibaoDocuments.values).containsExactly(cached)
            assertThat(store.metadata(LiveContentType.CAIBAO)?.lastFailure)
                .isEqualTo(LiveContentFailure.FILE_IO)
            assertThat(repository.caibaoRefreshState.first())
                .isEqualTo(LiveContentRefreshState.Failed(LiveContentFailure.FILE_IO))
        }

    private fun TestScope.repository(
        store: FakeLiveContentStore = FakeLiveContentStore(),
        trialRemote: TrialDataSource = FakeTrialDataSource(
            TrialRemoteResult.Success(TrialRemoteRecord("2026201", "007")),
        ),
        caibaoRemote: CaibaoDataSource = FakeCaibaoDataSource(descriptor("2026201")),
        fileStore: CaibaoFileStore = fileStore(),
        scope: CoroutineScope,
    ) = DefaultLiveContentRepository(
        store = store,
        trialDataSource = trialRemote,
        caibaoDataSource = caibaoRemote,
        fileStore = fileStore,
        clock = Clock.fixed(NOW, ZoneOffset.UTC),
        repositoryScope = scope,
    )

    private fun fileStore(bounds: ImageBounds? = ImageBounds(640, 480)) =
        CaibaoFileStore(
            rootDirectory = root,
            imageBoundsReader = ImageBoundsReader { bounds },
            atomicFileMover = AtomicFileMover { source, target ->
                Files.move(
                    source.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            },
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )

    private class FakeLiveContentStore(
        var latestOfficialIssue: String? = null,
        private val commitEvents: MutableList<String>? = null,
        private val fileRoot: File? = null,
    ) : LiveContentStore {
        val trial = MutableStateFlow<TrialNumber?>(null)
        val caibao = MutableStateFlow<CaibaoDocument?>(null)
        val caibaoDocuments = linkedMapOf<String, CaibaoDocument>()
        private val metadata = LiveContentType.entries.associateWith {
            MutableStateFlow<LiveContentRefreshMetadata?>(null)
        }
        var trialCommits = 0
        var failCaibaoCommit = false

        override fun observeTrial(): Flow<TrialNumber?> = trial

        override fun observeCaibao(): Flow<CaibaoDocument?> = caibao

        override fun observeMetadata(
            contentType: LiveContentType,
        ): Flow<LiveContentRefreshMetadata?> = metadata.getValue(contentType)

        override suspend fun metadata(contentType: LiveContentType): LiveContentRefreshMetadata? =
            metadata.getValue(contentType).value

        override suspend fun latestCaibao(): CaibaoDocument? = caibao.value

        override suspend fun allCaibao(): List<CaibaoDocument> =
            caibaoDocuments.values.toList()

        override suspend fun latestOfficialIssue(): String? = latestOfficialIssue

        override suspend fun commitTrial(
            trial: TrialNumber,
            metadata: LiveContentRefreshMetadata,
        ) {
            trialCommits += 1
            this.trial.value = trial
            setMetadata(metadata)
        }

        override suspend fun commitCaibao(
            document: CaibaoDocument,
            metadata: LiveContentRefreshMetadata,
        ) {
            if (failCaibaoCommit) throw IllegalStateException("Room failed")
            fileRoot?.let {
                check(File(it, document.localFileName).exists()) { "Room observed missing file" }
            }
            commitEvents?.add("room")
            caibaoDocuments[document.issue] = document
            updateCaibaoFlow()
            setMetadata(metadata)
        }

        override suspend fun recordMetadata(metadata: LiveContentRefreshMetadata) {
            setMetadata(metadata)
        }

        override suspend fun deleteCaibao(issue: String) {
            caibaoDocuments.remove(issue)
            updateCaibaoFlow()
        }

        fun setMetadata(value: LiveContentRefreshMetadata) {
            metadata.getValue(value.contentType).value = value
        }

        fun updateCaibaoFlow() {
            caibao.value = caibaoDocuments.values.maxWithOrNull(
                compareBy<CaibaoDocument> { it.cachedLocalDate }.thenBy { it.fetchedAtEpochMillis },
            )
        }
    }

    private class FakeTrialDataSource(
        private val result: TrialRemoteResult,
    ) : TrialDataSource {
        var calls = 0

        override suspend fun fetchLatest(): TrialRemoteResult {
            calls += 1
            return result
        }
    }

    private class BlockingTrialDataSource(
        private val gate: CompletableDeferred<Unit>,
    ) : TrialDataSource {
        val started = CompletableDeferred<Unit>()
        var calls = 0

        override suspend fun fetchLatest(): TrialRemoteResult {
            calls += 1
            started.complete(Unit)
            gate.await()
            return TrialRemoteResult.Success(TrialRemoteRecord("2026201", "007"))
        }
    }

    private class FakeCaibaoDataSource(
        private val descriptor: CaibaoRemoteDescriptor,
        private val image: CaibaoImageResult = CaibaoImageResult.Success(JPEG_BYTES, "image/jpeg"),
        private val events: MutableList<String>? = null,
    ) : CaibaoDataSource {
        var descriptorCalls = 0
        var imageCalls = 0

        override suspend fun fetchLatestDescriptor(): CaibaoDescriptorResult {
            descriptorCalls += 1
            events?.add("descriptor")
            return CaibaoDescriptorResult.Success(descriptor)
        }

        override suspend fun fetchImage(imageUrl: String): CaibaoImageResult {
            imageCalls += 1
            events?.add("image")
            return image
        }
    }

    private class FailingCaibaoDataSource(
        private val descriptorFailure: LiveContentRemoteFailure? = null,
        private val imageFailure: LiveContentRemoteFailure? = null,
    ) : CaibaoDataSource {
        override suspend fun fetchLatestDescriptor(): CaibaoDescriptorResult =
            descriptorFailure?.let(CaibaoDescriptorResult::Failure)
                ?: CaibaoDescriptorResult.Success(descriptor("2026201"))

        override suspend fun fetchImage(imageUrl: String): CaibaoImageResult =
            imageFailure?.let(CaibaoImageResult::Failure)
                ?: CaibaoImageResult.Success(JPEG_BYTES, "image/jpeg")
    }

    private fun trial(issue: String, number: String) = TrialNumber(
        issue = issue,
        number = number,
        source = TrialSource.CJCP_SIMULATED,
        sourcePageUrl = "https://m.cjcp.cn/kjhsjh/3dls/",
        sourceLocalDate = LocalDate.of(2026, 7, 30),
        fetchedAtEpochMillis = NOW.minusSeconds(86_400).toEpochMilli(),
    )

    private fun caibao(
        issue: String,
        fileName: String,
        date: LocalDate,
    ) = CaibaoDocument(
        issue = issue,
        edition = "A11",
        title = "彩吧彩报第三版",
        sourcePageUrl = "https://m.cz89.com/tuku/A11.htm",
        imageUrl = "https://tuku.cz89.com/ftp/app/$issue/A11.jpg",
        localFileName = fileName,
        sha256 = "0".repeat(64),
        mimeType = "image/jpeg",
        width = 640,
        height = 480,
        cachedLocalDate = date,
        fetchedAtEpochMillis = NOW.minusSeconds(86_400).toEpochMilli(),
    )

    private fun metadata(
        type: LiveContentType,
        failure: LiveContentFailure? = null,
    ) = LiveContentRefreshMetadata(
        contentType = type,
        attemptLocalDate = LocalDate.of(2026, 7, 31),
        autoAttemptCount = 1,
        lastAttemptEpochMillis = NOW.toEpochMilli(),
        lastSuccessLocalDate = null,
        lastSuccessEpochMillis = null,
        nextAllowedAutoAttemptEpochMillis = null,
        lastFailure = failure,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-07-31T09:00:00Z")
        val JPEG_BYTES: ByteArray =
            byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 2, 3)

        fun descriptor(issue: String) = CaibaoRemoteDescriptor(
            issue = issue,
            edition = "A11",
            title = "牛彩网 - 彩吧彩报第三版",
            sourcePageUrl = "https://m.cz89.com/tuku/A11.htm",
            imageUrl = "https://tuku.cz89.com/ftp/app/$issue/A11.jpg",
        )
    }
}
