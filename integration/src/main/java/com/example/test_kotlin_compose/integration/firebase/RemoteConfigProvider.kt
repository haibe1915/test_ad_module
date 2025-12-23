package com.example.test_kotlin_compose.integration.firebase

import com.example.test_kotlin_compose.integration.adManager.AdUnitName
import org.json.JSONObject

/**
 * Abstraction over Remote Config so other layers (ads, UI, domain) don't depend on Firebase APIs.
 *
 * Keep this interface stable; implementations can be Firebase, local JSON, mock for tests, etc.
 */
interface RemoteConfigProvider {

    /** Configure defaults/min fetch interval etc. */
    fun init()

    /** Fetch latest values and activate them. */
    suspend fun fetchAndActivate(): Boolean

    fun getJson(key: String): JSONObject?

    fun getString(key: String): String
    fun getBoolean(key: String): Boolean
    fun getLong(key: String): Long

    // ---- Convenience API used by the app ----

    fun showCMP(): Boolean
    fun getLogEventInsightState(): Boolean
    fun getAdPerHistoryItem(): Int
    fun getExperimentGroup(): String
    fun isDisableAds(): Boolean
    fun logDatabucket(): Boolean
    fun getTestRatingDialog(): Boolean
    fun getRatingDialogTimeout(): Int
    fun getShowOfferPage(): Boolean
    fun getStartPage(): Int
    fun getInterOpenApp(): Int
    fun getFirstOpenAdShow(): Boolean
    fun showLanguageBanner(): Boolean
    fun showOnboardNativeFull(): Boolean
    fun getMaxFlashOffer(): Int
    fun getFlashOfferCooldown(): Int
    fun getAdsRampUpTime(): Long

    fun getCountryInfo(): Map<String, Any>
    fun getConfigApp(): Map<String, Any>
    fun getConfigScreenLanguage(): Map<String, Any>
    fun getConfigScreenSplash(): Map<String, Any>
    fun getConfigScreenOnboard(): Map<String, Any>
    fun getConfigScreenScan(): Map<String, Any>
    fun getConfigScreenResult(): Map<String, Any>
    fun getConfigPreventAdClick(): Map<String, Any>
    fun getReloadTime(): Map<String, Any>
    fun getConfigAdInterstitial(): Map<String, Any>
    fun getConfigAdReward(): Map<String, Any>
    fun getConfigAdNative(): Map<String, Any>
    fun getConfigAdBanner(): Map<String, Any>
    fun getConfigAdAppOpen(): Map<String, Any>

    fun getRemoteAdUnitId(): Map<AdUnitName, String>
    fun getHighFloorAdUnitId(): Map<AdUnitName, String>
    fun getLowAdUnitIds(): Map<String, String>
    fun getHighAdUnitIds(): Map<String, String>
    fun getInterstitialAdUnitIds(): List<Map<String, String>>
    fun getNativeAdUnitIds(): List<Map<String, String>>

    fun getAdPlacesAppOpen(): Map<AdUnitName, Map<String, Any>>
    fun getAdPlacesInterstitial(): Map<AdUnitName, Map<String, Any>>
    fun getAdPlacesNative(): Map<AdUnitName, Map<String, Any>>
    fun getAdPlacesBanner(): Map<AdUnitName, Map<String, Any>>

    fun getAppVersionCtaRate(): List<String>
    fun getAdsNativePremium(): List<String>
    fun getCollapsibleBanner(): List<AdUnitName>

    fun getWaterfallApply(): Boolean
    fun getWaterfallLoadingMode(): Int
    fun getWaterfallDebug(): Boolean
    fun getMaxRetry(): Int
}

