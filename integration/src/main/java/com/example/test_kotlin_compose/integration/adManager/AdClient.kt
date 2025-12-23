package com.example.test_kotlin_compose.integration.adManager

import android.os.Handler
import android.os.Looper
import com.example.test_kotlin_compose.integration.firebase.AdRemoteConfig

// Constants
const val OPEN_APP_TEST_ID = "ca-app-pub-3940256099942544/9257395921"
const val NATIVE_TEST_ID = "ca-app-pub-3940256099942544/2247696110"
const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/9214589741"
const val INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712"
const val REWARD_TEST_ID = "ca-app-pub-3940256099942544/5224354917"

enum class AdUnitName {
    homeBanner,
    languageBanner,
    mapBanner,
    scanBanner,
    convertNative,
    resultNative,
    historyNative,
    languageNative,
    onboardNative,
    onboardNative2,
    onboardFullNative,
    settingNative,
    scanSuccessNative,
    customizeNative,
    uninstallNative,
    businessNative,
    resultReward,
    resultInterstitial,
    convertInterstitial,
    openInterstitial,
    uninstallInterstitial,
    premiumReward,
    globalOpen,
    back,
    none
}

fun stringToAdUnitName(value: String): AdUnitName {
    return try {
        AdUnitName.valueOf(value)
    } catch (e: IllegalArgumentException) {
        AdUnitName.none
    }
}

enum class AdType {
    native,
    banner,
    interstitial,
    openApp,
    reward,
}

enum class AdAction {
    show,
    loadFail,
    displayed,
    reload,
    click,
}

enum class AdState {
    loaded,
    loading,
    waiting,
    notLoaded,
}

enum class AdLoadState {
    loaded, loading, timeout, failed, none

}

// Default Reload Times
var defaultOpenAppReloadAdTime = 1000
var defaultInterstitialAdReloadTime = 1000
var defaultNativeAdReloadTime = 200
var defaultBannerAdReloadTime = 300
var defaultRewardAdReloadTime = 3000
var defaultNativeRefreshNewAd = 30

object AdClient {

    private lateinit var remoteConfig: AdRemoteConfig
    private var environment: AdEnvironment = AdEnvironment()

    /**
     * Must be called by host app once at startup.
     * Prefer calling from `Application.onCreate()`.
     */
    fun configure(
        provider: AdRemoteConfig,
        environment: AdEnvironment = AdEnvironment()
    ) {
        this.remoteConfig = provider
        this.environment = environment
    }

    /** Backward-compatible alias. */
    fun setRemoteConfigProvider(provider: AdRemoteConfig) {
        configure(provider, environment)
    }

    fun initialize() {
        check(::remoteConfig.isInitialized) {
            "AdClient.remoteConfig is not set. Call AdClient.configure() before initialize()."
        }

        _interstitialAdUnitIds = remoteConfig.getInterstitialAdUnitIds()
        _nativeAdUnitIds = remoteConfig.getNativeAdUnitIds()
        _defaultLowAdId = remoteConfig.getLowAdUnitIds()
        _defaultHighAdId = remoteConfig.getHighAdUnitIds()
        _disableAds = remoteConfig.isDisableAds()
        _remoteAdUnitId = remoteConfig.getRemoteAdUnitId()
        _highFloorAdUnitId = remoteConfig.getHighFloorAdUnitId()
        collapsibleBanner = remoteConfig.getCollapsibleBanner()

        // ad places are read by some managers/components
        adAppOpenPlaces = remoteConfig.getAdPlacesAppOpen()
        adNativePlaces = remoteConfig.getAdPlacesNative()
        adInterPlaces = remoteConfig.getAdPlacesInterstitial()
        adBannerPlaces = remoteConfig.getAdPlacesBanner()

        sessionStartTime = System.currentTimeMillis()

        if (canDismissAds()) {
            return
        }
    }

    fun reInitialize() {
        initialize()
    }

    fun canDismissAds(): Boolean {
        // Host app can always override
        if (environment.forceDisableAds) return true

        if (_disableAdsTemp) {
            val disableTime =
                (_configPreventAdClick["time_disable_ads_when_reached_max_ad_click"] as? Number)?.toLong()
                    ?: 1800L
            if (System.currentTimeMillis() - disableStartTime < disableTime * 1000) {
                return true
            }
        }
        _disableAdsTemp = false
        return _disableAds
    }

    fun getAdUnitId(adUnitName: AdUnitName): String {
        // Use test ids when debugging
        if (environment.isDebug) {
            return _testAdUnitId[adUnitName] ?: ""
        }

        // Prefer remote config ids, fallback to bundled defaults
        return _remoteAdUnitId[adUnitName]
            ?: _defaultAdUnitId[adUnitName]
            ?: ""
    }

