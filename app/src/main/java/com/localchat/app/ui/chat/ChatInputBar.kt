@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.localchat.app.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localchat.app.ui.theme.AppOnSurfaceMuted
import com.localchat.app.ui.theme.AppPrimary
import com.localchat.app.ui.theme.AppSurface

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    enabled: Boolean,
    isGenerating: Boolean,
    thinkingSupported: Boolean,
    thinkingEnabled: Boolean,
    onToggleThinking: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (thinkingSupported) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
            ) {
                Icon(
                    Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = if (thinkingEnabled) AppPrimary else AppOnSurfaceMuted,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text(
                    text = "Thinking",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = thinkingEnabled,
                    onCheckedChange = onToggleThinking,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(checkedTrackColor = AppPrimary),
                )
            }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                placeholder = { Text(if (enabled) "Message…" else "Preparing…") },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 160.dp),
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppSurface,
                    unfocusedContainerColor = AppSurface,
                    disabledContainerColor = AppSurface,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
            )

            val canSend = enabled && text.isNotBlank()
            IconButton(
                onClick = { if (isGenerating) onStop() else if (canSend) onSend() },
                enabled = enabled && (isGenerating || text.isNotBlank()),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isGenerating || canSend) AppPrimary else AppSurface,
                ),
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Icon(
                    imageVector = if (isGenerating) Icons.Filled.Stop else Icons.Filled.ArrowUpward,
                    contentDescription = if (isGenerating) "Stop generating" else "Send",
                    tint = if (isGenerating || canSend) MaterialTheme.colorScheme.onPrimary else AppOnSurfaceMuted,
                )
            }
        }
    }
}
