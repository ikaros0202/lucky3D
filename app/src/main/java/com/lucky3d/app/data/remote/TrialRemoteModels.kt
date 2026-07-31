package com.lucky3d.app.data.remote

data class TrialRemoteRecord(
    val issue: String,
    val number: String,
)

fun interface TrialHtmlParser {
    fun parse(html: String): RemoteParseResult<TrialRemoteRecord>
}

interface TrialDataSource {
    suspend fun fetchLatest(): TrialRemoteResult
}

sealed interface LiveContentRemoteFailure {
    data object Network : LiveContentRemoteFailure
    data object Http : LiveContentRemoteFailure
    data object TooLarge : LiveContentRemoteFailure
    data object InvalidContentType : LiveContentRemoteFailure
    data object InvalidSource : LiveContentRemoteFailure
    data object InvalidPayload : LiveContentRemoteFailure
}

sealed interface RemoteParseResult<out T> {
    data class Success<T>(val value: T) : RemoteParseResult<T>
    data class Failure(val failure: LiveContentRemoteFailure) : RemoteParseResult<Nothing>
}

sealed interface TrialRemoteResult {
    data class Success(val record: TrialRemoteRecord) : TrialRemoteResult
    data class Failure(val failure: LiveContentRemoteFailure) : TrialRemoteResult
}
