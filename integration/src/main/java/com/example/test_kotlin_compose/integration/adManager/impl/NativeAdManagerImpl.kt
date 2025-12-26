package com.example.test_kotlin_compose.integration.adManager.impl

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.test_kotlin_compose.integration.R
import com.example.test_kotlin_compose.integration.adManager.AdClient
import com.example.test_kotlin_compose.integration.adManager.AdType
import com.example.test_kotlin_compose.integration.adManager.api.NativeAdManagerInterface
import com.example.test_kotlin_compose.integration.adManager.waterFallAdHolder.NativeAdHolder
import com.example.test_kotlin_compose.integration.adManager.waterFallAdHolder.NativeAdPoolHolder
import com.example.test_kotlin_compose.integration.firebase.AdRemoteConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton


data class NativeConfig(
    val id: String,
    val adUnitKey: String,
    val adUnitId: String,
    val factoryId: String,
    val ad: NativeAd? = null
) {
    companion object {
        /**
         * Factory method that creates a [NativeConfig] for [adUnitKey] + [factoryId].
         *
         * The computed [NativeConfig.id] is `adUnitId + factoryId`.
         *
         * @param adClient used to resolve the real ad unit id.
         * @param adUnitKey logical key used by the app.
         * @param factoryId native layout/factory identifier.
         * @param ad optional loaded [NativeAd] to attach.
         */
        fun create(
            adClient: AdClient,
            adUnitKey: String,
            factoryId: String,
            ad: NativeAd? = null
        ): NativeConfig {
            val adUnitId = adClient.getAdUnitId(adUnitKey)
            val id = adUnitId + factoryId

            return NativeConfig(
                id = id,
                adUnitKey = adUnitKey,
                adUnitId = adUnitId,
                factoryId = factoryId,
                ad = ad
            )
        }

        /**
         * Computes the cache id used by this module for a native ad request.
         *
         * @return `adUnitId + factoryId`.
         */
        fun generateId(adClient: AdClient, adUnitKey: String, factoryId: String): String {
            val adUnitId = adClient.getAdUnitId(adUnitKey)
            return adUnitId + factoryId
        }
    }
}

object AdNativeHelper {

    /**
     * Returns the expected height for a native-ad layout [factoryId].
     *
     * @param factoryId your native-ad factory/layout identifier.
     * @param screenHeightDp screen height in dp, used for layouts that scale with screen.
     */
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

    /**
     * Maps a native-ad [factoryId] to a concrete XML layout resource id.
     *
     * This is used when inflating a [NativeAdView] for a given factory.
     */
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
    private val remoteConfig: AdRemoteConfig,
    private val adClient: AdClient,
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
    private var loadingCompleter: Any? = null
    var waterfallApply: Boolean = false
    private var newLoadingMode: Boolean = false
    private var priorityOrder: MutableList<String> = mutableListOf()

    /**
     * Initializes the native-ad manager.
     *
     * Loads ad-related config from [AdRemoteConfig] and [AdClient], builds waterfall priority,
     * and triggers [loadWaterFall] when waterfall mode is enabled.
     *
     * @param context kept for interface compatibility; this implementation uses injected
     * application [context].
     */
    override suspend fun init(context: Context) {
        configNative = (remoteConfig.getConfigAdNative() as? HashMap<String, Any>) ?: hashMapOf()
        waterfallReloadTime = (remoteConfig.getReloadTime() as? HashMap<String, Any>) ?: hashMapOf()

        // Remote-config waterfall items, keyed by symbolic name.
        adUnitIdMapper = adClient.getNativeId().toMutableMap()

        maxLoadFailCounter = (configNative["max_retry_count"] as? Number)?.toInt() ?: 2
        delayTime = (configNative["refresh_time"] as? Number)?.toInt() ?: 100

        adNativePremium = remoteConfig.getAdsNativePremium().toMutableList()
        newLoadingMode = remoteConfig.getWaterfallLoadingMode() == 1

        priorityOrder = mutableListOf<String>().apply {
            addAll(adUnitIdMapper.keys)
            add("N-Base-High")
            add("N-Base-Low")
        }

        loadWaterFall()
    }

    /**
     * Returns true if [key] is already queued/loading/cached.
     *
     * Used to prevent duplicate requests.
     */
    private fun contain(key: String): Boolean {
        return nativeAdMapper.containsKey(key) ||
                loadingNativeAdMapper.containsKey(key) ||
                waitingQueue.containsKey(key)
    }

    /**
     * Returns true if any native ad is cached for [adUnitKey] (for any factory id).
     */
    override fun isAdLoaded(adUnitKey: String): Boolean {
        // Consider an ad loaded if we already have a NativeConfig in the cache for any factoryId
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        return nativeAdMapper.keys.any { it.startsWith(adUnitId) }
    }

    /**
     * Destroys and removes all cached native ads associated with [adUnitKey].
     */
    override fun destroyAd(adUnitKey: String) {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        // Destroy and remove all ads for this unit
        val keysToRemove = nativeAdMapper.filter { it.value.adUnitId == adUnitId }.keys
        keysToRemove.forEach { key ->
            nativeAdMapper[key]?.ad?.destroy()
            nativeAdMapper.remove(key)
        }
    }

