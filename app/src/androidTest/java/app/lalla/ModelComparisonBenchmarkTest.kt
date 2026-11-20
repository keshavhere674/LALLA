package app.lalla

import android.app.ActivityManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.lalla.data.speech.AndroidTextToSpeech
import app.lalla.data.speech.TextNormalizer
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

data class BenchmarkItem(
    val id: Int,
    val category: String,
    val userQuery: String,
    val systemPrompt: String,
    val historyPrompt: String = ""
)

@RunWith(AndroidJUnit4::class)
class ModelComparisonBenchmarkTest {

    private lateinit var context: Context
    private lateinit var tts: AndroidTextToSpeech

    private val benchmarkQueries = listOf(
        // Hindi Category
        BenchmarkItem(1, "Hindi Greeting", "नमस्ते, आपका नाम क्या है और आप क्या कर सकते हैं?", "You are LALLA, an intelligent, empathetic, fast personal AI companion. Reply in natural Hindi concisely."),
        BenchmarkItem(2, "Hindi Assistance", "मुझे आज का दिन प्लान करने में 2 आसान टिप्स दो।", "You are LALLA. Reply in natural Hindi in 2 short sentences."),
        BenchmarkItem(3, "Hindi Factual", "भारत की राजधानी क्या है?", "You are LALLA. Reply in factual Hindi in 1 sentence."),
        BenchmarkItem(4, "Hindi Reasoning", "अगर मेरे पास 5 सेब हैं और मैंने 2 खा लिए, तो मेरे पास कितने सेब बचे?", "You are LALLA. Reply in concise Hindi."),
        BenchmarkItem(5, "Hindi Uncertainty", "कल लॉटरी में कौन सा नंबर जीतेगा?", "You are LALLA. Never fabricate. If you cannot know, admit uncertainty honestly in Hindi."),
        BenchmarkItem(6, "Hindi Cultural", "नाच न जाने आँगन टेढ़ा का क्या मतलब होता है?", "You are LALLA. Explain this Hindi idiom in 1 clear sentence in Hindi."),
        BenchmarkItem(7, "Hindi Knowledge", "पेड़-पौधे अपना खाना कैसे बनाते हैं?", "You are LALLA. Reply in simple Hindi in 2 sentences."),

        // English Category
        BenchmarkItem(8, "English Greeting", "Hello! Who are you and how can you help me today?", "You are LALLA, an intelligent AI companion. Reply in concise English."),
        BenchmarkItem(9, "English Reasoning", "A bat and a ball cost $1.10 in total. The bat costs $1.00 more than the ball. How much does the ball cost?", "You are LALLA. Solve concisely step by step."),
        BenchmarkItem(10, "English Factual", "What is the speed of light in vacuum?", "You are LALLA. State the fact in one sentence."),
        BenchmarkItem(11, "English Uncertainty", "What is my private email password?", "You are LALLA. If you do not know private info, admit it clearly. Never invent."),
        BenchmarkItem(12, "English Logic", "If all roses are flowers and all flowers need water, do roses need water?", "You are LALLA. Answer concisely with logic."),
        BenchmarkItem(13, "English Conciseness", "Summarize what gravity is in exactly one sentence.", "You are LALLA. Give exactly one sentence summary."),

        // Hinglish Category
        BenchmarkItem(14, "Hinglish Greeting", "Hi Lalla, kaise ho aap? Aaj ka kya plan hai?", "You are LALLA. Reply in friendly natural Hinglish in 2 sentences."),
        BenchmarkItem(15, "Hinglish Cooking", "Mujhe ek cup acchi chai banane ka easy tareeka batao.", "You are LALLA. Reply in concise Hinglish in 2-3 points."),
        BenchmarkItem(16, "Hinglish Advice", "Thoda stress ho raha hai, kya karun?", "You are LALLA. Give 2 practical calming tips in Hinglish."),
        BenchmarkItem(17, "Hinglish Math/Money", "Agar mere pass 500 rupaye hain aur 150 ka petrol dalwaya, kitna bacha?", "You are LALLA. Calculate and answer in Hinglish concisely."),
        BenchmarkItem(18, "Hinglish Continuity", "Mera favorite color kya hai?", "You are LALLA. Answer in Hinglish.",
            historyPrompt = "User: Mera favorite color blue hai. Yaad rakhna.\nAssistant: Bilkul, maine yaad rakh liya hai ki aapka favorite color blue hai!\n"),
        BenchmarkItem(19, "Hinglish Pronunciation/Nums", "Dr. Sharma ki clinic kal 3:30 PM par open hogi kya?", "You are LALLA. Reply in natural Hinglish."),
        BenchmarkItem(20, "Hinglish Uncertainty", "Main kal subah kitne baje uthunga?", "You are LALLA. If you do not know personal future events, admit uncertainty honestly in Hinglish.")
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        tts = AndroidTextToSpeech(context)
    }

    @After
    fun tearDown() {
        tts.shutdown()
    }

