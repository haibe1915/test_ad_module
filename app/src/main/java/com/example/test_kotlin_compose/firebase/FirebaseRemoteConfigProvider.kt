package com.example.test_kotlin_compose.firebase

import android.content.Context
import com.example.test_kotlin_compose.R
import com.example.test_kotlin_compose.config.RemoteConfigProvider
import com.example.test_kotlin_compose.integration.adManager.AdClient
import com.example.test_kotlin_compose.integration.firebase.AdRemoteConfig
import com.example.test_kotlin_compose.integration.config.AdsIdRemoteConfig
import com.example.test_kotlin_compose.integration.config.AdsIdType
import com.example.test_kotlin_compose.integration.config.bannerAdsPositionKeys
import com.example.test_kotlin_compose.integration.config.interstitialAdsPositionKeys
import com.example.test_kotlin_compose.integration.config.nativeAdsPositionKeys
import com.example.test_kotlin_compose.integration.config.openAdsPositionKeys
import com.example.test_kotlin_compose.integration.config.rewardAdsPositionKeys
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRemoteConfigProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : RemoteConfigProvider, AdRemoteConfig {

    private val instance: FirebaseRemoteConfig
        get() = Firebase.remoteConfig

    override fun init() {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        instance.setConfigSettingsAsync(configSettings)
        instance.setDefaultsAsync(R.xml.remote_config_defaults)
    }

    override suspend fun fetchAndActivate(): Boolean {
        return instance.fetchAndActivate().await()
    }

    override fun getJson(key: String): JSONObject? {
        val raw = instance.getString(key)
        return if (raw.isNotEmpty()) JSONObject(raw) else null
    }

    override fun getString(key: String): String = instance.getString(key)

    override fun getBoolean(key: String): Boolean = instance.getBoolean(key)

    override fun getLong(key: String): Long = instance.getLong(key)

    override fun showCMP(): Boolean = getBoolean("and_show_cmp")

    override fun getLogEventInsightState(): Boolean = getBoolean("disable_log_event_insight")

    override fun getAdPerHistoryItem(): Int = getLong("ad_per_history_item").toInt()

    override fun getExperimentGroup(): String = getString("experiment_group")

    override fun isDisableAds(): Boolean {
        // TODO wire to remote config when ready
        // return getBoolean("disable_ads")
        return false
    }

    override fun logDatabucket(): Boolean = getBoolean("log_databucket")

    override fun getTestRatingDialog(): Boolean = getBoolean("rating_dialog_test")

    override fun getRatingDialogTimeout(): Int = getLong("rating_dialog_timeout").toInt()

    override fun getShowOfferPage(): Boolean = getBoolean("offer_page")

    override fun getStartPage(): Int = getLong("start_page").toInt()

    override fun getInterOpenApp(): Int {
        // TODO wire to remote config when ready
        // return getLong("first_open_ad_type").toInt()
        return 0
    }

    override fun getFirstOpenAdShow(): Boolean = getBoolean("first_open_ad_show")

    override fun showLanguageBanner(): Boolean = getBoolean("show_language_banner")

    override fun showOnboardNativeFull(): Boolean = getBoolean("show_onboard_native_full")

    override fun getMaxFlashOffer(): Int = getLong("max_flash_offer").toInt()

    override fun getFlashOfferCooldown(): Int = getLong("flash_offer_cooldown").toInt()

    override fun getAdsRampUpTime(): Long = getLong("ads_ramp_up_time")

    override fun getCountryInfo(): Map<String, Any> = parseJsonToMap("country_info")

    override fun getConfigApp(): Map<String, Any> {
        val map = parseJsonToMap("config_app")
        return if (map.isEmpty()) mapOf("show_inter_when_switch_page" to false) else map
    }

    override fun getConfigScreenLanguage(): Map<String, Any> =
        parseJsonToMap("config_screen_language")

    override fun getConfigScreenSplash(): Map<String, Any> = parseJsonToMap("config_screen_splash")
    override fun getConfigScreenOnboard(): Map<String, Any> =
        parseJsonToMap("config_screen_onboard")

    override fun getConfigScreenScan(): Map<String, Any> = parseJsonToMap("config_screen_scan")
    override fun getConfigScreenResult(): Map<String, Any> = parseJsonToMap("config_screen_result")
    override fun getConfigPreventAdClick(): Map<String, Any> =
        parseJsonToMap("config_prevent_ad_click")

    override fun getReloadTime(): Map<String, Any> = parseJsonToMap("reload_time")

    override fun getConfigAdInterstitial(): Map<String, Any> =
        parseJsonToMap("config_ads_interstitial")

    override fun getConfigAdReward(): Map<String, Any> = parseJsonToMap("config_ads_reward")
    override fun getConfigAdNative(): Map<String, Any> = parseJsonToMap("config_ads_native")
    override fun getConfigAdBanner(): Map<String, Any> = parseJsonToMap("config_ads_banner")
    override fun getConfigAdAppOpen(): Map<String, Any> = parseJsonToMap("config_ads_app_open")

    override fun getRemoteAdUnitId(): Map<String, AdsIdType> {
        val adKeys: List<String> =
            openAdsPositionKeys +
                    nativeAdsPositionKeys +
                    bannerAdsPositionKeys +
                    interstitialAdsPositionKeys +
                    rewardAdsPositionKeys


        val result = mutableMapOf<String, AdsIdType>()
        adKeys.forEach { adkey ->
            val valueRemote = getString(adkey)
            if (valueRemote.isEmpty()) {
                return@forEach
            }
            try {
                val stringValue = parseJsonToMap(adkey)
                if (stringValue.isEmpty()) {
                    return@forEach
                }
                val adsIdRemote = AdsIdRemoteConfig(
                    referenceConfigKey = stringValue["reference_config_key"] as? String? ?: "",
                    lowIdName = stringValue["low_id_name"] as? String? ?: "",
                    lowId = stringValue["low_id"] as? String? ?: "",
                    highIdName = stringValue["high_id_name"] as? String? ?: "",
                    highId = stringValue["high_id"] as? String? ?: "",
                )



                result[adkey] = AdsIdType(
                    lowId = adsIdRemote.lowId ?: "",
                    highId = adsIdRemote.highId ?: ""
                )

                if (adsIdRemote.referenceConfigKey?.isNotEmpty() ?: false) {
                    result[adkey] = result[adsIdRemote.referenceConfigKey] ?: return@forEach
                    return@forEach
                }

                println("abcd")

            } catch (e: Exception) {
                return@forEach
            }
        }


        return result
    }

    override fun getHighFloorAdUnitId(): Map<String, String> {

        val jsonString = getString("high_floor_ad_unit")
        val result = mutableMapOf<String, String>()
        try {
            val jsonObject = JSONObject(jsonString)
            jsonObject.keys().forEach { key ->
                result[key] = jsonObject.getString(key)
            }
        } catch (_: Exception) {
        }
        return result
    }

    override fun getLowAdUnitIds(): Map<String, String> = parseJsonToStringMap("ads_wf_floor_low")

    override fun getHighAdUnitIds(): Map<String, String> = parseJsonToStringMap("ads_wf_floor_high")

    override fun getInterstitialAdUnitIds(): List<Map<String, String>> =
        parseJsonToListOfMaps("ads_inters_wf")

    override fun getNativeAdUnitIds(): List<Map<String, String>> =
        parseJsonToListOfMaps("ads_native_wf")

    override fun getAdPlacesAppOpen(): Map<String, Map<String, Any>> =
        parseAdPlaces("ad_places_app_open")

    override fun getAdPlacesInterstitial(): Map<String, Map<String, Any>> =
        parseAdPlaces("ad_places_interstitial")

    override fun getAdPlacesNative(): Map<String, Map<String, Any>> =
        parseAdPlaces("ad_places_native")

    override fun getAdPlacesBanner(): Map<String, Map<String, Any>> =
        parseAdPlaces("ad_places_banner")

    override fun getAppVersionCtaRate(): List<String> =
        parseJsonToStringList("app_versions_cta_show_rate")

    override fun getAdsNativePremium(): List<String> = parseJsonToStringList("ads_natives_premium")

    override fun getCollapsibleBanner(): List<String> {
        val jsonString = getString("collapsible_banner")
        val result = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                result.add(jsonArray.getString(i))
            }
        } catch (_: Exception) {
        }
        return result
    }

    override fun getWaterfallApply(): Boolean {
        // return getBoolean("waterfall_apply")
        return false
    }

    override fun getWaterfallLoadingMode(): Int = getLong("waterfall_loading_mode").toInt()

    override fun getWaterfallDebug(): Boolean = getBoolean("waterfall_debug")

    override fun getMaxRetry(): Int = getLong("max_retry").toInt()


    // ---- helpers ----

    private fun parseJsonToMap(key: String): Map<String, Any> {
        val jsonString = getString(key)
        return jsonToMap(jsonString)
    }

    private fun parseJsonToStringMap(key: String): Map<String, String> {
        val jsonString = getString(key)
        val result = mutableMapOf<String, String>()
        try {
            val jsonObject = JSONObject(jsonString)
            jsonObject.keys().forEach { k ->
                result[k] = jsonObject.getString(k)
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun parseJsonToStringList(key: String): List<String> {
        val jsonString = getString(key)
        val result = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                result.add(jsonArray.getString(i))
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun parseJsonToListOfMaps(key: String): List<Map<String, String>> {
        val jsonString = getString(key)
        val result = mutableListOf<Map<String, String>>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val itemObj = jsonArray.getJSONObject(i)
                val map = mutableMapOf<String, String>()
                itemObj.keys().forEach { k ->
                    map[k] = itemObj.getString(k)
                }
                result.add(map)
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun parseAdPlaces(key: String): Map<String, Map<String, Any>> {
        val jsonString = getString(key)
        val result = mutableMapOf<String, Map<String, Any>>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val itemObj = jsonArray.getJSONObject(i)
                val name = itemObj.optString("name")
                if (name.isNotBlank()) {
                    result[name] = jsonToMap(itemObj.toString())
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun jsonToMap(jsonString: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        if (jsonString.isEmpty()) return map
        try {
            val jsonObject = JSONObject(jsonString)
            jsonObject.keys().forEach { key ->
                val value = jsonObject.get(key)
                map[key] = when (value) {
                    is JSONObject -> jsonToMap(value.toString())
                    is JSONArray -> (0 until value.length()).map { idx -> value.get(idx) }
                    else -> value
                }
            }
        } catch (_: Exception) {
        }
        return map
    }
}
