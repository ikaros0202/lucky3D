package com.lucky3d.app.domain.livecontent

import com.lucky3d.app.core.model.LiveContentType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class LiveRefreshTrigger {
    AUTO_FOREGROUND,
    HOME_VISIBLE,
    CAIBAO_VISIBLE,
    MANUAL,
}

data class RefreshContext(
    val now: Instant,
    val zoneId: ZoneId,
    val metadata: LiveContentRefreshMetadata?,
)

sealed interface RefreshDecision {
    data object Fetch : RefreshDecision
    data class Skip(val reason: SkipReason) : RefreshDecision
}

data class LiveContentRefreshMetadata(
    val contentType: LiveContentType,
    val attemptLocalDate: LocalDate?,
    val autoAttemptCount: Int,
    val lastAttemptEpochMillis: Long?,
    val lastSuccessLocalDate: LocalDate?,
    val lastSuccessEpochMillis: Long?,
    val nextAllowedAutoAttemptEpochMillis: Long?,
    val lastFailure: LiveContentFailure?,
)

enum class SkipReason {
    BEFORE_RELEASE_WINDOW,
    ALREADY_SUCCEEDED_TODAY,
    COOLDOWN,
    DAILY_AUTO_LIMIT,
    TRIGGER_NOT_APPLICABLE,
}

sealed interface LiveContentRefreshState {
    data object Idle : LiveContentRefreshState
    data object Refreshing : LiveContentRefreshState
    data class Failed(val failure: LiveContentFailure) : LiveContentRefreshState
}

sealed interface LiveContentRefreshResult {
    data object Success : LiveContentRefreshResult
    data class Skipped(val reason: SkipReason) : LiveContentRefreshResult
    data class Failed(val failure: LiveContentFailure) : LiveContentRefreshResult
}

enum class LiveContentFailure {
    NETWORK,
    HTTP,
    EMPTY_RESPONSE,
    INVALID_HTML,
    INVALID_ISSUE,
    INVALID_NUMBER,
    UNAPPROVED_IMAGE_HOST,
    IMAGE_TOO_LARGE,
    INVALID_IMAGE,
    FILE_IO,
    DATABASE,
}

object RefreshPolicy {
    fun decideTrial(
        trigger: LiveRefreshTrigger,
        context: RefreshContext,
    ): RefreshDecision {
        if (trigger == LiveRefreshTrigger.MANUAL) return RefreshDecision.Fetch
        if (trigger !in TRIAL_AUTO_TRIGGERS) {
            return RefreshDecision.Skip(SkipReason.TRIGGER_NOT_APPLICABLE)
        }
        val localDateTime = context.now.atZone(context.zoneId)
        if (localDateTime.toLocalTime() < TRIAL_RELEASE_TIME) {
            return RefreshDecision.Skip(SkipReason.BEFORE_RELEASE_WINDOW)
        }
        return decideAutomatic(context, localDateTime.toLocalDate())
    }

    fun decideCaibao(
        trigger: LiveRefreshTrigger,
        context: RefreshContext,
    ): RefreshDecision {
        if (trigger == LiveRefreshTrigger.MANUAL) return RefreshDecision.Fetch
        if (trigger != LiveRefreshTrigger.CAIBAO_VISIBLE) {
            return RefreshDecision.Skip(SkipReason.TRIGGER_NOT_APPLICABLE)
        }
        return decideAutomatic(context, context.now.atZone(context.zoneId).toLocalDate())
    }

    private fun decideAutomatic(
        context: RefreshContext,
        today: LocalDate,
    ): RefreshDecision {
        val metadata = context.metadata ?: return RefreshDecision.Fetch
        if (metadata.lastSuccessLocalDate == today) {
            return RefreshDecision.Skip(SkipReason.ALREADY_SUCCEEDED_TODAY)
        }
        if (
            metadata.nextAllowedAutoAttemptEpochMillis?.let { context.now.toEpochMilli() < it } == true
        ) {
            return RefreshDecision.Skip(SkipReason.COOLDOWN)
        }
        val attemptsToday = if (metadata.attemptLocalDate == today) metadata.autoAttemptCount else 0
        if (attemptsToday >= MAX_DAILY_AUTO_ATTEMPTS) {
            return RefreshDecision.Skip(SkipReason.DAILY_AUTO_LIMIT)
        }
        return RefreshDecision.Fetch
    }

    private val TRIAL_AUTO_TRIGGERS = setOf(
        LiveRefreshTrigger.AUTO_FOREGROUND,
        LiveRefreshTrigger.HOME_VISIBLE,
    )
    private val TRIAL_RELEASE_TIME: LocalTime = LocalTime.of(16, 35)
    private const val MAX_DAILY_AUTO_ATTEMPTS = 3
}
