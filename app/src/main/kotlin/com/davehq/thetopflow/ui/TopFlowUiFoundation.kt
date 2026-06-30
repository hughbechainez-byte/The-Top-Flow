package com.davehq.thetopflow.ui

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TopFlowDarkScheme = darkColorScheme(
    primary = Color(0xFF5AD7A0),
    onPrimary = Color(0xFF06130E),
    secondary = Color(0xFF8FA2FF),
    onSecondary = Color(0xFF0B1024),
    tertiary = Color(0xFFFFC469),
    background = Color(0xFF05070D),
    onBackground = Color(0xFFF7F8FF),
    surface = Color(0xFF151A33),
    onSurface = Color(0xFFF7F8FF),
    surfaceVariant = Color(0xFF252B4D),
    onSurfaceVariant = Color(0xFFB9C0DA),
    outline = Color(0xFF3E4568)
)

@Immutable
data class StudioPalette(
    val oled: Color = Color(0xFF03050A),
    val deepIndigo: Color = Color(0xFF2D3458),
    val panel: Color = Color(0xFF343B64),
    val raised: Color = Color(0xFF444C80),
    val mint: Color = Color(0xFF5AD7A0),
    val cyan: Color = Color(0xFF62C7FF),
    val amber: Color = Color(0xFFFFC469),
    val coral: Color = Color(0xFFFF6658),
    val textStrong: Color = Color(0xFFF8F9FF),
    val textSoft: Color = Color(0xFFB8C0DA),
    val hairline: Color = Color(0x334DDBFF)
)

object TopFlowMotion {
    const val Fast = 120
    const val Standard = 220
    const val Emphasis = 320
}

object TopFlowShape {
    val Panel = RoundedCornerShape(26.dp)
    val Card = RoundedCornerShape(18.dp)
    val Control = RoundedCornerShape(16.dp)
    val Pill = RoundedCornerShape(999.dp)
}

object TopFlowType {
    val Title = TextStyle(fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold)
    val Section = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val Body = TextStyle(fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal)
    val Label = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    val Caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
}

@Composable
fun TopFlowTheme(
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        TopFlowDarkScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun StudioBackdrop(
    modifier: Modifier = Modifier,
    palette: StudioPalette = StudioPalette()
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.oled)
    )
}

@Composable
fun StudioScreen(
    title: String,
    subtitle: String,
    dockItems: List<StudioDockItem>,
    modifier: Modifier = Modifier,
    onMenu: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val palette = StudioPalette()
    Box(modifier = modifier.fillMaxSize()) {
        StudioBackdrop(palette = palette)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            StudioTopBar(title = title, subtitle = subtitle, onMenu = onMenu)
            Spacer(Modifier.height(10.dp))
            StudioDock(items = dockItems)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.weight(1f)) {
                content(PaddingValues(0.dp))
            }
        }
    }
}

@Composable
fun StudioTopBar(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onMenu: () -> Unit = {}
) {
    FloatingPanel(modifier = modifier.fillMaxWidth(), radius = 28.dp, elevation = 10.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = TopFlowType.Title, color = StudioPalette().textStrong, maxLines = 1)
                Text(subtitle, style = TopFlowType.Caption, color = StudioPalette().textSoft, maxLines = 1)
            }
            StudioCommand(label = "Menu", selected = false, onClick = onMenu)
        }
    }
}

@Composable
fun StudioDock(
    items: List<StudioDockItem>,
    modifier: Modifier = Modifier
) {
    FloatingPanel(modifier = modifier.fillMaxWidth(), radius = 999.dp, elevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                StudioCommand(
                    label = item.label,
                    selected = item.selected,
                    modifier = Modifier.weight(1f),
                    onClick = item.onClick
                )
            }
        }
    }
}

@Composable
fun FloatingPanel(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    elevation: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val palette = StudioPalette()
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(radius),
        colors = CardDefaults.cardColors(containerColor = palette.panel.copy(alpha = 0.82f)),
        border = BorderStroke(1.dp, palette.hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        content()
    }
}

@Composable
fun StudioCommand(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val palette = StudioPalette()
    Surface(
        modifier = modifier
            .height(52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = TopFlowShape.Pill,
        color = if (selected) palette.mint else palette.oled.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, if (selected) palette.mint else palette.hairline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                style = TopFlowType.Label,
                color = if (selected) Color(0xFF06130E) else palette.textStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RhymeSuggestionStrip(
    suggestions: List<String>,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onSuggestion: (String) -> Unit = {}
) {
    FloatingPanel(modifier = modifier.fillMaxWidth(), radius = 22.dp, elevation = 12.dp) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = if (loading) "Loading rhymes" else "Rhyme engine",
                style = TopFlowType.Caption,
                color = StudioPalette().amber
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (loading) {
                    item { RhymePill("Preparing index", enabled = false) }
                } else {
                    items(suggestions) { word ->
                        RhymePill(word, onClick = { onSuggestion(word) })
                    }
                }
            }
        }
    }
}

@Composable
fun RhymePill(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val palette = StudioPalette()
    Surface(
        modifier = Modifier
            .height(40.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = TopFlowShape.Pill,
        color = if (enabled) palette.raised else palette.raised.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, if (enabled) palette.mint.copy(alpha = 0.72f) else palette.hairline)
    ) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(label, style = TopFlowType.Label, color = palette.textStrong, maxLines = 1)
        }
    }
}

@Composable
fun FontPreviewRow(
    label: String,
    selected: Boolean,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    FloatingPanel(modifier = modifier.fillMaxWidth(), radius = 18.dp, elevation = if (selected) 10.dp else 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = TopFlowType.Label, color = StudioPalette().textStrong)
                Text(
                    "The quick brown flow",
                    style = TopFlowType.Body.copy(fontFamily = fontFamily),
                    color = StudioPalette().textSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(if (selected) StudioPalette().mint else StudioPalette().hairline, CircleShape)
            )
        }
    }
}

@Immutable
data class StudioDockItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit = {}
)

data class FontPreviewSpec(
    val label: String,
    val family: FontFamily
)

val TopFlowFontPreviews = listOf(
    FontPreviewSpec("System", FontFamily.SansSerif),
    FontPreviewSpec("Serif", FontFamily.Serif),
    FontPreviewSpec("Mono", FontFamily.Monospace),
    FontPreviewSpec("Casual", FontFamily.Cursive)
)
