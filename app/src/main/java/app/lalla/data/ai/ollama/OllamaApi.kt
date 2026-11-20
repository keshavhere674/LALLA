package app.lalla.data.ai.ollama

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = true,
    val options: OllamaOptions = OllamaOptions()
)

@Serializable
data class OllamaMessage(
    val role: String,
    val content: String
)

@Serializable
data class OllamaOptions(
    val temperature: Double = 0.7,
    @SerialName("num_ctx") val num_ctx: Int = 4096,
    @SerialName("num_predict") val num_predict: Int = 512,
    @SerialName("repeat_penalty") val repeat_penalty: Double = 1.1,
    @SerialName("top_p") val top_p: Double = 0.9
)

@Serializable
data class OllamaChatResponse(
    val model: String? = null,
    val message: OllamaMessage? = null,
    val done: Boolean = false
)
