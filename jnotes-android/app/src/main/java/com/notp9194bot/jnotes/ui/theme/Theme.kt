package com.notp9194bot.jnotes.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.notp9194bot.jnotes.data.model.Settings
import com.notp9194bot.jnotes.data.model.ThemeMode

@Composable
fun JNotesTheme(
    settings: Settings,
    content: @Composable () -> Unit,
) {
    val isDark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val accentColor: Color = settings.customAccentArgb
        ?.let { Color(it) }
        ?: Accents[settings.accentIdx.coerceIn(0, Accents.lastIndex)].seed

    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val baseScheme = if (settings.dynamicColor && supportsDynamic && settings.customAccentArgb == null) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (isDark) darkSchemeFor(accentColor, settings.amoled) else lightSchemeFor(accentColor)
    }

    val scheme = if (isDark && settings.amoled && settings.dynamicColor && supportsDynamic) {
        // override dynamic dark with pure black backgrounds for AMOLED
        baseScheme.copy(background = Color.Black, surface = Color.Black)
    } else baseScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
            scheme.background.toArgb()
        }
    }

    val baseDensity = LocalDensity.current
    val scaled = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale * settings.fontScale.coerceIn(0.7f, 1.6f),
    )

    CompositionLocalProvider(LocalDensity provides scaled) {
        MaterialTheme(
            colorScheme = scheme,
            typography = JNotesTypography,
            content = content,
        )
    }
}
