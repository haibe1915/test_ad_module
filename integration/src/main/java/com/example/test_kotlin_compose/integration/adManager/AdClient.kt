package com.example.test_kotlin_compose.integration.adManager

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.test_kotlin_compose.integration.adManager.api.AdClientGateway
import com.example.test_kotlin_compose.integration.firebase.AdRemoteConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Constants
const val OPEN_APP_TEST_ID = "ca-app-pub-3940256099942544/9257395921"
const val NATIVE_TEST_ID = "ca-app-pub-3940256099942544/2247696110"
const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/9214589741"
const val INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712"
const val REWARD_TEST_ID = "ca-app-pub-3940256099942544/5224354917"

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

// Default Reload Times
var defaultOpenAppReloadAdTime = 1000
var defaultInterstitialAdReloadTime = 1000
var defaultNativeAdReloadTime = 200
var defaultBannerAdReloadTime = 300
var defaultRewardAdReloadTime = 3000
var defaultNativeRefreshNewAd = 30

@Singleton
class AdClient @Inject constructor(
    private val remoteConfig: AdRemoteConfig,
    @ApplicationContext private val context: Context,
) : AdClientGateway {

    private var _remoteAdUnitId: Map<String, String> = emptyMap()
    private var _highFloorAdUnitId: Map<String, String> = emptyMap()

    private var _interstitialAdUnitIds: List<Map<String, String>> = emptyList()
    private var _nativeAdUnitIds: List<Map<String, String>> = emptyList()
    private var _defaultLowAdId: Map<String, String> = emptyMap()
    private var _defaultHighAdId: Map<String, String> = emptyMap()
    private var _disableAds = false

    var nonPreloadedAdUnitIds: List< String> = emptyList()
    var notReloadAd: List< String> = emptyList()
    var collapsibleBanner: List< String> = emptyList()
    var renewAdList: List< String> = emptyList()

    private var _configPreventAdClick: Map<String, Any> = emptyMap()
    var adClickCount = 0
    var sessionStartTime: Long = 0
    var disableStartTime: Long = 0
    private var _disableAdsTemp = false

    var adAppOpenPlaces: Map< String, Map<String, Any>> = emptyMap()
    var adNativePlaces: Map< String, Map<String, Any>> = emptyMap()
    var adInterPlaces: Map< String, Map<String, Any>> = emptyMap()
    var adBannerPlaces: Map< String, Map<String, Any>> = emptyMap()

    override fun reloadFromRemoteConfig() {
        _interstitialAdUnitIds = remoteConfig.getInterstitialAdUnitIds()
        _nativeAdUnitIds = remoteConfig.getNativeAdUnitIds()
        _defaultLowAdId = remoteConfig.getLowAdUnitIds()
        _defaultHighAdId = remoteConfig.getHighAdUnitIds()
        _disableAds = remoteConfig.isDisableAds()

        _remoteAdUnitId = remoteConfig.getRemoteAdUnitId().mapKeys { (k, _) ->  k }
        _highFloorAdUnitId = remoteConfig.getHighFloorAdUnitId().mapKeys { (k, _) ->  k }

        collapsibleBanner = remoteConfig.getCollapsibleBanner().map {  it }

        adAppOpenPlaces = remoteConfig.getAdPlacesAppOpen().mapKeys { (k, _) ->  k }
        adNativePlaces = remoteConfig.getAdPlacesNative().mapKeys { (k, _) ->  k}
        adInterPlaces = remoteConfig.getAdPlacesInterstitial().mapKeys { (k, _) ->  k }
        adBannerPlaces = remoteConfig.getAdPlacesBanner().mapKeys { (k, _) ->  k }

        sessionStartTime = System.currentTimeMillis()
    }

    fun initialize() {
        reloadFromRemoteConfig()
    }

    fun reInitialize() = initialize()

    override fun canDismissAds(): Boolean {
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

    override fun getAdUnitId(key:  String): String {
        return _remoteAdUnitId[key]
            ?: _defaultAdUnitId[key]
            ?: _testAdUnitId[key]
            ?: ""
    }

    override fun getHighFloor(key:  String): String? = _highFloorAdUnitId[key]

    override fun notifyAdClick() {
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

                Handler(Looper.getMainLooper()).postDelayed({
                    _disableAdsTemp = false
                }, 60000)
            }
        } else {
            adClickCount = 1
            sessionStartTime = System.currentTimeMillis()
        }
    }

    override fun getLowAdId(adType: AdType): String {
        return when (adType) {
            AdType.native -> _defaultLowAdId["native"] ?: NATIVE_TEST_ID
            AdType.banner -> _defaultLowAdId["banner"] ?: BANNER_TEST_ID
            AdType.interstitial -> _defaultLowAdId["interstitial"] ?: INTERSTITIAL_TEST_ID
            AdType.openApp -> _defaultLowAdId["openApp"] ?: OPEN_APP_TEST_ID
            AdType.reward -> _defaultLowAdId["reward"] ?: REWARD_TEST_ID
        }
    }

    override fun getHighAdId(adType: AdType): String {
        return when (adType) {
            AdType.native -> _defaultHighAdId["native"] ?: NATIVE_TEST_ID
            AdType.banner -> _defaultHighAdId["banner"] ?: BANNER_TEST_ID
            AdType.interstitial -> _defaultHighAdId["interstitial"] ?: INTERSTITIAL_TEST_ID
            AdType.openApp -> _defaultHighAdId["openApp"] ?: OPEN_APP_TEST_ID
            AdType.reward -> _defaultHighAdId["reward"] ?: REWARD_TEST_ID
        }
    }

    /** Remote-config waterfall mapping: placement name -> id. */
    override fun getInterstitialId(): Map<String, String> {
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

    override fun getInterstitialPremium(): List<String> {
        val result = mutableListOf<String>()
        for (element in _interstitialAdUnitIds) {
            if (element["is_premium"] == "true") {
                element.keys.firstOrNull()?.let { result.add(it) }
            }
        }
        return result
    }

    /** Remote-config waterfall mapping: placement name -> id. */
    override fun getNativeId(): Map<String, String> {
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

    override fun isDisablePreload(key:  String): Boolean = nonPreloadedAdUnitIds.contains(key)

    override fun getOpenAppFailReloadTime(): Int = defaultOpenAppReloadAdTime

    override fun getInterstitialAdFailReloadTime(): Int = defaultInterstitialAdReloadTime

    override fun getRewardAdFailReloadTime(): Int = defaultRewardAdReloadTime

    override fun getNativeAdReloadNewAdTime(): Int = defaultNativeRefreshNewAd

    // Default test IDs (debug) keyed by placement key
    private val _testAdUnitId: Map< String, String> = mapOf(
//         Strings.HomeBanner to BANNER_TEST_ID,
//         Strings.LanguageBanner to BANNER_TEST_ID,
//         Strings.MapBanner to BANNER_TEST_ID,
//         Strings.ScanBanner to BANNER_TEST_ID,
//         Strings.ConvertNative to NATIVE_TEST_ID,
//         Strings.ResultNative to NATIVE_TEST_ID,
//         Strings.HistoryNative to NATIVE_TEST_ID,
//         Strings.LanguageNative to NATIVE_TEST_ID,
//         Strings.OnboardNative to NATIVE_TEST_ID,
//         Strings.OnboardNative2 to NATIVE_TEST_ID,
//         Strings.OnboardFullNative to NATIVE_TEST_ID,
//         Strings.SettingNative to NATIVE_TEST_ID,
//         Strings.ScanSuccessNative to NATIVE_TEST_ID,
//         Strings.CustomizeNative to NATIVE_TEST_ID,
//         Strings.UninstallNative to NATIVE_TEST_ID,
//         Strings.BusinessNative to NATIVE_TEST_ID,
//         Strings.ResultReward to REWARD_TEST_ID,
//         Strings.ResultInterstitial to INTERSTITIAL_TEST_ID,
//         Strings.OpenInterstitial to INTERSTITIAL_TEST_ID,
//         Strings.UninstallInterstitial to INTERSTITIAL_TEST_ID,
//         Strings.PremiumReward to REWARD_TEST_ID,
//         Strings.GlobalOpen to OPEN_APP_TEST_ID,
//         Strings.Back to OPEN_APP_TEST_ID,
//         Strings.ConvertInterstitial to INTERSTITIAL_TEST_ID,
    )

    private val _defaultAdUnitId: Map< String, String> = mapOf(
//         Strings.HomeBanner to "ca-app-pub-1939315010587936/4609791862",
//         Strings.LanguageBanner to "ca-app-pub-1939315010587936/6215088392",
//         Strings.MapBanner to "ca-app-pub-1939315010587936/9609591237",
//         Strings.ScanBanner to "ca-app-pub-1939315010587936/5276229011",
//         Strings.ConvertNative to "ca-app-pub-1939315010587936/3296710194",
//         Strings.ResultNative to "ca-app-pub-1939315010587936/2994028680",
//         Strings.HistoryNative to "ca-app-pub-4273999304863934/9876466654",
//         Strings.LanguageNative to "ca-app-pub-1939315010587936/1983628522",
//         Strings.OnboardNative to "ca-app-pub-1939315010587936/7176816273",
//         Strings.OnboardNative2 to "ca-app-pub-1939315010587936/4418220171",
//         Strings.OnboardFullNative to "ca-app-pub-1939315010587936/4418220171",
//         Strings.SettingNative to "ca-app-pub-1939315010587936/1215172451",
//         Strings.ScanSuccessNative to "ca-app-pub-1939315010587936/9782031376",
//         Strings.CustomizeNative to "ca-app-pub-1939315010587936/8558558976",
//         Strings.UninstallNative to "ca-app-pub-1939315010587936/4037194148",
//         Strings.BusinessNative to "ca-app-pub-1939315010587936/9782031376",
//         Strings.ResultReward to "ca-app-pub-4273999304863934/9297983788",
//         Strings.ResultInterstitial to "ca-app-pub-1939315010587936/7647589563",
//         Strings.ConvertInterstitial to "ca-app-pub-1939315010587936/7044383511",
//         Strings.OpenInterstitial to "ca-app-pub-1939315010587936/5750824001",
//         Strings.UninstallInterstitial to "ca-app-pub-1939315010587936/8530972922",
//         Strings.PremiumReward to "ca-app-pub-1939315010587936/4138375654",
//         Strings.GlobalOpen to "ca-app-pub-1939315010587936/7528170065",
//         Strings.Back to "ca-app-pub-1939315010587936/5711996253",
    )
}

