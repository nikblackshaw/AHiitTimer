package com.hiittimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hiittimer.audio.AudioCueService
import com.hiittimer.data.PreferencesStore
import com.hiittimer.data.WorkoutLibrary
import com.hiittimer.haptics.HapticsService
import com.hiittimer.speech.SpeechService
import com.hiittimer.ui.HiitTimerApp

class MainActivity : ComponentActivity() {

    private lateinit var library: WorkoutLibrary
    private lateinit var preferences: PreferencesStore
    private lateinit var audio: AudioCueService
    private lateinit var haptics: HapticsService
    private lateinit var speech: SpeechService

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = PreferencesStore(this)
        library = WorkoutLibrary(filesDir)
        audio = AudioCueService()
        haptics = HapticsService(this)
        speech = SpeechService(this)

        setContent {
            HiitTimerApp(
                library = library,
                preferences = preferences,
                audio = audio,
                haptics = haptics,
                speech = speech,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Engine re-sync is handled by wall-clock measurement; nothing to do.
    }

    override fun onDestroy() {
        speech.shutdown()
        super.onDestroy()
    }
}
