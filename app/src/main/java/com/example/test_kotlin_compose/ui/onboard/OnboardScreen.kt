package com.example.test_kotlin_compose.ui.onboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.test_kotlin_compose.integration.adComponent.NativeAdComposable
import com.example.test_kotlin_compose.ui.component.MyAppBar
import com.example.test_kotlin_compose.ui.home.HomeViewModel
import com.example.test_kotlin_compose.util.AdUnitKeys

@Composable
fun OnboardScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateBack: (() -> Unit)? = null,
    onNavigateForward: (() -> Unit)? = null
) {
    val nativeAdManager = viewModel.nativeAdManager
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MyAppBar(
                title = "Onboard Screen",
                onBackClick = onNavigateBack,
                onForwardClick = onNavigateForward
            )
        },
        content = { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NativeAdComposable(
                    adUnitKey = AdUnitKeys.NativeHistoryScreen,
                    factoryId = "adFactoryHistoryItem",
                    manager = nativeAdManager,
                    adClient = viewModel.adClient,
                    modifier = Modifier,
                    autoLoad = true,
                    highFloor = true,
                )
            }
        }
    )
}