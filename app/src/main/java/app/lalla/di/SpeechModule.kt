package app.lalla.di

import app.lalla.data.speech.AndroidSpeechRecognizer
import app.lalla.data.speech.AndroidTextToSpeech
import app.lalla.ui.speech.SpeechRecognizer
import app.lalla.ui.speech.TextToSpeech
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SpeechModule {

    @Binds
    @Singleton
    abstract fun bindSpeechRecognizer(impl: AndroidSpeechRecognizer): SpeechRecognizer

    @Binds
    @Singleton
    abstract fun bindTextToSpeech(impl: AndroidTextToSpeech): TextToSpeech
}
