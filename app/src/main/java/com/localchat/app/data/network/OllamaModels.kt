package com.localchat.app.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TagsResponse(
    val models: List<OllamaModelDto> = emptyList(),
)

@Serializable
data class OllamaModelDto(
    val name: String,
    val model: String = name,
    val size: Long = 0L,
    @SerialName("modified_at") val modifiedAt: String? = null,
    val details: OllamaModelDetails? = null,
)

@Serializable
data class OllamaModelDetails(
    val family: String? = null,
    @SerialName("parameter_size") val parameterSize: String? = null,
    @SerialName("quantization_level") val quantizationLevel: String? = null,
)

@Serializable
data class ShowRequest(
    val model: String,
)

@Serializable
data class ShowResponse(
    val capabilities: List<String> = emptyList(),
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String = "",
    val thinking: String? = null,
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = true,
    val think: Boolean? = null,
)

@Serializable
data class ChatStreamChunk(
    val model: String? = null,
    val message: ChatMessageDto? = null,
    val done: Boolean = false,
    val error: String? = null,
)

sealed class OllamaException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ConnectionFailed(host: String, cause: Throwable? = null) :
        OllamaException("Can't reach Ollama at $host. Is `ollama serve` running in Termux?", cause)
    class HttpError(code: Int, body: String) :
        OllamaException("Ollama returned HTTP $code: $body")
    class StreamError(reason: String) : OllamaException(reason)
}
