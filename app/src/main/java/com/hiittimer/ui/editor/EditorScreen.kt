package com.hiittimer.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hiittimer.domain.TimeFormat
import com.hiittimer.domain.Workout
import com.hiittimer.ui.components.AdjusterRow
import com.hiittimer.ui.components.SectionHeader

/** Create or edit a workout. Saving a preset creates a copy (presets stay untouched). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initial: Workout,
    isPreset: Boolean,
    onCancel: () -> Unit,
    onSave: (Workout) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var warmUp by remember { mutableStateOf(initial.warmUpSeconds) }
    var workSecs by remember { mutableStateOf(initial.workSeconds.coerceIn(Workout.Limits.work)) }
    var restSecs by remember { mutableStateOf(initial.restSeconds) }
    var coolDown by remember { mutableStateOf(initial.coolDownSeconds) }
    var rounds by remember { mutableStateOf(initial.rounds) }
    var sets by remember { mutableStateOf(initial.sets) }
    var names by remember {
        mutableStateOf(initial.exerciseNames?.toMutableList() ?: MutableList(rounds * sets) { "" })
    }

    fun ensureNames(r: Int, s: Int) {
        val need = r * s
        while (names.size < need) names.add("")
        if (names.size > need) names = names.take(need).toMutableList()
    }

    val draft = initial.copy(
        name = name, warmUpSeconds = warmUp, workSeconds = workSecs,
        restSeconds = restSecs, coolDownSeconds = coolDown,
        rounds = rounds, sets = sets,
        exerciseNames = names.take(rounds * sets),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isPreset) "Duplicate Workout" else "Workout", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel") } },
                actions = {
                    Button(onClick = {
                        var w = draft.validated()
                        val t = name.trim()
                        w = w.copy(name = t.ifEmpty { w.name })
                        onSave(w)
                    }) { Text("Save") }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(40) },
                label = { Text("Workout name") }, singleLine = true,
            )
            SectionHeader("Intervals")
            DurationRow("Warm up", "Optional lead-in before the first round", warmUp, Workout.Limits.warmUp) { warmUp = it }
            DurationRow("Work", null, workSecs, Workout.Limits.work) { workSecs = it }
            DurationRow("Rest", if (restSecs == 0) "No rest between rounds" else null, restSecs, Workout.Limits.rest) { restSecs = it }
            DurationRow("Cool down", "Optional period after the last round", coolDown, Workout.Limits.coolDown) { coolDown = it }

            SectionHeader("Repeats")
            AdjusterRow("Rounds", "$rounds", null, rounds > Workout.Limits.rounds.first,
                rounds < Workout.Limits.rounds.last,
                { rounds = (rounds - 1).coerceIn(Workout.Limits.rounds); ensureNames(rounds, sets) },
                { rounds = (rounds + 1).coerceIn(Workout.Limits.rounds); ensureNames(rounds, sets) })
            AdjusterRow("Sets", "$sets", "A set repeats the full block of rounds",
                sets > Workout.Limits.sets.first, sets < Workout.Limits.sets.last,
                { sets = (sets - 1).coerceIn(Workout.Limits.sets); ensureNames(rounds, sets) },
                { sets = (sets + 1).coerceIn(Workout.Limits.sets); ensureNames(rounds, sets) })
            Text("Total time ${TimeFormat.duration(draft.validated().totalDuration.toDouble())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            SectionHeader("Exercise names")
            Text("Optional. Each name is announced when its interval starts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            for (set in 1..sets) {
                if (sets > 1) {
                    Text("Set $set", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
                for (round in 1..rounds) {
                    val idx = (set - 1) * rounds + (round - 1)
                    OutlinedTextField(
                        value = names.getOrElse(idx) { "" },
                        onValueChange = {
                            ensureNames(rounds, sets)
                            val m = names.toMutableList(); m[idx] = it; names = m
                        },
                        label = { Text("Round $round") }, singleLine = true,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
            if (isPreset) {
                Text("Saving creates a copy in My Workouts and leaves the preset unchanged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DurationRow(title: String, caption: String?, seconds: Int, range: IntRange, onChange: (Int) -> Unit) {
    AdjusterRow(
        title = title,
        value = if (seconds == 0) "Off" else TimeFormat.duration(seconds.toDouble()),
        caption = caption,
        canDecrement = seconds > range.first,
        canIncrement = seconds < range.last,
        onDecrement = { onChange((seconds - Workout.Limits.step).coerceIn(range)) },
        onIncrement = { onChange((seconds + Workout.Limits.step).coerceIn(range)) },
    )
}
