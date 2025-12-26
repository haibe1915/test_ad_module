package com.example.test_kotlin_compose.integration.adManager.impl

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.test_kotlin_compose.integration.adManager.AdClient
import com.example.test_kotlin_compose.integration.adManager.api.OpenAdManagerInterface
import com.example.test_kotlin_compose.integration.firebase.AdRemoteConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAdManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfig: AdRemoteConfig,
    private val adClient: AdClient,
) : OpenAdManagerInterface {

    private val appOpenAdMapper = mutableMapOf<String, AppOpenAd?>()
    private val loadingAd = mutableSetOf<String>()
    private var configAppOpen = mapOf<String, Any>()
    private var showedTime: Date? = null
    private var cooldownTime: Int = 0
    private var delayTime: Int = 100
    var isDisable: Boolean = false
    private var isShowingAd: Boolean = false

    private var cachedNextAd: Boolean = false
    private val cachedOpenAds = mutableListOf<AppOpenAd>()
    private var doneGetPermission: Boolean = false
    private var parallelLoadingMode: Boolean = false
    private var waitingCallback: (() -> Unit)? = null
    private var waitingString: String? = null

    /**
     * Initializes app-open ad settings.
     *
     * Reads ad-related values from [AdRemoteConfig] and [AdClient].
     * This should be called once at app startup before any load/show calls.
     *
     * @param context kept for interface compatibility; this implementation uses the injected
     * application [context].
     */
    override suspend fun init(context: Context) {
        configAppOpen = remoteConfig.getConfigAdAppOpen()
        cooldownTime = (configAppOpen["time interval"] as? Number)?.toInt() ?: cooldownTime
        delayTime = adClient.getOpenAppFailReloadTime()
        parallelLoadingMode = (configAppOpen["ads_pair_load_mode"] as? Int) == 1
    }

    /**
     * Returns the currently cached [AppOpenAd] instance (if any) for the given [adUnitKey].
     *
     * @param adUnitKey logical key used by [AdClient] to resolve the real ad unit id.
     * @return an [AppOpenAd] (typed as [Any] for interface compatibility), or `null`.
     */
    override fun getLoadedAd(adUnitKey: String): Any? {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        return appOpenAdMapper[adUnitId]
    }

    /**
     * Checks whether an ad is cached for [adUnitKey].
     *
     * Note: this checks the mapping key; if the mapped value is `null`, this still returns true
     * if the key exists. Prefer [isAdAvailable] when you need a usable ad instance.
     */
    override fun isAdLoaded(adUnitKey: String): Boolean {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        return appOpenAdMapper.containsKey(adUnitId)
    }

    /**
     * Removes and forgets the cached ad for [adUnitKey].
     *
     * This does not call `dispose()` on the object because the Android SDK does not expose it
     * for app-open ads; releasing the reference is enough.
     */
    override fun destroyAd(adUnitKey: String) {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        appOpenAdMapper.remove(adUnitId)
    }

    /**
     * Clears all cached app-open ads.
     */
    override fun destroyAll() {
        appOpenAdMapper.clear()
    }

    /**
     * Updates a flag used by splash logic to decide whether to immediately continue once the
     * ad is loaded.
     *
     * In your flow, this typically becomes `true` after the permission dialog finishes.
     */
    fun updateDoneGetPermission(value: Boolean) {
        doneGetPermission = value
    }

    /**
     * Returns `true` if we recently showed an app-open ad and should respect the cooldown.
     */
    private fun availableCooldowmTime(): Boolean {
        if (showedTime == null) return false
        val diff = Date().time - showedTime!!.time
        return diff < cooldownTime * 1000
    }

    /**
     * Starts loading an app-open ad intended for the splash flow.
     *
     * This method supports:
     * - high-floor fallback (try high; if fails, try low)
     * - optional timeout callback
     * - optional callback when an ad is loaded AND permission is already granted
     *
     * @param adUnitKey logical key used to resolve ad unit ids.
     * @param isHighFloor whether to try the high-floor id first.
     * @param callback invoked when the flow should continue (e.g., navigate away from splash).
     * @param timeoutCallback invoked if loading didn't finish within [duration].
     * @param duration timeout duration in ms.
     */
    fun loadAdInSplash(
        adUnitKey: String,
        isHighFloor: Boolean? = true,
        callback: (() -> Unit)? = null,
        timeoutCallback: (() -> Unit)? = null,
        duration: Int = 5000,
    ) {
        if (parallelLoadingMode) {
            loadOpenAdSequential(adUnitKey, isHighFloor, callback, timeoutCallback, duration)
        } else {
            loadOpenAdSequential(adUnitKey, isHighFloor, callback, timeoutCallback, duration)
        }
    }

    /**
     * Preloads an app-open ad in the background.
     *
     * No callbacks are invoked. This is meant for proactive caching.
     */
    override fun preloadAd(adUnitKey: String) {
        if (adClient.canDismissAds()) return

        val adUnitId = adClient.getAdUnitId(adUnitKey)

        if (loadingAd.contains(adUnitId) || appOpenAdMapper.containsKey(adUnitId)) {
            return
        }

        loadingAd.add(adUnitId)
        load(adUnitKey, true)
    }

    /**
     * Loads an app-open ad and stores it into [appOpenAdMapper].
     *
     * This is used by both preload and post-show refresh.
     */
    private fun load(
        adUnitKey: String,
        isHighFloor: Boolean? = true,
    ) {
        if (adClient.canDismissAds()) return

        val adUnitId = adClient.getAdUnitId(adUnitKey)
        val highFloorAdUnitId = adClient.getHighFloor(adUnitKey)

        val currentAdUnitId =
            if (isHighFloor == true && highFloorAdUnitId != null) highFloorAdUnitId else adUnitId

        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            currentAdUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                /**
                 * Called by GMA when the ad successfully loads. We cache it under the *low* id key
                 * ([adUnitId]) so the rest of the manager can find it consistently.
                 */
                override fun onAdLoaded(ad: AppOpenAd) {
                    loadingAd.remove(adUnitId)
                    appOpenAdMapper[adUnitId] = ad
                }

                /**
                 * Called by GMA when loading fails. When high-floor is enabled, we retry once with
                 * the low-floor id after [delayTime].
                 */
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (isHighFloor == true && highFloorAdUnitId != null) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            load(adUnitKey, false)
                        }, delayTime.toLong())
                    } else {
                        loadingAd.remove(adUnitId)
                    }
                }
            },
        )
    }

    /**
     * Sequential "load for splash" flow.
     *
     * Implements:
     * - optional timeout that triggers [timeoutCallback] and flips [cachedNextAd]
     * - high-floor fallback
     * - caching behavior: if we timed out, keep the ad in [cachedOpenAds] for later.
     */
    private fun loadOpenAdSequential(
        adUnitKey: String,
        isHighFloor: Boolean? = true,
        callback: (() -> Unit)? = null,
        timeoutCallback: (() -> Unit)? = null,
        duration: Int = 5000,
    ) {
        if (adClient.canDismissAds()) return

        var adComplete = false

        if (timeoutCallback != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (!adComplete) {
                    cachedNextAd = true
                    timeoutCallback.invoke()
                }
            }, duration.toLong())
        }

        val adUnitId = adClient.getAdUnitId(adUnitKey)
        val highFloorAdUnitId = adClient.getHighFloor(adUnitKey)

        val currentAdUnitId =
            if (isHighFloor == true && highFloorAdUnitId?.isNotEmpty() ?: false) highFloorAdUnitId else adUnitId

        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            context,
            currentAdUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                /**
                 * Caches or stores the loaded ad depending on whether we already timed out.
                 *
                 * If [doneGetPermission] is true, also triggers [callback] to continue the flow.
                 */
                override fun onAdLoaded(ad: AppOpenAd) {
                    loadingAd.remove(adUnitId)

                    if (cachedNextAd) {
                        cachedOpenAds.add(ad)
                        cachedNextAd = false
                    } else if (doneGetPermission) {
                        adComplete = true
                        appOpenAdMapper[adUnitId] = ad
                        callback?.invoke()
                        doneGetPermission = false
                    } else {
                        adComplete = true
                        appOpenAdMapper[adUnitId] = ad
                    }
                }

                /**
                 * On failure we optionally retry with low-floor id (once) if high-floor was used.
                 */
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    adComplete = true
                    appOpenAdMapper[adUnitId] = null
                    if (!cachedNextAd && isHighFloor == true) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadAdInSplash(
                                adUnitKey,
                                false,
                                callback,
                                timeoutCallback,
                                duration,
                            )
                        }, delayTime.toLong())
                    }
                }
            },
        )
    }

    /**
     * Shows an app-open ad if one is available.
     *
     * If no ad is available, this method calls [onShowAdComplete] immediately and triggers
     * a background [load] to refill the cache.
     */
    fun showAdIfAvailable(
        activity: Activity,
        adUnitKey: String,
        onShowAdComplete: () -> Unit,
    ) {
        if (isShowingAd) return

        if (adClient.canDismissAds()) {
            onShowAdComplete()
            return
        }

        if (!isAdAvailable(adUnitKey)) {
            onShowAdComplete()
            load(adUnitKey)
            return
        }

        val adUnitId = adClient.getAdUnitId(adUnitKey)
        val highAdUnitId = adClient.getHighFloor(adUnitKey)
        var appOpenAd = highAdUnitId?.let { appOpenAdMapper[it] }
        if (appOpenAd == null) {
            appOpenAd = appOpenAdMapper[adUnitId]
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            /**
             * Called when the ad is dismissed. We clear cache, update cooldown time, and reload.
             */
            override fun onAdDismissedFullScreenContent() {
                isShowingAd = false
                appOpenAdMapper.remove(adUnitId)
                onShowAdComplete()
                load(adUnitKey)
                showedTime = Date()
            }

            /**
             * If the ad fails to show, we clear it, continue, and attempt to reload.
             */
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isShowingAd = false
                appOpenAdMapper.remove(adUnitId)
                onShowAdComplete()
                load(adUnitKey)
            }

            /**
             * Marks manager state as "showing" to prevent concurrent shows.
             */
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }
        }
        isShowingAd = true
        appOpenAd?.show(activity)
    }

    /**
     * Returns true if an ad is cached for either low-floor or high-floor id.
     */
    private fun isAdAvailable(adUnitKey: String): Boolean {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        val highAdUnitId = adClient.getHighFloor(adUnitKey)
        return appOpenAdMapper.containsKey(adUnitId) ||
            (highAdUnitId != null && appOpenAdMapper.containsKey(highAdUnitId))
    }

    /**
     * Loads an app-open ad and shows it immediately.
     *
     * Supports:
     * - cooldown blocking
     * - timeout callback
     * - high-floor fallback
     *
     * @param activity activity used to show the ad.
     * @param adUnitKey logical key for ad unit id lookup.
     * @param callback invoked after the ad flow is complete (or skipped).
     * @param timeoutCallback invoked if loading hasn't completed within [duration].
     * @param duration timeout duration in ms.
     * @param isHighFloor whether to attempt high-floor id first.
     */
    fun loadAdAndShow(
        activity: Activity,
        adUnitKey: String,
        callback: (() -> Unit)? = null,
        timeoutCallback: (() -> Unit)? = null,
        duration: Int = 5000,
        isHighFloor: Boolean = true,
    ) {
        if (adClient.canDismissAds() || availableCooldowmTime()) {
            callback?.invoke()
            return
        }

        var adComplete = false

        if (timeoutCallback != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (!adComplete) {
                    cachedNextAd = true
                    timeoutCallback.invoke()
                }
            }, duration.toLong())
        }

        val adUnitId = adClient.getAdUnitId(adUnitKey)
        val highFloorId = adClient.getHighFloor(adUnitKey)
        val currentAdUnitId = if (isHighFloor && highFloorId != null) highFloorId else adUnitId

        AppOpenAd.load(
            context,
            currentAdUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                /**
                 * On load success, cache and immediately show.
                 */
                override fun onAdLoaded(ad: AppOpenAd) {
                    adComplete = true
                    appOpenAdMapper[adUnitId] = ad
                    showAdIfAvailable(activity, adUnitKey) {
                        callback?.invoke()
                    }
                }

                /**
                 * On load failure, when high-floor is enabled we retry once with low-floor after 1s.
                 * Otherwise we just continue the flow by calling [callback].
                 */
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (isHighFloor && highFloorId != null) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadAdAndShow(
                                activity,
                                adUnitKey,
                                callback,
                                null,
                                duration,
                                false,
                            )
                        }, 1000)
                    } else {
                        adComplete = true
                        callback?.invoke()
                    }
                }
            },
        )
    }
}
