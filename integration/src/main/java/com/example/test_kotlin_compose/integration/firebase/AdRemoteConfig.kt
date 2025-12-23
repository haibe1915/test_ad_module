package com.example.test_kotlin_compose.integration.firebase

import com.example.test_kotlin_compose.integration.adManager.AdUnitName

/**
 * Ads-only config contract for the integration module.
 *
 * Host apps should implement this (e.g. using Firebase Remote Config, local JSON, etc.).
 * The integration module should ONLY depend on this interface for ad configuration.
 */
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

    /** High-floor mapping (optional). */
    fun getHighFloorAdUnitId(): Map<AdUnitName, String>

    /** Remote-config ad unit ids for fixed placements. */
    fun getRemoteAdUnitId(): Map<AdUnitName, String>

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

    /** Ad places (per-screen placement config). */
    fun getAdPlacesAppOpen(): Map<AdUnitName, Map<String, Any>>
    fun getAdPlacesInterstitial(): Map<AdUnitName, Map<String, Any>>
    fun getAdPlacesNative(): Map<AdUnitName, Map<String, Any>>
    fun getAdPlacesBanner(): Map<AdUnitName, Map<String, Any>>

    /** Misc ad-related lists. */
    fun getAdsNativePremium(): List<String>
    fun getCollapsibleBanner(): List<AdUnitName>

    /** Optional screen-level ad behavior configs (used by some click logic). */
    fun getConfigScreenLanguage(): Map<String, Any>
}