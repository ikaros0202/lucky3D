package com.lucky3d.app.feature.caibao

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucky3d.app.R
import com.lucky3d.app.core.model.CaibaoDocument
import com.lucky3d.app.core.ui.Lucky3dDesign
import com.lucky3d.app.domain.livecontent.LiveContentRefreshState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CaibaoRoute(
    modifier: Modifier = Modifier,
    viewModel: CaibaoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val decodedImage by rememberDecodedCaibaoImage(state)
    val image = (decodedImage as? CaibaoDecodeState.Loaded)?.image

    LaunchedEffect(viewModel) {
        viewModel.onVisible()
    }
    LaunchedEffect(decodedImage, state.document) {
        if (decodedImage == CaibaoDecodeState.Failed) {
            state.document?.let(viewModel::onImageDecodeFailed)
        }
    }

    CaibaoScreen(
        state = state,
        image = image,
        onRefresh = viewModel::refresh,
        onSelectIssue = viewModel::selectIssue,
        onPrevious = viewModel::selectPrevious,
        onNext = viewModel::selectNext,
        modifier = modifier,
    )
}

@Composable
fun CaibaoScreen(
    state: CaibaoUiState,
    image: ImageBitmap?,
    onRefresh: () -> Unit,
    onSelectIssue: (String) -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CaibaoHeader(
            document = state.document,
            selectedIssue = state.selectedIssue,
            issueOptions = state.issueOptions,
            onSelectIssue = onSelectIssue,
            onPrevious = onPrevious,
            onNext = onNext,
            onRefresh = onRefresh,
        )

        if (state.hasCachedContent && image != null) {
            CaibaoReader(
                state = state,
                image = image,
                onRefresh = onRefresh,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        } else {
            CaibaoEmptyState(
                refreshState = state.refreshState,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = stringResource(R.string.responsible_play_notice),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 5.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun CaibaoHeader(
    document: CaibaoDocument?,
    selectedIssue: String?,
    issueOptions: List<String>,
    onSelectIssue: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRefresh: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val deep = Lucky3dDesign.colors.primaryDeep
    val displayedIssue = selectedIssue ?: document?.issue
    val visibleIssueOptions = remember(issueOptions, displayedIssue) {
        compactCaibaoIssueOptions(issueOptions, displayedIssue)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(deep, MaterialTheme.colorScheme.primary),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.caibao_title),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.caibao_refresh),
                    tint = Color.White,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (displayedIssue != null) {
                Box {
                    val issueButtonDescription = stringResource(
                        R.string.caibao_issue_button_a11y,
                        displayedIssue,
                    )
                    Button(
                        onClick = { expanded = true },
                        enabled = visibleIssueOptions.isNotEmpty(),
                        modifier = Modifier
                            .height(48.dp)
                            .widthIn(min = 116.dp)
                            .semantics { contentDescription = issueButtonDescription },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.16f),
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = ButtonDefaults.ContentPadding,
                    ) {
                        Text(
                            text = stringResource(R.string.caibao_issue, displayedIssue),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.heightIn(
                            max = CAIBAO_ISSUE_MENU_ITEM_HEIGHT * MAX_VISIBLE_ISSUE_OPTIONS,
                        ),
                    ) {
                        visibleIssueOptions.forEach { issue ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.caibao_issue, issue)) },
                                onClick = {
                                    expanded = false
                                    onSelectIssue(issue)
                                },
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(
                        R.string.caibao_edition,
                        document?.edition ?: DEFAULT_CAIBAO_EDITION,
                    ),
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onPrevious,
                    enabled = issueOptions.indexOf(displayedIssue) < issueOptions.lastIndex,
                ) {
                    Text(
                        text = stringResource(R.string.caibao_previous_issue),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                TextButton(
                    onClick = onNext,
                    enabled = issueOptions.indexOf(displayedIssue) > 0,
                ) {
                    Text(
                        text = stringResource(R.string.caibao_next_issue),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.caibao_no_cached_issue),
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

internal fun compactCaibaoIssueOptions(
    issueOptions: List<String>,
    @Suppress("UNUSED_PARAMETER") selectedIssue: String? = null,
): List<String> = issueOptions.distinct().sortedDescending()

@Composable
private fun CaibaoReader(
    state: CaibaoUiState,
    image: ImageBitmap,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        ZoomableCaibaoImage(
            image = image,
            contentDescription = stringResource(
                R.string.caibao_image_description,
                requireNotNull(state.document).issue,
            ),
            modifier = Modifier.fillMaxSize(),
        )

        when (state.refreshState) {
            LiveContentRefreshState.Refreshing -> CaibaoStatusBanner(
                text = stringResource(R.string.caibao_updating),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp),
            )

            is LiveContentRefreshState.Failed -> CaibaoStatusBanner(
                text = stringResource(R.string.caibao_cached_failure),
                onRefresh = onRefresh,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp),
            )

            LiveContentRefreshState.Idle -> Unit
        }
    }
}

@Composable
private fun ZoomableCaibaoImage(
    image: ImageBitmap,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val uiMode = LocalConfiguration.current.uiMode
    var scale by remember(image, uiMode) { mutableFloatStateOf(1f) }
    var offset by remember(image, uiMode) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val transformState = rememberTransformableState { _, zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
        val maxX = viewport.width * (nextScale - 1f) / 2f
        val maxY = viewport.height * (nextScale - 1f) / 2f
        scale = nextScale
        offset = if (nextScale == MIN_SCALE) {
            Offset.Zero
        } else {
            Offset(
                x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                y = (offset.y + panChange.y).coerceIn(-maxY, maxY),
            )
        }
    }

    Image(
        bitmap = image,
        contentDescription = contentDescription,
        modifier = modifier
            .onSizeChanged { viewport = it }
            .pointerInput(image) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = MIN_SCALE
                        offset = Offset.Zero
                    },
                )
            }
            .transformable(transformState)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun CaibaoStatusBanner(
    text: String,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
            )
            onRefresh?.let {
                TextButton(onClick = it) {
                    Text(stringResource(R.string.caibao_retry))
                }
            }
        }
    }
}

