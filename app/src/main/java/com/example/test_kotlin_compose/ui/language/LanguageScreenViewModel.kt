package com.example.test_kotlin_compose.ui.language

import androidx.lifecycle.ViewModel
import com.example.test_kotlin_compose.integration.adManager.RewardAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.BannerAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.InterstialAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.NativeAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.OpenAdManagerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    val nativeAdManager: NativeAdManagerImpl,
    val interstitialAdManager: InterstialAdManagerImpl,
    val openAdManager: OpenAdManagerImpl,
    val bannerAdManager: BannerAdManagerImpl,
    val rewardAdManager: RewardAdManagerImpl
) : ViewModel() {

}