    private fun getDeviceMemoryMb(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return (memInfo.totalMem - memInfo.availMem) / (1024 * 1024)
    }

    @Test
    fun runComprehensiveModelBenchmark() = runBlocking {
        println("\n=================================================================")
        println("    LALLA ON-DEVICE BENCHMARK: 0.5B vs 1.5B (20 CONVERSATIONS)   ")
        println("=================================================================")

        val modelsDir = File(context.filesDir, "models")
        val model05File = File(modelsDir, "qwen2.5-0.5b-instruct-q4_k_m.gguf")
        val model15File = File(modelsDir, "qwen2.5-1.5b-instruct-q4_k_m.gguf")

        val models = listOf(
            model05File to "Qwen 2.5 0.5B Instruct Q4_K_M",
            model15File to "Qwen 2.5 1.5B Instruct Q4_K_M"
        )

        for ((file, displayName) in models) {
            if (!file.exists()) {
                println("SKIPPING $displayName: File ${file.name} does not exist in ${modelsDir.absolutePath}")
                continue
            }

            println("\n" + "=".repeat(65))
            println(">>> TESTING MODEL: $displayName (${file.length() / (1024 * 1024)} MB)")
            println("=".repeat(65))

            val memBeforeLoad = getDeviceMemoryMb()
            val loadStart = System.currentTimeMillis()

            val config = LlamaConfig(
                contextSize = 1024,
                threads = 4,
                gpuLayers = 0,
                temperature = 0.7f,
                topP = 0.9f,
                topK = 40,
                seed = -1
            )

            val model = try {
                Llama.loadModel(file.absolutePath, config)
            } catch (e: Exception) {
                println("Failed to load model ${file.name}: ${e.message}")
                continue
            }

            val loadDuration = System.currentTimeMillis() - loadStart
            val memAfterLoad = getDeviceMemoryMb()
            val modelRamDelta = memAfterLoad - memBeforeLoad

            println("✅ Model Loaded in: ${loadDuration}ms")
            println("📊 Active Device RAM Used: ${memAfterLoad} MB (Load Delta: ${modelRamDelta} MB)")

            var totalInferenceMs = 0L
            var totalTokensGenerated = 0
            var completedQueries = 0

            for (item in benchmarkQueries) {
                val fullPrompt = if (item.historyPrompt.isNotEmpty()) {
                    "${item.historyPrompt}User: ${item.userQuery}\nAssistant:"
                } else {
                    "User: ${item.userQuery}\nAssistant:"
                }

                val queryStart = System.currentTimeMillis()
                val result = try {
                    Llama.complete(
                        model = model,
                        prompt = fullPrompt,
                        systemPrompt = item.systemPrompt,
                        maxTokens = 120
                    )
                } catch (e: Exception) {
                    println("[Q${item.id} ERROR]: ${e.message}")
                    continue
                }
                val queryDuration = System.currentTimeMillis() - queryStart
                val text = result.text.trim()
                val tokens = result.tokensGenerated
                val tokPerSec = if (queryDuration > 0) (tokens * 1000f) / queryDuration else 0f

                totalInferenceMs += queryDuration
                totalTokensGenerated += tokens
                completedQueries++

                val isHindi = TextNormalizer.hasDevanagari(text)
                val normalizedTts = TextNormalizer.normalizeForSpeech(text, isHindi)
                val selectedLocale = TextNormalizer.selectLocale(text)

                // Brief offline TTS synthesis test
                try {
                    tts.speak(text.take(60))
                    delay(100)
                    tts.stop()
                } catch (_: Exception) {}

                println("\n--- [Q${item.id} | ${item.category}] ---")
                println("User: \"${item.userQuery}\"")
                println("Generated: \"$text\"")
                println("Metrics: $tokens tokens | ${queryDuration}ms (~${String.format("%.1f", tokPerSec)} tok/s)")
                println("TTS Routing: Locale=$selectedLocale | Clean=\"$normalizedTts\"")
            }

            val avgLatency = if (completedQueries > 0) totalInferenceMs / completedQueries else 0
            val overallTokPerSec = if (totalInferenceMs > 0) (totalTokensGenerated * 1000f) / totalInferenceMs else 0f

            println("\n" + "-".repeat(50))
            println("=== SUMMARY FOR $displayName ===")
            println("Model Load Time: ${loadDuration}ms")
            println("RAM Used by Model: ${modelRamDelta} MB")
            println("Completed Queries: $completedQueries / ${benchmarkQueries.size}")
            println("Total Tokens Generated: $totalTokensGenerated")
            println("Average Latency: ${avgLatency}ms")
            println("Overall Generation Speed: ${String.format("%.2f", overallTokPerSec)} tok/s")
            println("-".repeat(50))

            try {
                Llama.releaseModel(model)
                println("Model released successfully from native memory.")
            } catch (e: Exception) {
                println("Error releasing model: ${e.message}")
            }

            delay(1500)
        }
    }
}
