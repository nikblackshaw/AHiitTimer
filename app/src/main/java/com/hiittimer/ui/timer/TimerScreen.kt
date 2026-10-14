package com.hiittimer.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiittimer.domain.EngineStatus
import com.hiittimer.domain.TimeFormat
import com.hiittimer.domain.Workout
import com.hiittimer.domain.WorkoutEngine
import com.hiittimer.domain.WorkoutState
import com.hiittimer.ui.components.IndicatorPill
import com.hiittimer.ui.components.LargeButton
import com.hiittimer.ui.components.ProgressRing
import com.hiittimer.ui.components.StatChip
import com.hiittimer.ui.theme.PhaseColors

/** The main screen: ready state, running workout and completion summary. */
@Composable
fun TimerScreen(
    workout: Workout,
    engine: WorkoutEngine?,
    state: WorkoutState?,
    status: EngineStatus?,
    dark: Boolean,
    onStart: () -> Unit,
    onPauseResume: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp).padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (engine == null || state == null || status == null ||
            status == EngineStatus.IDLE
        ) {
            ReadyContent(workout, dark, onStart)
        } else if (status == EngineStatus.FINISHED) {
            FinishedContent(engine, state, onDismiss)
        } else {
            ActiveContent(engine, state, status, dark, onPauseResume, onDismiss)
        }
    }
}

@Composable
private fun ColumnScope.ReadyContent(workout: Workout, dark: Boolean, onStart: () -> Unit) {
    val accent = if (dark) PhaseColors.Accent else PhaseColors.AccentLight
    Spacer(Modifier.height(8.dp))
    Text(
        "READY", style = MaterialTheme.typography.labelLarge,
        color = accent, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp,
    )
    Text(
        workout.name, style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2,
    )
    Text(
        "Total ${TimeFormat.duration(workout.totalDuration.toDouble())}",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    StatGrid(workout, dark)
    Spacer(Modifier.weight(1f))
    LargeButton(text = "Start", onClick = onStart, tint = accent,
        icon = { Icon(Icons.Default.PlayArrow, null, Modifier.padding(end = 8.dp)) })
}

@Composable
private fun StatGrid(workout: Workout, dark: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip("WORK", TimeFormat.duration(workout.workSeconds.toDouble()),
                if (dark) PhaseColors.Work else PhaseColors.WorkLight, Modifier.weight(1f))
            StatChip("REST", if (workout.restSeconds > 0) TimeFormat.duration(workout.restSeconds.toDouble()) else "None",
                if (dark) PhaseColors.Rest else PhaseColors.RestLight, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val accent = if (dark) PhaseColors.Accent else PhaseColors.AccentLight
            StatChip("ROUNDS", "${workout.rounds}", accent, Modifier.weight(1f))
            StatChip("SETS", "${workout.sets}", accent, Modifier.weight(1f))
        }
        if (workout.warmUpSeconds > 0 || workout.coolDownSeconds > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (workout.warmUpSeconds > 0) {
                    StatChip("WARM UP", TimeFormat.duration(workout.warmUpSeconds.toDouble()),
                        if (dark) PhaseColors.WarmUp else PhaseColors.WarmUpLight, Modifier.weight(1f))
                } else Spacer(Modifier.weight(1f))
                if (workout.coolDownSeconds > 0) {
                    StatChip("COOL DOWN", TimeFormat.duration(workout.coolDownSeconds.toDouble()),
                        if (dark) PhaseColors.CoolDown else PhaseColors.CoolDownLight, Modifier.weight(1f))
                } else Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ColumnScope.ActiveContent(
    engine: WorkoutEngine, state: WorkoutState, status: EngineStatus, dark: Boolean,
    onPauseResume: () -> Unit, onDismiss: () -> Unit,
) {
    val tint = PhaseColors.forPhase(state.phase.phase, dark)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IndicatorPill("ROUND", "${maxOf(state.phase.round, 1)} / ${state.workout.rounds}")
        IndicatorPill("SET", "${maxOf(state.phase.set, 1)} / ${state.workout.sets}")
        Spacer(Modifier.weight(1f))
        if (status == EngineStatus.PAUSED) {
            Text(
                "PAUSED", style = MaterialTheme.typography.labelSmall,
                color = if (dark) PhaseColors.Accent else PhaseColors.AccentLight,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    ProgressRing(
        progress = state.phaseProgress.toFloat(),
        color = tint,
        modifier = Modifier.fillMaxWidth(0.92f).aspectRatio(1f),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(state.phase.phase.title, style = MaterialTheme.typography.titleLarge,
                color = tint, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            state.phase.name?.let {
                Text(it.uppercase(), style = MaterialTheme.typography.titleSmall,
                    color = tint.copy(alpha = 0.9f), fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, maxLines = 2)
            }
            Text(
                TimeFormat.clock(state.secondsRemaining.toDouble()),
                style = MaterialTheme.typography.displayLarge,
                color = tint.copy(alpha = 0.95f), fontWeight = FontWeight.Bold,
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    val next = state.nextPhase
    Text(
        if (next != null) "NEXT · ${next.displayName.uppercase()} · ${TimeFormat.clock(next.duration)}" else "FINAL INTERVAL",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.weight(1f))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LargeButton(
            text = if (status == EngineStatus.RUNNING) "Pause" else "Resume",
            onClick = onPauseResume, tint = tint,
            modifier = Modifier.weight(1f),
            icon = {
                Icon(
                    if (status == EngineStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null, Modifier.padding(end = 8.dp),
                )
            },
        )
        FilledIconButton(
            onClick = onDismiss, modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) { Icon(Icons.Default.Refresh, contentDescription = "Reset workout") }
    }
}

@Composable
private fun ColumnScope.FinishedContent(engine: WorkoutEngine, state: WorkoutState, onDismiss: () -> Unit, dark: Boolean = false) {
    val green = MaterialTheme.colorScheme.tertiary
    Spacer(Modifier.height(12.dp))
    Icon(Icons.Default.CheckCircle, null, tint = green, modifier = Modifier.size(68.dp))
    Text("WORKOUT COMPLETE", style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Black, letterSpacing = 3.sp, textAlign = TextAlign.Center)
    Text(TimeFormat.duration(engine.plan.totalDuration),
        style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    val accent = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip("ROUNDS", "${state.workout.rounds}", accent, Modifier.weight(1f))
            StatChip("SETS", "${state.workout.sets}", accent, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip("WORK TIME", TimeFormat.duration(state.workout.workDuration.toDouble()),
                Color(0xFFFF4D4D), Modifier.weight(1f))
            StatChip("REST TIME", TimeFormat.duration(state.workout.restDuration.toDouble()),
                Color(0xFF3BA8FF), Modifier.weight(1f))
        }
    }
    Spacer(Modifier.weight(1f))
    LargeButton(text = "Done", onClick = onDismiss)
}
