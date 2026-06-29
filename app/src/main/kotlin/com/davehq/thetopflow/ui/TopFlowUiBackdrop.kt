package com.davehq.thetopflow.ui

import android.content.Context
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    Surface(
        color = Color.Black,
        modifier = Modifier.fillMaxSize()
    ) {}
}
