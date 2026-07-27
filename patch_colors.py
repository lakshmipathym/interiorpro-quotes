import re

file_path = "/app/applet/app/src/main/java/com/example/ui/theme/Color.kt"
with open(file_path, "r") as f:
    content = f.read()

new_content = """package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// --- Modern Premium Theme Colors ---

// Primary Accent
val PrimaryBlue = Color(0xFF2563EB)
val PrimaryBlueDark = Color(0xFF3B82F6)

// Dark Theme Backgrounds (Deep Dark)
val DarkBackground = Color(0xFF09090B)
val DarkSurface = Color(0xFF18181B)
val DarkSurfaceElevated = Color(0xFF27272A)

// Light Theme Backgrounds
val LightBackground = Color(0xFFFAFAFA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF4F4F5)

// Status Colors
val SuccessColor = Color(0xFF10B981)
val ErrorColor = Color(0xFFEF4444)
val WarningColor = Color(0xFFF59E0B)

// Text Colors
val TextHighContrastLight = Color(0xFF09090B)
val TextMutedLight = Color(0xFF71717A)

val TextHighContrastDark = Color(0xFFFAFAFA)
val TextMutedDark = Color(0xFFA1A1AA)

// Outline/Border
val BorderLight = Color(0xFFE4E4E7)
val BorderDark = Color(0xFF27272A)
"""
with open(file_path, "w") as f:
    f.write(new_content)
