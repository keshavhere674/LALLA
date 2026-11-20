package app.lalla.data.ai.ollama

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
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class OllamaEngine(
    private val baseUrl: String = "http://localhost:11434",
    private val modelName: String = "qwen2.5:3b",
    private val temperature: Double = 0.7
) : AiEngine {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /** Test if Ollama is reachable. Returns true if healthy. */
    suspend fun testConnection(): Boolean {
        return try {
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/tags")
                .get()
                .build()
            val response = client.newCall(request).execute()
            response.close()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    override fun streamChat(prompt: AiPrompt): Flow<String> = flow {
        var lastException: Exception? = null

        repeat(3) { attempt ->
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
                    options = OllamaOptions(
                        temperature = temperature,
                        num_predict = 512,
                        num_ctx = 4096,
                        repeat_penalty = 1.1,
                        top_p = 0.9
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

                    val source = response.body?.byteStream()
                        ?: throw RuntimeException("Empty response body from Ollama")

                    val reader = BufferedReader(InputStreamReader(source))
                    var line: String? = reader.readLine()
                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) {
                            try {
                                val parsed = json.decodeFromString<OllamaChatResponse>(trimmed)
                                parsed.message?.content?.let { token ->
                                    if (token.isNotEmpty()) emit(token)
                                }
                                if (parsed.done) return@use
                            } catch (_: Exception) {
                                // Skip malformed chunk
                            }
                        }
                        line = reader.readLine()
                    }
                }
                // Success - return
                return@flow
            } catch (e: IOException) {
                lastException = e
                if (attempt < 2) delay(1200L * (attempt + 1))
            } catch (e: RuntimeException) {
                throw e // Don't retry HTTP-level errors
            }
        }

        throw lastException ?: RuntimeException("Ollama connection failed after 3 attempts")
    }.flowOn(Dispatchers.IO)

    override suspend fun chat(prompt: AiPrompt): AiResponse {
        val builder = StringBuilder()
        streamChat(prompt).collect { token -> builder.append(token) }
        return AiResponse(text = builder.toString())
    }
}
