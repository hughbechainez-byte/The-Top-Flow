package com.davehq.thetopflow.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davehq.thetopflow.R

data class NoteFontSpec(
    val id: String,
    val label: String,
    val family: FontFamily
)

private fun normalizedNoteFont(fontId: String): String {
    return when (fontId.lowercase()) {
        "slim" -> "space_grotesk"
        "pixel" -> "silkscreen"
        "terminal" -> "share_tech_mono_regular"
        else -> fontId.lowercase()
    }
}

fun noteFontFamily(fontId: String): FontFamily {
    return when (normalizedNoteFont(fontId)) {
        "space_grotesk" -> FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))
        "share_tech_mono_regular" -> FontFamily(Font(R.font.share_tech_mono_regular, FontWeight.Normal))
        "silkscreen" -> FontFamily(Font(R.font.silkscreen, FontWeight.Normal))
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
}

val noteFontOptions: List<NoteFontSpec> = listOf(
    NoteFontSpec("space_grotesk", "Space Grotesk", FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))),
    NoteFontSpec("sans", "Sans", FontFamily.SansSerif),
    NoteFontSpec("serif", "Serif", FontFamily.Serif),
    NoteFontSpec("monospace", "Monospace", FontFamily.Monospace),
    NoteFontSpec("share_tech_mono_regular", "Share Tech Mono", FontFamily(Font(R.font.share_tech_mono_regular, FontWeight.Normal))),
    NoteFontSpec("silkscreen", "Silkscreen", FontFamily(Font(R.font.silkscreen, FontWeight.Normal)))
)

fun noteFontLabel(fontId: String): String {
    val normalized = normalizedNoteFont(fontId)
    return noteFontOptions.firstOrNull { it.id == normalized }?.label
        ?: noteFontOptions.firstOrNull { it.id == normalizedNoteFont("sans") }?.label
        ?: "Sans"
}

fun clampNoteFontSize(fontSizeSp: Int): Int = fontSizeSp.coerceIn(14, 28)

private val NotesDarkScheme = darkColorScheme(
    primary = Color(0xFF84FFEE),
    onPrimary = Color(0xFF00201D),
    secondary = Color(0xFFB8C8FF),
    onSecondary = Color(0xFF172348),
    tertiary = Color(0xFFFFC875),
    onTertiary = Color(0xFF402D00),
    background = Color.Black,
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF0B0F14),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF151A20),
    onSurfaceVariant = Color(0xFFC3CAD4),
    outline = Color(0xFF5A6472),
    outlineVariant = Color(0xFF252D36)
)

private val NotesLightScheme = lightColorScheme(
    primary = Color(0xFF006A60),
    onPrimary = Color.White,
    secondary = Color(0xFF405989),
    onSecondary = Color.White,
    tertiary = Color(0xFF745B00),
    onTertiary = Color.White,
    background = Color(0xFFFAFCFF),
    onBackground = Color(0xFF171A1F),
    surface = Color.White,
    onSurface = Color(0xFF171A1F),
    surfaceVariant = Color(0xFFE3E8F0),
    onSurfaceVariant = Color(0xFF424A55),
    outline = Color(0xFF737B86),
    outlineVariant = Color(0xFFD0D6DF)
)

private val NotesTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = noteFontFamily("space_grotesk"),
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = noteFontFamily("space_grotesk"),
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = noteFontFamily("space_grotesk"),
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = noteFontFamily("space_grotesk"),
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = noteFontFamily("space_grotesk"),
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = noteFontFamily("space_grotesk"),
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
        fontFamily = noteFontFamily("space_grotesk"),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = noteFontFamily("space_grotesk"),
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

private val NotesShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

@Composable
fun NotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> NotesDarkScheme
        else -> NotesLightScheme
    }
    val colorScheme = if (darkTheme) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color(0xFF0B0F14),
            surfaceVariant = Color(0xFF151A20),
            onBackground = Color(0xFFE8EAED),
            onSurface = Color(0xFFE8EAED)
        )
    } else {
        baseScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NotesTypography,
        shapes = NotesShapes,
        content = content
    )
}
