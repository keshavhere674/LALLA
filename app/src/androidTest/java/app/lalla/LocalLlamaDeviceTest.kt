package app.lalla

import android.app.ActivityManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.lalla.data.ai.local.LocalLlamaEngine
import app.lalla.data.ai.local.ModelManager
import app.lalla.domain.model.AiPrompt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LocalLlamaDeviceTest {

    private lateinit var context: Context
    private lateinit var modelManager: ModelManager
    private lateinit var engine: LocalLlamaEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        modelManager = ModelManager(context)
        engine = LocalLlamaEngine(modelManager)
    }

    private fun getMemoryUsageMb(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    @Test
    fun testOnDeviceModelVerification() {
        val modelFile = modelManager.getModelFile()
        assertNotNull("Model file must be found in app-specific storage", modelFile)
        assertTrue("Model file must exist", modelFile!!.exists())
        assertTrue("Model must be valid GGUF", modelManager.validateGguf(modelFile))
        println("=== ON-DEVICE BENCHMARK: MODEL VERIFIED ===")
        println("Model Path: ${modelFile.absolutePath}")
        println("Model Size: ${modelFile.length() / (1024 * 1024)} MB")
    }

    @Test
    fun testOnDeviceInferenceEnglish() = runBlocking {
        println("=== ON-DEVICE BENCHMARK: ENGLISH INFERENCE ===")
        val memBefore = getMemoryUsageMb()
        val startTime = System.currentTimeMillis()

        val prompt = AiPrompt(
            systemInstruction = "You are LALLA, a helpful assistant. Keep responses under 2 sentences.",
            userMessage = "Tell me a fun fact about India in one sentence."
        )

        val response = engine.chat(prompt)
        val durationMs = System.currentTimeMillis() - startTime
        val memAfter = getMemoryUsageMb()

        println("Response Text: ${response.text}")
        println("Duration: ${durationMs}ms")
        println("Memory Delta: ${memAfter - memBefore} MB (Heap: ${memAfter} MB)")

        assertNotNull(response.text)
        assertTrue("Response text must not be blank", response.text.isNotBlank())
    }

    @Test
    fun testOnDeviceInferenceHindi() = runBlocking {
        println("=== ON-DEVICE BENCHMARK: HINDI INFERENCE ===")
        val startTime = System.currentTimeMillis()

        val prompt = AiPrompt(
            systemInstruction = "You are LALLA. Always reply in Hindi or Hinglish concisely.",
            userMessage = "नमस्ते! आप कौन हैं?"
        )

        val response = engine.chat(prompt)
        val durationMs = System.currentTimeMillis() - startTime

        println("Response Hindi: ${response.text}")
        println("Duration: ${durationMs}ms")

        assertNotNull(response.text)
        assertTrue("Response must not be blank", response.text.isNotBlank())
    }
}
