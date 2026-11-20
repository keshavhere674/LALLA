package app.lalla.data.ai.openai

import app.lalla.data.ai.AiEngine
import app.lalla.domain.model.AiPrompt
import app.lalla.domain.model.AiResponse
import app.lalla.domain.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OpenAiCompatibleEngine(
    private val baseUrl: String = "http://localhost:3000/v1",
    private val modelName: String = "gemini-1.5-flash",
    private val apiKey: String = "",
    private val temperature: Double = 0.7,
    private val client: OkHttpClient = defaultClient
) : AiEngine {

    companion object {
        val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectionPool(okhttp3.ConnectionPool(5, 10, TimeUnit.MINUTES))
                .connectTimeout(15, TimeUnit.SECONDS)
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

    override fun streamChat(prompt: AiPrompt): Flow<String> = flow {
        val messages = mutableListOf<OpenAiMessage>()

        if (prompt.systemInstruction.isNotBlank()) {
            messages.add(OpenAiMessage(role = "system", content = prompt.systemInstruction))
        }

        for (msg in prompt.history) {
            val roleStr = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
            }
            messages.add(OpenAiMessage(role = roleStr, content = msg.content))
        }

        messages.add(OpenAiMessage(role = "user", content = prompt.userMessage))

        val chatRequest = OpenAiChatRequest(
            model = modelName,
            messages = messages,
            stream = true,
            temperature = temperature
        )

        val requestBody = json.encodeToString(chatRequest)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val endpointUrl = if (normalizedBaseUrl.endsWith("/chat/completions")) {
            normalizedBaseUrl
        } else if (normalizedBaseUrl.endsWith("/v1")) {
            "$normalizedBaseUrl/chat/completions"
        } else {
            "$normalizedBaseUrl/v1/chat/completions"
        }

        val requestBuilder = Request.Builder()
            .url(endpointUrl)
            .post(requestBody)

        if (apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw RuntimeException("AI Provider error (HTTP ${response.code}): $errorBody")
            }

            val source = response.body?.source()
                ?: throw RuntimeException("Empty response body from AI Provider")

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                val trimmed = line.trim()
                if (trimmed.startsWith("data:")) {
                    val dataContent = trimmed.removePrefix("data:").trim()
                    if (dataContent == "[DONE]") {
                        break
                    }
                    if (dataContent.isNotEmpty()) {
                        try {
                            val chunk = json.decodeFromString<OpenAiChatChunk>(dataContent)
                            val token = chunk.choices.firstOrNull()?.delta?.content
                            if (!token.isNullOrEmpty()) {
                                emit(token)
                            }
                        } catch (_: Exception) {
                            // Skip non-JSON SSE lines
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun chat(prompt: AiPrompt): AiResponse {
        val builder = StringBuilder()
        streamChat(prompt).collect { token ->
            builder.append(token)
        }
        return AiResponse(text = builder.toString())
    }
}
