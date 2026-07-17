package com.localchat.app.data

import android.content.Context
import com.localchat.app.data.db.AppDatabase
import com.localchat.app.data.db.ChatDao
import com.localchat.app.data.db.ConversationEntity
import com.localchat.app.data.db.MessageEntity
import com.localchat.app.data.network.ChatRequest
import com.localchat.app.data.network.ChatStreamChunk
import com.localchat.app.data.network.OllamaClient
import com.localchat.app.data.network.OllamaModelDto
import com.localchat.app.data.prefs.SettingsStore
import kotlinx.coroutines.flow.Flow

class ChatRepository(context: Context) {

    private val dao: ChatDao = AppDatabase.getInstance(context).chatDao()
    private val ollama = OllamaClient()
    val settings = SettingsStore(context)

    // --- Conversations & messages (local persistence) ---

    fun observeConversations(): Flow<List<ConversationEntity>> = dao.observeConversations()

    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> = dao.observeMessages(conversationId)

    suspend fun getMessages(conversationId: Long): List<MessageEntity> = dao.getMessages(conversationId)

    suspend fun createConversation(title: String, modelName: String, thinkingEnabled: Boolean): Long {
        val now = System.currentTimeMillis()
        return dao.insertConversation(
            ConversationEntity(
                title = title,
                modelName = modelName,
                thinkingEnabled = thinkingEnabled,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun renameConversation(id: Long, title: String) = dao.renameConversation(id, title)

    suspend fun deleteConversation(id: Long) = dao.deleteConversation(id)

    suspend fun setConversationModel(conversation: ConversationEntity, modelName: String, thinkingEnabled: Boolean) {
        dao.updateConversation(conversation.copy(modelName = modelName, thinkingEnabled = thinkingEnabled))
    }

    suspend fun touchConversation(id: Long) = dao.touchConversation(id, System.currentTimeMillis())

    suspend fun insertMessage(message: MessageEntity): Long = dao.insertMessage(message)

    suspend fun updateMessage(message: MessageEntity) = dao.updateMessage(message)

    suspend fun deleteMessage(message: MessageEntity) = dao.deleteMessage(message)

    suspend fun getLastMessage(conversationId: Long): MessageEntity? = dao.getLastMessage(conversationId)

    suspend fun stopStreaming(conversationId: Long) = dao.stopStreaming(conversationId)

    // --- Ollama network access ---

    suspend fun listModels(host: String): List<OllamaModelDto> = ollama.listModels(host)

    suspend fun getCapabilities(host: String, model: String): Set<String> = ollama.getCapabilities(host, model)

    fun streamChat(host: String, request: ChatRequest): Flow<ChatStreamChunk> = ollama.streamChat(host, request)
}
