package com.example.test_kotlin_compose.integration.init.impl

import android.app.Activity
import android.content.Context
import com.example.test_kotlin_compose.integration.adManager.AdClient
import com.example.test_kotlin_compose.integration.adManager.api.CompositeAdManager
import com.example.test_kotlin_compose.integration.adManager.impl.BannerAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.impl.InterstialAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.impl.NativeAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.impl.OpenAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.impl.RewardAdManagerImpl
import com.example.test_kotlin_compose.integration.config.bannerAdsPositionKeys
import com.example.test_kotlin_compose.integration.config.interstitialAdsPositionKeys
import com.example.test_kotlin_compose.integration.config.nativeAdsPositionKeys
import com.example.test_kotlin_compose.integration.config.openAdsPositionKeys
import com.example.test_kotlin_compose.integration.config.rewardAdsPositionKeys
import com.example.test_kotlin_compose.integration.init.api.AdsInitializer
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdsInitializerImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val adClient: AdClient,
    private val adInterstitialManager: InterstialAdManagerImpl,
    private val adOpenManager: OpenAdManagerImpl,
    private val adRewardManager: RewardAdManagerImpl,
    private val adBannerManager: BannerAdManagerImpl,
    private val adNativeManager: NativeAdManagerImpl,
) : AdsInitializer {

    private var initialized = false

    private suspend fun checkConsent(activity: Activity): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val params = ConsentRequestParameters.Builder().build()
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

            consentInformation.requestConsentInfoUpdate(activity, params, {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    continuation.resumeWith(Result.success(true))
                }
            }, {
                continuation.resumeWith(Result.success(false))
            })
        }
    }

    override suspend fun initialize(
        activity: Activity,
        externalScope: CoroutineScope,
        nativeAdsKeys: List<String>,
        bannerAdsKeys: List<String>,
        interstitialAdsKeys: List<String>,
        rewardAdsKeys: List<String>,
        openAdsKeys: List<String>
    ) {
        if (initialized) return
        initialized = true

        externalScope.launch {
            nativeAdsPositionKeys = nativeAdsKeys
            bannerAdsPositionKeys = bannerAdsKeys
            interstitialAdsPositionKeys = interstitialAdsKeys
            rewardAdsPositionKeys = rewardAdsKeys
            openAdsPositionKeys = openAdsKeys

            val remoteConfigJob = async { adClient.reloadFromRemoteConfig() }
            val cmpJob = async { checkConsent(activity) }

            awaitAll(remoteConfigJob, cmpJob)

            MobileAds.initialize(appContext)

            CompositeAdManager(
                listOf(
                    adNativeManager,
                    adInterstitialManager,
                    adOpenManager,
                    adRewardManager,
                    adBannerManager,
                )
            ).init(appContext)
        }

    }
}