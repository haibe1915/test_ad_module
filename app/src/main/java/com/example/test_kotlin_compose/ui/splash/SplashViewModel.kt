package com.example.test_kotlin_compose.ui.splash

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test_kotlin_compose.integration.adManager.AdUnitName
import com.example.test_kotlin_compose.integration.adManager.BannerAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.CompositeAdManager
import com.example.test_kotlin_compose.integration.adManager.InterstialAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.NativeAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.OpenAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.RewardAdManagerImpl
import com.example.test_kotlin_compose.config.RemoteConfigProvider
import com.example.test_kotlin_compose.navigation.AppDestination
import com.example.test_kotlin_compose.navigation.Home
import com.example.test_kotlin_compose.navigation.Language
import com.example.test_kotlin_compose.navigation.Onboard
import com.example.test_kotlin_compose.navigation.Splash
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val adNativeManager: NativeAdManagerImpl,
    private val adInterstitialManager: InterstialAdManagerImpl,
    private val adOpenManager: OpenAdManagerImpl,
    private val adRewardManager: RewardAdManagerImpl,
    private val adBannerManager: BannerAdManagerImpl,
    private val remoteConfig: RemoteConfigProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _destination = MutableStateFlow<AppDestination>(Splash)
    val destination = _destination.asStateFlow()

    // State to trigger permission request in UI
    private val _shouldRequestNotificationPermission = MutableStateFlow(false)
    val shouldRequestNotificationPermission = _shouldRequestNotificationPermission.asStateFlow()

    private var isMobileAdsInitialized = false

    var adType = 0

    fun initApp(activity: Activity) {
        adType = remoteConfig.getInterOpenApp()
        viewModelScope.launch {
            val remoteConfigJob = async { fetchRemoteConfig() }
            val cmpJob = async { checkConsent(activity) }

            awaitAll(remoteConfigJob, cmpJob)

            if (!isMobileAdsInitialized) {
                MobileAds.initialize(activity) {}
                val allAdsManager = CompositeAdManager(
                    listOf(
                        adNativeManager,
                        adInterstitialManager,
                        adOpenManager,
                        adRewardManager,
                        adBannerManager
                    )
                )
                allAdsManager.init(context)
                isMobileAdsInitialized = true
            }

            startNavigate(activity)
        }
    }

    private suspend fun fetchRemoteConfig() {
        remoteConfig.fetchAndActivate()
        delay(500)
    }

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

    private fun startNavigate(activity: Activity) {
        val isPremium = false // LocalStorage.premiumState


        adNativeManager.preloadAd(
            context = activity,
            adUnitName = AdUnitName.languageNative,
            factoryId = "language_native_ad",
            adChoicesPlacement = null,
            saved = null
        )

//        viewModelScope.launch {
//            delay(2000)
//            navigateToNextScreen()
//        }

        if (isPremium) {
            navigateToNextScreen()
            return
        }

        if (adType == 1) {
            loadAndShowInterstitial(activity)
        } else {
            loadAndShowAppOpen(activity)
        }
    }

    private fun loadAndShowInterstitial(activity: Activity) {
        if (!isNotificationPermissionGranted(activity)) {
            // 1. Start loading ad
            adInterstitialManager.loadAdInSplash(
                adUnitName = AdUnitName.openInterstitial,
                isHighFloor = true,
                callback = { showRouteAfterAd() },
                timeoutCallback = {
                    adInterstitialManager.cacheNextAd()
                    showRouteAfterAd()
                },
                duration = 5000
            )

            // 2. Trigger UI to show permission request
            _shouldRequestNotificationPermission.value = true
        } else {
            adInterstitialManager.loadAdAndShow(
                activity = activity,
                adUnitName = AdUnitName.openInterstitial,
                isHighFloor = true,
                timeoutCallback = {
                    adInterstitialManager.cacheNextAd()
                    showRouteAfterAd()
                },
                duration = 5000,
                callback = { showRouteAfterAd() }
            )
        }
    }

    // Call this from your UI when the permission request is finished
    fun onPermissionResult(activity: Activity) {
        _shouldRequestNotificationPermission.value = false // Reset state
        if (adType == 1) {
            adInterstitialManager.updateDoneGetPermission(true)
            adInterstitialManager.showAd(
                activity = activity,
                adUnitName = AdUnitName.openInterstitial,
                callback = { showRouteAfterAd() },
            )
        } else {
            adOpenManager.updateDoneGetPermission(true)
            adOpenManager.showAdIfAvailable(
                activity = activity,
                adUnitName = AdUnitName.globalOpen,
                onShowAdComplete = { showRouteAfterAd() },
            )
        }

    }

    private fun loadAndShowAppOpen(activity: Activity) {
        if (!isNotificationPermissionGranted(activity)) {
            // 1. Start loading ad
            adOpenManager.loadAdInSplash(
                adUnitName = AdUnitName.globalOpen,
                isHighFloor = true,
                callback = { showRouteAfterAd() },
                timeoutCallback = {
                    adOpenManager.cacheNextAd()
                    showRouteAfterAd()
                },
                duration = 5000
            )

            _shouldRequestNotificationPermission.value = true
        } else {
            adOpenManager.loadAdAndShow(
                activity = activity,
                adUnitName = AdUnitName.globalOpen,
                isHighFloor = true,
                timeoutCallback = {
                    adOpenManager.cacheNextAd()
                    showRouteAfterAd()
                },
                duration = 5000,
                callback = { showRouteAfterAd() }
            )
        }
    }

    private fun navigateToNextScreen() {
        val isCompleteOnboard = false
        val isCompleteLanguage = false

        _destination.value = when {
            isCompleteOnboard -> Home
            isCompleteLanguage -> Onboard
            else -> Language
        }
    }

    private fun showRouteAfterAd() {
        viewModelScope.launch {
            delay(2000)
            navigateToNextScreen()
        }
    }

    private fun isNotificationPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}