    /**
     * Destroys and clears all cached/loading/queued native ads and resets counters.
     */
    override fun destroyAll() {
        nativeAdMapper.values.forEach { it.ad?.destroy() }
        nativeAdMapper.clear()
        loadingNativeAdMapper.clear()
        waitingQueue.clear()
        failCount = 0
    }

    /**
     * Returns a loaded [NativeAd] for [adUnitKey].
     *
     * If waterfall mode is enabled, this returns the next available ad from the waterfall pool.
     * Otherwise it returns the first cached ad matching the resolved ad unit id.
     */
    override fun getLoadedAd(adUnitKey: String): NativeAd? {
        if (waterfallApply) {
            return getWaterFall()
        } else {
            val adUnitId = adClient.getAdUnitId(adUnitKey)
            return nativeAdMapper.values.firstOrNull { it.adUnitId == adUnitId }?.ad
        }
    }

    /**
     * Returns the next available waterfall native ad.
     *
     * Waterfall behavior:
     * - in new-loading-mode: prefers [nativeAdPoolHolder] first, then base-high, then base-low.
     * - in old-loading-mode: scans [priorityOrder] and returns the first holder with an ad.
     *
     * This method also clears the ad from its holder to avoid showing the same instance twice.
     */
    fun getWaterFall(): NativeAd? {
        if (newLoadingMode) {
            if (nativeAdPoolHolder?.nativeAd != null) {
                val tmpNativeAd = nativeAdPoolHolder?.nativeAd
                nativeAdPoolHolder?.nativeAd = null
                return tmpNativeAd
            } else if (nativeAdHolderUnitIds["N-Base-High"]?.nativeAd != null) {
                val tmpNativeAd = nativeAdHolderUnitIds["N-Base-High"]?.nativeAd
                nativeAdHolderUnitIds["N-Base-High"]?.nativeAd = null
                return tmpNativeAd
            } else if (nativeAdHolderUnitIds["N-Base-Low"]?.nativeAd != null) {
                val tmpNativeAd = nativeAdHolderUnitIds["N-Base-Low"]?.nativeAd
                nativeAdHolderUnitIds["N-Base-Low"]?.nativeAd = null
                return tmpNativeAd
            } else {
                return null
            }
        }
        for (name in priorityOrder) {
            val holder = nativeAdHolderUnitIds[name]
            if (holder != null && holder.nativeAd != null) {
                val tmpNativeAd = holder.nativeAd
                holder.nativeAd = null
                return tmpNativeAd
            }
        }
        return null
    }

    /**
     * Pauses background preloading for all waterfall holders.
     */
    override fun pauseWaterFall() {
        for (holder in nativeAdHolderUnitIds.values) {
            holder.pause = true
        }
        nativeAdPoolHolder?.pause = true
    }

    /**
     * Resumes background preloading for all waterfall holders.
     */
    override fun resumeWaterFall() {
        for (holder in nativeAdHolderUnitIds.values) {
            holder.resumeLoadAd()
        }
        nativeAdPoolHolder?.resumeLoadAd()
    }

    /**
     * Loads the waterfall preloading system.
     *
     * This creates and triggers base holders (low/high) and then either:
     * - new-loading-mode: creates a [NativeAdPoolHolder] that rotates through configured ids
     * - old-loading-mode: creates a [NativeAdHolder] per configured id
     *
     * Ramp-up delay is applied between startup loads via [applyDelay].
     */
    override suspend fun loadWaterFall() {
        waterfallApply = remoteConfig.getWaterfallApply()
        newLoadingMode = remoteConfig.getWaterfallLoadingMode() == 1

        if (waterfallApply && nativeAdHolderUnitIds.isEmpty()) {
            maybeLoadAd("N-Base-Low", adClient.getLowAdId(AdType.native))
            applyDelay()
            maybeLoadAd("N-Base-High", adClient.getHighAdId(AdType.native))
            applyDelay()
            if (newLoadingMode && nativeAdPoolHolder == null) {

                nativeAdPoolHolder = NativeAdPoolHolder(
                    context,
                    this,
                    adUnitIdMapper.values.toList(),
                    adUnitIdMapper.keys.toList(),
                    adClient,
                )
                nativeAdPoolHolder?.loadAd()
            } else {
                for (entry in adUnitIdMapper) {
                    val name = entry.key
                    val unitId = entry.value
                    if (unitId.isNotEmpty()) {

                        nativeAdHolderUnitIds[name] = NativeAdHolder(
                            context,
                            this,
                            unitId,

                            adClient,
                        )
                        nativeAdHolderUnitIds[name]?.loadAd()
                        applyDelay()
                    }
                }
            }
        }
    }

    /**
     * Returns an available (ready) waterfall holder, if implemented.
     *
     * TODO: implement selection logic similar to the interstitial manager.
     */
    override fun getAvailableAdHolder(): Any? {
        TODO("Not yet implemented")
    }

    /**
     * Returns an available (ready) high-priority waterfall holder, if implemented.
     *
     * TODO: implement selection logic similar to the interstitial manager.
     */
    override fun getHighAvailableAdHolder(): Any? {
        TODO("Not yet implemented")
    }

