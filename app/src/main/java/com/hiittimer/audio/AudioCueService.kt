package com.hiittimer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Plays interval cues using tones synthesised at runtime (no bundled assets). */
class AudioCueService : AudioCuePlayer {
    override var volume: Float = 0.8f

    private val cache = mutableMapOf<AudioCue, ShortArray>()
    private val io = CoroutineScope(Dispatchers.IO)

    override fun play(cue: AudioCue) {
        val samples = cache.getOrPut(cue) { ToneGenerator.samplesFor(cue) }
        if (samples.isEmpty()) return
        val vol = volume
        io.launch {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(ToneGenerator.SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.setVolume(vol)
                track.write(samples, 0, samples.size)
                track.play()
            } catch (_: Exception) { /* audio is best-effort */ }
        }
    }

    override fun activate() { /* nothing needed on Android */ }
    override fun deactivate() { /* nothing needed on Android */ }
}
