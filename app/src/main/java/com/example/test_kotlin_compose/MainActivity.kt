package com.example.test_kotlin_compose


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.test_kotlin_compose.integration.adManager.NativeAdManagerImpl
import com.example.test_kotlin_compose.navigation.Home
import com.example.test_kotlin_compose.navigation.Language
import com.example.test_kotlin_compose.navigation.Onboard
import com.example.test_kotlin_compose.navigation.Splash
import com.example.test_kotlin_compose.ui.home.HomeScreen
import com.example.test_kotlin_compose.ui.language.LanguageScreen
import com.example.test_kotlin_compose.ui.onboard.OnboardScreen
import com.example.test_kotlin_compose.ui.splash.SplashScreen
import com.example.test_kotlin_compose.ui.theme.Test_kotlin_composeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            Test_kotlin_composeTheme {
                TestKotlinComposeApp()
            }
        }
    }
}

@Composable
fun TestKotlinComposeApp() {
    val navController = rememberNavController()
    Test_kotlin_composeTheme {
        val currentBackStack by navController.currentBackStackEntryAsState()
        val currentDestination = currentBackStack?.destination
        Scaffold(
            topBar = {

            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Splash.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(route = Splash.route) {
                    SplashScreen(
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }
                composable(route = Home.route) {
                    HomeScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateForward = { }
                    )
                }
                composable(route = Language.route) {
                    LanguageScreen(
                        onNavigateBack = { },
                        onNavigateForward = { navController.navigate(Onboard.route) }
                    )
                }
                composable(route = Onboard.route) {
                    OnboardScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateForward = { navController.navigate(Home.route) }
                    )
                }
            }
        }
    }
}

fun NavHostController.navigateSingleTopTo(route: String) =
    this.navigate(route) {
        popUpTo(
            this@navigateSingleTopTo.graph.findStartDestination().id
        ) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
