package com.hiittimer.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale
import java.util.UUID

/**
 * Speaks workout cues with Android TextToSpeech.
 * Announcements interrupt rather than overlap: a new phase cue flushes the queue.
 */
class SpeechService(context: Context) : SpeechPlayer {
    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = mutableListOf<() -> Unit>()

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (ready) {
                    val run = pending.toList(); pending.clear(); run.forEach { it() }
                }
            }
        } catch (_: Exception) { tts = null }
    }

    fun availableVoices(): List<SpeechVoiceInfo> = try {
        tts?.voices?.map { info(it) }?.sortedBy { it.displayName } ?: emptyList()
    } catch (_: Exception) { emptyList() }

    fun voicesGroupedByLanguage(preferred: String?): List<Pair<String, List<SpeechVoiceInfo>>> {
        val all = voices(preferred)
        val order = mutableListOf<String>()
        val buckets = mutableMapOf<String, MutableList<SpeechVoiceInfo>>()
        for (v in all) {
            if (buckets[v.languageCode] == null) { order.add(v.languageCode); buckets[v.languageCode] = mutableListOf() }
            buckets[v.languageCode]?.add(v)
        }
        return order.mapNotNull { code -> buckets[code]?.let { displayName(code) to it.toList() } }
    }

    fun voices(preferred: String?): List<SpeechVoiceInfo> {
        val all = availableVoices()
        val code = defaultLanguageCode(preferred) ?: return all
        val matches = all.filter { SpeechVoiceInfo.matches(code, it) }
        return if (matches.isEmpty()) all else matches + all.filter { it !in matches }
    }

    fun voiceInfo(id: String?): SpeechVoiceInfo? {
        if (id == null) return null
        availableVoices().firstOrNull { it.id == id }?.let { return it }
        return try {
            tts?.voices?.firstOrNull { it.name == id }?.let { info(it) }
        } catch (_: Exception) { null }
    }

    fun preview(text: String, voiceId: String?, rate: Double, volume: Double) =
        speak(text, voiceId, null, rate, volume)

    override fun speak(text: String, voiceId: String?, languageCode: String?, rate: Double, volume: Double) {
        if (text.isEmpty()) return
        val run: () -> Unit = task@{
            try {
                val engine = tts ?: return@task
                val voice = voiceId?.let { id -> engine.voices?.firstOrNull { it.name == id } }
                if (voice != null) {
                    engine.voice = voice
                } else {
                    val tag = languageCode?.takeIf { it.isNotEmpty() }
                        ?: Locale.getDefault().toLanguageTag()
                    engine.language = Locale.forLanguageTag(tag)
                }
                engine.setSpeechRate(clampedRate(rate))
                val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0.0, 1.0).toFloat()) }
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString())
            } catch (_: Exception) { }
        }
        if (ready) run() else pending.add(run)
    }

    override fun stop() {
        try { tts?.stop() } catch (_: Exception) { }
    }

    fun shutdown() {
        try { tts?.shutdown() } catch (_: Exception) { }
        tts = null
    }

    companion object {
        private fun info(v: Voice): SpeechVoiceInfo {
            val locale = v.locale
            return SpeechVoiceInfo(
                id = v.name,
                name = v.name.substringAfter('#').substringAfterLast('.').ifEmpty { v.name },
                languageCode = locale.toLanguageTag(),
                localeIdentifier = locale.toLanguageTag(),
            )
        }

        /** Maps app 0..1 rate onto a TTS rate (0.5x..2x, 1x at 0.5). */
        fun clampedRate(rate: Double): Float =
            if (rate <= 0) 1f else (0.5f + rate.coerceIn(0.0, 1.0).toFloat() * 1.5f).coerceIn(0.25f, 3f)

        fun displayName(code: String): String = try {
            Locale.forLanguageTag(code.replace('_', '-')).getDisplayName(Locale.getDefault()).ifEmpty { code }
        } catch (_: Exception) { code }

        private fun defaultLanguageCode(preferred: String?): String? {
            if (!preferred.isNullOrEmpty()) return preferred.substringBefore('_').substringBefore('-')
            return try { Locale.getDefault().language } catch (_: Exception) { null }
        }
    }
}
