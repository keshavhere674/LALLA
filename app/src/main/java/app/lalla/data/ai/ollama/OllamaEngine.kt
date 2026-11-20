package app.lalla.data.ai.ollama

import android.util.Log
import app.lalla.data.ai.AiEngine
import app.lalla.domain.model.AiPrompt
import app.lalla.domain.model.AiResponse
import app.lalla.domain.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OllamaEngine(
    private val baseUrl: String = "http://localhost:11434",
    private val modelName: String = "qwen2.5:3b",
    private val temperature: Double = 0.7,
    private val client: OkHttpClient = defaultClient
) : AiEngine {

    companion object {
        private const val TAG = "LALLA_OLLAMA"

        val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectionPool(okhttp3.ConnectionPool(5, 10, TimeUnit.MINUTES))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** Test if Ollama is reachable and model is available */
    suspend fun testConnection(): Boolean {
        return try {
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/tags")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun streamChat(prompt: AiPrompt): Flow<String> = flow {
        var lastException: Exception? = null
        val startTime = System.currentTimeMillis()
        var firstTokenTime = -1L
        var tokenCount = 0

        repeat(2) { attempt ->
            try {
                val messages = mutableListOf<OllamaMessage>()

                if (prompt.systemInstruction.isNotBlank()) {
                    messages.add(OllamaMessage(role = "system", content = prompt.systemInstruction))
                }

                for (msg in prompt.history) {
                    val roleStr = when (msg.role) {
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.SYSTEM -> "system"
                    }
                    messages.add(OllamaMessage(role = roleStr, content = msg.content))
                }

                messages.add(OllamaMessage(role = "user", content = prompt.userMessage))

                val chatRequest = OllamaChatRequest(
                    model = modelName,
                    messages = messages,
                    stream = true,
                    keepAlive = "60m",
                    options = OllamaOptions(
                        temperature = temperature,
                        num_ctx = 2048,
                        num_predict = 180,
                        repeat_penalty = 1.1,
                        top_p = 0.9,
                        top_k = 40
                    )
                )

                val requestBody = json.encodeToString(chatRequest)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val normalizedBaseUrl = baseUrl.trimEnd('/')
                val request = Request.Builder()
                    .url("$normalizedBaseUrl/api/chat")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        throw RuntimeException("Ollama error (HTTP ${response.code}): $errorBody")
                    }

                    val source = response.body?.source()
                        ?: throw RuntimeException("Empty response body from Ollama")

                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) {
                            try {
                                val parsed = json.decodeFromString<OllamaChatResponse>(trimmed)
                                parsed.message?.content?.let { token ->
                                    if (token.isNotEmpty()) {
                                        tokenCount++
                                        if (firstTokenTime == -1L) {
                                            firstTokenTime = System.currentTimeMillis() - startTime
                                            Log.i(TAG, "⚡ Time to first token: ${firstTokenTime}ms")
                                        }
                                        emit(token)
                                    }
                                }
                                if (parsed.done) {
                                    val totalDuration = System.currentTimeMillis() - startTime
                                    Log.i(TAG, "🏁 Total duration: ${totalDuration}ms, tokens: $tokenCount, TTFT: ${firstTokenTime}ms")
                                    return@use
                                }
                            } catch (_: Exception) {
                                // Skip malformed chunk
                            }
                        }
                    }
                }
                return@flow
            } catch (e: IOException) {
                lastException = e
                Log.w(TAG, "Ollama connection attempt $attempt failed: ${e.message}")
                if (attempt < 1) delay(800L)
            } catch (e: RuntimeException) {
                throw e
            }
        }

        throw lastException ?: RuntimeException("Ollama connection failed after retries")
    }.flowOn(Dispatchers.IO)

    override suspend fun chat(prompt: AiPrompt): AiResponse {
        val builder = StringBuilder()
        streamChat(prompt).collect { token -> builder.append(token) }
        return AiResponse(text = builder.toString())
    }
}
