package app.lalla.data.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isBackgroundListeningEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_BACKGROUND_LISTENING, false)
    )
    val isBackgroundListeningEnabled: StateFlow<Boolean> = _isBackgroundListeningEnabled.asStateFlow()

    private val _wakeWordSensitivity = MutableStateFlow(
        prefs.getFloat(KEY_WAKE_SENSITIVITY, DEFAULT_SENSITIVITY)
    )
    val wakeWordSensitivity: StateFlow<Float> = _wakeWordSensitivity.asStateFlow()

    fun setBackgroundListeningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND_LISTENING, enabled).apply()
        _isBackgroundListeningEnabled.value = enabled
    }

    fun isBackgroundListeningEnabledSync(): Boolean {
        return prefs.getBoolean(KEY_BACKGROUND_LISTENING, false)
    }

    fun setWakeWordSensitivity(sensitivity: Float) {
        val clamped = sensitivity.coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY)
        prefs.edit().putFloat(KEY_WAKE_SENSITIVITY, clamped).apply()
        _wakeWordSensitivity.value = clamped
    }

    fun getWakeWordSensitivitySync(): Float {
        return prefs.getFloat(KEY_WAKE_SENSITIVITY, DEFAULT_SENSITIVITY)
    }

    companion object {
        private const val PREFS_NAME = "lalla_app_settings"
        private const val KEY_BACKGROUND_LISTENING = "master_bg_listening"
        private const val KEY_WAKE_SENSITIVITY = "wake_word_sensitivity"

        const val DEFAULT_SENSITIVITY = 1200f
        const val MIN_SENSITIVITY = 400f
        const val MAX_SENSITIVITY = 3000f
    }
}
