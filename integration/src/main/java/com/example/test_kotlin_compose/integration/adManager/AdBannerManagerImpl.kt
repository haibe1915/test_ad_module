package com.example.test_kotlin_compose.integration.adManager

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.test_kotlin_compose.integration.firebase.RemoteConfigProvider
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BannerAdManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfig: RemoteConfigProvider
) : BannerAdManagerInterface {

    private val bannerAdMapper = mutableMapOf<String, AdView>()
    var maxLoadFailCounter = 3
    var delayTime = 100

    override suspend fun init(context: Context) {
        // Initialize any configuration if needed
        // maxLoadFailCounter = ...
        // delayTime = ...
    }

    override fun isAdLoaded(adUnitName: AdUnitName): Boolean {
        val adUnitId = AdClient.getAdUnitId(adUnitName)
        return bannerAdMapper.containsKey(adUnitId)
    }

    override fun destroyAd(adUnitName: AdUnitName) {
        val adUnitId = AdClient.getAdUnitId(adUnitName)
        bannerAdMapper[adUnitId]?.destroy()
        bannerAdMapper.remove(adUnitId)
    }

    override fun destroyAll() {
        bannerAdMapper.values.forEach { it.destroy() }
        bannerAdMapper.clear()
    }

    override fun getLoadedAd(adUnitName: AdUnitName): Any? {
        TODO("Not yet implemented")
    }

    fun get(adUnitName: AdUnitName): AdView? {
        val adUnitId = AdClient.getAdUnitId(adUnitName)
        return bannerAdMapper[adUnitId]
    }

    fun save(adUnitName: AdUnitName, adView: AdView) {
        val adUnitId = AdClient.getAdUnitId(adUnitName)
        bannerAdMapper[adUnitId] = adView
    }

    fun createAdSize(adUnitName: AdUnitName): AdSize {
        // Logic to determine ad size based on adUnitName or configuration
        // For now returning default BANNER
        // return AdSize.BANNER

        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density

        var adWidthPixels = outMetrics.widthPixels.toFloat()
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    fun getCollapsibleExtras(adUnitName: AdUnitName): Bundle? {
         // val collapsiblePosition = AdClient.collapsibleBanner.contains(adUnitName)
         // Logic to get collapsible position string from AdClient or RemoteConfig if needed
         // Based on Dart code: AdsClient.adBannerPlaces[widget.adUnitName]?["banner_collapsible_position"]

         // This part needs to be adapted based on how AdClient stores this info in Kotlin
         // Assuming AdClient has a way to get this map or we access RemoteConfig directly
         val adPlaces = remoteConfig.getAdPlacesBanner()
         val position = adPlaces[adUnitName]?.get("banner_collapsible_position") as? String

         if (position != null) {
             val extras = Bundle()
             extras.putString("collapsible", position)
             return extras
         }
         return null
    }
}
