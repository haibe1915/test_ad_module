package com.example.test_kotlin_compose.integration.adManager

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.test_kotlin_compose.integration.R
import com.example.test_kotlin_compose.integration.firebase.AdRemoteConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton


data class NativeConfig(
    val id: String,
    val adUnitName: AdUnitName,
    val adUnitId: String,
    val factoryId: String,
    val ad: NativeAd? = null
) {
    companion object {
        // This acts as your factory constructor
        fun create(
            adUnitName: AdUnitName,
            factoryId: String,
            ad: NativeAd? = null
        ): NativeConfig {
            val adUnitId = AdClient.getAdUnitId(adUnitName)
            val id = adUnitId + factoryId

            return NativeConfig(
                id = id,
                adUnitName = adUnitName,
                adUnitId = adUnitId,
                factoryId = factoryId,
                ad = ad
            )
        }

        // Helper method
        fun generateId(adUnitName: AdUnitName, factoryId: String): String {
            val adUnitId = AdClient.getAdUnitId(adUnitName)
            return adUnitId + factoryId
        }
    }
}

object AdNativeHelper {

    fun getAdNativeHeight(factoryId: String, screenHeightDp: Dp): Dp {
        return when (factoryId) {
            "adFactoryLanguage-v2" -> 200.dp
            "adFactoryLanguage-v3" -> 220.dp
            "adFactoryLanguage" -> 200.dp
            "adFactoryConvert" -> 147.dp
            "adFactoryConvert-v2" -> 147.dp
            "adFactoryConvert-v3" -> 200.dp
            "adFactoryConvert-v4" -> 200.dp
            "adFactoryHistoryItem" -> 70.dp
            "adFactoryProduct" -> 150.dp
            "adFactorySetting" -> 360.dp
            "adFactoryOnboard" -> screenHeightDp - 60.dp // Dynamic calculation
            "adFactorySquare" -> 200.dp
            else -> 200.dp
        }
    }

    fun getLayoutIdFromFactory(factoryId: String): Int {
        return when (factoryId) {
            "adFactoryLanguage" -> R.layout.language_native_ad
            "adFactoryLanguage-v3" -> R.layout.language_native_ad_v2
            "adFactoryConvert-v3" -> R.layout.convert_native_ad_v3
            "adFactoryConvert-v4" -> R.layout.convert_native_ad_v4
            "adFactoryConvert" -> R.layout.convert_native_ad
            "adFactoryConvert-v2" -> R.layout.convert_native_ad_v2
            "adFactoryHistoryItem" -> R.layout.history_item_native_ad
            "adFactoryLanguage-v2" -> R.layout.language_native_ad_no_media
            "adFactorySetting" -> R.layout.setting_native_ad
            "adFactoryOnboard" -> R.layout.onboard_native_ad
            "adFactoryProduct" -> R.layout.product_native_ad
            "adFactorySquare" -> R.layout.square_native_ad

            else -> R.layout.language_native_ad
        }
    }


}

