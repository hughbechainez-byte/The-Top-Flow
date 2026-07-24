package com.davehq.thetopflow.ui

import android.content.Context
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
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
 * Premium OLED studio backdrop — static, no looping travel animations.
 * Soft vignette + quiet corner bloom only. Zero per-frame motion so the
 * surface feels intentional rather than gimmicky.
 */
@Composable
fun NeonOledBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // True OLED black base
        drawRect(Color.Black)

        // Soft vertical vignette (edges darker, center slightly lifted)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF05080C),
                    Color(0xFF000000),
                    Color(0xFF000000),
                    Color(0xFF04060A)
                )
            )
        )

        // Quiet radial bloom top-left (mint) — static, low alpha
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x1420F6D0),
                    Color(0x00000000)
                ),
                center = Offset(size.width * 0.12f, size.height * 0.08f),
                radius = size.minDimension * 0.55f
            ),
            radius = size.minDimension * 0.55f,
            center = Offset(size.width * 0.12f, size.height * 0.08f)
        )

        // Quiet radial bloom bottom-right (cool blue) for depth balance
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x106080FF),
                    Color(0x00000000)
                ),
                center = Offset(size.width * 0.92f, size.height * 0.88f),
                radius = size.minDimension * 0.48f
            ),
            radius = size.minDimension * 0.48f,
            center = Offset(size.width * 0.92f, size.height * 0.88f)
        )

        // Hairline left studio rail — static accent, no travel head
        val leftRailX = size.width * 0.028f
        drawLine(
            Color(0x2240FFD0),
            Offset(leftRailX, size.height * 0.12f),
            Offset(leftRailX, size.height * 0.88f),
            strokeWidth = 1.1f
        )
    }
}
