package com.hiittimer.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hiittimer.data.Preferences
import com.hiittimer.data.ThemeMode
import com.hiittimer.speech.SpeechVoiceInfo

/** User preferences: sounds, haptics, speech, appearance and screen behaviour. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: Preferences,
    currentVoiceName: String,
    onUpdate: ((Preferences) -> Preferences) -> Unit,
    onReset: () -> Unit,
    onOpenVoices: () -> Unit,
    onClose: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                actions = { TextButton(onClick = onClose) { Text("Done", fontWeight = FontWeight.Bold) } },
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item {
                Section("Feedback")
                ToggleRow("Interval sounds", prefs.soundEnabled) {
                    onUpdate { it.copy(soundEnabled = it.soundEnabled.not()) }
                }
                ToggleRow("Countdown ticks (3 · 2 · 1)", prefs.countdownTicksEnabled, enabled = prefs.soundEnabled) {
                    onUpdate { it.copy(countdownTicksEnabled = it.countdownTicksEnabled.not()) }
                }
                SliderRow("Volume ${ (prefs.volume * 100).toInt()}%", prefs.volume.toFloat(),
                    enabled = prefs.soundEnabled) {
                    onUpdate { p -> p.copy(volume = it.toDouble()) }
                }
                ToggleRow("Haptic feedback", prefs.hapticsEnabled) {
                    onUpdate { it.copy(hapticsEnabled = it.hapticsEnabled.not()) }
                }
            }
            item {
                Section("Announcements")
                ToggleRow("Spoken announcements", prefs.announcementsEnabled) {
                    onUpdate { it.copy(announcementsEnabled = it.announcementsEnabled.not()) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = prefs.announcementsEnabled) { onOpenVoices() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Voice", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(currentVoiceName, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SliderRow("Speech rate ${(prefs.speechRate * 100).toInt()}%", prefs.speechRate.toFloat(),
                    enabled = prefs.announcementsEnabled, range = 0.1f..1f) {
                    onUpdate { p -> p.copy(speechRate = it.toDouble()) }
                }
                SliderRow("Announcement volume ${(prefs.speechVolume * 100).toInt()}%",
                    prefs.speechVolume.toFloat(), enabled = prefs.announcementsEnabled) {
                    onUpdate { p -> p.copy(speechVolume = it.toDouble()) }
                }
                Text("Announces each exercise by name, plus interval cues.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Section("Appearance")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = prefs.theme == ThemeMode.LIGHT,
                        onClick = { onUpdate { it.copy(theme = ThemeMode.LIGHT) } },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("Light") }
                    SegmentedButton(
                        selected = prefs.theme == ThemeMode.DARK,
                        onClick = { onUpdate { it.copy(theme = ThemeMode.DARK) } },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("Dark") }
                }
                Text("Light uses a clean white background. Dark uses the near-black background.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp))
            }
            item {
                Section("Display")
                ToggleRow("Keep screen awake", prefs.keepScreenAwake) {
                    onUpdate { it.copy(keepScreenAwake = it.keepScreenAwake.not()) }
                }
                Text("Prevents the display from sleeping while a workout is running.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                TextButton(
                    onClick = onReset,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.padding(vertical = 16.dp),
                ) { Text("Reset to defaults") }
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, enabled: Boolean = true, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (enabled) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onSurfaceVariant)
        Switch(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled)
    }
}

@Composable
private fun SliderRow(label: String, value: Float, enabled: Boolean = true, range: ClosedFloatingPointRange<Float> = 0f..1f, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = range, enabled = enabled)
    }
}

/** Lists installed TTS voices with a preview button on every row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicePickerScreen(
    groups: List<Pair<String, List<SpeechVoiceInfo>>>,
    selectedId: String?,
    onSelectDefault: () -> Unit,
    onSelect: (SpeechVoiceInfo) -> Unit,
    onPreview: (SpeechVoiceInfo) -> Unit,
    onClose: () -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Voice", fontWeight = FontWeight.Bold) },
            navigationIcon = { TextButton(onClick = onClose) { Text("Back") } },
        )
    }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth().clickable { onSelectDefault() }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle, null,
                        tint = if (selectedId == null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("  Use default voice", style = MaterialTheme.typography.bodyLarge)
                }
                Text("The default follows your device language.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }
            groups.forEach { (language, voices) ->
                item {
                    Text(language, style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                }
                items(voices, key = { it.id }) { voice ->
                    val selected = selectedId == voice.id
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.weight(1f).clickable { onSelect(voice) },
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle, null,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text("  ${voice.displayName}", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { onPreview(voice) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Preview ${voice.displayName}")
                        }
                    }
                }
            }
        }
    }
}
