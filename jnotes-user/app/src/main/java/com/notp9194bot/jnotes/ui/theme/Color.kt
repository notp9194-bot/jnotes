package com.notp9194bot.jnotes.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class AccentPalette(
    val name: String,
    val seed: Color,
)

/**
 * Vibrant, modern palette. These accents are tuned to be punchy in both light
 * and dark modes — high chroma but not harsh.
 */
val Accents: List<AccentPalette> = listOf(
    AccentPalette("Electric Violet", Color(0xFF7C3AED)),
    AccentPalette("Sunset Coral",   Color(0xFFFF5A5F)),
    AccentPalette("Tropical Teal",  Color(0xFF06B6D4)),
    AccentPalette("Lime Pop",       Color(0xFF84CC16)),
    AccentPalette("Hot Pink",       Color(0xFFEC4899)),
    AccentPalette("Royal Indigo",   Color(0xFF4F46E5)),
    AccentPalette("Mango",          Color(0xFFF59E0B)),
    AccentPalette("Mint",           Color(0xFF10B981)),
    AccentPalette("Sky Blue",       Color(0xFF0EA5E9)),
    AccentPalette("Tangerine",      Color(0xFFF97316)),
    AccentPalette("Magenta",        Color(0xFFD946EF)),
    AccentPalette("Spring Green",   Color(0xFF22C55E)),
)

fun lightSchemeFor(seed: Color) = lightColorScheme(
    primary = seed,
    onPrimary = Color.White,
    primaryContainer = seed.copy(alpha = 0.22f),
    onPrimaryContainer = seed.darken(0.55f),
    secondary = seed.shiftHue().darken(0.05f),
    onSecondary = Color.White,
    secondaryContainer = seed.shiftHue().copy(alpha = 0.16f),
    onSecondaryContainer = seed.shiftHue().darken(0.55f),
    tertiary = seed.shiftHue(140f),
    onTertiary = Color.White,
    tertiaryContainer = seed.shiftHue(140f).copy(alpha = 0.18f),
    onTertiaryContainer = seed.shiftHue(140f).darken(0.55f),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1A1A21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A21),
    surfaceVariant = Color(0xFFF1ECFB),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF7A7585),
    error = Color(0xFFD32F2F),
)

fun darkSchemeFor(seed: Color, amoled: Boolean = false) = darkColorScheme(
    primary = seed.lighten(0.10f),
    onPrimary = Color.Black,
    primaryContainer = seed.copy(alpha = 0.34f),
    onPrimaryContainer = Color.White,
    secondary = seed.shiftHue().lighten(0.18f),
    onSecondary = Color.Black,
    secondaryContainer = seed.shiftHue().copy(alpha = 0.22f),
    onSecondaryContainer = Color.White,
    tertiary = seed.shiftHue(140f).lighten(0.18f),
    onTertiary = Color.Black,
    tertiaryContainer = seed.shiftHue(140f).copy(alpha = 0.22f),
    onTertiaryContainer = Color.White,
    background = if (amoled) Color.Black else Color(0xFF12121A),
    onBackground = Color(0xFFEDE7F6),
    surface = if (amoled) Color.Black else Color(0xFF1B1B25),
    onSurface = Color(0xFFEDE7F6),
    surfaceVariant = if (amoled) Color(0xFF0A0A0A) else Color(0xFF2A2A37),
    onSurfaceVariant = Color(0xFFC9C3DA),
    outline = Color(0xFF7E7A8E),
    error = Color(0xFFFF6B6B),
)

fun Color.darken(fraction: Float): Color = Color(
    red = red * (1 - fraction.coerceIn(0f, 1f)),
    green = green * (1 - fraction.coerceIn(0f, 1f)),
    blue = blue * (1 - fraction.coerceIn(0f, 1f)),
    alpha = alpha,
)

fun Color.lighten(fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = red + (1 - red) * f,
        green = green + (1 - green) * f,
        blue = blue + (1 - blue) * f,
        alpha = alpha,
    )
}

/**
 * Returns a complementary-ish accent by rotating the hue. Used to pick a
 * secondary/tertiary color so the UI is colorful without clashing.
 */
fun Color.shiftHue(degrees: Float = 35f): Color {
    val r = red; val g = green; val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    val v = max
    val s = if (max == 0f) 0f else d / max
    var h = when {
        d == 0f -> 0f
        max == r -> ((g - b) / d) % 6f
        max == g -> (b - r) / d + 2f
        else     -> (r - g) / d + 4f
    } * 60f
    if (h < 0f) h += 360f
    h = (h + degrees + 360f) % 360f

    val c = v * s
    val x = c * (1 - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (rp, gp, bp) = when {
        h < 60f  -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else     -> Triple(c, 0f, x)
    }
    return Color(rp + m, gp + m, bp + m, alpha)
}
