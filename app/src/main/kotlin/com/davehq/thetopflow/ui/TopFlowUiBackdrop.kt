package com.davehq.thetopflow.ui

import android.content.Context
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import kotlin.math.sin

@Suppress("unused")
object TopFlowUiBackdropBridge {
    @JvmStatic
    fun createPremiumBackdrop(context: Context): View {
        return ComposeView(context).apply {
            isClickable = false
            isFocusable = false
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                PremiumStudioBackdrop()
            }
        }
    }
}

@Composable
fun PremiumStudioBackdrop() {
    Surface(
        color = Color.Black,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            if (w <= 0f || h <= 0f) return@Canvas

            val railLeft = Color(0x1A76EFFF)
            val railRight = Color(0x188CF4FF)
            val railSoft = Color(0x0FFFEB4A)
            val lineStrong = Color(0x3A7DFFAC)

            for (index in 0..8) {
                val y = h * (index / 8f)
                val alpha = if (index == 0 || index == 8) 1f else 0.42f
                drawLine(
                    color = railSoft.copy(alpha = 0.12f * alpha),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = if (index % 4 == 0) 1.5f else 0.8f
                )
            }

            for (x in 0..12) {
                val xpos = w * (x / 12f)
                val active = x % 3 == 0
                val railColor = if (active) railLeft else railSoft
                drawLine(
                    color = railColor.copy(alpha = if (active) 0.26f else 0.1f),
                    start = Offset(xpos, 0f),
                    end = Offset(xpos, h),
                    strokeWidth = if (active) 1.2f else 0.6f
                )
            }

            val path = Path()
            val top = h * 0.25f
            val amp = h * 0.055f
            path.moveTo(0f, top)
            val segments = 84
            for (step in 0..segments) {
                val x = w * (step / segments.toFloat())
                val phase = (step * 0.36f)
                val y = top + sin(phase) * amp + (step % 2) * 6f * if (step % 12 < 6) 1f else -1f
                path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = Color(0x4A76D3FF),
                alpha = 0.84f,
                style = Stroke(width = 1.4f)
            )

            for (x in 0..15) {
                val baseX = w * (x / 15f)
                val waveAmp = (x * 11) % 24
                for (yStep in 0..3) {
                    val y = h * 0.58f + waveAmp + yStep * (h * 0.08f) + (yStep % 2) * 4f
                    val color = if (x % 4 == 0) lineStrong else railRight
                    drawLine(
                        color = color.copy(alpha = 0.18f),
                        start = Offset(baseX, y),
                        end = Offset(baseX + h * 0.04f + ((yStep + x) % 5) * 14f, y),
                        strokeWidth = 0.9f
                    )
                }
            }

            for (tick in 0..28) {
                val x = (w * 0.08f) + (tick * (w * 0.028f))
                if (x >= w - 24f) break
                val p = (tick % 2 == 0)
                val px = if (p) railLeft.copy(alpha = 0.15f) else railRight.copy(alpha = 0.09f)
                val yA = h * 0.72f
                drawLine(
                    color = px,
                    start = Offset(x, yA),
                    end = Offset(x, yA - (if (p) 18f else 10f) + sin(tick.toFloat()) * 4f),
                    strokeWidth = if (p) 1.1f else 0.6f
                )
            }

            drawLine(
                color = lineStrong.copy(alpha = 0.18f),
                start = Offset(0f, h * 0.68f),
                end = Offset(w, h * 0.68f),
                strokeWidth = 1.1f
            )
        }
    }
}
