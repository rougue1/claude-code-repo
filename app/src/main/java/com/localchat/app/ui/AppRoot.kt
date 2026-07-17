@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.localchat.app.ui

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localchat.app.ui.chat.ChatScreen
import com.localchat.app.ui.chat.ChatViewModel
import com.localchat.app.ui.components.SettingsDialog
import com.localchat.app.ui.sidebar.SidebarDrawerContent
import kotlinx.coroutines.launch

@Composable
fun AppRoot(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSettings by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarDrawerContent(
                conversations = uiState.filteredConversations,
                currentConversationId = uiState.currentConversationId,
                searchQuery = uiState.sidebarQuery,
                onSearchQueryChange = viewModel::updateSidebarQuery,
                onNewChat = {
                    viewModel.newChat()
                    scope.launch { drawerState.close() }
                },
                onSelectConversation = { id ->
                    viewModel.selectConversation(id)
                    scope.launch { drawerState.close() }
                },
                onRenameConversation = viewModel::renameConversation,
                onDeleteConversation = viewModel::deleteConversation,
            )
        },
    ) {
        ChatScreen(
            uiState = uiState,
            onMenuClick = { scope.launch { drawerState.open() } },
            onSelectModel = viewModel::selectModel,
            onToggleThinking = viewModel::toggleThinking,
            onSend = viewModel::sendMessage,
            onStop = viewModel::stopGeneration,
            onRegenerate = viewModel::regenerateLast,
            onRetryConnection = viewModel::retryConnection,
            onOpenSettings = { showSettings = true },
            onDismissTransientError = viewModel::clearTransientError,
        )
    }

    if (showSettings) {
        SettingsDialog(
            currentHost = uiState.ollamaHost,
            onDismiss = { showSettings = false },
            onSave = viewModel::updateOllamaHost,
        )
    }
}
