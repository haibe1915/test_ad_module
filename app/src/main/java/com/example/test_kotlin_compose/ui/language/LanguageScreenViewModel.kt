package com.example.test_kotlin_compose.ui.language

import androidx.lifecycle.ViewModel
import com.example.test_kotlin_compose.integration.adManager.AdClient
import com.example.test_kotlin_compose.integration.adManager.impl.RewardAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.impl.BannerAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.impl.InterstialAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.impl.NativeAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.impl.OpenAdManagerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    val nativeAdManager: NativeAdManagerImpl,
    val interstitialAdManager: InterstialAdManagerImpl,
    val openAdManager: OpenAdManagerImpl,
    val bannerAdManager: BannerAdManagerImpl,
    val rewardAdManager: RewardAdManagerImpl,
    val adClient: AdClient
) : ViewModel() {

}