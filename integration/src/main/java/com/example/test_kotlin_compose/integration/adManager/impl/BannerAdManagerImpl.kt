package com.example.test_kotlin_compose.integration.adManager.impl

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.test_kotlin_compose.integration.adManager.AdClient
import com.example.test_kotlin_compose.integration.adManager.api.BannerAdManagerInterface
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BannerAdManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adClient: AdClient
) : BannerAdManagerInterface {

    private val bannerAdMapper = mutableMapOf<String, AdView>()
    var maxLoadFailCounter = 3
    var delayTime = 100

    /**
     * Initializes the banner-ad manager.
     *
     * This implementation currently doesn't pull remote config, but the hook exists so the app
     * can initialize all ad managers in a uniform way.
     *
     * @param context kept for interface compatibility; this implementation uses injected
     * application [context].
     */
    override suspend fun init(context: Context) {
        // Initialize any configuration if needed
    }

    /**
     * Returns true if a banner [AdView] is cached for [adUnitKey].
     *
     * Note: this is purely an in-memory cache check. It doesn't guarantee the ad has already
     * finished loading; that depends on the [AdView] lifecycle in your UI.
     */
    override fun isAdLoaded(adUnitKey: String): Boolean {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        return bannerAdMapper.containsKey(adUnitId)
    }

    /**
     * Destroys and removes the cached banner ad associated with [adUnitKey].
     *
     * Call this when a screen is permanently leaving and you don't want to reuse the banner.
     */
    override fun destroyAd(adUnitKey: String) {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        bannerAdMapper[adUnitId]?.destroy()
        bannerAdMapper.remove(adUnitId)
    }

    /**
     * Destroys and clears all cached banner ads.
     */
    override fun destroyAll() {
        bannerAdMapper.values.forEach { it.destroy() }
        bannerAdMapper.clear()
    }

    /**
     * Returns the cached [AdView] for [adUnitKey] if present.
     *
     * @return [AdView] typed as [Any] for interface compatibility, or `null`.
     */
    override fun getLoadedAd(adUnitKey: String): Any? {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        return bannerAdMapper[adUnitId]
    }

    /**
     * Convenience typed getter for the cached banner [AdView] for [adUnitKey].
     */
    fun get(adUnitKey: String): AdView? {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        return bannerAdMapper[adUnitId]
    }

    /**
     * Caches a banner [AdView] under the resolved ad unit id for [adUnitKey].
     *
     * @param adUnitKey logical key resolved to the real ad unit id via [AdClient].
     * @param adView the [AdView] instance to cache.
     */
    fun save(adUnitKey: String, adView: AdView) {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        bannerAdMapper[adUnitId] = adView
    }

    /**
     * Creates an anchored adaptive [AdSize] for the current device orientation.
     *
     * This uses the device screen width in dp and returns
     * [AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize].
     *
     * Note: `defaultDisplay` / `getMetrics` are deprecated on newer Android versions; we keep
     * this implementation for broad compatibility. If you want to fully modernize it, migrate
     * to `WindowMetrics`.
     */
    @Suppress("DEPRECATION")
    fun createAdSize(): AdSize {
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

    /**
     * Returns the extras bundle for collapsible banners (if configured).
     *
     * Google Mobile Ads supports a `collapsible` extra, commonly set to `top` or `bottom`.
     * We read that value from [AdClient.adBannerPlaces] at key `banner_collapsible_position`.
     *
     * @return a [Bundle] with `collapsible` set, or `null` when not configured.
     */
    fun getCollapsibleExtras(adUnitKey: String): Bundle? {
        val position = adClient.adBannerPlaces[adUnitKey]?.get("banner_collapsible_position") as? String

        return if (position != null) {
            Bundle().apply { putString("collapsible", position) }
        } else {
            null
        }
    }
}