@Singleton
class NativeAdManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfig: AdRemoteConfig
) : NativeAdManagerInterface {

    private val nativeAdMapper = ConcurrentHashMap<String, NativeConfig>()
    private val loadingNativeAdMapper = ConcurrentHashMap<String, NativeConfig>()
    private var adUnitIdMapper: MutableMap<String, String> = mutableMapOf()
    private val nativeAdHolderUnitIds = ConcurrentHashMap<String, NativeAdHolder>()
    private val loadingAd = mutableSetOf<String>()
    private var adNativePremium: MutableList<String> = mutableListOf()
    private val savedNativeAdMapper = ConcurrentHashMap<NativeAd, String>()
    private val newNativeFactoryMapper = ConcurrentHashMap<NativeAd, String>()
    private var waterfallReloadTime: MutableMap<String, Any> = mutableMapOf()
    private val waitingQueue = LinkedHashMap<String, NativeConfig>()
    private var failCount: Int = 0
    var maxLoadFailCounter: Int = 3
    private var delayTime: Int = 100
    var configNative: MutableMap<String, Any> = mutableMapOf()
    private var nativeAdPoolHolder: NativeAdPoolHolder? = null
    private var loadingCompleter: Any? = null   // replace with your real type
    var waterfallApply: Boolean = false
    private var newLoadingMode: Boolean = false
    private var priorityOrder: MutableList<String> = mutableListOf()

    override suspend fun init(context: Context) {
        configNative = (remoteConfig.getConfigAdNative() as? HashMap<String, Any>) ?: hashMapOf()
        waterfallReloadTime = (remoteConfig.getReloadTime() as? HashMap<String, Any>) ?: hashMapOf()

        // Replace with your real source of native ad unit IDs
        adUnitIdMapper = AdClient.getNativeId().toMutableMap()

        maxLoadFailCounter = (configNative["max_retry_count"] as? Number)?.toInt() ?: 2
        delayTime = (configNative["refresh_time"] as? Number)?.toInt() ?: 100

        // premium/ad exclusions if you have them in RemoteConfig
        adNativePremium = remoteConfig.getAdsNativePremium().toMutableList()

        // example: waterfall loading mode from RemoteConfig
        newLoadingMode = remoteConfig.getWaterfallLoadingMode() == 1

        priorityOrder = mutableListOf<String>().apply {
            addAll(adUnitIdMapper.keys)
            add("N-Base-High")
            add("N-Base-Low")
        }

        loadWaterFall()
    }

    override fun preloadAd(
        context: Context,
        adUnitName: AdUnitName,
        factoryId: String,
        adChoicesPlacement: NativeAdOptions.AdChoicesPlacement?,
        saved: Boolean?
    ) {
        if (waterfallApply) return
        val adUnitId = AdClient.getAdUnitId(adUnitName)
        val configId = adUnitId + factoryId
        if (contain(configId)) return
        waitingQueue.putIfAbsent(configId, NativeConfig.create(adUnitName, factoryId))
        checkAndLoadAds(saved)
    }

    private fun contain(key: String): Boolean {
        return nativeAdMapper.containsKey(key) ||
                loadingNativeAdMapper.containsKey(key) ||
                waitingQueue.containsKey(key)
    }

    private fun checkAndLoadAds(saved: Boolean?) {
        if (waitingQueue.isNotEmpty()) {
            val nativeConfig = waitingQueue.entries.first().value
            val adUnitName = nativeConfig.adUnitName
            val factoryId = nativeConfig.factoryId
            waitingQueue.remove(nativeConfig.id)
            failCount = 0
            // start initial load with high floor
            load(
                adUnitName = adUnitName,
                factoryId = factoryId,
                adChoicesPlacement = null,
                isHighFloor = true,
                saved = saved
            )
        }
    }

    private fun load(
        adUnitName: AdUnitName,
        factoryId: String,
        adChoicesPlacement: NativeAdOptions.AdChoicesPlacement?,
        isHighFloor: Boolean?,
        saved: Boolean?
    ) {
        if (AdClient.canDismissAds()) return
        var adUnitId = AdClient.getAdUnitId(adUnitName)
        val highFloorAdUnitId = AdClient.getHighFloor(adUnitName)

        if (isHighFloor ?: true) {
            adUnitId = highFloorAdUnitId ?: adUnitId
        }

        val key = adUnitId + factoryId

        // mark as loading
        loadingNativeAdMapper[key] = NativeConfig.create(adUnitName, factoryId)

        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                // Success
                val nativeConfig = NativeConfig.create(adUnitName, factoryId, nativeAd)
                nativeAdMapper[key] = nativeConfig
                waitingQueue.remove(key)
                loadingNativeAdMapper.remove(key)

                saved?.let { saveFlag ->
                    if (saveFlag) {
                        savedNativeAdMapper[nativeAd] = key
                    } else {
                        newNativeFactoryMapper[nativeAd] = key
                    }
                }
                // Optionally trigger loading next queued ad
                checkAndLoadAds(saved)
            }
            .withAdListener(object : AdListener() {
                override fun onAdClicked() {
                    AdClient.notifyAdClick()
                    if (adUnitName == AdUnitName.languageNative) {
                        if (remoteConfig.getConfigScreenLanguage()["skip_screen_when_press_ad"] == true) {
                            // handle skip behavior if needed
                        }
                    }

                    if (adUnitName == AdUnitName.onboardNative || adUnitName == AdUnitName.onboardNative2) {
                        // handle onboard specific click behavior if needed
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    val highFloor = isHighFloor ?: true
                    val handler = Handler(Looper.getMainLooper())

                    if (highFloor) {
                        // First failure at high floor: delay then retry with low floor
                        handler.postDelayed({
                            load(adUnitName, factoryId, adChoicesPlacement, false, saved)
                        }, delayTime.toLong())
                    } else {
                        // Failure at low floor: count failures and possibly give up
                        failCount++
                        handler.postDelayed({
                            if (failCount >= maxLoadFailCounter) {
                                waitingQueue.remove(adUnitName.name)
                                val lowKey =
                                    (AdClient.getAdUnitId(adUnitName) ?: "") + factoryId
                                loadingNativeAdMapper.remove(lowKey)
                                // any completer logic can go here if you add real types
                                return@postDelayed
                            }
                            // Retry again with low floor
                            load(adUnitName, factoryId, adChoicesPlacement, false, saved)
                        }, delayTime.toLong())
                    }
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    override fun isAdLoaded(adUnitName: AdUnitName): Boolean {
        // Consider an ad loaded if we already have a NativeConfig in the cache for any factoryId
        val adUnitId = AdClient.getAdUnitId(adUnitName)
        return nativeAdMapper.keys.any { it.startsWith(adUnitId) }
    }

    override fun destroyAd(adUnitName: AdUnitName) {
        val adUnitId = AdClient.getAdUnitId(adUnitName)
        // Destroy and remove all ads for this unit
        val keysToRemove = nativeAdMapper.filter { it.value.adUnitId == adUnitId }.keys
        keysToRemove.forEach { key ->
            nativeAdMapper[key]?.ad?.destroy()
            nativeAdMapper.remove(key)
        }
    }

    override fun destroyAll() {
        nativeAdMapper.values.forEach { it.ad?.destroy() }
        nativeAdMapper.clear()
        loadingNativeAdMapper.clear()
        waitingQueue.clear()
        failCount = 0
    }

    override fun getLoadedAd(adUnitName: AdUnitName): NativeAd? {
        if(waterfallApply){
            return getWaterFall()
        }else {
            val adUnitId = AdClient.getAdUnitId(adUnitName)
            return nativeAdMapper.values.firstOrNull { it.adUnitId == adUnitId }?.ad
        }
    }

    fun getWaterFall(): NativeAd?{
        if(newLoadingMode){
            if(nativeAdPoolHolder?.nativeAd != null){
                val tmpNativeAd = nativeAdPoolHolder?.nativeAd
                nativeAdPoolHolder?.nativeAd = null
                return tmpNativeAd
            } else if(nativeAdHolderUnitIds["N-Base-High"]?.nativeAd != null) {
                val tmpNativeAd = nativeAdHolderUnitIds["N-Base-High"]?.nativeAd
                nativeAdHolderUnitIds["N-Base-High"]?.nativeAd = null
                return tmpNativeAd
            } else if(nativeAdHolderUnitIds["N-Base-Low"]?.nativeAd != null){
                val tmpNativeAd = nativeAdHolderUnitIds["N-Base-Low"]?.nativeAd
                nativeAdHolderUnitIds["N-Base-Low"]?.nativeAd = null
                return tmpNativeAd
            } else {
                return null
            }
        }
        for(name in priorityOrder){
            val holder = nativeAdHolderUnitIds[name]
            if(holder != null && holder.nativeAd != null){
                val tmpNativeAd = holder.nativeAd
                holder.nativeAd = null
                return tmpNativeAd
            }
        }
        return null
    }

    override fun pauseWaterFall() {
        for (holder in nativeAdHolderUnitIds.values) {
            holder.pause = true
        }
        nativeAdPoolHolder?.pause = true
    }

    override fun resumeWaterFall() {
        for (holder in nativeAdHolderUnitIds.values) {
            holder.resumeLoadAd()
        }
        nativeAdPoolHolder?.resumeLoadAd()
    }

    override suspend fun loadWaterFall() {
        waterfallApply = remoteConfig.getWaterfallApply()
        newLoadingMode = remoteConfig.getWaterfallLoadingMode() == 1

        if (waterfallApply && nativeAdHolderUnitIds.isEmpty()) {
            maybeLoadAd("N-Base-Low", AdClient.getLowAdId(AdType.native))
            applyDelay()
            maybeLoadAd("N-Base-High", AdClient.getHighAdId(AdType.native))
            applyDelay()
            if (newLoadingMode && nativeAdPoolHolder == null) {

                nativeAdPoolHolder = NativeAdPoolHolder(
                    context,
                    this,
                    adUnitIdMapper.values.toList(),
                    adUnitIdMapper.keys.toList()
                )
                nativeAdPoolHolder?.loadAd()
            } else {
                for (entry in adUnitIdMapper) {
                    val key = entry.key
                    val value = entry.value
                    if (value.isNotEmpty()) {

                        nativeAdHolderUnitIds[key] = NativeAdHolder(
                            context,
                            this,
                            value,
                            key
                        )
                        nativeAdHolderUnitIds[key]?.loadAd()
                        applyDelay()
                    }
                }
            }
        }
    }

    override fun getAvailableAdHolder(): NativeAdHolder? {
        return nativeAdHolderUnitIds.values.firstOrNull { it.nativeAd != null }
    }

    override fun getHighAvailableAdHolder(): NativeAdHolder? {
        // Implement logic to prioritize high floor ads if needed
        return getAvailableAdHolder()
    }

    fun addLoadingAd(adUnitId: String) {
        loadingAd.add(adUnitId)
    }

    fun removeLoadingAd(adUnitId: String) {
        loadingAd.remove(adUnitId)
    }

    fun getWaterfallReloadTime(): Map<String, Any> {
        return waterfallReloadTime
    }

    private fun maybeLoadAd(name: String, adUnitId: String) {
        if (adUnitId.isEmpty()) return
        // Assuming a default factoryId for waterfall ads or passing it in
        val factoryId = "default"
        nativeAdHolderUnitIds[name] = NativeAdHolder(
            context,
            this,
            adUnitId,
            name
        )
        nativeAdHolderUnitIds[name]?.loadAd()
    }

    private suspend fun applyDelay() {
        kotlinx.coroutines.delay(remoteConfig.getAdsRampUpTime())
    }

}