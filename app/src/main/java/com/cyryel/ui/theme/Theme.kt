package com.cyryel.ui.theme

import androidx.compose.material3.MaterialTheme
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

@Composable
fun TiendaCyryelTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}
