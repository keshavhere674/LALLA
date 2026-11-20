package app.lalla.ui.speech

import kotlinx.coroutines.flow.Flow

interface SpeechRecognizer {
    fun startListening(): Flow<SttResult>
    fun stopListening()
}

sealed class SttResult {
    data class Partial(val text: String) : SttResult()
    data class Final(val text: String) : SttResult()
    data class Error(val message: String) : SttResult()
}
