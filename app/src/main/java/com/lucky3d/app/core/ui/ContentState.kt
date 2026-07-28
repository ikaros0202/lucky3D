package com.lucky3d.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class MessageKind {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
}

@Immutable
data class InlineMessage(
    val kind: MessageKind,
    val title: String,
    val detail: String? = null,
    val actionLabel: String? = null,
)

@Composable
fun InlineStatusBanner(
    message: InlineMessage,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val semantic = LocalLucky3dSemanticColors.current
    val (container, content) = when (message.kind) {
        MessageKind.INFO -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
        MessageKind.SUCCESS -> semantic.successContainer to semantic.onSuccessContainer
        MessageKind.WARNING -> semantic.warningContainer to semantic.onWarningContainer
        MessageKind.ERROR -> MaterialTheme.colorScheme.errorContainer to
            MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, content.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = message.icon(),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(message.title, style = MaterialTheme.typography.labelLarge)
                message.detail?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (message.actionLabel != null && onAction != null) {
                OutlinedButton(onClick = onAction) {
                    Text(message.actionLabel)
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    detail: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun InlineLoading(
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun InlineMessage.icon(): ImageVector = when (kind) {
    MessageKind.INFO -> Icons.Outlined.Info
    MessageKind.SUCCESS -> Icons.Outlined.CheckCircle
    MessageKind.WARNING -> Icons.Outlined.WarningAmber
    MessageKind.ERROR -> Icons.Outlined.ErrorOutline
}

@Composable
fun semanticStatusColor(kind: MessageKind): Color {
    val semantic = LocalLucky3dSemanticColors.current
    return when (kind) {
        MessageKind.INFO -> MaterialTheme.colorScheme.primary
        MessageKind.SUCCESS -> semantic.success
        MessageKind.WARNING -> semantic.warning
        MessageKind.ERROR -> MaterialTheme.colorScheme.error
    }
}
