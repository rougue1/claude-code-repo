package com.localchat.app.ui.chat

import com.localchat.app.data.db.ConversationEntity
import com.localchat.app.data.db.MessageEntity
import com.localchat.app.data.network.OllamaModelDto

data class ChatUiState(
    val isInitializing: Boolean = true,
    val connectionError: String? = null,
    val ollamaHost: String = "",
    val models: List<OllamaModelDto> = emptyList(),
    val selectedModel: String? = null,
    val isCheckingCapabilities: Boolean = false,
    val thinkingSupported: Boolean = false,
    val thinkingEnabled: Boolean = false,
    val conversations: List<ConversationEntity> = emptyList(),
    val currentConversationId: Long? = null,
    val messages: List<MessageEntity> = emptyList(),
    val streamingMessage: MessageEntity? = null,
    val isGenerating: Boolean = false,
    val sidebarQuery: String = "",
    val transientError: String? = null,
) {
    val inputEnabled: Boolean
        get() = !isInitializing && !isCheckingCapabilities && connectionError == null && selectedModel != null

    val filteredConversations: List<ConversationEntity>
        get() = if (sidebarQuery.isBlank()) conversations
        else conversations.filter { it.title.contains(sidebarQuery, ignoreCase = true) }

    // While a response is streaming, its live content lives here (in-memory) rather than
    // in the DB-backed `messages` list, so token-by-token updates don't touch Room at all.
    val displayMessages: List<MessageEntity>
        get() = if (streamingMessage == null) messages
        else messages.map { if (it.id == streamingMessage.id) streamingMessage else it }
}
