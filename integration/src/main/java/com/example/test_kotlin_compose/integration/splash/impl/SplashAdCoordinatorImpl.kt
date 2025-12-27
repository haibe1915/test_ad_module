package com.example.test_kotlin_compose.integration.splash.impl

import android.app.Activity
import com.example.test_kotlin_compose.integration.adManager.AdType
import com.example.test_kotlin_compose.integration.adManager.impl.InterstialAdManagerImpl
import com.example.test_kotlin_compose.integration.adManager.impl.OpenAdManagerImpl
import com.example.test_kotlin_compose.integration.splash.api.NotificationPermissionChecker
import com.example.test_kotlin_compose.integration.splash.api.SplashAdCoordinator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SplashAdCoordinatorImpl @Inject constructor(
    private val adInterstitialManager: InterstialAdManagerImpl,
    private val adOpenManager: OpenAdManagerImpl,
    private val permissionChecker: NotificationPermissionChecker,
) : SplashAdCoordinator {

    override fun loadAndShowSplash(
        activity: Activity,
        adType: AdType,
        adUnitKey: String,
        onAfterAd: () -> Unit,
        timeoutMs: Int,
        isHighFloor: Boolean,
    ): SplashAdCoordinator.Action {
        val hasPermission = permissionChecker.isComplete(activity)

        return when (adType) {
            AdType.interstitial -> {
                if (!hasPermission) {
                    adInterstitialManager.loadAdInSplash(
                        adUnitKey = adUnitKey,
                        isHighFloor = isHighFloor,
                        callback = onAfterAd,
                        timeoutCallback = onAfterAd,
                        duration = timeoutMs,
                    )
                    SplashAdCoordinator.Action.RequestNotificationPermission
                } else {
                    adInterstitialManager.loadAdAndShow(
                        activity = activity,
                        adUnitKey = adUnitKey,
                        isHighFloor = isHighFloor,
                        timeoutCallback = onAfterAd,
                        duration = timeoutMs,
                        callback = onAfterAd,
                    )
                    SplashAdCoordinator.Action.None
                }
            }

            AdType.openApp -> {
                if (!hasPermission) {
                    adOpenManager.loadAdInSplash(
                        adUnitKey = adUnitKey,
                        isHighFloor = isHighFloor,
                        callback = onAfterAd,
                        timeoutCallback = onAfterAd,
                        duration = timeoutMs,
                    )
                    SplashAdCoordinator.Action.RequestNotificationPermission
                } else {
                    adOpenManager.loadAdAndShow(
                        activity = activity,
                        adUnitKey = adUnitKey,
                        isHighFloor = isHighFloor,
                        timeoutCallback = onAfterAd,
                        duration = timeoutMs,
                        callback = onAfterAd,
                    )
                    SplashAdCoordinator.Action.None
                }
            }

            else -> {
                onAfterAd()
                SplashAdCoordinator.Action.None
            }
        }
    }

    override fun onPermissionResult(
        activity: Activity,
        adType: AdType,
        adUnitKey: String,
        onAfterAd: () -> Unit,
    ) {
        when (adType) {
            AdType.interstitial -> {
                adInterstitialManager.updateDoneGetPermission(true)
                adInterstitialManager.showAd(
                    activity = activity,
                    adUnitKey = adUnitKey,
                    callback = onAfterAd,
                )
            }

            AdType.openApp -> {
                adOpenManager.updateDoneGetPermission(true)
                adOpenManager.showAd(
                    activity = activity,
                    adUnitKey = adUnitKey,
                    onShowAdComplete = onAfterAd,
                )
            }

            else -> onAfterAd()
        }
    }
}
