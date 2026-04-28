package com.notp9194bot.jnotes.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

data class NotePalette(val name: String, val light: Color, val dark: Color)

/**
 * Vibrant, modern note tints. Light tones are airy and cheerful; dark tones
 * are deeply saturated so notes feel distinct on AMOLED screens.
 */
val NoteColors: List<NotePalette> = listOf(
    NotePalette("Default", Color(0xFFFFFFFF), Color(0xFF1B1B25)),
    NotePalette("Sunshine", Color(0xFFFFF1B8), Color(0xFF3A2E10)),
    NotePalette("Mint", Color(0xFFCBF3DC), Color(0xFF14352A)),
    NotePalette("Ocean", Color(0xFFC5E4FB), Color(0xFF0D2E48)),
    NotePalette("Coral", Color(0xFFFFD2D2), Color(0xFF3F1A20)),
    NotePalette("Lilac", Color(0xFFE6D5FF), Color(0xFF26173F)),
    NotePalette("Tangerine", Color(0xFFFFD8B5), Color(0xFF3A2110)),
    NotePalette("Lime", Color(0xFFE0F7B0), Color(0xFF233010)),
    NotePalette("Pink Pop", Color(0xFFFFC8E5), Color(0xFF3A1029)),
    NotePalette("Aqua", Color(0xFFB6F1F0), Color(0xFF0F2F2F)),
)

@Composable
@ReadOnlyComposable
fun noteColor(idx: Int, isDark: Boolean): Color {
    val p = NoteColors[idx.coerceIn(0, NoteColors.lastIndex)]
    return if (isDark) p.dark else p.light
}
