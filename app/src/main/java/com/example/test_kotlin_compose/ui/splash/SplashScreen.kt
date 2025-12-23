package com.example.test_kotlin_compose.ui.splash

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.test_kotlin_compose.navigation.Home
import com.example.test_kotlin_compose.navigation.Language
import com.example.test_kotlin_compose.navigation.Onboard
import com.example.test_kotlin_compose.navigation.Splash

@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val destination by viewModel.destination.collectAsState()

    // Trigger Init only once
    LaunchedEffect(Unit) {
        val activity = context as? Activity
        if (activity != null) {
            viewModel.initApp(activity)
        }
    }

    // Handle Navigation Events
    LaunchedEffect(destination) {
        when (destination) {
            Home -> onNavigate("home")
            Onboard -> onNavigate("onboarding")
            Language -> onNavigate("language")
            Splash -> Unit
        }
    }

    if (viewModel.shouldRequestNotificationPermission.collectAsState().value) {
        NotificationPermissionRequester {
            val activity = context as? Activity
            if (activity != null) {
                viewModel.onPermissionResult(activity)
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
//        Image(
//            painter = painterResource(id = R.drawable.splash_background), // Add your asset
//            contentDescription = null,
//            modifier = Modifier.fillMaxSize(),
//            contentScale = ContentScale.Crop
//        )

        // Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Loading Text
            Text(
                text = "Loading...",
                color = Color.Black, // Or StaticVariable.textColor
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .height(10.dp),
                color = Color.Green, // Or StaticVariable.mainColor
                trackColor = Color.Green.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-text
            Text(
                text = "(Please wait a moment)",
                color = Color.Black,
                fontSize = 14.sp
            )
        }
    }
}