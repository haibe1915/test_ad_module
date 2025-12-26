package com.example.test_kotlin_compose.integration.splash.api

import android.app.Activity
import com.example.test_kotlin_compose.integration.adManager.AdType

/**
 * Public API for coordinating splash ad flow.
 *
 * Integration module does not request runtime permissions. Instead it returns [Action]
 * to tell the host app whether it should trigger the notification permission UI.
 */
interface SplashAdCoordinator {

    sealed class Action {
        data object None : Action()
        data object RequestNotificationPermission : Action()
    }

    /**
     * Start splash flow.
     *
     * @return [Action.RequestNotificationPermission] if the host app should request POST_NOTIFICATIONS.
     */
    fun loadAndShowSplash(
        activity: Activity,
        adType: AdType,
        adUnitKey: String,
        onAfterAd: () -> Unit,
        timeoutMs: Int = 5_000,
        isHighFloor: Boolean = true,
    ): Action

    /**
     * Continue splash ad flow after the host app finishes permission request (granted/denied).
     */
    fun onPermissionResult(
        activity: Activity,
        adType: AdType,
        adUnitKey: String,
        onAfterAd: () -> Unit,
    )
}
