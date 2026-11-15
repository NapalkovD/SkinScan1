package com.skinscan.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF00897B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4DB6AC),
    tertiary = Color(0xFF3949AB),
    background = Color(0xFFF4F9F8),
    surface = Color.White,
    surfaceVariant = Color(0xFFE3F1EF),
    onSurfaceVariant = Color(0xFF3F4948)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DB6AC),
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFF80CBC4),
    tertiary = Color(0xFF9FA8DA),
    background = Color(0xFF0F1514),
    surface = Color(0xFF162120),
    surfaceVariant = Color(0xFF1F2F2D)
)

@Composable
fun SkinScanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}

val SeverityLow = Color(0xFF2E7D32)
val SeverityMedium = Color(0xFFEF6C00)
val SeverityHigh = Color(0xFFC62828)

fun severityColor(s: String) = when (s) {
    "high" -> SeverityHigh
    "medium" -> SeverityMedium
    else -> SeverityLow
}

fun severityLabel(s: String) = when (s) {
    "high" -> "Высокая срочность"
    "medium" -> "Средняя срочность"
    else -> "Низкая срочность"
}
