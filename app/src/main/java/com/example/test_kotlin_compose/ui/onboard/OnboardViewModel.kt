package com.example.test_kotlin_compose.ui.onboard

import androidx.lifecycle.ViewModel
import com.example.test_kotlin_compose.integration.adManager.BannerAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.InterstialAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.NativeAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.OpenAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.RewardAdManagerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardViewModel @Inject constructor(
    val nativeAdManager: NativeAdManagerImpl,
    val interstitialAdManager: InterstialAdManagerImpl,
    val openAdManager: OpenAdManagerImpl,
    val bannerAdManager: BannerAdManagerImpl,
    val rewardAdManager: RewardAdManagerImpl
) : ViewModel() {

}