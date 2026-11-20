package app.lalla.ui.speech

interface TextToSpeech {
    fun speak(text: String)
    fun stop()
    fun shutdown()
}
