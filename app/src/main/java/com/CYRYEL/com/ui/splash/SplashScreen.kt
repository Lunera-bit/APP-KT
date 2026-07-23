package com.CYRYEL.com.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onReady: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF08126B)
) {
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.Asset("splash_animation.json"))
    val composition = compositionResult.value

    LaunchedEffect(composition) {
        if (composition != null) {
            delay(1200)
            onReady()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            modifier = Modifier.size(286.dp),
            iterations = 1
        )
    }
}
