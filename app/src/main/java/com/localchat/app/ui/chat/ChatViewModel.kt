package com.localchat.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localchat.app.data.ChatRepository
import com.localchat.app.data.db.MessageEntity
import com.localchat.app.data.network.ChatMessageDto
import com.localchat.app.data.network.ChatRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val capabilityCache = mutableMapOf<String, Set<String>>()
    private val currentConversationIdFlow = MutableStateFlow<Long?>(null)
    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            repository.settings.ollamaHost.collect { host ->
                if (host != _uiState.value.ollamaHost) {
                    _uiState.update { it.copy(ollamaHost = host) }
                    refreshConnection()
                }
            }
        }
        viewModelScope.launch {
            repository.observeConversations().collect { convs ->
                _uiState.update { it.copy(conversations = convs) }
            }
        }
        viewModelScope.launch {
            currentConversationIdFlow.flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else repository.observeMessages(id)
            }.collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }

    fun retryConnection() = refreshConnection()

    fun updateOllamaHost(url: String) {
        viewModelScope.launch { repository.settings.setOllamaHost(url) }
    }

    private fun refreshConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitializing = true, connectionError = null) }
            val host = _uiState.value.ollamaHost
            val models = try {
                repository.listModels(host)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isInitializing = false, connectionError = e.message ?: "Failed to connect to Ollama")
                }
                return@launch
            }
            if (models.isEmpty()) {
                _uiState.update {
                    it.copy(
                        models = models,
                        isInitializing = false,
                        connectionError = "Connected, but no models are installed. Run `ollama pull <model>` in Termux.",
                    )
                }
                return@launch
            }
            val lastModel = repository.settings.lastModel.first()
            val chosen = models.map { it.name }.firstOrNull { it == lastModel } ?: models.first().name
            _uiState.update { it.copy(models = models, selectedModel = chosen) }
            checkCapabilities(chosen)
            repository.settings.setLastModel(chosen)
            _uiState.update { it.copy(isInitializing = false, thinkingEnabled = false) }
        }
    }

    fun refreshModels() = refreshConnection()

    /** Runs the loading stage: block input, ask Ollama what this model supports, unblock. */
    private suspend fun checkCapabilities(model: String): Boolean {
        _uiState.update { it.copy(isCheckingCapabilities = true) }
        val caps = capabilityCache.getOrPut(model) {
            runCatching { repository.getCapabilities(_uiState.value.ollamaHost, model) }.getOrDefault(emptySet())
        }
        val supportsThinking = "thinking" in caps
        _uiState.update { it.copy(isCheckingCapabilities = false, thinkingSupported = supportsThinking) }
        return supportsThinking
    }

    fun selectModel(name: String) {
        val state = _uiState.value
        if (state.isGenerating || name == state.selectedModel) return
        viewModelScope.launch {
            _uiState.update { it.copy(selectedModel = name) }
            checkCapabilities(name)
            repository.settings.setLastModel(name)
            _uiState.update { it.copy(thinkingEnabled = false) }
            val convId = _uiState.value.currentConversationId
            val conv = _uiState.value.conversations.firstOrNull { it.id == convId }
            if (conv != null) repository.setConversationModel(conv, name, thinkingEnabled = false)
        }
    }

    fun selectConversation(id: Long) {
        val state = _uiState.value
        if (state.isGenerating || id == state.currentConversationId) return
        val conv = state.conversations.firstOrNull { it.id == id } ?: return
        currentConversationIdFlow.value = id
        _uiState.update { it.copy(currentConversationId = id, selectedModel = conv.modelName) }
        viewModelScope.launch {
            val supportsThinking = checkCapabilities(conv.modelName)
            repository.settings.setLastModel(conv.modelName)
            _uiState.update { it.copy(thinkingEnabled = supportsThinking && conv.thinkingEnabled) }
        }
    }

    fun newChat() {
        if (_uiState.value.isGenerating) return
        currentConversationIdFlow.value = null
        _uiState.update { it.copy(currentConversationId = null, messages = emptyList()) }
    }

    fun deleteConversation(id: Long) {
        if (_uiState.value.isGenerating && id == _uiState.value.currentConversationId) return
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (id == _uiState.value.currentConversationId) newChat()
        }
    }

    fun renameConversation(id: Long, title: String) {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch { repository.renameConversation(id, trimmed) }
    }

    fun toggleThinking(enabled: Boolean) {
        if (!_uiState.value.thinkingSupported) return
        _uiState.update { it.copy(thinkingEnabled = enabled) }
        val convId = _uiState.value.currentConversationId ?: return
        viewModelScope.launch {
            val conv = _uiState.value.conversations.firstOrNull { it.id == convId } ?: return@launch
            repository.setConversationModel(conv, conv.modelName, enabled)
        }
    }

    fun updateSidebarQuery(query: String) {
        _uiState.update { it.copy(sidebarQuery = query) }
    }

    fun clearTransientError() {
        _uiState.update { it.copy(transientError = null) }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val state = _uiState.value
        if (!state.inputEnabled || state.isGenerating) return
        val model = state.selectedModel ?: return
        val thinkParam = if (state.thinkingSupported && state.thinkingEnabled) true else null

        generationJob = viewModelScope.launch {
            val convId = state.currentConversationId ?: run {
                val title = if (trimmed.length > 48) trimmed.take(48) + "…" else trimmed
                val id = repository.createConversation(title, model, state.thinkingEnabled)
                currentConversationIdFlow.value = id
                _uiState.update { it.copy(currentConversationId = id) }
                id
            }
            repository.insertMessage(
                MessageEntity(conversationId = convId, role = "user", content = trimmed, createdAt = System.currentTimeMillis())
            )
            generateResponse(convId, model, thinkParam)
        }
    }

    fun regenerateLast() {
        val state = _uiState.value
        val convId = state.currentConversationId ?: return
        val model = state.selectedModel ?: return
        if (state.isGenerating || !state.inputEnabled) return
        val thinkParam = if (state.thinkingSupported && state.thinkingEnabled) true else null

        generationJob = viewModelScope.launch {
            val last = repository.getLastMessage(convId)
            if (last != null && last.role == "assistant") repository.deleteMessage(last)
            generateResponse(convId, model, thinkParam)
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
    }

    private suspend fun generateResponse(convId: Long, model: String, thinkParam: Boolean?) {
        val history = repository.getMessages(convId).map { ChatMessageDto(role = it.role, content = it.content) }
        val createdAt = System.currentTimeMillis()
        val placeholderId = repository.insertMessage(
            MessageEntity(conversationId = convId, role = "assistant", content = "", isStreaming = true, createdAt = createdAt)
        )
        _uiState.update { it.copy(isGenerating = true) }
        val contentBuilder = StringBuilder()
        val thinkingBuilder = StringBuilder()

        fun liveMessage(streaming: Boolean, error: Boolean = false) = MessageEntity(
            id = placeholderId,
            conversationId = convId,
            role = "assistant",
            content = contentBuilder.toString(),
            thinking = thinkingBuilder.toString().ifBlank { null },
            isStreaming = streaming,
            isError = error,
            createdAt = createdAt,
        )

        try {
            repository.streamChat(
                _uiState.value.ollamaHost,
                ChatRequest(model = model, messages = history, stream = true, think = thinkParam),
            ).collect { chunk ->
                chunk.message?.content?.let { contentBuilder.append(it) }
                chunk.message?.thinking?.let { thinkingBuilder.append(it) }
                // Only touch in-memory state per token — writing to Room here would force its
                // observed Flow to re-query and re-render the whole conversation on every token.
                _uiState.update { it.copy(streamingMessage = liveMessage(streaming = !chunk.done)) }
            }
            repository.updateMessage(liveMessage(streaming = false))
            repository.touchConversation(convId)
        } catch (e: CancellationException) {
            withContext(NonCancellable) { repository.updateMessage(liveMessage(streaming = false)) }
            throw e
        } catch (e: Exception) {
            repository.updateMessage(liveMessage(streaming = false, error = true))
            _uiState.update { it.copy(transientError = e.message ?: "Generation failed") }
        } finally {
            _uiState.update { it.copy(isGenerating = false, streamingMessage = null) }
        }
    }
}

class ChatViewModelFactory(private val repository: ChatRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(repository) as T
}
