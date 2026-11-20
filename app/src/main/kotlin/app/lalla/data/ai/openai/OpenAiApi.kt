package app.lalla.data.ai.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val stream: Boolean = true,
    val temperature: Double = 0.7
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiChatChunk(
    val id: String? = null,
    val choices: List<OpenAiChoice> = emptyList()
)

@Serializable
data class OpenAiChoice(
    val delta: OpenAiDelta? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class OpenAiDelta(
    val role: String? = null,
    val content: String? = null
)
