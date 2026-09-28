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

private sealed interface AppScreen {
    data object Splash : AppScreen
    data object Login : AppScreen
    data object Register : AppScreen
    data object Home : AppScreen
    data class OrderDetail(val orderId: String) : AppScreen
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var currentScreen: AppScreen by remember { mutableStateOf(AppScreen.Splash) }

        LaunchedEffect(Unit) {
            delay(1800)
            currentScreen = AppScreen.Login
        }

        val screen = currentScreen
        when (screen) {
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
                DashboardScreen(
                    onOrderSelected = { orderId ->
                        currentScreen = AppScreen.OrderDetail(orderId)
                    }
                )
            }
            is AppScreen.OrderDetail -> {
                OrderDetailScreen(
                    orderId = screen.orderId,
                    onBack = { currentScreen = AppScreen.Home }
                )
            }
        }
    }
}