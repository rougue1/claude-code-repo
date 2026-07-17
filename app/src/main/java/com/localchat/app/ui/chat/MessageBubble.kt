@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.localchat.app.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localchat.app.data.db.MessageEntity
import com.localchat.app.ui.components.MarkdownText
import com.localchat.app.ui.theme.AppError
import com.localchat.app.ui.theme.AssistantBubble
import com.localchat.app.ui.theme.ThinkingBubble
import com.localchat.app.ui.theme.UserBubble

@Composable
fun MessageBubble(
    message: MessageEntity,
    isLastAssistantMessage: Boolean,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == "user"
    val clipboard = LocalClipboardManager.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            if (!isUser && !message.thinking.isNullOrBlank()) {
                ThinkingBlock(thinking = message.thinking, isStreaming = message.isStreaming)
            }

            if (isUser || message.content.isNotBlank() || message.isStreaming) {
                Surface(
                    color = if (isUser) UserBubble else AssistantBubble,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp,
                    ),
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                if (message.content.isNotBlank()) clipboard.setText(AnnotatedString(message.content))
                            },
                        ),
                ) {
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (isUser) {
                            Text(
                                text = message.content,
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        } else if (message.content.isBlank() && message.isStreaming) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp).size(16.dp), strokeWidth = 2.dp)
                        } else {
                            MarkdownText(text = message.content)
                        }
                    }
                }
            }

            if (message.isError) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = AppError, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Generation failed",
                        color = AppError,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            if (!isUser && isLastAssistantMessage && !message.isStreaming) {
                IconButton(onClick = onRegenerate, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Regenerate",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingBlock(thinking: String, isStreaming: Boolean) {
    var expanded by remember(isStreaming) { mutableStateOf(isStreaming) }

    Surface(
        color = ThinkingBubble,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.combinedClickable(onClick = { expanded = !expanded }, onLongClick = {}),
            ) {
                Icon(
                    Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = if (isStreaming) "Thinking…" else "Thought process",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp),
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = thinking,
                    fontFamily = FontFamily.Monospace,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
