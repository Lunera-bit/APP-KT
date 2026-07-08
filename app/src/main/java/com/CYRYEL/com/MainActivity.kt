package com.CYRYEL.com

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.CYRYEL.com.navigation.AppNavGraph
import com.CYRYEL.com.ui.splash.ForceUpdateScreen
import com.CYRYEL.com.ui.splash.SplashScreen
import com.CYRYEL.com.ui.theme.TiendaCyryelTheme
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TiendaCyryelTheme {
                MainContent()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
private fun MainContent() {
    var isReady by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }
    var requiresUpdate by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        try {
            val doc = FirebaseFirestore.getInstance().collection("config").document("version").get().await()
            val minVersionCode = doc.getLong("minVersionCode") ?: 0L
            requiresUpdate = BuildConfig.VERSION_CODE < minVersionCode
        } catch (_: Exception) {
            requiresUpdate = false
        }
        delay(2000)
        isReady = true
    }

    if (showSplash) {
        SplashScreen(
            isReady = isReady,
            onReady = { showSplash = false }
        )
    } else if (requiresUpdate == true) {
        ForceUpdateScreen()
    } else {
        Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
            val navController = rememberNavController()
            AppNavGraph(
                navController = navController,
                modifier = Modifier.statusBarsPadding()
            )
        }
    }
}
