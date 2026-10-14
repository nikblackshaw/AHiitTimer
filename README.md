# HIIT Timer — Android

Native Android port of the iOS **Hiit-Timer** app, rebuilt with a fresh,
modern Material 3 look. Same functionality as iOS:

- 6 built-in presets (Tabata, 40/20, 30/30, 10-Minute, Sprint, Beginner)
- Custom workouts with warm-up / work / rest / cool-down, rounds × sets
- Per-interval exercise names, announced via on-device TTS
- Wall-clock-accurate engine (pause/resume/reset, 3·2·1 countdown ticks)
- Interval sounds synthesised at runtime (no audio assets), volume control
- Haptics per phase, keep-screen-awake, light/dark theme
- JSON persistence for workouts + preferences

## Stack

Kotlin · Jetpack Compose (Material 3) · Coroutines/StateFlow ·
kotlinx.serialization · TextToSpeech · AudioTrack synth · Vibrator

## Build

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Requires Android SDK with platform 36 + JDK 17.