    /**
     * Creates a base waterfall holder named [name] and starts loading if [adUnitId] is not empty.
     */
    private fun maybeLoadAd(name: String, adUnitId: String) {
        if (adUnitId.isEmpty()) return
        nativeAdHolderUnitIds[name] = NativeAdHolder(
            context,
            this,
            adUnitId,

            adClient,
        )
        nativeAdHolderUnitIds[name]?.loadAd()
    }

    /**
     * Applies ramp-up delay between waterfall loads.
     */
    private suspend fun applyDelay() {
        delay(remoteConfig.getAdsRampUpTime())
    }

    /**
     * Marks [adUnitId] as currently loading to prevent duplicate in-flight loads.
     */
    fun addLoadingAd(adUnitId: String) {
        loadingAd.add(adUnitId)
    }

    /**
     * Removes [adUnitId] from the "currently loading" set.
     */
    fun removeLoadingAd(adUnitId: String) {
        loadingAd.remove(adUnitId)
    }

    /**
     * Returns the remote-configured reload timing settings used by waterfall holders.
     */
    fun getWaterfallReloadTime(): Map<String, Any> {
        return waterfallReloadTime
    }

    /**
     * If there is at least one queued native request, starts loading the first one.
     *
     * This enforces sequential loading using [waitingQueue] + [failCount].
     */
    private fun checkAndLoadAds(saved: Boolean?) {
        if (waitingQueue.isNotEmpty()) {
            val nativeConfig = waitingQueue.entries.first().value
            val adUnitKey = nativeConfig.adUnitKey
            val factoryId = nativeConfig.factoryId
            waitingQueue.remove(nativeConfig.id)
            failCount = 0
            // start initial load with high floor
            load(
                adUnitKey = adUnitKey,
                factoryId = factoryId,
                adChoicesPlacement = null,
                isHighFloor = true,
                saved = saved
            )
        }
    }

    /**
     * Enqueues a native ad request for [adUnitKey] + [factoryId] and starts loading sequentially.
     *
     * This is a non-waterfall flow used when you want a specific factory/layout.
     *
     * @param context kept for interface compatibility; this implementation uses injected
     * application [context].
     * @param adUnitKey logical key resolved to ad unit id via [AdClient].
     * @param factoryId layout/factory identifier.
     * @param adChoicesPlacement optional placement for AdChoices overlay.
     * @param saved if true, the loaded ad is recorded as a "saved" ad; if false it is recorded
     * as a "new" ad for layout switching.
     */
    override fun preloadAd(
        context: Context,
        adUnitKey: String,
        factoryId: String,
        adChoicesPlacement: NativeAdOptions.AdChoicesPlacement?,
        saved: Boolean?
    ) {
        if (waterfallApply) return
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        val configId = adUnitId + factoryId
        if (contain(configId)) return
        waitingQueue.putIfAbsent(configId, NativeConfig.create(adClient, adUnitKey, factoryId))
        checkAndLoadAds(saved)
    }

    /**
     * Performs the actual native-ad load.
     *
     * Behavior:
     * - tries high-floor id first when [isHighFloor] is true
     * - on high-floor failure: waits [delayTime] then retries low-floor once
     * - on low-floor failure: increments [failCount] and retries until [maxLoadFailCounter]
     *
     * Successful loads are cached in [nativeAdMapper] using key `adUnitId + factoryId`.
     */
    private fun load(
        adUnitKey: String,
        factoryId: String,
        adChoicesPlacement: NativeAdOptions.AdChoicesPlacement?,
        isHighFloor: Boolean?,
        saved: Boolean?
    ) {
        if (adClient.canDismissAds()) return
        var adUnitId = adClient.getAdUnitId(adUnitKey)
        val highFloorAdUnitId = adClient.getHighFloor(adUnitKey)

        if (isHighFloor ?: true) {
            adUnitId = highFloorAdUnitId ?: adUnitId
        }

        val key = adUnitId + factoryId

        // mark as loading
        loadingNativeAdMapper[key] = NativeConfig.create(adClient, adUnitKey, factoryId)

        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                // Success
                val nativeConfig = NativeConfig.create(adClient, adUnitKey, factoryId, nativeAd)
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
                    adClient.notifyAdClick()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    val highFloor = isHighFloor ?: true
                    val handler = Handler(Looper.getMainLooper())

                    if (highFloor) {
                        // First failure at high floor: delay then retry with low floor
                        handler.postDelayed({
                            load(adUnitKey, factoryId, adChoicesPlacement, false, saved)
                        }, delayTime.toLong())
                    } else {
                        // Failure at low floor: count failures and possibly give up
                        failCount++
                        handler.postDelayed({
                            if (failCount >= maxLoadFailCounter) {
                                // Remove this request, and stop.
                                waitingQueue.remove(key)
                                loadingNativeAdMapper.remove(key)
                                return@postDelayed
                            }
                            // Retry again with low floor
                            load(adUnitKey, factoryId, adChoicesPlacement, false, saved)
                        }, delayTime.toLong())
                    }
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }


}
