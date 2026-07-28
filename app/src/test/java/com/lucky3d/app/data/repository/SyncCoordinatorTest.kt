package com.lucky3d.app.data.repository

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.data.local.DrawEntity
import com.lucky3d.app.data.local.SyncMetadataEntity
import com.lucky3d.app.data.mapper.toEntity
import com.lucky3d.app.data.remote.OfficialDataResult
import com.lucky3d.app.data.remote.OfficialDraw
import com.lucky3d.app.data.remote.OfficialDrawDataSource
import com.lucky3d.app.data.remote.OfficialDrawPage
import com.lucky3d.app.domain.attributes.DrawNumber
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncCoordinatorTest {
    @Test
    fun `new draw is committed once after full remote success`() = runTest {
        val existing = draw("2026197", "232").toEntity()
        val store = FakeStore(existing)
        val remote = FakeRemote(
            recent = success(listOf(draw("2026198", "685"), draw("2026197", "232"))),
        )
        val replays = FakeReplayRefresher()

        val result = coordinator(remote, store, replays).sync(SyncTrigger.MANUAL)

        assertThat(result).isEqualTo(SyncResult.Updated(1, 0, 1, "2026198"))
        assertThat(store.commits).hasSize(1)
        assertThat(store.draws.keys).containsExactly("2026197", "2026198")
        assertThat(replays.issues).containsExactly("2026198")
    }

    @Test
    fun `unchanged draw updates metadata without rewriting draw`() = runTest {
        val existing = draw("2026198", "685").toEntity()
        val store = FakeStore(existing)
        val remote = FakeRemote(recent = success(listOf(draw("2026198", "685"))))
        val replays = FakeReplayRefresher()

        val result = coordinator(remote, store, replays).sync(SyncTrigger.MANUAL)

        assertThat(result).isEqualTo(SyncResult.Updated(0, 0, 1, "2026198"))
        assertThat(store.commits.single()).isEmpty()
        assertThat(replays.issues).isEmpty()
    }

    @Test
    fun `official correction replaces draw and requests replay revision`() = runTest {
        val old = draw("2026198", "000", fingerprint = "OLD").toEntity()
        val store = FakeStore(old)
        val remote = FakeRemote(recent = success(listOf(draw("2026198", "685"))))
        val replays = FakeReplayRefresher()

        val result = coordinator(remote, store, replays).sync(SyncTrigger.MANUAL)

        assertThat(result).isEqualTo(SyncResult.Updated(0, 1, 0, "2026198"))
        assertThat(store.draws.getValue("2026198").officialFingerprint).isEqualTo("FP-2026198-685")
        assertThat(store.metadata?.correctedIssuesJson).isEqualTo("[\"2026198\"]")
        assertThat(replays.issues).containsExactly("2026198")
    }

    @Test
    fun `long gap downloads every range page before one commit`() = runTest {
        val local = draw("2026001", "001").toEntity()
        val all = (2026002..2026102).map { issue -> draw(issue.toString(), issue.toString().takeLast(3)) }
        val recent = all.takeLast(100)
        val remote = FakeRemote(
            recent = success(recent),
            ranges = ArrayDeque(
                listOf(
                    success(all.take(100), total = 101, page = 1),
                    success(all.takeLast(1), total = 101, page = 2),
                ),
            ),
        )
        val store = FakeStore(local)
        val replays = FakeReplayRefresher()

        val result = coordinator(remote, store, replays).sync(SyncTrigger.MANUAL)

        assertThat(result).isEqualTo(SyncResult.Updated(101, 0, 0, "2026102"))
        assertThat(remote.rangePages).containsExactly(1, 2).inOrder()
        assertThat(store.commits).hasSize(1)
        assertThat(store.commits.single()).hasSize(101)
        assertThat(store.draws).hasSize(102)
    }

    @Test
    fun `network failure preserves every local draw`() = runTest {
        val local = draw("2026198", "685").toEntity()
        val store = FakeStore(local)
        val remote = FakeRemote(recent = OfficialDataResult.NetworkFailure("offline"))

        val result = coordinator(remote, store).sync(SyncTrigger.MANUAL)

        assertThat(result).isEqualTo(SyncResult.Failed("NETWORK"))
        assertThat(store.draws.values).containsExactly(local)
        assertThat(store.commits).isEmpty()
        assertThat(store.metadata?.lastFailureType).isEqualTo("NETWORK")
    }

    @Test
    fun `incomplete second range page preserves all local draws`() = runTest {
        val local = draw("2026001", "001").toEntity()
        val all = (2026002..2026102).map { issue -> draw(issue.toString(), issue.toString().takeLast(3)) }
        val remote = FakeRemote(
            recent = success(all.takeLast(100)),
            ranges = ArrayDeque(
                listOf(
                    success(all.take(100), total = 101, page = 1),
                    OfficialDataResult.InvalidPayload("changed structure"),
                ),
            ),
        )
        val store = FakeStore(local)

        val result = coordinator(remote, store).sync(SyncTrigger.MANUAL)

        assertThat(result).isEqualTo(SyncResult.Failed("INVALID_PAYLOAD"))
        assertThat(store.draws.values).containsExactly(local)
        assertThat(store.commits).isEmpty()
    }

    @Test
    fun `auto sync is throttled for five minutes but manual sync bypasses limit`() = runTest {
        val now = 1_000_000L
        val store = FakeStore(
            draw("2026198", "685").toEntity(),
            metadata = SyncMetadataEntity(lastSuccessEpochMillis = now - 60_000L),
        )
        val remote = FakeRemote(recent = success(listOf(draw("2026198", "685"))))
        val coordinator = coordinator(remote, store, time = now)

        assertThat(coordinator.sync(SyncTrigger.AUTO)).isEqualTo(SyncResult.Throttled)
        assertThat(remote.recentCalls).isEqualTo(0)
        assertThat(coordinator.sync(SyncTrigger.MANUAL))
            .isEqualTo(SyncResult.Updated(0, 0, 1, "2026198"))
        assertThat(remote.recentCalls).isEqualTo(1)
    }

    @Test
    fun `second event does not start another concurrent sync`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val remote = BlockingRemote(gate)
        val store = FakeStore(draw("2026197", "232").toEntity())
        val coordinator = coordinator(remote, store)

        val first = async { coordinator.sync(SyncTrigger.MANUAL) }
        remote.started.await()
        val second = coordinator.sync(SyncTrigger.AUTO)
        gate.complete(Unit)

        assertThat(second).isEqualTo(SyncResult.AlreadyRunning)
        assertThat(first.await()).isEqualTo(SyncResult.Updated(1, 0, 1, "2026198"))
        assertThat(remote.calls).isEqualTo(1)
    }

    private fun coordinator(
        remote: OfficialDrawDataSource,
        store: FakeStore,
        replay: FakeReplayRefresher = FakeReplayRefresher(),
        time: Long = 1_000_000L,
    ) = SyncCoordinator(remote, store, replay, TimeProvider { time })

    private fun draw(
        issue: String,
        number: String,
        fingerprint: String = "FP-$issue-$number",
    ) = OfficialDraw(
        issue = issue,
        drawDate = LocalDate.of(2026, 1, 1),
        number = DrawNumber.parse(number),
        detailUrl = "https://www.cwl.gov.cn/c/$issue.shtml",
        fingerprint = fingerprint,
    )

    private fun success(
        draws: List<OfficialDraw>,
        total: Int? = null,
        page: Int = 1,
    ) = OfficialDataResult.Success(
        OfficialDrawPage(draws, total, page, 100),
    )

    private class FakeStore(
        vararg initial: DrawEntity,
        metadata: SyncMetadataEntity? = null,
    ) : SyncStore {
        val draws = initial.associateByTo(sortedMapOf()) { it.issue }
        var metadata: SyncMetadataEntity? = metadata
        val commits = mutableListOf<List<DrawEntity>>()

        override suspend fun latest(): DrawEntity? = draws.values.maxByOrNull { it.issue }

        override suspend fun byIssues(issues: Set<String>): List<DrawEntity> =
            issues.mapNotNull(draws::get)

        override suspend fun metadata(): SyncMetadataEntity? = metadata

        override suspend fun commit(draws: List<DrawEntity>, metadata: SyncMetadataEntity) {
            commits += draws.toList()
            draws.forEach { this.draws[it.issue] = it }
            this.metadata = metadata
        }

        override suspend fun recordFailure(metadata: SyncMetadataEntity) {
            this.metadata = metadata
        }
    }

    private class FakeRemote(
        private val recent: OfficialDataResult,
        private val ranges: ArrayDeque<OfficialDataResult> = ArrayDeque(),
    ) : OfficialDrawDataSource {
        var recentCalls = 0
        val rangePages = mutableListOf<Int>()

        override suspend fun fetchRecent(issueCount: Int): OfficialDataResult {
            recentCalls += 1
            return recent
        }

        override suspend fun fetchRange(
            issueStart: String,
            issueEnd: String,
            pageNumber: Int,
            pageSize: Int,
        ): OfficialDataResult {
            rangePages += pageNumber
            return ranges.removeFirst()
        }
    }

    private class BlockingRemote(
        private val gate: CompletableDeferred<Unit>,
    ) : OfficialDrawDataSource {
        val started = CompletableDeferred<Unit>()
        var calls = 0

        override suspend fun fetchRecent(issueCount: Int): OfficialDataResult {
            calls += 1
            started.complete(Unit)
            gate.await()
            return successStatic(
                listOf(
                    OfficialDraw(
                        issue = "2026198",
                        drawDate = LocalDate.of(2026, 7, 27),
                        number = DrawNumber.parse("685"),
                        detailUrl = "https://www.cwl.gov.cn/c/2026198.shtml",
                        fingerprint = "FP-2026198-685",
                    ),
                    OfficialDraw(
                        issue = "2026197",
                        drawDate = LocalDate.of(2026, 1, 1),
                        number = DrawNumber.parse("232"),
                        detailUrl = "https://www.cwl.gov.cn/c/2026197.shtml",
                        fingerprint = "FP-2026197-232",
                    ),
                ),
            )
        }

        override suspend fun fetchRange(
            issueStart: String,
            issueEnd: String,
            pageNumber: Int,
            pageSize: Int,
        ): OfficialDataResult = error("Range should not be requested")
    }

    private class FakeReplayRefresher : ReplayRefresher {
        val issues = sortedSetOf<String>()

        override suspend fun refresh(issues: Set<String>) {
            this.issues += issues
        }
    }

    companion object {
        private fun successStatic(draws: List<OfficialDraw>) =
            OfficialDataResult.Success(OfficialDrawPage(draws, null, 1, 100))
    }
}
