package com.example.test_kotlin_compose.ui.language

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.test_kotlin_compose.integration.adComponent.BannerAdComposable
import com.example.test_kotlin_compose.integration.adComponent.NativeAdComposable
import com.example.test_kotlin_compose.integration.adManager. RewardAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.AdUnitName
import com.example.test_kotlin_compose.integration.adManager.BannerAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.InterstialAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.NativeAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.OpenAdManagerImpl
import com.example.test_kotlin_compose.ui.component.MyAppBar

@Composable
fun LanguageScreen(
    viewModel: LanguageViewModel = hiltViewModel(),
    onNavigateBack: (() -> Unit)? = null,
    onNavigateForward: (() -> Unit)? = null
) {
    val nativeAdManager: NativeAdManagerImpl = viewModel.nativeAdManager
    val interstitialAdManager: InterstialAdManagerImpl = viewModel.interstitialAdManager
    val openAdManager: OpenAdManagerImpl = viewModel.openAdManager
    val bannerAdManager: BannerAdManagerImpl = viewModel.bannerAdManager
    val rewardAdManager: RewardAdManagerImpl = viewModel.rewardAdManager
    val scrollState = rememberScrollState()
    val overscrollEffect = rememberOverscrollEffect()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MyAppBar(
                title = "Language Screen",
                onBackClick = onNavigateBack,
                onForwardClick = onNavigateForward
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)

            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .overscroll(overscrollEffect)
                    ) {
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    interstitialAdManager.preloadAd(adUnitName = AdUnitName.convertInterstitial)
                                }
                            }
                        ) {
                            Text("Preload Intersitial Ad")
                        }
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    interstitialAdManager.show(
                                        activity,
                                        adUnitName = AdUnitName.convertInterstitial,
                                        callback = null,
                                        null,
                                        null,
                                        null,
                                        false
                                    )
                                }
                            }
                        ) {
                            Text("Show Intersitial Ad")
                        }
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    interstitialAdManager.loadAdAndShow(
                                        activity,
                                        adUnitName = AdUnitName.convertInterstitial,
                                        callback = null,
                                        null,
                                        5000,
                                        null
                                    )
                                }
                            }
                        ) {
                            Text("Load And Show Intersitial Ad")
                        }
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    openAdManager.preloadAd(AdUnitName.globalOpen)
                                }
                            }
                        ) {
                            Text("Preload Open Ad")
                        }
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    openAdManager.showAdIfAvailable(
                                        activity,
                                        AdUnitName.globalOpen,
                                        {}
                                    )
                                }
                            }
                        ) {
                            Text("Show Open Ad")
                        }
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    openAdManager.loadAdAndShow(
                                        activity,
                                        AdUnitName.globalOpen,
                                        null,
                                        null,
                                        5000

                                    )
                                }
                            }
                        ) {
                            Text("Load And Show Open Ad")
                        }
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    rewardAdManager.loadAd(AdUnitName.resultReward)
                                }
                            }
                        ) {
                            Text("Preload Reward Ad")
                        }
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    rewardAdManager.showAd(
                                        activity,
                                        AdUnitName.resultReward,
                                        onUserEarnedReward = { reward ->
                                            // Handle reward
                                        }
                                    )
                                }
                            }
                        ) {
                            Text("Show Reward Ad")
                        }
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    rewardAdManager.loadAdAndShow(
                                        activity,
                                        AdUnitName.resultReward,
                                        onUserEarnedReward = { reward ->
                                            // Handle reward
                                        },
                                        duration = 5000
                                    )
                                }
                            }
                        ) {
                            Text("Load And Show Reward Ad")
                        }
                    }
                }

                // 4. The Ad stays at the bottom naturally
                NativeAdComposable(
                    adUnitName = AdUnitName.languageNative,
                    factoryId = "adFactoryLanguage",
                    manager = nativeAdManager,
                    modifier = Modifier,
                    showLoadingCard = true,
                    keepSize = false,
                    autoLoad = true,
                    highFloor = true
                )
                Box(
                    Modifier
                        .height(20.dp)
                        .fillMaxWidth()
                ) {
                }
                BannerAdComposable(
                    adUnitName = AdUnitName.languageBanner,
                    showLoadingCard = true,
                    keepSize = false,
                    retryNumber = 3,
                    adManager = bannerAdManager
                )
            }
        }
    )
}
