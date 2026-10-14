package com.hiittimer.domain

import kotlinx.serialization.Serializable
import java.util.UUID

/** The four kinds of interval a workout can contain. */
@Serializable
enum class WorkoutPhase {
    WARM_UP, WORK, REST, COOL_DOWN;

    val title: String get() = when (this) {
        WARM_UP -> "WARM UP"
        WORK -> "WORK"
        REST -> "REST"
        COOL_DOWN -> "COOL DOWN"
    }

    val accessibilityDescription: String get() = when (this) {
        WARM_UP -> "Warm up"
        WORK -> "Work"
        REST -> "Rest"
        COOL_DOWN -> "Cool down"
    }

    /** True when the body should be recovering rather than working. */
    val isRecovery: Boolean get() = this != WORK
}
