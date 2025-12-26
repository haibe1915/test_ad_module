package com.example.test_kotlin_compose.integration.init.api

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.CoroutineScope

/**
 * Public entry point to initialize the Google Mobile Ads SDK and all ad managers.
 *
 * Host apps should call this once (usually after Remote Config fetch/activate).
 */
interface AdsInitializer {
    /**
     * Idempotent init.
     */
    suspend fun initialize(
        activity: Activity,externalScope: CoroutineScope,
        nativeAdsKeys: List<String>,
        bannerAdsKeys: List<String>,
        interstitialAdsKeys: List<String>,
        rewardAdsKeys: List<String>,
        openAdsKeys: List<String>

    )

}