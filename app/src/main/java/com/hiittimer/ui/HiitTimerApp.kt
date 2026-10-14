package com.hiittimer.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.hiittimer.audio.AudioCueService
import com.hiittimer.data.PreferencesStore
import com.hiittimer.data.ThemeMode
import com.hiittimer.data.WorkoutLibrary
import com.hiittimer.domain.EngineStatus
import com.hiittimer.domain.Workout
import com.hiittimer.domain.WorkoutEngine
import com.hiittimer.domain.WorkoutPlan
import com.hiittimer.haptics.HapticsService
import com.hiittimer.speech.SpeechService
import com.hiittimer.ui.editor.EditorScreen
import com.hiittimer.ui.library.LibraryScreen
import com.hiittimer.ui.settings.SettingsScreen
import com.hiittimer.ui.settings.VoicePickerScreen
import com.hiittimer.ui.theme.HiitTimerTheme
import com.hiittimer.ui.timer.TimerScreen

private sealed interface Sheet { data object Library : Sheet; data object Settings : Sheet; data object Voices : Sheet }
private sealed interface EditorTarget {
    data object Create : EditorTarget
    data class Edit(val workout: Workout) : EditorTarget
}

/** Owns the workout engine and navigation. Mirrors iOS RootView. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiitTimerApp(
    library: WorkoutLibrary,
    preferences: PreferencesStore,
    audio: AudioCueService,
    haptics: HapticsService,
    speech: SpeechService,
) {
    val prefs by preferences.preferences.collectAsState()
    val custom by library.customWorkouts.collectAsState()
    val context = LocalContext.current

    var engine by remember { mutableStateOf<WorkoutEngine?>(null) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var editor by remember { mutableStateOf<EditorTarget?>(null) }

    val selectedWorkout = library.workout(prefs.selectedWorkoutID) ?: Workout.presets[0]

    // Keep the screen awake while running, per preference (mirrors iOS ScreenWakeLock).
    val status by engine?.status?.collectAsState() ?: remember { mutableStateOf<EngineStatus?>(null) }
    DisposableEffect(status, prefs.keepScreenAwake) {
        val window = (context as? Activity)?.window
        val shouldKeep = prefs.keepScreenAwake && status == EngineStatus.RUNNING
        if (shouldKeep) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    fun startWorkout() {
        haptics.prepare()
        val newEngine = WorkoutEngine(
            plan = WorkoutPlan(selectedWorkout),
            audio = audio, haptics = haptics, preferences = preferences, speech = speech,
        )
        engine = newEngine
        newEngine.start()
    }

    fun dismissWorkout() {
        speech.stop()
        engine?.reset()
        engine?.invalidate()
        engine = null
    }

    HiitTimerTheme(mode = prefs.theme) {
        val eng = engine
        val engState by eng?.state?.collectAsState() ?: remember { mutableStateOf(null) }
        val engStatus by eng?.status?.collectAsState() ?: remember { mutableStateOf<EngineStatus?>(null) }

        when {
            editor != null -> {
                val target = editor!!
                EditorScreen(
                    initial = when (target) {
                        EditorTarget.Create -> Workout(name = "New Workout", workSeconds = 30, restSeconds = 15, rounds = 8)
                        is EditorTarget.Edit -> target.workout
                    },
                    isPreset = (target as? EditorTarget.Edit)?.workout?.isPreset == true,
                    onCancel = { editor = null },
                    onSave = { w ->
                        if (w.isPreset) library.duplicate(w) else library.save(w)
                        editor = null
                        // If we were opened from the library sheet, stay there.
                    },
                )
            }
            sheet == null -> {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = { Text("Tabata & HIIT Timer", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { sheet = Sheet.Library }) {
                                    Icon(Icons.Default.List, contentDescription = "Choose workout")
                                }
                            },
                            actions = {
                                IconButton(onClick = { sheet = Sheet.Settings }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                            ),
                        )
                    },
                ) { padding ->
                    val dark = prefs.theme == ThemeMode.DARK
                    TimerScreen(
                        workout = selectedWorkout,
                        engine = if (engStatus == EngineStatus.IDLE) null else eng,
                        state = engState,
                        status = engStatus,
                        dark = dark,
                        onStart = ::startWorkout,
                        onPauseResume = {
                            if (engStatus == EngineStatus.RUNNING) eng?.pause() else eng?.resume()
                        },
                        onDismiss = ::dismissWorkout,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
            sheet == Sheet.Library -> {
                LibraryScreen(
                    presets = Workout.presets,
                    custom = custom,
                    selectedId = prefs.selectedWorkoutID,
                    onSelect = { w ->
                        preferences.update { it.copy(selectedWorkoutID = w.id) }
                        // Never swap a running workout out from underneath the user.
                        val st = engine?.status?.value
                        if (engine == null || st == EngineStatus.FINISHED || st == EngineStatus.IDLE) {
                            engine?.invalidate(); engine = null
                        }
                        sheet = null
                    },
                    onNew = { editor = EditorTarget.Create },
                    onEdit = { editor = EditorTarget.Edit(it) },
                    onDelete = { library.delete(it.id) },
                    onClose = { sheet = null },
                )
            }
            sheet == Sheet.Settings -> {
                SettingsScreen(
                    prefs = prefs,
                    currentVoiceName = speech.voiceInfo(prefs.speechVoiceID)?.displayName ?: "Default",
                    onUpdate = { preferences.update(it) },
                    onReset = { speech.stop(); preferences.reset() },
                    onOpenVoices = { sheet = Sheet.Voices },
                    onClose = { sheet = null },
                )
            }
            sheet == Sheet.Voices -> {
                VoicePickerScreen(
                    groups = speech.voicesGroupedByLanguage(prefs.speechLanguageCode),
                    selectedId = prefs.speechVoiceID,
                    onSelectDefault = {
                        speech.stop()
                        preferences.update { it.copy(speechVoiceID = null, speechLanguageCode = null) }
                    },
                    onSelect = { voice ->
                        speech.stop()
                        preferences.update { it.copy(speechVoiceID = voice.id, speechLanguageCode = voice.languageCode) }
                    },
                    onPreview = { voice ->
                        speech.preview("Next exercise: burpees.", voice.id, prefs.speechRate, prefs.speechVolume)
                    },
                    onClose = { speech.stop(); sheet = Sheet.Settings },
                )
            }
        }
    }
}
