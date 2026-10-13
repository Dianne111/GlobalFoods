package com.globalfoods.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import globalfoods.sharedui.generated.resources.Res
import globalfoods.sharedui.generated.resources.global_foods_logo
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit = {}) {
    LaunchedEffect(Unit) {
        delay(1500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.global_foods_logo),
            contentDescription = "Global Foods México",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = 250.dp, height = 150.dp)
        )
    }
}
