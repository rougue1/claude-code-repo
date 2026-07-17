package com.localchat.app.data.network

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OllamaClient {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // streaming responses can stay open indefinitely
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** GET /api/tags — list locally installed models. */
    suspend fun listModels(host: String): List<OllamaModelDto> = withIo {
        val request = Request.Builder().url("$host/api/tags").get().build()
        execute(request, host).use { response ->
            val body = response.readBodyOrThrow(host)
            json.decodeFromString(TagsResponse.serializer(), body).models
        }
    }

    /** POST /api/show — cheap, non-inference way to read a model's declared capabilities. */
    suspend fun getCapabilities(host: String, model: String): Set<String> = withIo {
        val payload = json.encodeToString(ShowRequest.serializer(), ShowRequest(model = model))
        val request = Request.Builder()
            .url("$host/api/show")
            .post(payload.toRequestBody(jsonMediaType))
            .build()
        execute(request, host).use { response ->
            val body = response.readBodyOrThrow(host)
            json.decodeFromString(ShowResponse.serializer(), body).capabilities.toSet()
        }
    }

    /** POST /api/chat with stream:true — emits one ChatStreamChunk per NDJSON line. */
    fun streamChat(host: String, request: ChatRequest): Flow<ChatStreamChunk> = flow {
        val payload = json.encodeToString(ChatRequest.serializer(), request)
        val httpRequest = Request.Builder()
            .url("$host/api/chat")
            .post(payload.toRequestBody(jsonMediaType))
            .build()

        val call = httpClient.newCall(httpRequest)
        try {
            val response = try {
                call.execute()
            } catch (e: IOException) {
                throw OllamaException.ConnectionFailed(host, e)
            }
            response.use { resp ->
                if (!resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    throw OllamaException.HttpError(resp.code, body)
                }
                val source = resp.body?.source() ?: throw OllamaException.StreamError("Empty response body")
                while (!source.exhausted()) {
                    currentCoroutineContext().ensureActive()
                    val line = source.readUtf8Line() ?: break
                    if (line.isBlank()) continue
                    val chunk = json.decodeFromString(ChatStreamChunk.serializer(), line)
                    if (chunk.error != null) throw OllamaException.StreamError(chunk.error)
                    emit(chunk)
                }
            }
        } finally {
            call.cancel()
        }
    }.flowOn(Dispatchers.IO)

    private fun execute(request: Request, host: String): okhttp3.Response = try {
        httpClient.newCall(request).execute()
    } catch (e: IOException) {
        throw OllamaException.ConnectionFailed(host, e)
    }

    private fun okhttp3.Response.readBodyOrThrow(host: String): String {
        val bodyString = body?.string().orEmpty()
        if (!isSuccessful) throw OllamaException.HttpError(code, bodyString)
        return bodyString
    }

    private suspend fun <T> withIo(block: suspend () -> T): T {
        return kotlinx.coroutines.withContext(Dispatchers.IO) { block() }
    }
}
