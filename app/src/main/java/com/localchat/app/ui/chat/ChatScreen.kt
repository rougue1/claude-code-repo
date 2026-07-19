package com.localchat.app.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localchat.app.ui.components.ConnectionBanner
import com.localchat.app.ui.components.ModelPickerDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onMenuClick: () -> Unit,
    onSelectModel: (String) -> Unit,
    onToggleThinking: (Boolean) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onRegenerate: () -> Unit,
    onRetryConnection: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissTransientError: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.transientError) {
        val err = uiState.transientError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(err)
        onDismissTransientError()
    }

    val isNearBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= info.totalItemsCount - 2
        }
    }
    val displayMessages = uiState.displayMessages
    // Calling scrollToItem directly (not via scope.launch) means Compose cancels the
    // previous in-flight scroll when this effect restarts on the next token, instead of
    // stacking a new animated-scroll coroutine on top of it every single chunk.
    LaunchedEffect(displayMessages.size, displayMessages.lastOrNull()?.content, displayMessages.lastOrNull()?.thinking) {
        if (displayMessages.isNotEmpty() && isNearBottom) {
            listState.scrollToItem(displayMessages.size - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "Chats")
                    }
                },
                title = {
                    ModelPickerDropdown(
                        models = uiState.models,
                        selectedModel = uiState.selectedModel,
                        isChecking = uiState.isCheckingCapabilities,
                        onSelect = onSelectModel,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            ChatInputBar(
                text = draft,
                onTextChange = { draft = it },
                onSend = { if (draft.isNotBlank()) { onSend(draft); draft = "" } },
                onStop = onStop,
                enabled = uiState.inputEnabled,
                isGenerating = uiState.isGenerating,
                thinkingSupported = uiState.thinkingSupported,
                thinkingEnabled = uiState.thinkingEnabled,
                onToggleThinking = onToggleThinking,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            uiState.connectionError?.let { error ->
                ConnectionBanner(message = error, onRetry = onRetryConnection)
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    uiState.isInitializing -> LoadingIndicator("Connecting to Ollama…")
                    displayMessages.isEmpty() -> EmptyState(hasModel = uiState.selectedModel != null)
                    else -> {
                        val lastAssistantIndex = displayMessages.indexOfLast { it.role == "assistant" }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(displayMessages, key = { _, m -> m.id }) { index, message ->
                                MessageBubble(
                                    message = message,
                                    isLastAssistantMessage = index == lastAssistantIndex,
                                    onRegenerate = onRegenerate,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator(label: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun EmptyState(hasModel: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (hasModel) "Start a new conversation" else "No model selected",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
