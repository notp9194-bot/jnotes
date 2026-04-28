package com.notp9194bot.jnotes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val sans = FontFamily.SansSerif

val JNotesTypography = Typography(
    displayLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold,    fontSize = 36.sp, lineHeight = 44.sp),
    displayMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Bold,   fontSize = 28.sp, lineHeight = 36.sp),
    displaySmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    headlineMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    headlineSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium,  fontSize = 18.sp, lineHeight = 24.sp),
    titleLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold,   fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium,    fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium,     fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal,      fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal,     fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal,      fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium,     fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium,    fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium,     fontSize = 11.sp, lineHeight = 14.sp),
)
