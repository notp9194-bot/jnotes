package com.notp9194bot.jnotesadmin.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Purple = Color(0xFF7C3AED)
private val PurpleDark = Color(0xFFB39DFF)
private val Slate = Color(0xFF0F172A)

private val LightScheme = lightColorScheme(
    primary = Purple,
    secondary = Color(0xFF06B6D4),
    tertiary = Color(0xFFEC4899),
)

private val DarkScheme = darkColorScheme(
    primary = PurpleDark,
    secondary = Color(0xFF22D3EE),
    tertiary = Color(0xFFF472B6),
    background = Slate,
)

@Composable
fun AdminTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
