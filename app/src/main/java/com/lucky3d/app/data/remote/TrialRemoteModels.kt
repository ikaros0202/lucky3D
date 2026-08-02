package com.lucky3d.app.data.remote

data class TrialRemoteRecord(
    val issue: String,
    val number: String,
)

fun interface TrialHtmlParser {
    fun parse(html: String): RemoteParseResult<TrialRemoteRecord>

    fun parseAll(html: String): RemoteParseResult<List<TrialRemoteRecord>> =
        when (val result = parse(html)) {
            is RemoteParseResult.Success -> RemoteParseResult.Success(listOf(result.value))
            is RemoteParseResult.Failure -> result
        }
}

interface TrialDataSource {
    suspend fun fetchLatest(): TrialRemoteResult

    suspend fun fetchHistoryPage(page: Int): TrialRemoteHistoryResult =
        when (val result = fetchLatest()) {
            is TrialRemoteResult.Success -> TrialRemoteHistoryResult.Success(page, listOf(result.record))
            is TrialRemoteResult.Failure -> TrialRemoteHistoryResult.Failure(page, result.failure)
        }
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

sealed interface TrialRemoteHistoryResult {
    data class Success(val page: Int, val records: List<TrialRemoteRecord>) : TrialRemoteHistoryResult
    data class Failure(val page: Int, val failure: LiveContentRemoteFailure) : TrialRemoteHistoryResult
}
