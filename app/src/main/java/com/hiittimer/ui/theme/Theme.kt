package com.hiittimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.hiittimer.data.ThemeMode
import com.hiittimer.domain.WorkoutPhase

// Sleek modern palette. Dark = near-black #0B0F14; light = clean white.
private val DarkBg = Color(0xFF0B0F14)
private val DarkSurface = Color(0xFF161D27)
private val DarkRaised = Color(0xFF212B3A)

object PhaseColors {
    val Work = Color(0xFFFF4D4D)
    val WorkLight = Color(0xFFD62525)
    val Rest = Color(0xFF3BA8FF)
    val RestLight = Color(0xFF0572D8)
    val WarmUp = Color(0xFFFFB31F)
    val WarmUpLight = Color(0xFFC07200)
    val CoolDown = Color(0xFF34D399)
    val CoolDownLight = Color(0xFF008D67)
    val Accent = Color(0xFFFF9012)
    val AccentLight = Color(0xFFFF7A1A)

    fun forPhase(phase: WorkoutPhase, dark: Boolean): Color = when (phase) {
        WorkoutPhase.WARM_UP -> if (dark) WarmUp else WarmUpLight
        WorkoutPhase.WORK -> if (dark) Work else WorkLight
        WorkoutPhase.REST -> if (dark) Rest else RestLight
        WorkoutPhase.COOL_DOWN -> if (dark) CoolDown else CoolDownLight
    }
}

private val DarkScheme = darkColorScheme(
    primary = PhaseColors.Accent,
    onPrimary = Color.White,
    secondary = PhaseColors.Rest,
    background = DarkBg,
    onBackground = Color(0xFFF2F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF2F5F9),
    surfaceVariant = DarkRaised,
    onSurfaceVariant = Color(0xFFB9C4D4),
    tertiary = PhaseColors.CoolDown,
)

private val LightScheme = lightColorScheme(
    primary = PhaseColors.AccentLight,
    onPrimary = Color.White,
    secondary = PhaseColors.RestLight,
    background = Color.White,
    onBackground = Color(0xFF111418),
    surface = Color(0xFFF2F2F7),
    onSurface = Color(0xFF111418),
    surfaceVariant = Color(0xFFE5E5EC),
    onSurfaceVariant = Color(0xFF5B6472),
    tertiary = PhaseColors.CoolDownLight,
)

@Composable
fun HiitTimerTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = mode == ThemeMode.DARK
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        content = content,
    )
}
