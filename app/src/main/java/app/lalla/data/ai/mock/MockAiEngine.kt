package app.lalla.data.ai.mock

import app.lalla.data.ai.AiEngine
import app.lalla.domain.model.AiPrompt
import app.lalla.domain.model.AiResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MockAiEngine : AiEngine {
    private val cannedResponses = listOf(
        "Namaste! Main LALLA hoon, aapki personal AI companion. Aap mujhse Hindi, English ya Hinglish mein kuch bhi baat kar sakte hain!",
        "I'm here for you! Main aapki baatein sun rahi hoon aur yaad bhi rakh rahi hoon. How can I help you today?",
        "Haan bilkul, main samajh gayi. Aapki har baat mere liye important hai. Aur bataiye?",
        "That's wonderful! Is there anything specific you would like me to remember or help you with?"
    )

    override fun streamChat(prompt: AiPrompt): Flow<String> = flow {
        val selected = cannedResponses.random()
        val words = selected.split(" ")
        for (word in words) {
            emit("$word ")
            delay(40)
        }
    }

    override suspend fun chat(prompt: AiPrompt): AiResponse {
        return AiResponse(text = cannedResponses.random())
    }
}
