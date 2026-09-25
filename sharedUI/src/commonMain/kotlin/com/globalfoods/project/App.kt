package com.globalfoods.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay

private enum class AppScreen {
    Splash,
    Login,
    Register,
    Home
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf(AppScreen.Splash) }

        LaunchedEffect(Unit) {
            delay(1800)
            currentScreen = AppScreen.Login
        }

        when (currentScreen) {
            AppScreen.Splash -> SplashScreen()
            AppScreen.Login -> {
                LoginScreen(
                    onNavigateToRegister = { currentScreen = AppScreen.Register },
                    onLoginSuccess = { currentScreen = AppScreen.Home }
                )
            }
            AppScreen.Register -> {
                RegisterScreen(
                    onNavigateToLogin = { currentScreen = AppScreen.Login },
                    onRegisterClick = { _, _, _, _, _ ->
                        currentScreen = AppScreen.Login
                    }
                )
            }
            AppScreen.Home -> {
                DashboardScreen()
            }
        }
    }
}