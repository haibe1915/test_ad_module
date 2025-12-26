package com.example.test_kotlin_compose.integration.adManager.api

import android.content.Context
import com.google.android.gms.ads.nativead.NativeAdOptions

interface AdManager {
    suspend fun init(context: Context)

    fun isAdLoaded(adUnitKey: String): Boolean

    fun destroyAd(adUnitKey: String)

    fun destroyAll()

    fun getLoadedAd(adUnitKey: String): Any?
}

interface Preloadable

interface InterstitialPreloadable : Preloadable {
    fun preloadAd(adUnitKey: String, ignoreCooldown: Boolean? = false)
}

interface OpenAdPreloadable : Preloadable {
    fun preloadAd(adUnitKey: String)
}

interface NativePreloadable : Preloadable {
    fun preloadAd(
        context: Context,
        adUnitKey: String,
        factoryId: String,
        adChoicesPlacement: NativeAdOptions.AdChoicesPlacement? = null,
        saved: Boolean? = null,
    )
}

interface RewardPreloadable : Preloadable {
    fun preloadAd(adUnitKey: String)
}

interface WaterfallAdManager : AdManager {
    fun pauseWaterFall()
    fun resumeWaterFall()
    suspend fun loadWaterFall()
    fun getAvailableAdHolder(): Any?
    fun getHighAvailableAdHolder(): Any?
}

class CompositeAdManager(private val managers: List<AdManager>) : AdManager {

    override suspend fun init(context: Context) {
        managers.forEach { it.init(context) }
    }

    override fun isAdLoaded(adUnitKey: String): Boolean {
        return managers.any { it.isAdLoaded(adUnitKey) }
    }

    override fun destroyAd(adUnitKey: String) {
        managers.forEach { it.destroyAd(adUnitKey) }
    }

    override fun destroyAll() {
        managers.forEach { it.destroyAll() }
    }

    override fun getLoadedAd(adUnitKey: String): Any? {
        return managers.firstNotNullOfOrNull { it.getLoadedAd(adUnitKey) }
    }
}

interface NativeAdManagerInterface : WaterfallAdManager, NativePreloadable

interface InterstitialAdManagerInterface : WaterfallAdManager, InterstitialPreloadable

interface BannerAdManagerInterface : AdManager

interface OpenAdManagerInterface : AdManager, OpenAdPreloadable

interface RewardAdManagerInterface : AdManager, RewardPreloadable
