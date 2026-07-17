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
import androidx.compose.material3.Surface
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
 */
@Composable
fun NeonOledBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "neon_rail")
    val sweep by transition.animateFloat(
        initialValue = -0.15f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(7_200, easing = LinearEasing), RepeatMode.Restart),
        label = "neon_sweep"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(Color.Black)
        val railY = size.height * 0.18f
        val secondRailY = size.height * 0.78f
        val head = size.width * sweep
        drawLine(Color(0x2458FFE8), Offset(0f, railY), Offset(size.width, railY), strokeWidth = 1.5f)
        drawLine(Color(0x1C739BFF), Offset(0f, secondRailY), Offset(size.width, secondRailY), strokeWidth = 1f)
        drawLine(Color(0x8820F6D0), Offset((head - 96f).coerceAtLeast(0f), railY), Offset((head + 12f).coerceAtMost(size.width), railY), strokeWidth = 2.5f)
        drawCircle(Color(0xCC84FFEE), radius = 3f, center = Offset(head.coerceIn(0f, size.width), railY), style = Stroke(width = 1.5f))
    }
}
