package app.lalla.ui.speech

interface TextToSpeech {
    suspend fun speak(text: String)
    fun stop()
    fun shutdown()
}
