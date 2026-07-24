package com.davehq.thetopflow.ui

import android.content.Context
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

private const val TOP_FLOW_TAG = "TopFlow"

@Suppress("unused")
object TopFlowUiBackdropBridge {
    @JvmStatic
    fun createPremiumBackdrop(context: Context): View {
        Log.d(TOP_FLOW_TAG, "compose_host_created host=premium_backdrop")
        return ComposeView(context).apply {
            isClickable = false
            isFocusable = false
            attachComposeOwners(this, context, "premium_backdrop")
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    Log.d(
                        TOP_FLOW_TAG,
                        "compose_host_attached host=premium_backdrop lifecycle=${v.findViewTreeLifecycleOwner() != null}"
                    )
                }

                override fun onViewDetachedFromWindow(v: View) {
                    Log.d(TOP_FLOW_TAG, "compose_host_disposed host=premium_backdrop strategy=view_tree_lifecycle")
                }
            })
            setContent {
                PremiumStudioBackdrop()
            }
            Log.d(TOP_FLOW_TAG, "compose_content_set host=premium_backdrop composable=PremiumStudioBackdrop")
        }
    }

    private fun attachComposeOwners(view: View, context: Context, host: String) {
        val lifecycleOwner = context as? LifecycleOwner
        val savedStateOwner = context as? SavedStateRegistryOwner
        val viewModelStoreOwner = context as? ViewModelStoreOwner
        if (lifecycleOwner != null) {
            view.setViewTreeLifecycleOwner(lifecycleOwner)
        }
        if (savedStateOwner != null) {
            view.setViewTreeSavedStateRegistryOwner(savedStateOwner)
        }
        if (viewModelStoreOwner != null) {
            view.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        }
        Log.d(
            TOP_FLOW_TAG,
            "compose_owner_attached host=$host lifecycle=${lifecycleOwner != null} savedState=${savedStateOwner != null} viewModel=${viewModelStoreOwner != null}"
        )
    }
}

@Composable
fun PremiumStudioBackdrop() {
    NeonOledBackdrop()
}

/**
 * Lightweight OLED treatment: only Canvas draw work changes each frame, so it
 * does not remeasure note content or compete with typing/recording work.
 * 30.2 strengthens the visual framework with a soft vertical accent + dual
 * sweeping rails while remaining draw-only and low-cost.
 */
@Composable
fun NeonOledBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "neon_rail")
    val sweep by transition.animateFloat(
        initialValue = -0.12f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(8_400, easing = LinearEasing), RepeatMode.Restart),
        label = "neon_sweep"
    )
    val secondarySweep by transition.animateFloat(
        initialValue = 1.08f,
        targetValue = -0.08f,
        animationSpec = infiniteRepeatable(tween(11_200, easing = LinearEasing), RepeatMode.Restart),
        label = "neon_sweep_secondary"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(Color.Black)

        // Soft vertical studio rail (left edge) — static, zero extra cost
        val leftRailX = size.width * 0.035f
        drawLine(
            Color(0x1840FFD0),
            Offset(leftRailX, size.height * 0.08f),
            Offset(leftRailX, size.height * 0.92f),
            strokeWidth = 1.25f
        )

        val railY = size.height * 0.16f
        val secondRailY = size.height * 0.82f
        val head = size.width * sweep
        val head2 = size.width * secondarySweep

        // Primary horizontal rail + traveling head
        drawLine(Color(0x2258FFE8), Offset(0f, railY), Offset(size.width, railY), strokeWidth = 1.4f)
        drawLine(
            Color(0xAA20F6D0),
            Offset((head - 110f).coerceAtLeast(0f), railY),
            Offset((head + 18f).coerceAtMost(size.width), railY),
            strokeWidth = 2.6f
        )
        drawCircle(
            Color(0xCC84FFEE),
            radius = 3.2f,
            center = Offset(head.coerceIn(0f, size.width), railY),
            style = Stroke(width = 1.6f)
        )

        // Secondary lower rail (opposite direction) for depth without noise
        drawLine(Color(0x18739BFF), Offset(0f, secondRailY), Offset(size.width, secondRailY), strokeWidth = 1.1f)
        drawLine(
            Color(0x6680A8FF),
            Offset((head2 - 72f).coerceAtLeast(0f), secondRailY),
            Offset((head2 + 14f).coerceAtMost(size.width), secondRailY),
            strokeWidth = 1.8f
        )
    }
}
