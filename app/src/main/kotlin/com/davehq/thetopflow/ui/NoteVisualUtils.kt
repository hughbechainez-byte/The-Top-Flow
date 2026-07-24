package com.davehq.thetopflow.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real editor paper color — dark OLED tint of the note color.
 * Contrast must be measured against this, not the raw noteColor int.
 */
fun topFlowEditorSurface(noteColor: Int): Color {
    val base = Color(noteColor)
    return Color(
        red = (0.03f + base.red * 0.13f).coerceIn(0f, 1f),
        green = (0.035f + base.green * 0.13f).coerceIn(0f, 1f),
        blue = (0.05f + base.blue * 0.13f).coerceIn(0f, 1f),
        alpha = 1f
    )
}

fun topFlowReadableText(surface: Color, preferred: Color, fallback: Color): Color {
    if (topFlowContrast(surface, preferred) >= 3.2f) return preferred
    val light = Color(0xFFE8EAED)
    val dark = Color(0xFF0B0E12)
    return if (topFlowContrast(surface, light) >= topFlowContrast(surface, dark)) light else dark
}

fun topFlowReadableMeta(surface: Color, preferred: Color): Color {
    val soft = preferred.copy(alpha = 0.72f)
    if (topFlowContrast(surface, soft) >= 2.4f) return soft
    val light = Color(0xFFB8C0CC)
    val dark = Color(0xFF3A4250)
    return if (topFlowContrast(surface, light) >= topFlowContrast(surface, dark)) light else dark
}

fun topFlowContrast(a: Color, b: Color): Float {
    val l1 = topFlowLuminance(a) + 0.05f
    val l2 = topFlowLuminance(b) + 0.05f
    val hi = maxOf(l1, l2)
    val lo = minOf(l1, l2)
    return hi / lo
}

private fun topFlowLuminance(color: Color): Float {
    fun lin(c: Float): Float =
        if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    return 0.2126f * lin(color.red) + 0.7152f * lin(color.green) + 0.0722f * lin(color.blue)
}

/**
 * Accurate HSV color wheel: hue from angle (no +180 offset), saturation from radius,
 * drag + tap, visible selection indicator at the true pick point.
 */
@Composable
fun TopFlowColorWheel(
    color: Int,
    accent: Int,
    onChange: (Int) -> Unit
) {
    val hsv = remember(color) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(color, it) }
    }
    var hue by remember(color) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(color) { mutableFloatStateOf(hsv[1].coerceIn(0f, 1f)) }
    var value by remember(color) { mutableFloatStateOf(hsv[2].coerceIn(0.18f, 1f)) }

    fun commit(h: Float = hue, s: Float = saturation, v: Float = value) {
        hue = ((h % 360f) + 360f) % 360f
        saturation = s.coerceIn(0f, 1f)
        value = v.coerceIn(0.18f, 1f)
        onChange(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }

    fun pick(offset: Offset, size: Size) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val dx = offset.x - center.x
        val dy = offset.y - center.y
        // atan2: 0° at +X — matches Compose drawArc startAngle (3 o'clock = red)
        val degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val radius = min(size.width, size.height) / 2f
        val distance = sqrt(dx * dx + dy * dy)
        val mappedHue = ((degrees % 360f) + 360f) % 360f
        commit(h = mappedHue, s = (distance / radius).coerceIn(0f, 1f))
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .size(210.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset -> pick(offset, size) }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> pick(offset, size) },
                            onDrag = { change, _ ->
                                pick(change.position, size)
                                change.consume()
                            }
                        )
                    }
            ) {
                val radius = min(size.width, size.height) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                for (angle in 0 until 360 step 3) {
                    drawArc(
                        color = Color(android.graphics.Color.HSVToColor(floatArrayOf(angle.toFloat(), 1f, value))),
                        startAngle = angle.toFloat(),
                        sweepAngle = 4f,
                        useCenter = true
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(0f, 0f, value))),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius * 0.55f
                    ),
                    radius = radius * 0.55f,
                    center = center
                )
                val rad = Math.toRadians(hue.toDouble())
                val indR = radius * saturation
                val ix = center.x + cos(rad).toFloat() * indR
                val iy = center.y + sin(rad).toFloat() * indR
                drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(ix, iy), style = Stroke(width = 2.5.dp.toPx()))
                drawCircle(Color(color), radius = 4.5.dp.toPx(), center = Offset(ix, iy))
                drawCircle(
                    Color(accent).copy(alpha = 0.5f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        Text(
            "Brightness ${(value * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium
        )
        Slider(
            value = value,
            onValueChange = { commit(v = it) },
            valueRange = 0.18f..1f
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(
                listOf(
                    0xFF0ECDBE.toInt(),
                    0xFF84FFEE.toInt(),
                    0xFF6C63FF.toInt(),
                    0xFFFFC875.toInt(),
                    0xFFFF8A80.toInt(),
                    0xFFE8EAED.toInt(),
                    0xFF05070D.toInt(),
                    0xFF000000.toInt()
                )
            ) { swatch ->
                val selected = swatch == color
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(swatch))
                        .border(
                            BorderStroke(if (selected) 2.dp else 1.dp, Color(accent)),
                            CircleShape
                        )
                        .clickable { onChange(swatch) }
                )
            }
        }
    }
}
