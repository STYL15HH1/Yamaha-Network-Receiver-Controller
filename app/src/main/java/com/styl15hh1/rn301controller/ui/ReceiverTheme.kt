package com.styl15hh1.rn301controller.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Shared audio-controller palette; appearance preference remains authoritative. */
@Composable
internal fun ReceiverTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) darkColorScheme(
        primary = Color(0xFF89DEC6), onPrimary = Color(0xFF06382D),
        primaryContainer = Color(0xFF233F38), onPrimaryContainer = Color(0xFFB4F3DF),
        secondary = Color(0xFFA7C7BE), onSecondary = Color(0xFF17372F),
        secondaryContainer = Color(0xFF2A403A), onSecondaryContainer = Color(0xFFD0E9E1),
        tertiary = Color(0xFF89CEC8), tertiaryContainer = Color(0xFF244340),
        background = Color(0xFF101414), onBackground = Color(0xFFE6EDEC),
        surface = Color(0xFF171C1C), onSurface = Color(0xFFE6EDEC),
        surfaceVariant = Color(0xFF303A39), onSurfaceVariant = Color(0xFFAAB9B5),
        surfaceContainerLowest = Color(0xFF0C1010), surfaceContainerLow = Color(0xFF191F1E),
        surfaceContainer = Color(0xFF202726), surfaceContainerHigh = Color(0xFF28302F),
        surfaceContainerHighest = Color(0xFF323C39),
        outline = Color(0xFF64756E), outlineVariant = Color(0xFF34413C)
    ) else lightColorScheme(
        primary = Color(0xFF146B55), onPrimary = Color.White,
        primaryContainer = Color(0xFFD5EEE4), onPrimaryContainer = Color(0xFF154F3D),
        secondary = Color(0xFF426659), onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0EDE6), onSecondaryContainer = Color(0xFF244C3E),
        tertiary = Color(0xFF326B65), tertiaryContainer = Color(0xFFD7EDE9),
        background = Color(0xFFF3F6F4), onBackground = Color(0xFF18211D),
        surface = Color(0xFFFAFCFA), onSurface = Color(0xFF18211D),
        surfaceVariant = Color(0xFFE2E9E4), onSurfaceVariant = Color(0xFF50625A),
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF4F8F5),
        surfaceContainer = Color(0xFFEDF2EE), surfaceContainerHigh = Color(0xFFE5ECE6),
        surfaceContainerHighest = Color(0xFFDAE4DD),
        outline = Color(0xFF788A80), outlineVariant = Color(0xFFCCD9D0)
    )
    val base = Typography()
    MaterialTheme(colorScheme = colors,
        shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(22.dp), large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp)),
        typography = base.copy(
            headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Light)
        ), content = content)
}
