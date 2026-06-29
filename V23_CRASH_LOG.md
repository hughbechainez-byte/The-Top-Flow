# V23 Crash Log

Date captured: 2026-06-29
Source attachment: `C:\Users\blowb\Downloads\AKjOo3Ny.txt.part`

## Summary

Release 23.0, versionCode 56, package `com.davehq.thetopflow`, targetSdk 35, crashes on demand during startup on `google/blazer/blazer:17/CP2A.260605.012/2026062301`.

Severity: blocker. The capture shows the same fatal startup crash twice, first at 11:11:36 and again at 11:12:00.

## Crash Signature

```text
FATAL EXCEPTION: main
Process: com.davehq.thetopflow
java.lang.IllegalStateException: ViewTreeLifecycleOwner not found from android.widget.FrameLayout
    at androidx.compose.ui.platform.WindowRecomposer_androidKt.createLifecycleAwareWindowRecomposer
    at androidx.compose.ui.platform.AbstractComposeView.resolveParentCompositionContext
    at androidx.compose.ui.platform.AbstractComposeView.onAttachedToWindow
```

Immediately before both crashes, the app logs:

```text
rhyme_trace stage=editor_services
rhyme_trace stage=popup_created
rhyme_trace stage=preload_start
wm_on_create_called / wm_on_start_called / wm_on_resume_called
```

## Likely Fault Area

This is most likely the 22.2+ Compose backdrop bridge, not the rhyme engine, notes, media, or appcast path.

Relevant code:

- `app/src/main/java/com/davehq/thetopflow/MainActivity.java`: `MainActivity` extends plain `Activity`; `buildUi()` creates a root `FrameLayout`, calls `setContentView(root)`, then attaches `TopFlowUiBackdropBridge.createPremiumBackdrop(this)`.
- `app/src/main/kotlin/com/davehq/thetopflow/ui/TopFlowUiBackdrop.kt`: `createPremiumBackdrop()` returns a `ComposeView` and calls `setContent { PremiumStudioBackdrop() }`.

The fatal exception says Compose cannot find a `ViewTreeLifecycleOwner` from the attached `FrameLayout`, so the `ComposeView` cannot create its lifecycle-aware recomposer.

## Next Fix Direction

Fix before any further 23.x release:

- Either provide the required view-tree owners before attaching the Compose backdrop, or move `MainActivity` to a proper lifecycle owner base such as `ComponentActivity`.
- If the lifecycle migration is risky, temporarily replace the Compose backdrop with the existing Java `View` backdrop so release startup is stable.
- Verify with a release install on device, then confirm notes, editor input, rhyme popup, recording/playback, settings, and update checking still open normally.

