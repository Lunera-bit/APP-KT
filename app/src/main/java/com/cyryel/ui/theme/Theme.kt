package com.cyryel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = AzulRey,
    onPrimary = Blanco,
    primaryContainer = AzulReyClaro,
    secondary = AmarilloVibrante,
    onSecondary = AzulRey,
    background = Blanco,
    onBackground = AzulOscuroTexto,
    surface = Blanco,
    onSurface = AzulOscuroTexto,
    surfaceVariant = GrisClaro,
    onSurfaceVariant = GrisTexto,
    error = RojoBadge
)

private val DarkColors = darkColorScheme(
    primary = AzulReyClaro,
    onPrimary = Blanco,
    primaryContainer = AzulRey,
    secondary = AmarilloVibrante,
    onSecondary = Blanco,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E2E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2D2D3A),
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = RojoBadge
)

@Composable
fun TiendaCyryelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
