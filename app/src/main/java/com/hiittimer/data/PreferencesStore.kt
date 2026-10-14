package com.hiittimer.data

import com.hiittimer.domain.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/** Which palette the app uses. */
enum class ThemeMode { LIGHT, DARK }

/** User preferences, persisted as JSON in SharedPreferences. Mirrors iOS Preferences. */
@kotlinx.serialization.Serializable
data class Preferences(
    val soundEnabled: Boolean = true,
    val countdownTicksEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val keepScreenAwake: Boolean = true,
    /** 0..1, applied to every cue. */
    val volume: Double = 0.8,
    val selectedWorkoutID: String = Workout.defaultPresetID,
    val theme: ThemeMode = ThemeMode.LIGHT,
    val announcementsEnabled: Boolean = true,
    /** 0..1, mapped onto the TTS rate range. */
    val speechRate: Double = 0.5,
    /** 0..1, applied to every announcement. */
    val speechVolume: Double = 0.9,
    val speechVoiceID: String? = null,
    val speechLanguageCode: String? = null,
)

/**
 * Loads and saves [Preferences]. Call [update] to change a value; it persists immediately.
 */
class PreferencesStore(
    private val prefs: android.content.SharedPreferences,
    private val key: String = "com.hiittimer.preferences",
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _preferences: MutableStateFlow<Preferences> = MutableStateFlow(load())
    val preferences: StateFlow<Preferences> = _preferences.asStateFlow()

    constructor(context: android.content.Context) : this(
        context.applicationContext.getSharedPreferences("hiittimer", android.content.Context.MODE_PRIVATE)
    )

    private fun load(): Preferences = try {
        val raw = prefs.getString(key, null) ?: return Preferences()
        json.decodeFromString(Preferences.serializer(), raw)
    } catch (_: Exception) { Preferences() }

    fun update(transform: (Preferences) -> Preferences) {
        val updated = transform(_preferences.value)
        _preferences.value = updated
        persist(updated)
    }

    fun reset() = update { Preferences() }

    private fun persist(p: Preferences) {
        try { prefs.edit().putString(key, json.encodeToString(Preferences.serializer(), p)).apply() }
        catch (_: Exception) { }
    }
}
