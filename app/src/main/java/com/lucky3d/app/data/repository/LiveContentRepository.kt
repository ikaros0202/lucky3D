package com.lucky3d.app.data.repository

import com.lucky3d.app.core.model.CaibaoDocument
import com.lucky3d.app.core.model.TrialNumber
import com.lucky3d.app.domain.livecontent.LiveContentFailure
import com.lucky3d.app.domain.livecontent.LiveContentRefreshResult
import com.lucky3d.app.domain.livecontent.LiveContentRefreshState
import com.lucky3d.app.domain.livecontent.LiveRefreshTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface CaibaoImageReadResult {
    data class Loaded(val bytes: ByteArray) : CaibaoImageReadResult
    data class Unavailable(val failure: LiveContentFailure) : CaibaoImageReadResult
}

interface LiveContentRepository {
    val trialNumber: Flow<TrialNumber?>
    val trialNumbers: Flow<List<TrialNumber>>
        get() = trialNumber.map { it?.let(::listOf).orEmpty() }
    val trialRefreshState: Flow<LiveContentRefreshState>
    val caibaoDocument: Flow<CaibaoDocument?>
    val caibaoDocuments: Flow<List<CaibaoDocument>>
        get() = caibaoDocument.map { it?.let(::listOf).orEmpty() }
    val caibaoRefreshState: Flow<LiveContentRefreshState>

    suspend fun refreshTrial(trigger: LiveRefreshTrigger): LiveContentRefreshResult
    suspend fun refreshTrialHistory(
        trigger: LiveRefreshTrigger,
        requiredWindow: Int = 100,
        requiredIssues: Set<String> = emptySet(),
    ): LiveContentRefreshResult =
        refreshTrial(trigger)
    suspend fun refreshCaibao(trigger: LiveRefreshTrigger): LiveContentRefreshResult
    suspend fun refreshCaibaoIssue(issue: String): LiveContentRefreshResult =
        refreshCaibao(LiveRefreshTrigger.MANUAL)
    suspend fun readCaibaoImage(document: CaibaoDocument): CaibaoImageReadResult
    suspend fun invalidateCaibaoImage(document: CaibaoDocument)
    suspend fun cleanCaibaoCache()
}
