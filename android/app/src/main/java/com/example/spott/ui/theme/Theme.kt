package com.example.spott.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = SpottTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC1F2E5),
    onPrimaryContainer = SpottInk,
    secondary = Color(0xFF4A635D),
    secondaryContainer = Color(0xFFDCECE5),
    tertiary = Color(0xFF795900),
    tertiaryContainer = SpottAmber,
    onTertiaryContainer = Color(0xFF302300),
    background = Color(0xFFF6F9F6),
    surface = Color(0xFFF6F9F6)
)

private val DarkColorScheme = darkColorScheme(
    primary = SpottMint,
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFFC1F2E5),
    secondary = Color(0xFFB3CCC3),
    secondaryContainer = Color(0xFF334C44),
    tertiary = SpottAmber,
    tertiaryContainer = Color(0xFF594300),
    onTertiaryContainer = Color(0xFFFFE4A1),
    background = Color(0xFF101917),
    surface = Color(0xFF101917)
)

@Composable
fun SpottTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
