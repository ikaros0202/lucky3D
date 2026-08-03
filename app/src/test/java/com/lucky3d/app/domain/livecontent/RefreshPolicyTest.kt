package com.lucky3d.app.domain.livecontent

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.core.model.LiveContentType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class RefreshPolicyTest {
    @Test
    fun `trial automatic refresh opens exactly at 18 30`() {
        assertTrialSkip(at("2026-07-31T18:29:59"), SkipReason.BEFORE_RELEASE_WINDOW)
        assertThat(trialDecision(at("2026-07-31T18:30:00"))).isEqualTo(RefreshDecision.Fetch)
    }

    @Test
    fun `trial automatic refresh stops after today's success`() {
        val now = at("2026-07-31T19:00:00")

        assertTrialSkip(
            now,
            SkipReason.ALREADY_SUCCEEDED_TODAY,
            metadata = metadata(
                LiveContentType.TRIAL_NUMBER,
                lastSuccessLocalDate = LocalDate.of(2026, 7, 31),
            ),
        )
    }

    @Test
    fun `trial automatic cooldown opens at exactly thirty minutes`() {
        val attemptedAt = at("2026-07-31T18:30:00")
        val metadata = metadata(
            LiveContentType.TRIAL_NUMBER,
            nextAllowed = attemptedAt.plusSeconds(30 * 60),
        )

        assertTrialSkip(attemptedAt.plusSeconds(29 * 60 + 59), SkipReason.COOLDOWN, metadata)
        assertThat(trialDecision(attemptedAt.plusSeconds(30 * 60), metadata))
            .isEqualTo(RefreshDecision.Fetch)
    }

    @Test
    fun `trial third automatic attempt may fetch and fourth is limited`() {
        val now = at("2026-07-31T19:00:00")

        assertThat(
            trialDecision(
                now,
                metadata(LiveContentType.TRIAL_NUMBER, attemptDate = LocalDate.of(2026, 7, 31), attempts = 2),
            ),
        ).isEqualTo(RefreshDecision.Fetch)
        assertTrialSkip(
            now,
            SkipReason.DAILY_AUTO_LIMIT,
            metadata(LiveContentType.TRIAL_NUMBER, attemptDate = LocalDate.of(2026, 7, 31), attempts = 3),
        )
    }

    @Test
    fun `trial automatic attempt count resets across Beijing date`() {
        val now = at("2026-08-01T18:30:00")

        assertThat(
            trialDecision(
                now,
                metadata(LiveContentType.TRIAL_NUMBER, attemptDate = LocalDate.of(2026, 7, 31), attempts = 3),
            ),
        ).isEqualTo(RefreshDecision.Fetch)
    }

    @Test
    fun `trial manual bypasses release success cooldown and automatic limit`() {
        val now = at("2026-07-31T10:00:00")
        val metadata = metadata(
            LiveContentType.TRIAL_NUMBER,
            attemptDate = LocalDate.of(2026, 7, 31),
            attempts = 3,
            lastSuccessLocalDate = LocalDate.of(2026, 7, 31),
            nextAllowed = now.plusSeconds(86_400),
        )

        assertThat(
            RefreshPolicy.decideTrial(
                LiveRefreshTrigger.MANUAL,
                RefreshContext(now, BEIJING, metadata),
            ),
        ).isEqualTo(RefreshDecision.Fetch)
    }

    @Test
    fun `trial rejects caibao visibility trigger`() {
        assertThat(
            RefreshPolicy.decideTrial(
                LiveRefreshTrigger.CAIBAO_VISIBLE,
                RefreshContext(at("2026-07-31T17:00:00"), BEIJING, null),
            ),
        ).isEqualTo(RefreshDecision.Skip(SkipReason.TRIGGER_NOT_APPLICABLE))
    }

    @Test
    fun `caibao first visibility check fetches`() {
        assertThat(caibaoDecision(at("2026-07-31T00:00:00"))).isEqualTo(RefreshDecision.Fetch)
    }

    @Test
    fun `caibao automatic refresh stops after today's successful check`() {
        val now = at("2026-07-31T12:00:00")

        assertCaibaoSkip(
            now,
            SkipReason.ALREADY_SUCCEEDED_TODAY,
            metadata = metadata(
                LiveContentType.CAIBAO,
                lastSuccessLocalDate = LocalDate.of(2026, 7, 31),
            ),
        )
    }

    @Test
    fun `caibao automatic cooldown opens at exactly two hours`() {
        val attemptedAt = at("2026-07-31T08:00:00")
        val metadata = metadata(
            LiveContentType.CAIBAO,
            nextAllowed = attemptedAt.plusSeconds(2 * 60 * 60),
        )

        assertCaibaoSkip(attemptedAt.plusSeconds(2 * 60 * 60 - 1), SkipReason.COOLDOWN, metadata)
        assertThat(caibaoDecision(attemptedAt.plusSeconds(2 * 60 * 60), metadata))
            .isEqualTo(RefreshDecision.Fetch)
    }

    @Test
    fun `caibao third automatic check may fetch and fourth is limited`() {
        val now = at("2026-07-31T12:00:00")

        assertThat(
            caibaoDecision(
                now,
                metadata(LiveContentType.CAIBAO, attemptDate = LocalDate.of(2026, 7, 31), attempts = 2),
            ),
        ).isEqualTo(RefreshDecision.Fetch)
        assertCaibaoSkip(
            now,
            SkipReason.DAILY_AUTO_LIMIT,
            metadata(LiveContentType.CAIBAO, attemptDate = LocalDate.of(2026, 7, 31), attempts = 3),
        )
    }

    @Test
    fun `caibao automatic attempt count resets across Beijing date`() {
        val now = at("2026-08-01T00:00:00")

        assertThat(
            caibaoDecision(
                now,
                metadata(LiveContentType.CAIBAO, attemptDate = LocalDate.of(2026, 7, 31), attempts = 3),
            ),
        ).isEqualTo(RefreshDecision.Fetch)
    }

    @Test
    fun `caibao manual bypasses success cooldown and automatic limit`() {
        val now = at("2026-07-31T12:00:00")
        val metadata = metadata(
            LiveContentType.CAIBAO,
            attemptDate = LocalDate.of(2026, 7, 31),
            attempts = 3,
            lastSuccessLocalDate = LocalDate.of(2026, 7, 31),
            nextAllowed = now.plusSeconds(86_400),
        )

        assertThat(
            RefreshPolicy.decideCaibao(
                LiveRefreshTrigger.MANUAL,
                RefreshContext(now, BEIJING, metadata),
            ),
        ).isEqualTo(RefreshDecision.Fetch)
    }

    @Test
    fun `caibao rejects foreground and home triggers`() {
        val context = RefreshContext(at("2026-07-31T12:00:00"), BEIJING, null)

        assertThat(RefreshPolicy.decideCaibao(LiveRefreshTrigger.AUTO_FOREGROUND, context))
            .isEqualTo(RefreshDecision.Skip(SkipReason.TRIGGER_NOT_APPLICABLE))
        assertThat(RefreshPolicy.decideCaibao(LiveRefreshTrigger.HOME_VISIBLE, context))
            .isEqualTo(RefreshDecision.Skip(SkipReason.TRIGGER_NOT_APPLICABLE))
    }

    private fun trialDecision(
        now: Instant,
        metadata: LiveContentRefreshMetadata? = null,
    ): RefreshDecision = RefreshPolicy.decideTrial(
        LiveRefreshTrigger.AUTO_FOREGROUND,
        RefreshContext(now, BEIJING, metadata),
    )

    private fun caibaoDecision(
        now: Instant,
        metadata: LiveContentRefreshMetadata? = null,
    ): RefreshDecision = RefreshPolicy.decideCaibao(
        LiveRefreshTrigger.CAIBAO_VISIBLE,
        RefreshContext(now, BEIJING, metadata),
    )

    private fun assertTrialSkip(
        now: Instant,
        reason: SkipReason,
        metadata: LiveContentRefreshMetadata? = null,
    ) {
        assertThat(trialDecision(now, metadata)).isEqualTo(RefreshDecision.Skip(reason))
    }

    private fun assertCaibaoSkip(
        now: Instant,
        reason: SkipReason,
        metadata: LiveContentRefreshMetadata? = null,
    ) {
        assertThat(caibaoDecision(now, metadata)).isEqualTo(RefreshDecision.Skip(reason))
    }

    private fun metadata(
        contentType: LiveContentType,
        attemptDate: LocalDate? = null,
        attempts: Int = 0,
        lastSuccessLocalDate: LocalDate? = null,
        nextAllowed: Instant? = null,
    ) = LiveContentRefreshMetadata(
        contentType = contentType,
        attemptLocalDate = attemptDate,
        autoAttemptCount = attempts,
        lastAttemptEpochMillis = null,
        lastSuccessLocalDate = lastSuccessLocalDate,
        lastSuccessEpochMillis = null,
        nextAllowedAutoAttemptEpochMillis = nextAllowed?.toEpochMilli(),
        lastFailure = null,
    )

    private fun at(localDateTime: String): Instant =
        ZonedDateTime.parse("$localDateTime+08:00[Asia/Shanghai]").toInstant()

    private companion object {
        val BEIJING: ZoneId = ZoneId.of("Asia/Shanghai")
    }
}
