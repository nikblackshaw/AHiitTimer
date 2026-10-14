package com.hiittimer.speech

import kotlinx.serialization.Serializable

/** A selectable TTS voice, flattened so it can be stored in preferences. */
@Serializable
data class SpeechVoiceInfo(
    val id: String,
    val name: String,
    val languageCode: String,
    val localeIdentifier: String,
) {
    val displayName: String get() {
        val locale = java.util.Locale.forLanguageTag(localeIdentifier.replace('_', '-'))
        val langLocale = java.util.Locale.forLanguageTag(languageCode.replace('_', '-'))
        val language = langLocale.getDisplayLanguage(java.util.Locale.getDefault()).ifEmpty { languageCode }
        val region = locale.getDisplayName(java.util.Locale.getDefault()).ifEmpty { localeIdentifier }
        return if (region.equals(language, ignoreCase = true)) "$name — $language" else "$name — $region"
    }

    companion object {
        fun matches(languageCode: String, voice: SpeechVoiceInfo): Boolean =
            voice.languageCode == languageCode || voice.languageCode.startsWith("$languageCode-") ||
                voice.languageCode.startsWith("${languageCode}_")
    }
}

interface SpeechPlayer {
    fun speak(text: String, voiceId: String?, languageCode: String?, rate: Double, volume: Double)
    fun stop()
}
