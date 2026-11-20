package app.lalla.data.ai.local

import android.util.Log
import app.lalla.data.ai.AiEngine
import app.lalla.domain.model.AiPrompt
import app.lalla.domain.model.AiResponse
import app.lalla.domain.model.MessageRole
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import dev.ffmpegkit.llama.LlamaResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalLlamaEngine @Inject constructor(
    private val modelManager: ModelManager
) : AiEngine {

    companion object {
        private const val TAG = "LocalLlamaEngine"
    }

    private val mutex = Mutex()
    private var activeModel: LlamaModel? = null
    private var loadedModelPath: String? = null
    var selectedModelName: String = ModelManager.DEFAULT_MODEL_NAME
        private set

    fun switchModel(modelName: String) {
        if (selectedModelName != modelName) {
            selectedModelName = modelName
            release()
        }
    }

    /**
     * Ensures the specified local model is loaded into native memory.
     */
    suspend fun ensureModelLoaded(modelName: String = selectedModelName): LlamaModel = withContext(Dispatchers.Default) {
        val modelFile = modelManager.getModelFile(modelName)
            ?: throw IllegalStateException("On-device model file not found in storage: '$modelName'.")

        if (activeModel != null && loadedModelPath == modelFile.absolutePath) {
            return@withContext activeModel!!
        }

        // Release old model if path changed
        activeModel?.let {
            try {
                Llama.releaseModel(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing previous model: ${e.message}")
            }
        }

        Log.i(TAG, "Loading on-device model from: ${modelFile.absolutePath} (${modelFile.length() / (1024 * 1024)} MB)")
        val loadStart = System.currentTimeMillis()

        // 4 threads on 8-core CPU provides sweet spot between thermal throttling and speed
        val config = LlamaConfig(
            contextSize = 1024,
            threads = 4,
            gpuLayers = 0,
            temperature = 0.7f,
            topP = 0.9f,
            topK = 40,
            seed = -1
        )

        val model = Llama.loadModel(modelFile.absolutePath, config)
        val loadDuration = System.currentTimeMillis() - loadStart
        Log.i(TAG, "✅ On-device model loaded successfully in ${loadDuration}ms")

        activeModel = model
        loadedModelPath = modelFile.absolutePath
        model
    }

    override fun streamChat(prompt: AiPrompt): Flow<String> = flow {
        val fullPrompt = buildChatPrompt(prompt)
        val startTime = System.currentTimeMillis()

        val result: LlamaResult = mutex.withLock {
            val model = ensureModelLoaded()
            Log.d(TAG, "Executing on-device inference for prompt length: ${fullPrompt.length}")
            Llama.complete(
                model = model,
                prompt = fullPrompt,
                systemPrompt = prompt.systemInstruction,
                maxTokens = 150
            )
        }

        val totalTime = System.currentTimeMillis() - startTime
        val text = result.text.trim()
        val tokensGenerated = result.tokensGenerated
        val tokensPerSec = if (totalTime > 0) (tokensGenerated * 1000f) / totalTime else 0f

        Log.i(TAG, "🏁 Inference complete in ${totalTime}ms: $tokensGenerated tokens (~${String.format("%.1f", tokensPerSec)} tok/s)")

        // Stream generated words chunk by chunk for smooth UI rendering
        val words = text.split(" ")
        for (i in words.indices) {
            if (!currentCoroutineContext().isActive) break
            val chunk = if (i == words.lastIndex) words[i] else words[i] + " "
            emit(chunk)
            delay(15) // Subtle 15ms pacing for fluid UI streaming animation
        }
    }.flowOn(Dispatchers.Default)

    override suspend fun chat(prompt: AiPrompt): AiResponse = withContext(Dispatchers.Default) {
        val fullPrompt = buildChatPrompt(prompt)
        val result: LlamaResult = mutex.withLock {
            val model = ensureModelLoaded()
            Llama.complete(
                model = model,
                prompt = fullPrompt,
                systemPrompt = prompt.systemInstruction,
                maxTokens = 150
            )
        }
        AiResponse(text = result.text.trim())
    }

    suspend fun chatWithModel(prompt: AiPrompt, modelName: String): AiResponse = withContext(Dispatchers.Default) {
        val fullPrompt = buildChatPrompt(prompt)
        val result: LlamaResult = mutex.withLock {
            val model = ensureModelLoaded(modelName)
            Llama.complete(
                model = model,
                prompt = fullPrompt,
                systemPrompt = prompt.systemInstruction,
                maxTokens = 150
            )
        }
        AiResponse(text = result.text.trim())
    }

    private fun buildChatPrompt(prompt: AiPrompt): String {
        val sb = StringBuilder()
        // Include recent history (last 4 turns)
        val recent = prompt.history.takeLast(4)
        for (msg in recent) {
            val role = if (msg.role == MessageRole.USER) "User" else "Assistant"
            sb.append("$role: ${msg.content}\n")
        }
        sb.append("User: ${prompt.userMessage}\nAssistant:")
        return sb.toString()
    }

    fun release() {
        activeModel?.let {
            try {
                Llama.releaseModel(it)
                activeModel = null
                loadedModelPath = null
                Log.i(TAG, "On-device model released from native memory")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to release model: ${e.message}")
            }
        }
    }
}
