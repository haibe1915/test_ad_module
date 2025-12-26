package com.example.test_kotlin_compose.integration.firebase

import com.example.test_kotlin_compose.integration.config.AdsIdType

interface AdRemoteConfig {

    /** Global kill-switch for ads from remote config. */
    fun isDisableAds(): Boolean

    /** Waterfall feature flags/settings. */
    fun getWaterfallApply(): Boolean
    fun getWaterfallLoadingMode(): Int
    fun getWaterfallDebug(): Boolean
    fun getMaxRetry(): Int

    /** Backoff / timing settings shared across ad formats. */
    fun getReloadTime(): Map<String, Any>

    /** Ramp-up delay between waterfall loads (ms). */
    fun getAdsRampUpTime(): Long

    /** High-floor mapping (optional). Key = placement name. */
    fun getHighFloorAdUnitId(): Map<String, String>

    /** Remote-config ad unit ids for fixed placements. Key = placement name. */
    fun getRemoteAdUnitId(): Map<String, AdsIdType>

    /** Waterfall floor ids per format. */
    fun getLowAdUnitIds(): Map<String, String>
    fun getHighAdUnitIds(): Map<String, String>
    fun getInterstitialAdUnitIds(): List<Map<String, String>>
    fun getNativeAdUnitIds(): List<Map<String, String>>

    /** Per-format config. */
    fun getConfigAdInterstitial(): Map<String, Any>
    fun getConfigAdReward(): Map<String, Any>
    fun getConfigAdNative(): Map<String, Any>
    fun getConfigAdBanner(): Map<String, Any>
    fun getConfigAdAppOpen(): Map<String, Any>

    /** Ad places (per-screen placement config). Key = placement name. */
    fun getAdPlacesAppOpen(): Map<String, Map<String, Any>>
    fun getAdPlacesInterstitial(): Map<String, Map<String, Any>>
    fun getAdPlacesNative(): Map<String, Map<String, Any>>
    fun getAdPlacesBanner(): Map<String, Map<String, Any>>

    /** Misc ad-related lists. */
    fun getAdsNativePremium(): List<String>

    /** List of placement names that support collapsible banner behavior. */
    fun getCollapsibleBanner(): List<String>

    /** Optional screen-level ad behavior configs (used by some click logic). */
    fun getConfigScreenLanguage(): Map<String, Any>
}