package com.cyryel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.cyryel.navigation.AppNavGraph
import com.cyryel.ui.theme.TiendaCyryelTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SideEffect {
                window.statusBarColor = Color(0xFF1A3A6B).toArgb()
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
            }
            TiendaCyryelTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            }
        }
    }
}
