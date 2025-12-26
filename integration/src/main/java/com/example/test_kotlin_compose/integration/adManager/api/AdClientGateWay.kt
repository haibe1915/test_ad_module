// `integration/src/main/java/com/example/test_kotlin_compose/integration/adManager/api/AdGateway.kt`
package com.example.test_kotlin_compose.integration.adManager.api

import com.example.test_kotlin_compose.integration.adManager.AdType

interface AdClientGateway {
    /** Re\-loads all ad config/placements from the configured remote source. */
    fun reloadFromRemoteConfig()

    /** Whether ads should be dismissed/disabled right now (global + temporary protections). */
    fun canDismissAds(): Boolean

    /** Returns the ad unit id for a placement key, or empty string if unknown. */
    fun getAdUnitId(key: String): String

    /** Returns the high\-floor id for a placement key, or null if absent. */
    fun getHighFloor(key: String): String?

    /** Low/High default ids by ad type (often used as fallback). */
    fun getLowAdId(adType: AdType): String
    fun getHighAdId(adType: AdType): String

    /** Click tracking hook for your anti\-spam/disable logic. */
    fun notifyAdClick()

    /** Waterfall data (as your existing code expects). */
    fun getInterstitialId(): Map<String, String>
    fun getInterstitialPremium(): List<String>
    fun getNativeId(): Map<String, String>

    fun isDisablePreload(key: String): Boolean

    fun getOpenAppFailReloadTime(): Int
    fun getInterstitialAdFailReloadTime(): Int
    fun getRewardAdFailReloadTime(): Int
    fun getNativeAdReloadNewAdTime(): Int
}
