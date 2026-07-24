# Integration notes (30.2)

## Rap Ready knob

`RapReadyControls.kt` provides:
- `hasHeadphonesConnected(context)`
- `rememberHeadphonesConnected()`
- `RapReadyKnob(...)` composable (only interactive when headphones are on)

Wire into `VoicePanel` inside `NotesScreens.kt` by adding after the recording list:

```kotlin
var rapReadyAmount by remember { mutableFloatStateOf(62f) }
RapReadyKnob(
    accent = Color(note.accentColor),
    amount = rapReadyAmount,
    onAmountChange = { rapReadyAmount = it },
    onApply = {
        // Future: offline / playback-chain processing using the RapReady amount.
        // Full multi-stage DSP port is tracked as a follow-up milestone.
    }
)
```

## Materialize transition

Replace the 160ms Crossfade with a slightly longer, scale-aware transition for a more deliberate notepad materialize:

```kotlin
Crossfade(
    targetState = selectedNote == null,
    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
    label = "notes_screen_crossfade"
)
```

Add import:
`import androidx.compose.animation.core.FastOutSlowInEasing`
