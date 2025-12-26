package com.example.test_kotlin_compose.config

import org.json.JSONObject

/**
 * App-level remote config abstraction.
 *
 * This lives in the :app module because it's not meant to be reused by the integration/ads SDK.
 * Implementations can be Firebase Remote Config, local JSON, mock for tests, etc.
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

    /**
     * Note: ads-kill switch is also part of AdRemoteConfig.
     * For convenience, the app can access it here too.
     */
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

    fun getAppVersionCtaRate(): List<String>
}

