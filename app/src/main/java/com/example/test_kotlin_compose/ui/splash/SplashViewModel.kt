package com.example.test_kotlin_compose.ui.splash

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test_kotlin_compose.config.RemoteConfigProvider
import com.example.test_kotlin_compose.integration.adManager.AdType
import com.example.test_kotlin_compose.integration.adManager.impl.NativeAdManagerImpl
import com.example.test_kotlin_compose.integration.init.api.AdsInitializer
import com.example.test_kotlin_compose.integration.splash.api.SplashAdCoordinator
import com.example.test_kotlin_compose.navigation.AppDestination
import com.example.test_kotlin_compose.navigation.Home
import com.example.test_kotlin_compose.navigation.Language
import com.example.test_kotlin_compose.navigation.Onboard
import com.example.test_kotlin_compose.navigation.Splash
import com.example.test_kotlin_compose.util.AdUnitKeys
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
    private val remoteConfig: RemoteConfigProvider,
    private val splashAdCoordinator: SplashAdCoordinator,
    private val adsInitializer: AdsInitializer,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _destination = MutableStateFlow<AppDestination>(Splash)
    val destination = _destination.asStateFlow()

    var adType = 0

    // State to trigger permission request in UI
    private val _shouldRequestNotificationPermission = MutableStateFlow(false)
    val shouldRequestNotificationPermission = _shouldRequestNotificationPermission.asStateFlow()

    fun initApp(activity: Activity) {
        adType = remoteConfig.getInterOpenApp()
        viewModelScope.launch {
            val remoteConfigJob = async { fetchRemoteConfig() }

            val adsInitializeJob = async {
                adsInitializer.initialize(
                    activity, this,
                    nativeAdsKeys = listOf(
                        "native_common",
                        "native_popup_country",
                        "native_onboard_screen",
                        "native_history_screen",
                        "native_text_screen",
                        "native_document_translate",
                        "native_setting_screen",
                        "native_camera_screen",
                        "native_save_document",
                        "native_screen_translate_screen",
                        "native_conversation_screen",
                        "native_ad_warning",
                        "native_pick_file_screen",
                        "native_process_translate_file"
                    ),
                    bannerAdsKeys = emptyList(),
                    interstitialAdsKeys = listOf(
                        "inter_common",
                        "inter_back",
                        "inter_after_translate_screen",
                        "inter_open_app"
                    ),
                    rewardAdsKeys = emptyList(),
                    openAdsKeys = listOf(
                        "app_open_back",
                        "app_open_open_app"
                    )
                )
            }

            awaitAll(remoteConfigJob, adsInitializeJob)

            startNavigate(activity)
        }
    }

    private suspend fun fetchRemoteConfig() {
        remoteConfig.fetchAndActivate()
        delay(500)
    }

    private fun startNavigate(activity: Activity) {
        val isPremium = false // LocalStorage.premiumState


        adNativeManager.preloadAd(
            context = activity,
            adUnitKey = AdUnitKeys.NativeCommon,
            factoryId = "language_native_ad",
            adChoicesPlacement = null,
            saved = null
        )

        if (isPremium) {
            navigateToNextScreen()
            return
        }

        val action = splashAdCoordinator.loadAndShowSplash(
            activity = activity,
            adType = AdType.openApp,
            adUnitKey = AdUnitKeys.AppOpenOpenApp,
            onAfterAd = { showRouteAfterAd() },
            timeoutMs = 50000,
            isHighFloor = true,
        )

        _shouldRequestNotificationPermission.value =
            action is SplashAdCoordinator.Action.RequestNotificationPermission
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

    fun onPermissionResult(activity: Activity) {
        _shouldRequestNotificationPermission.value = false
        splashAdCoordinator.onPermissionResult(
            activity = activity,
            adType = AdType.openApp,
            adUnitKey = AdUnitKeys.AppOpenOpenApp,
            onAfterAd = { showRouteAfterAd() },
        )
    }
}
