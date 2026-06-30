package com.cyryel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    secondary = AccentOrange,
    background = BackgroundGray
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueDark,
    secondary = AccentOrange
)

@Composable
fun TiendaCyryelTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