@Composable
private fun CaibaoEmptyState(
    refreshState: LiveContentRefreshState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (refreshState is LiveContentRefreshState.Failed) {
                Text(
                    text = stringResource(R.string.caibao_empty),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Button(onClick = onRefresh) {
                    Text(stringResource(R.string.caibao_retry))
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(30.dp))
                Text(
                    text = stringResource(R.string.caibao_loading),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun rememberDecodedCaibaoImage(state: CaibaoUiState) = produceState<CaibaoDecodeState>(
    initialValue = if (state.imageBytes == null) {
        CaibaoDecodeState.Empty
    } else {
        CaibaoDecodeState.Loading
    },
    key1 = state.imageBytes,
    key2 = state.document,
) {
    val bytes = state.imageBytes
    val document = state.document
    value = if (bytes == null || document == null) {
        CaibaoDecodeState.Empty
    } else {
        withContext(Dispatchers.Default) {
            val options = BitmapFactory.Options().apply {
                inSampleSize = imageSampleSize(document.width, document.height)
            }
            runCatching {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
            }.getOrNull()?.let(CaibaoDecodeState::Loaded) ?: CaibaoDecodeState.Failed
        }
    }
}

private fun imageSampleSize(width: Int, height: Int): Int {
    var sampleSize = 1
    while (width / sampleSize > MAX_DECODE_DIMENSION ||
        height / sampleSize > MAX_DECODE_DIMENSION
    ) {
        sampleSize *= 2
    }
    return sampleSize
}

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val MAX_DECODE_DIMENSION = 2_048
private const val MAX_VISIBLE_ISSUE_OPTIONS = 6
private val CAIBAO_ISSUE_MENU_ITEM_HEIGHT = 48.dp
private const val DEFAULT_CAIBAO_EDITION = "A11"

private sealed interface CaibaoDecodeState {
    data object Empty : CaibaoDecodeState
    data object Loading : CaibaoDecodeState
    data class Loaded(val image: ImageBitmap) : CaibaoDecodeState
    data object Failed : CaibaoDecodeState
}