    private val _testAdUnitId = mapOf(
        AdUnitName.homeBanner to BANNER_TEST_ID,
        AdUnitName.languageBanner to BANNER_TEST_ID,
        AdUnitName.mapBanner to BANNER_TEST_ID,
        AdUnitName.scanBanner to BANNER_TEST_ID,
        AdUnitName.convertNative to NATIVE_TEST_ID,
        AdUnitName.resultNative to NATIVE_TEST_ID,
        AdUnitName.historyNative to NATIVE_TEST_ID,
        AdUnitName.languageNative to NATIVE_TEST_ID,
        AdUnitName.onboardNative to NATIVE_TEST_ID,
        AdUnitName.onboardNative2 to NATIVE_TEST_ID,
        AdUnitName.onboardFullNative to NATIVE_TEST_ID,
        AdUnitName.settingNative to NATIVE_TEST_ID,
        AdUnitName.scanSuccessNative to NATIVE_TEST_ID,
        AdUnitName.customizeNative to NATIVE_TEST_ID,
        AdUnitName.uninstallNative to NATIVE_TEST_ID,
        AdUnitName.businessNative to NATIVE_TEST_ID,
        AdUnitName.resultReward to REWARD_TEST_ID,
        AdUnitName.resultInterstitial to INTERSTITIAL_TEST_ID,
        AdUnitName.openInterstitial to INTERSTITIAL_TEST_ID,
        AdUnitName.uninstallInterstitial to INTERSTITIAL_TEST_ID,
        AdUnitName.premiumReward to REWARD_TEST_ID,
        AdUnitName.globalOpen to OPEN_APP_TEST_ID,
        AdUnitName.back to OPEN_APP_TEST_ID,
        AdUnitName.convertInterstitial to INTERSTITIAL_TEST_ID
    )

    private val _defaultAdUnitId = mapOf(
        AdUnitName.homeBanner to "ca-app-pub-1939315010587936/4609791862",
        AdUnitName.languageBanner to "ca-app-pub-1939315010587936/6215088392",
        AdUnitName.mapBanner to "ca-app-pub-1939315010587936/9609591237",
        AdUnitName.scanBanner to "ca-app-pub-1939315010587936/5276229011",
        AdUnitName.convertNative to "ca-app-pub-1939315010587936/3296710194",
        AdUnitName.resultNative to "ca-app-pub-1939315010587936/2994028680",
        AdUnitName.historyNative to "ca-app-pub-4273999304863934/9876466654",
        AdUnitName.languageNative to "ca-app-pub-1939315010587936/1983628522",
        AdUnitName.onboardNative to "ca-app-pub-1939315010587936/7176816273",
        AdUnitName.onboardNative2 to "ca-app-pub-1939315010587936/4418220171",
        AdUnitName.onboardFullNative to "ca-app-pub-1939315010587936/4418220171",
        AdUnitName.settingNative to "ca-app-pub-1939315010587936/1215172451",
        AdUnitName.scanSuccessNative to "ca-app-pub-1939315010587936/9782031376",
        AdUnitName.customizeNative to "ca-app-pub-1939315010587936/8558558976",
        AdUnitName.uninstallNative to "ca-app-pub-1939315010587936/4037194148",
        AdUnitName.businessNative to "ca-app-pub-1939315010587936/9782031376",
        AdUnitName.resultReward to "ca-app-pub-4273999304863934/9297983788",
        AdUnitName.resultInterstitial to "ca-app-pub-1939315010587936/7647589563",
        AdUnitName.convertInterstitial to "ca-app-pub-1939315010587936/7044383511",
        AdUnitName.openInterstitial to "ca-app-pub-1939315010587936/5750824001",
        AdUnitName.uninstallInterstitial to "ca-app-pub-1939315010587936/8530972922",
        AdUnitName.premiumReward to "ca-app-pub-1939315010587936/4138375654",
        AdUnitName.globalOpen to "ca-app-pub-1939315010587936/7528170065",
        AdUnitName.back to "ca-app-pub-1939315010587936/5711996253"
    )

    private var _remoteAdUnitId: Map<AdUnitName, String> = emptyMap()
    private var _interstitialAdUnitIds: List<Map<String, String>> = emptyList()
    private var _nativeAdUnitIds: List<Map<String, String>> = emptyList()
    private var _defaultLowAdId: Map<String, String> = emptyMap()
    private var _defaultHighAdId: Map<String, String> = emptyMap()
    private var _disableAds = false
    var nonPreloadedAdUnitIds: List<AdUnitName> = emptyList()
    var notReloadAd: List<AdUnitName> = emptyList()
    var collapsibleBanner: List<AdUnitName> = emptyList()
    var renewAdList: List<AdUnitName> = emptyList()

