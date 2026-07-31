package com.lucky3d.app.feature.caibao

import com.lucky3d.app.core.model.CaibaoDocument
import com.lucky3d.app.domain.livecontent.LiveContentRefreshState

data class CaibaoUiState(
    val document: CaibaoDocument? = null,
    val imageBytes: ByteArray? = null,
    val refreshState: LiveContentRefreshState = LiveContentRefreshState.Idle,
) {
    val localImageAvailable: Boolean
        get() = imageBytes != null

    val hasCachedContent: Boolean
        get() = document != null && localImageAvailable
}
