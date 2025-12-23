package com.example.test_kotlin_compose.integration.adManager

import android.content.Context
import com.google.android.gms.ads.nativead.NativeAdOptions

interface AdManager {
    suspend fun init(context: Context)

    // Check if ready
    fun isAdLoaded(adUnitName: AdUnitName): Boolean

    // Clean up specific ad
    fun destroyAd(adUnitName: AdUnitName)

    // Clean up everything (e.g. on app termination)
    fun destroyAll()

    fun getLoadedAd(adUnitName: AdUnitName): Any?
}

interface Preloadable

interface InterstitialPreloadable : Preloadable {
    fun preloadAd(adUnitName: AdUnitName, ignoreCooldown: Boolean? = false)
}

interface OpenAdPreloadable : Preloadable {
    fun preloadAd(adUnitName: AdUnitName)
}

interface NativePreloadable : Preloadable {
    fun preloadAd(
        context: Context,
        adUnitName: AdUnitName,
        factoryId: String,
        adChoicesPlacement: NativeAdOptions.AdChoicesPlacement? = null,
        saved: Boolean? = null
    )
}
interface RewardPreloadable : Preloadable {
    fun preloadAd(adUnitName: AdUnitName)
}

interface WaterfallAdManager : AdManager {
    fun pauseWaterFall()
    fun resumeWaterFall()
    suspend fun loadWaterFall()
    fun getAvailableAdHolder(): Any?
    fun getHighAvailableAdHolder(): Any?

}

class CompositeAdManager(private val managers: List<AdManager>) : AdManager {

    override suspend  fun init(context: Context) {
        // This iterates through all child managers and calls their init method
        managers.forEach { it.init(context) }
    }

    override fun isAdLoaded(adUnitName: AdUnitName): Boolean {
        // Returns true if ANY manager has this ad loaded
        return managers.any { it.isAdLoaded(adUnitName) }
    }

    override fun destroyAd(adUnitName: AdUnitName) {
        managers.forEach { it.destroyAd(adUnitName) }
    }

    override fun destroyAll() {
        managers.forEach { it.destroyAll() }
    }

    override fun getLoadedAd(adUnitName: AdUnitName): Any? {
        // Returns the first non-null ad found
        return managers.firstNotNullOfOrNull { it.getLoadedAd(adUnitName) }
    }
}

interface NativeAdManagerInterface : WaterfallAdManager, NativePreloadable

interface InterstitialAdManagerInterface : WaterfallAdManager, InterstitialPreloadable

interface BannerAdManagerInterface : AdManager

interface OpenAdManagerInterface : AdManager, OpenAdPreloadable

interface RewardAdManagerInterface : AdManager, RewardPreloadable