    // private lateinit var timer: Timer // Not used in init logic directly
    private var _highFloorAdUnitId: Map<AdUnitName, String> = emptyMap()

    // Assuming CtaRate is a data class you have defined
    // private var _highFloorShowRate: Map<AdUnitName, CtaRate> = emptyMap() 
    private var _configPreventAdClick: Map<String, Any> = emptyMap()
    var adClickCount = 0
    var sessionStartTime: Long = 0
    var disableStartTime: Long = 0
    private var _disableAdsTemp = false
    var adAppOpenPlaces: Map<AdUnitName, Map<String, Any>> = emptyMap()
    var adNativePlaces: Map<AdUnitName, Map<String, Any>> = emptyMap()
    var adInterPlaces: Map<AdUnitName, Map<String, Any>> = emptyMap()
    var adBannerPlaces: Map<AdUnitName, Map<String, Any>> = emptyMap()

    fun notifyAdClick() {
        val now = System.currentTimeMillis()
        val timePerSession =
            (_configPreventAdClick["time_per_session"] as? Number)?.toLong() ?: 300L
        val maxAdClick =
            (_configPreventAdClick["max_ad_click_per_session"] as? Number)?.toInt() ?: 6

        if (now - sessionStartTime < timePerSession * 1000) {
            adClickCount++
            if (adClickCount >= maxAdClick) {
                adClickCount = 0
                _disableAdsTemp = true
                disableStartTime = System.currentTimeMillis()
                // LocalStorage.premiumState = true // Mock

                Handler(Looper.getMainLooper()).postDelayed({
                    _disableAdsTemp = false
                    // LocalStorage.premiumState = false // Mock
                }, 60000)
            }
        } else {
            adClickCount = 1
            sessionStartTime = System.currentTimeMillis()
        }
    }

    fun getLowAdId(adType: AdType): String {
        return when (adType) {
            AdType.native -> _defaultLowAdId["native"] ?: NATIVE_TEST_ID
            AdType.banner -> _defaultLowAdId["banner"] ?: BANNER_TEST_ID
            AdType.interstitial -> _defaultLowAdId["interstitial"] ?: INTERSTITIAL_TEST_ID
            AdType.openApp -> _defaultLowAdId["openApp"] ?: OPEN_APP_TEST_ID
            AdType.reward -> _defaultLowAdId["reward"] ?: REWARD_TEST_ID
        }
    }

    fun getHighAdId(adType: AdType): String {
        return when (adType) {
            AdType.native -> _defaultHighAdId["native"] ?: NATIVE_TEST_ID
            AdType.banner -> _defaultHighAdId["banner"] ?: BANNER_TEST_ID
            AdType.interstitial -> _defaultHighAdId["interstitial"] ?: INTERSTITIAL_TEST_ID
            AdType.openApp -> _defaultHighAdId["openApp"] ?: OPEN_APP_TEST_ID
            AdType.reward -> _defaultHighAdId["reward"] ?: REWARD_TEST_ID
        }
    }

    fun getInterstitialId(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (element in _interstitialAdUnitIds) {
            element.keys.firstOrNull()?.let { key ->
                element[key]?.let { value ->
                    result[key] = value
                }
            }
        }
        return result
    }

    fun getInterstitialPremium(): List<String> {
        val result = mutableListOf<String>()
        for (element in _interstitialAdUnitIds) {
            if (element["is_premium"] == "true") {
                element.keys.firstOrNull()?.let { result.add(it) }
            }
        }
        return result
    }

    fun getNativeId(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (element in _nativeAdUnitIds) {
            element.keys.firstOrNull()?.let { key ->
                element[key]?.let { value ->
                    result[key] = value
                }
            }
        }
        return result
    }

    fun isDisablePreload(adUnitName: AdUnitName): Boolean {
        return nonPreloadedAdUnitIds.contains(adUnitName)
    }

    fun getOpenAppFailReloadTime(): Int = defaultOpenAppReloadAdTime

    fun getInterstitialAdFailReloadTime(): Int = defaultInterstitialAdReloadTime

    fun getRewardAdFailReloadTime(): Int = defaultRewardAdReloadTime

    fun getNativeAdReloadNewAdTime(): Int = defaultNativeRefreshNewAd

    fun getHighFloor(adUnitName: AdUnitName): String? {
        return _highFloorAdUnitId[adUnitName]
    }
}
