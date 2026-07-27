import re

file_path = "/app/applet/app/src/main/java/com/example/ui/theme/Theme.kt"
with open(file_path, "r") as f:
    content = f.read()

new_content = """package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = Color.White,
    secondary = PrimaryBlueDark,
    onSecondary = Color.White,
    tertiary = SuccessColor,
    onTertiary = Color.White,
    error = ErrorColor,
    onError = Color.White,
    background = DarkBackground,
    onBackground = TextHighContrastDark,
    surface = DarkSurface,
    onSurface = TextHighContrastDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextMutedDark,
    outline = BorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = PrimaryBlue,
    onSecondary = Color.White,
    tertiary = SuccessColor,
    onTertiary = Color.White,
    error = ErrorColor,
    onError = Color.White,
    background = LightBackground,
    onBackground = TextHighContrastLight,
    surface = LightSurface,
    onSurface = TextHighContrastLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextMutedLight,
    outline = BorderLight
)

@Composable
fun MyApplicationTheme(
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
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
"""
with open(file_path, "w") as f:
    f.write(new_content)
