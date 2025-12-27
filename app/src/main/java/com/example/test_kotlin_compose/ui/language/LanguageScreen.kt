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
import com.example.test_kotlin_compose.integration.adComponent.NativeAdComposable
import com.example.test_kotlin_compose.integration.adManager.impl.RewardAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.impl.BannerAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.impl.InterstialAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.impl.NativeAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.impl.OpenAdManagerImpl
import com.example.test_kotlin_compose.ui.component.MyAppBar
import com.example.test_kotlin_compose.util.AdUnitKeys

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
                                    interstitialAdManager.preloadAd(adUnitKey = AdUnitKeys.InterCommon)
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
                                        adUnitKey = AdUnitKeys.InterCommon,
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
                                        adUnitKey = AdUnitKeys.InterCommon,
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
                                    openAdManager.preloadAd(adUnitKey = AdUnitKeys.AppOpenOpenApp)
                                }
                            }
                        ) {
                            Text("Preload Open Ad")
                        }
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    openAdManager.showAd(
                                        activity,
                                        adUnitKey = AdUnitKeys.AppOpenOpenApp,
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
                                        adUnitKey = AdUnitKeys.AppOpenOpenApp,
                                        null,
                                        null,
                                        5000

                                    )
                                }
                            }
                        ) {
                            Text("Load And Show Open Ad")
                        }
//                        Button(
//                            onClick = {
//                                val activity = context as? Activity
//                                if (activity != null) {
//                                    rewardAdManager.preloadAd(adUnitKey = AdUnitKeys.ResultReward)
//                                }
//                            }
//                        ) {
//                            Text("Preload Reward Ad")
//                        }
//                        Button(
//                            onClick = {
//                                val activity = context as? Activity
//                                if (activity != null) {
//                                    rewardAdManager.showAd(
//                                        activity,
//                                        adUnitKey = AdUnitKeys.ResultReward,
//                                        onUserEarnedReward = { reward ->
//                                            // Handle reward
//                                        }
//                                    )
//                                }
//                            }
//                        ) {
//                            Text("Show Reward Ad")
//                        }
//                        Button(
//                            onClick = {
//                                val activity = context as? Activity
//                                if (activity != null) {
//                                    rewardAdManager.loadAdAndShow(
//                                        activity,
//                                        adUnitKey = AdUnitKeys.ResultReward,
//                                        onUserEarnedReward = { reward ->
//                                            // Handle reward
//                                        },
//                                        duration = 5000
//                                    )
//                                }
//                            }
//                        ) {
//                            Text("Load And Show Reward Ad")
//                        }
                    }
                }

                NativeAdComposable(
                    adUnitKey = AdUnitKeys.NativeCommon,
                    factoryId = "adFactoryLanguage",
                    manager = nativeAdManager,
                    adClient = viewModel.adClient,
                    modifier = Modifier,
                    autoLoad = true,
                    highFloor = true
                )
                Box(
                    Modifier
                        .height(20.dp)
                        .fillMaxWidth()
                ) {
                }
//                BannerAdComposable(
//                    adUnitKey = AdUnitKeys.LanguageBanner,
//                    adClient = viewModel.adClient,
//                    showLoadingCard = true,
//                    keepSize = false,
//                    retryNumber = 3,
//                    adManager = bannerAdManager
//                )
            }
        }
    )
}
