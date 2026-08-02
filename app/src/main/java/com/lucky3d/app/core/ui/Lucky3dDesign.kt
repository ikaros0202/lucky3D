package com.lucky3d.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Lucky3dDesign {
    val colors: Lucky3dVisualColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLucky3dVisualColors.current
}

@Composable
fun FlowingCinnabarHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val visualColors = Lucky3dDesign.colors
    CompositionLocalProvider(
        LocalContentColor provides Color.White,
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            visualColors.primaryDeep,
                            MaterialTheme.colorScheme.primary,
                        ),
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        color = LocalContentColor.current.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            actions()
        }
    }
}

@Composable
fun CrystalNumberBall(
    digit: Char,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val visualColors = Lucky3dDesign.colors
    val primary = MaterialTheme.colorScheme.primary
    NumberBallContainer(
        modifier = modifier
            .sizeIn(minWidth = 56.dp, minHeight = 56.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .drawWithCache {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val base = Brush.radialGradient(
                    colors = listOf(
                        visualColors.crystalHighlight,
                        primary.copy(alpha = 0.96f),
                        visualColors.primaryDeep,
                    ),
                    center = Offset(size.width * 0.38f, size.height * 0.30f),
                    radius = radius * 1.35f,
                )
                val upperLeftFacet = Path().apply {
                    moveTo(size.width * 0.06f, size.height * 0.38f)
                    lineTo(size.width * 0.32f, size.height * 0.08f)
                    lineTo(size.width * 0.49f, size.height * 0.34f)
                    close()
                }
                val upperRightFacet = Path().apply {
                    moveTo(size.width * 0.32f, size.height * 0.08f)
                    lineTo(size.width * 0.73f, size.height * 0.07f)
                    lineTo(size.width * 0.57f, size.height * 0.40f)
                    lineTo(size.width * 0.49f, size.height * 0.34f)
                    close()
                }
                val rightFacet = Path().apply {
                    moveTo(size.width * 0.73f, size.height * 0.07f)
                    lineTo(size.width * 0.96f, size.height * 0.39f)
                    lineTo(size.width * 0.68f, size.height * 0.56f)
                    lineTo(size.width * 0.57f, size.height * 0.40f)
                    close()
                }
                val lowerRightFacet = Path().apply {
                    moveTo(size.width * 0.68f, size.height * 0.56f)
                    lineTo(size.width * 0.93f, size.height * 0.72f)
                    lineTo(size.width * 0.62f, size.height * 0.96f)
                    lineTo(size.width * 0.48f, size.height * 0.65f)
                    close()
                }
                val lowerLeftFacet = Path().apply {
                    moveTo(size.width * 0.07f, size.height * 0.63f)
                    lineTo(size.width * 0.48f, size.height * 0.65f)
                    lineTo(size.width * 0.62f, size.height * 0.96f)
                    lineTo(size.width * 0.23f, size.height * 0.87f)
                    close()
                }
                val centerFacet = Path().apply {
                    moveTo(size.width * 0.49f, size.height * 0.34f)
                    lineTo(size.width * 0.68f, size.height * 0.56f)
                    lineTo(size.width * 0.48f, size.height * 0.65f)
                    lineTo(size.width * 0.24f, size.height * 0.50f)
                    close()
                }
                onDrawBehind {
                    drawCircle(brush = base, radius = radius, center = center)
                    drawPath(
                        path = upperLeftFacet,
                        color = visualColors.crystalHighlight.copy(alpha = 0.34f),
                    )
                    drawPath(
                        path = upperRightFacet,
                        color = visualColors.crystalRose.copy(alpha = 0.20f),
                    )
                    drawPath(
                        path = rightFacet,
                        color = visualColors.crystalHighlight.copy(alpha = 0.16f),
                    )
                    drawPath(
                        path = lowerRightFacet,
                        color = visualColors.crystalLavender.copy(alpha = 0.20f),
                    )
                    drawPath(
                        path = lowerLeftFacet,
                        color = visualColors.crystalHighlight.copy(alpha = 0.14f),
                    )
                    drawPath(
                        path = centerFacet,
                        color = visualColors.crystalRose.copy(alpha = 0.18f),
                    )
                    drawCircle(
                        color = visualColors.crystalHighlight.copy(alpha = 0.78f),
                        radius = radius * 0.12f,
                        center = Offset(size.width * 0.29f, size.height * 0.22f),
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.74f),
                        radius = radius - 1.5.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                    drawCircle(
                        color = visualColors.crystalBorder.copy(alpha = 0.82f),
                        radius = radius * 0.89f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
            },
    ) {
        Text(
            text = digit.toString(),
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 36.sp,
        )
    }
}

@Composable
fun MatteNumberBall(
    text: String,
    selected: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    NumberBallContainer(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = if (selected) {
                    backgroundColor
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = CircleShape,
            )
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                this.selected = selected
            },
    ) {
        Text(
            text = text,
            color = contentColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 36.sp,
        )
    }
}

@Composable
fun CompactSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
        )
        action?.invoke()
    }
}

@Composable
private fun NumberBallContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
        content = content,
    )
}
