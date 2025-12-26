package com.example.test_kotlin_compose.integration.adManager.impl

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.test_kotlin_compose.integration.adManager.AdClient
import com.example.test_kotlin_compose.integration.adManager.api.RewardAdManagerInterface
import com.example.test_kotlin_compose.integration.firebase.AdRemoteConfig
import com.example.test_kotlin_compose.util.LoadingDialogUtil
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardAdManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfig: AdRemoteConfig,
    private val adClient: AdClient,
) : RewardAdManagerInterface {

    private val rewardedAdMapper = mutableMapOf<String, RewardedAd>()
    private val loadingAd = mutableSetOf<String>()
    private var configReward = mapOf<String, Any>()
    private var showedTime: Date? = null
    private var cooldownTime: Int = 90
    private var delayTime: Int = 100
    private var isShowingAd: Boolean = false

    /**
     * Initializes the rewarded-ad manager from remote config.
     *
     * Reads cooldown and retry delay settings from [AdRemoteConfig] and [AdClient].
     * Call this once at app startup before calling [preloadAd], [showAd], or [loadAdAndShow].
     *
     * @param context kept for interface compatibility; this implementation uses injected
     * application [context].
     */
    override suspend fun init(context: Context) {
        configReward = remoteConfig.getConfigAdReward()
        cooldownTime = (configReward["time interval"] as? Number)?.toInt() ?: cooldownTime
        delayTime = adClient.getRewardAdFailReloadTime()
    }

    /**
     * Returns the loaded rewarded ad for [adUnitKey] if one is cached.
     *
     * @param adUnitKey logical key resolved to an ad unit id via [AdClient].
     * @return a [RewardedAd] (typed as [Any] for interface compatibility), or `null`.
     */
    override fun getLoadedAd(adUnitKey: String): Any? {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        return rewardedAdMapper[adUnitId]
    }

    /**
     * Returns true if there is a cached rewarded ad for [adUnitKey].
     */
    override fun isAdLoaded(adUnitKey: String): Boolean {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        return rewardedAdMapper.containsKey(adUnitId)
    }

    /**
     * Removes the cached rewarded ad associated with [adUnitKey].
     */
    override fun destroyAd(adUnitKey: String) {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        rewardedAdMapper.remove(adUnitId)
    }

    /**
     * Clears all cached rewarded ads.
     */
    override fun destroyAll() {
        rewardedAdMapper.clear()
    }

    // Load Ad Logic

    /**
     * Preloads a rewarded ad for [adUnitKey] and stores it in memory.
     *
     * This method:
     * - does nothing if ads are disabled via [AdClient.canDismissAds]
     * - avoids duplicate in-flight loads via [loadingAd]
     * - uses high-floor id first (if available) and falls back to low-floor id after [delayTime]
     */
    override fun preloadAd(adUnitKey: String) {
        if (adClient.canDismissAds()) return

        val adUnitId = adClient.getAdUnitId(adUnitKey)
        val highFloorAdUnitId = adClient.getHighFloor(adUnitKey)

        if (loadingAd.contains(adUnitId) || rewardedAdMapper.containsKey(adUnitId)) {
            return
        }

        loadingAd.add(adUnitId)
        load(adUnitKey, adUnitId, highFloorAdUnitId, true)
    }

    /**
     * Internal load implementation used by [preloadAd] (and high-floor fallback).
     *
     * @param adUnitKey logical key used for diagnostics/logging if needed.
     * @param adUnitId low-floor ad unit id (used as the cache key).
     * @param highFloorAdUnitId optional high-floor id.
     * @param isHighFloor whether to load using [highFloorAdUnitId] when available.
     */
    private fun load(
        adUnitKey: String,
        adUnitId: String,
        highFloorAdUnitId: String?,
        isHighFloor: Boolean,
    ) {
        val currentAdUnitId = if (isHighFloor && highFloorAdUnitId != null) highFloorAdUnitId else adUnitId

        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            currentAdUnitId,
            request,
            object : RewardedAdLoadCallback() {
                /**
                 * Called by Google Mobile Ads when loading succeeds.
                 * We cache under the low-floor [adUnitId] so the rest of the manager can find it.
                 */
                override fun onAdLoaded(ad: RewardedAd) {
                    loadingAd.remove(adUnitId)
                    rewardedAdMapper[adUnitId] = ad
                }

                /**
                 * Called by Google Mobile Ads when loading fails.
                 * If high-floor is enabled we retry once with low-floor id after [delayTime].
                 */
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (isHighFloor && highFloorAdUnitId != null) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            load(adUnitKey, adUnitId, highFloorAdUnitId, false)
                        }, delayTime.toLong())
                    } else {
                        loadingAd.remove(adUnitId)
                    }
                }
            },
        )
    }

    // Show Ad Logic

    /**
     * Shows a cached rewarded ad (if available).
     *
     * Behavior:
     * - if an ad is available: shows it, calls [onUserEarnedReward] when the user earns reward,
     *   then calls [onAdDismissed] on dismiss.
     * - if no ad is available: calls [onAdFailedToShow] and triggers [preloadAd].
     * - after dismiss/fail-to-show: removes cached instance and triggers [preloadAd] to refill.
     *
     * @param activity foreground activity required by the GMA SDK for showing.
     * @param adUnitKey logical key to resolve the ad unit id.
     * @param onUserEarnedReward called with the RewardItem (typed as [Any] to keep module API
     * consistent).
     * @param onAdDismissed optional callback invoked after the ad is dismissed.
     * @param onAdFailedToShow optional callback invoked if ad couldn't be shown.
     */
    fun showAd(
        activity: Activity,
        adUnitKey: String,
        onUserEarnedReward: (Any) -> Unit,
        onAdDismissed: (() -> Unit)? = null,
        onAdFailedToShow: (() -> Unit)? = null,
    ) {
        if (isShowingAd) return

        if (adClient.canDismissAds()) {
            onAdDismissed?.invoke()
            return
        }

        val adUnitId = adClient.getAdUnitId(adUnitKey)
        val rewardedAd = rewardedAdMapper[adUnitId]

        if (rewardedAd != null) {
            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                /**
                 * Called when the rewarded ad is dismissed.
                 * We clear cache, mark cooldown, and start preloading the next ad.
                 */
                override fun onAdDismissedFullScreenContent() {
                    isShowingAd = false
                    rewardedAdMapper.remove(adUnitId)
                    onAdDismissed?.invoke()
                    preloadAd(adUnitKey)
                    showedTime = Date()
                }

                /**
                 * Called when the rewarded ad fails to show.
                 * We clear cache and try preloading again.
                 */
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    isShowingAd = false
                    rewardedAdMapper.remove(adUnitId)
                    onAdFailedToShow?.invoke()
                    preloadAd(adUnitKey)
                }

                /**
                 * Marks manager state as "showing" to prevent concurrent shows.
                 */
                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                }
            }
            isShowingAd = true
            rewardedAd.show(activity) { rewardItem ->
                onUserEarnedReward(rewardItem)
            }
        } else {
            onAdFailedToShow?.invoke()
            preloadAd(adUnitKey)
        }
    }

    /**
     * Loads a rewarded ad and immediately shows it.
     *
     * This method is designed for “tap to watch ad” flows:
     * - shows a loading dialog via [LoadingDialogUtil]
     * - supports optional timeout via [timeoutCallback]
     * - supports high-floor fallback (try high; if fails, try low after ~1s)
     *
     * @param activity activity used to show the rewarded ad.
     * @param adUnitKey logical key used to resolve ad unit ids.
     * @param onUserEarnedReward callback for user reward event.
     * @param onAdDismissed optional callback after ad dismiss.
     * @param onAdFailedToShow optional callback if load/show fails.
     * @param timeoutCallback optional callback fired if ad load isn't complete within [duration].
     * @param duration timeout duration in ms.
     * @param isHighFloor whether to attempt high-floor id first.
     */
    fun loadAdAndShow(
        activity: Activity,
        adUnitKey: String,
        onUserEarnedReward: (Any) -> Unit,
        onAdDismissed: (() -> Unit)? = null,
        onAdFailedToShow: (() -> Unit)? = null,
        timeoutCallback: (() -> Unit)? = null,
        duration: Int = 5000,
        isHighFloor: Boolean = true,
    ) {
        if (adClient.canDismissAds()) {
            onAdDismissed?.invoke()
            return
        }

        LoadingDialogUtil.showLoadingDialog(activity)
        var adComplete = false

        if (timeoutCallback != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                LoadingDialogUtil.dismissLoadingDialog()
                if (!adComplete) {
                    timeoutCallback.invoke()
                }
            }, duration.toLong())
        }

        val adUnitId = adClient.getAdUnitId(adUnitKey)
        val highFloorId = adClient.getHighFloor(adUnitKey)
        val currentAdUnitId = if (isHighFloor && highFloorId != null) highFloorId else adUnitId

        RewardedAd.load(
            context,
            currentAdUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                /**
                 * On load success: hide loading UI, cache the ad, and show it immediately.
                 */
                override fun onAdLoaded(ad: RewardedAd) {
                    LoadingDialogUtil.dismissLoadingDialog()
                    adComplete = true
                    rewardedAdMapper[adUnitId] = ad
                    showAd(activity, adUnitKey, onUserEarnedReward, onAdDismissed, onAdFailedToShow)
                }

                /**
                 * On load failure: retry once with low-floor id if high-floor was used,
                 * otherwise dismiss loading UI and invoke [onAdFailedToShow].
                 */
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (isHighFloor && highFloorId != null) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadAdAndShow(
                                activity,
                                adUnitKey,
                                onUserEarnedReward,
                                onAdDismissed,
                                onAdFailedToShow,
                                timeoutCallback,
                                duration,
                                false,
                            )
                        }, 1000)
                    } else {
                        adComplete = true
                        LoadingDialogUtil.dismissLoadingDialog()
                        onAdFailedToShow?.invoke()
                    }
                }
            },
        )
    }
}
