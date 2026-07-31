package com.lucky3d.app.feature.caibao

import androidx.compose.runtime.Immutable
import com.lucky3d.app.core.model.CaibaoDocument
import com.lucky3d.app.domain.livecontent.LiveContentRefreshState

@Immutable
data class CaibaoUiState(
    val document: CaibaoDocument? = null,
    val refreshState: LiveContentRefreshState = LiveContentRefreshState.Idle,
    val localImageAvailable: Boolean = false,
) {
    val hasCachedContent: Boolean
        get() = document != null
}
