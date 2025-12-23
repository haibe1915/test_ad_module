package com.example.test_kotlin_compose.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.test_kotlin_compose.ui.home.HomeScreen
import com.example.test_kotlin_compose.ui.language.LanguageScreen
import com.example.test_kotlin_compose.ui.onboard.OnboardScreen
import com.example.test_kotlin_compose.ui.splash.SplashScreen

interface AppDestination {
    val icon: ImageVector
    val route: String
    val screen: @Composable () -> Unit
}

/**
 * Rally app navigation destinations
 */
object Home : AppDestination {
    override val icon = Icons.Filled.PieChart
    override val route = "home"
    override val screen: @Composable () -> Unit = { HomeScreen() }
}

object Language : AppDestination {
    override val icon = Icons.Filled.AttachMoney
    override val route = "language"
    override val screen: @Composable () -> Unit = { LanguageScreen() }
}

object Onboard : AppDestination {
    override val icon = Icons.Filled.MoneyOff
    override val route = "onboard"
    override val screen: @Composable () -> Unit = { OnboardScreen() }
}

object Splash : AppDestination {
    override val icon = Icons.Filled.MoneyOff
    override val route = "splash"
    override val screen: @Composable () -> Unit = { SplashScreen(
        onNavigate = {}
    ) }
}