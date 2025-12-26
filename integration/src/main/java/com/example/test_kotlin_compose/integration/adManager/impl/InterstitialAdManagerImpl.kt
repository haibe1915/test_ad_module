package com.example.test_kotlin_compose.integration.adManager.impl

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.test_kotlin_compose.integration.adManager.AdClient
import com.example.test_kotlin_compose.integration.adManager.AdState
import com.example.test_kotlin_compose.integration.adManager.AdType
import com.example.test_kotlin_compose.integration.adManager.api.InterstitialAdManagerInterface
import com.example.test_kotlin_compose.integration.adManager.waterFallAdHolder.InterstitialAdHolder
import com.example.test_kotlin_compose.integration.adManager.waterFallAdHolder.InterstitialAdPoolHolder
import com.example.test_kotlin_compose.integration.firebase.AdRemoteConfig
import com.example.test_kotlin_compose.util.LoadingDialogUtil
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterstialAdManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfig: AdRemoteConfig,
    private val adClient: AdClient,
) : InterstitialAdManagerInterface {

    // Internal State
    private var waitingCallback: (() -> Unit)? = null
    private var waitingadUnitKey: String? = null

    private val interstitialAdMapper = mutableMapOf<String, InterstitialAd>()
    private val interstitialAdHolderUnitIds = mutableMapOf<String, InterstitialAdHolder>()
    private var adUnitIdMapper = mapOf<String, String>()
    private val loadingAd = mutableSetOf<String>()

    private var showedTime: Date? = null
    private var cooldownTime: Int = 90
    private var delayTime: Int = 100

    private var configInterstitial = mapOf<String, Any>()
    private var waterfallReloadTime = mapOf<String, Any>()

    private var cachedNextAd: Boolean = false
    private val cachedInterstitialAds = mutableListOf<InterstitialAd>()
    private var adInterPremium = listOf<String>()

    private var doneGetPermission: Boolean = false
    private var adState: AdState = AdState.notLoaded

    private var onAdLoadedCallback: (() -> Unit)? = null
    private var waterfallApply: Boolean = true
    private var newLoadingMode: Boolean = false

    private var priorityOrder = listOf<String>()
    private var interstitialAdPoolHolder: InterstitialAdPoolHolder? = null
    private var parallelLoadingMode: Boolean = false


    /**
     * Returns a currently loaded interstitial ad instance for [adUnitKey], if any.
     *
     * @param adUnitKey logical key used by [AdClient] to resolve an ad unit id.
     * @return the loaded ad object, or null if none is cached.
     */
    override fun getLoadedAd(adUnitKey: String): Any? {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        return interstitialAdMapper[adUnitId]
    }

    /**
     * Initializes remote configuration and internal state, then triggers waterfall preloading.
     *
     * Notes:
     * - This is expected to be called once during app startup.
     * - Waterfall mode may immediately start loading multiple ads.
     */
    override suspend fun init(context: Context) {
        configInterstitial = remoteConfig.getConfigAdInterstitial()
        waterfallReloadTime = remoteConfig.getReloadTime()
        cooldownTime = (configInterstitial["time interval"] as? Int) ?: 60
        parallelLoadingMode = (configInterstitial["ads pair load mode"] as? Boolean) ?: false

        adUnitIdMapper = adClient.getInterstitialId()
        delayTime = adClient.getInterstitialAdFailReloadTime()
        adInterPremium = adClient.getInterstitialPremium()

        waterfallApply = false
        newLoadingMode = remoteConfig.getWaterfallLoadingMode() == 1

        priorityOrder = mutableListOf<String>().apply {
            addAll(adUnitIdMapper.keys)
            add("I-Base-High")
            add("I-Base-Low")
        }
        loadWaterFall()
    }

    /**
     * Checks if an interstitial for [adUnitKey] is currently cached and ready to show.
     */
    override fun isAdLoaded(adUnitKey: String): Boolean {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        return interstitialAdMapper.containsKey(adUnitId)
    }

    /**
     * Removes any cached interstitial ad associated with [adUnitKey].
     *
     * Note: this does not dispose the underlying Google Mobile Ads object.
     */
    override fun destroyAd(adUnitKey: String) {
        val adUnitId = adClient.getAdUnitId(adUnitKey)
        interstitialAdMapper.remove(adUnitId)
    }

    /** Adds [adUnitId] to the in-flight loading set to prevent duplicate loads. */
    fun addLoadingAd(adUnitId: String) {
        loadingAd.add(adUnitId)
    }

    /** Removes [adUnitId] from the in-flight loading set. */
    fun removeLoadingAd(adUnitId: String) {
        loadingAd.remove(adUnitId)
    }

    /**
     * Exposes the configured backoff/reload time map used by waterfall holders.
     */
    fun getWaterfallReloadTime(): Map<String, Any> {
        return waterfallReloadTime
    }

    /** Updates the internal last-show timestamp used for cooldown checks. */
    fun updateShowedTime() {
        showedTime = Date()
    }

    /**
     * Updates the "done-get-permission" flag used by the splash preload flow.
     * When true, [loadAdSequential] will invoke the callback immediately after the ad loads.
     */
    fun updateDoneGetPermission(value: Boolean) {
        doneGetPermission = value
    }

    /**
     * Returns true if the cooldown window after showing an interstitial has not elapsed.
     */
    private fun availableCooldowmTime(): Boolean {
        if (showedTime == null) return false
        val diff = Date().time - showedTime!!.time
        return diff < cooldownTime * 1000
    }

    /**
     * Preloads an interstitial for [adUnitKey].
     *
     * In waterfall mode, holders/pool manage preloading, so this only triggers non-waterfall load.
     */
    override fun preloadAd(adUnitKey: String, ignoreCooldown: Boolean?) {
        if (!waterfallApply) {
            load(adUnitKey, isHighFloor = true, ignoreCooldown = ignoreCooldown)
        }
    }

    /**
     * Loads an interstitial and immediately shows it.
     *
     * Behavior:
     * - Shows a loading dialog while waiting.
     * - Supports a timeout callback that may choose to cache the loaded ad for later.
     * - Supports retrying once from high-floor id -> low-floor id.
     */
    fun loadAdAndShow(
        activity: Activity,
        adUnitKey: String,
        callback: (() -> Unit)? = null,
        timeoutCallback: (() -> Unit)? = null,
        duration: Int = 5000,
        isHighFloor: Boolean? = true
    ) {
        if (adClient.canDismissAds() || availableCooldowmTime()) {
            callback?.invoke()
            return
        }

        LoadingDialogUtil.showLoadingDialog(activity)

        var adComplete = false

        if (timeoutCallback != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                LoadingDialogUtil.dismissLoadingDialog()
                if (!adComplete) {
                    cachedNextAd = true
                    timeoutCallback.invoke()
                }
            }, duration.toLong())
        }

        var adUnitId = adClient.getAdUnitId(adUnitKey)
        val highFloorId = adClient.getHighFloor(adUnitKey)

        if (isHighFloor ?: true) {
            adUnitId = highFloorId ?: adUnitId
        }

        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    LoadingDialogUtil.dismissLoadingDialog()
                    if (cachedNextAd) {
                        cachedInterstitialAds.add(ad)
                        cachedNextAd = false
                    } else {
                        adComplete = true
                        interstitialAdMapper[adUnitId] = ad
                        show(
                            activity,
                            adUnitKey,
                            callback,
                            null,
                            null,
                            null,
                            true
                        )
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    adComplete = true
                    if (cachedNextAd) {
                        cachedNextAd = false
                        LoadingDialogUtil.dismissLoadingDialog()
                        return
                    }

                    if (isHighFloor ?: true) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadAdAndShow(
                                activity,
                                adUnitKey,
                                callback,
                                null,
                                duration,
                                false
                            )
                        }, 1000)
                        return
                    }
                    LoadingDialogUtil.dismissLoadingDialog()

                    callback?.invoke()
                }
            }
        )
    }

    /**
     * Splash-friendly loading API that uses sequential load behavior.
     */
    fun loadAdInSplash(
        adUnitKey: String,
        isHighFloor: Boolean?,
        callback: (() -> Unit)? = null,
        timeoutCallback: (() -> Unit)? = null,
        duration: Int = 5000,
    ) {
        loadAdSequential(
            adUnitKey,
            isHighFloor,
            callback,
            timeoutCallback,
            duration
        )
    }

    /**
     * Loads an interstitial without showing it immediately.
     *
     * Differences vs [load]:
     * - Used in Splash: supports timeout + special "doneGetPermission" behavior.
     * - Prevents duplicate load requests via [loadingAd].
     */
    private fun loadAdSequential(
        adUnitKey: String,
        isHighFloor: Boolean?,
        callback: (() -> Unit)? = null,
        timeoutCallback: (() -> Unit)? = null,
        duration: Int = 5000,
    ) {
        if (adClient.canDismissAds()) return

        var adComplete = false
        var adUnitId = adClient.getAdUnitId(adUnitKey)
        val highFloorAdUnitId = adClient.getHighFloor(adUnitKey)

        if (isHighFloor ?: true) {
            adUnitId = highFloorAdUnitId ?: adUnitId
        }

        if (cachedInterstitialAds.isNotEmpty()) {
            val cachedAd = cachedInterstitialAds.removeAt(0)
            interstitialAdMapper[adUnitId] = cachedAd
            return
        }

        if (loadingAd.contains(adUnitId) || interstitialAdMapper.containsKey(adUnitId)) {
            return
        }

        loadingAd.add(adUnitId)

        if (timeoutCallback != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (!adComplete) {
                    cachedNextAd = true
                    timeoutCallback.invoke()
                }
            }, duration.toLong())
        }

        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingAd.remove(adUnitId)

                    if (cachedNextAd) {
                        cachedInterstitialAds.add(ad)
                        cachedNextAd = false
                    } else if (doneGetPermission) {
                        adComplete = true
                        interstitialAdMapper[adUnitId] = ad
                        callback?.invoke()
                        doneGetPermission = false
                    } else {
                        adComplete = true
                        interstitialAdMapper[adUnitId] = ad
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    loadingAd.remove(adUnitId)
                    adComplete = true

                    if (!cachedNextAd && (isHighFloor ?: false)) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadAdSequential(adUnitKey, false, callback, timeoutCallback, duration)
                        }, 1000)
                        return
                    }
                    callback?.invoke()
                }
            }
        )
    }

    /**
     * Loads and caches an interstitial without showing.
     *
     * - Tries high-floor first when requested, then falls back to low-floor after [delayTime].
     * - In waterfall mode, prefer using holders instead of calling this directly.
     */
    private fun load(
        adUnitKey: String,
        isHighFloor: Boolean?,
        ignoreCooldown: Boolean? = false
    ) {
        if (adClient.canDismissAds()) {
            return
        }

        var adUnitId = adClient.getAdUnitId(adUnitKey)
        val highFloorAdUnitId = adClient.getHighFloor(adUnitKey)
        if (isHighFloor ?: true) {
            adUnitId = highFloorAdUnitId ?: adUnitId
        }

        val loadHighFloor = (isHighFloor ?: true) && highFloorAdUnitId != null

        if (cachedInterstitialAds.isNotEmpty()) {
            interstitialAdMapper[adUnitId] = cachedInterstitialAds.removeAt(0)
            return
        }

        if (loadingAd.contains(adUnitId) || interstitialAdMapper.containsKey(adUnitId)) {
            return
        }

        loadingAd.add(adUnitId)

        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingAd.remove(adUnitId)
                    interstitialAdMapper.putIfAbsent(adUnitId, ad)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (loadHighFloor) {
                        val handler = Handler(Looper.getMainLooper())
                        handler.postDelayed({
                            loadingAd.remove(adUnitId)
                            load(adUnitKey, false)
                        }, delayTime.toLong())
                    } else {
                        adState = AdState.notLoaded
                    }
                }
            }
        )
    }

    /**
     * Shows a cached interstitial if available (sequential lookup).
     */
    fun showAd(
        activity: Activity,
        adUnitKey: String,
        callback: (() -> Unit)? = null
    ) {
        showSavedAdSequential(
            activity,
            adUnitKey,
            callback
        )
    }

    /**
     * Finds the best cached interstitial (prefers high floor if present) and shows it.
     */
    private fun showSavedAdSequential(
        activity: Activity,
        adUnitKey: String,
        callback: (() -> Unit)? = null
    ) {
        var adUnitId = adClient.getAdUnitId(adUnitKey)
        val highFloorAdUnitId = adClient.getHighFloor(adUnitKey)

        if (highFloorAdUnitId != null && interstitialAdMapper.containsKey(highFloorAdUnitId)) {
            adUnitId = highFloorAdUnitId
        }

        if (interstitialAdMapper.containsKey(adUnitId)) {
            show(
                activity,
                adUnitKey,
                callback,
                null,
                null,
                null,
                false
            )
        }
    }

    /**
     * Unified show entrypoint.
     *
     * Routes to waterfall or non-waterfall show implementation based on [waterfallApply].
     */
    fun show(
        activity: Activity,
        adUnitKey: String,
        callback: (() -> Unit)? = null,
        failedCallBack: (() -> Unit)? = null,
        dismissAdCallBack: (() -> Unit)? = null,
        loadingDialog: (() -> Unit)? = null,
        ignoreCooldown: Boolean
    ) {
        var adUnitId = adClient.getAdUnitId(adUnitKey)
        val highAdUnitId = adClient.getHighFloor(adUnitKey)
        if (highAdUnitId != null && interstitialAdMapper.containsKey(highAdUnitId)) {
            adUnitId = highAdUnitId
        }
        if (waterfallApply) {
            showWaterfall(
                activity,
                adUnitKey,
                adUnitId,
                highAdUnitId,
                callback,
                failedCallBack,
                dismissAdCallBack,
                loadingDialog,
                ignoreCooldown
            )
        } else {
            showNonWaterfall(
                activity,
                adUnitKey,
                adUnitId,
                highAdUnitId,
                callback,
                failedCallBack,
                dismissAdCallBack,
                loadingDialog,
                ignoreCooldown
            )
        }
    }

    /**
     * Shows an interstitial using the legacy non-waterfall cache/double-id logic.
     *
     * If an ad isn't ready but is currently loading, this will optionally show a loading dialog
     * and poll until timeout.
     */
    fun showNonWaterfall(
        activity: Activity,
        adUnitKey: String,
        adUnitId: String,
        highAdUnitId: String?,
        callback: (() -> Unit)?,
        failedCallBack: (() -> Unit)?,
        dismissAdCallBack: (() -> Unit)?,
        loadingDialog: (() -> Unit)? = null,
        ignoreCooldown: Boolean
    ) {
        if (interstitialAdMapper.containsKey(adUnitId)) {
            val interstitialAd = interstitialAdMapper[adUnitId]
            if (adState == AdState.waiting) {
                LoadingDialogUtil.dismissLoadingDialog()
                adState = AdState.loaded
            }

            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    onCallback(
                        callback = failedCallBack ?: callback,
                        dismissAdCallback = dismissAdCallBack
                    )
                    showedTime = Date()
                }

                override fun onAdDismissedFullScreenContent() {
                    onCallback(
                        callback = failedCallBack ?: callback,
                        dismissAdCallback = dismissAdCallBack
                    )
                    showedTime = Date()
                    preloadAd(adUnitKey)
                }

                override fun onAdClicked() {
                    adClient.notifyAdClick()
                    onCallback(
                        callback = failedCallBack ?: callback,
                        dismissAdCallback = dismissAdCallBack
                    )
                }
            }

            interstitialAd?.setOnPaidEventListener { /* TODO */ }
            interstitialAd?.show(activity)
            interstitialAdMapper.remove(adUnitId)

        } else if ((loadingAd.contains(adUnitId) || (highAdUnitId != null && loadingAd.contains(highAdUnitId))) && adState != AdState.waiting) {
            loadingDialog?.invoke() ?: LoadingDialogUtil.showLoadingDialog(activity, "")
            onAdLoadedCallback = callback
            adState = AdState.waiting
            var loadTime = 0
            val timeout = (configInterstitial["timeout"] as? Int) ?: 5

            val handler = Handler(Looper.getMainLooper())
            val checkRunnable = object : Runnable {
                override fun run() {
                    if (interstitialAdMapper.containsKey(adUnitId)) {
                        LoadingDialogUtil.dismissLoadingDialog()
                        show(activity, adUnitKey, callback, ignoreCooldown = ignoreCooldown)
                        adState = AdState.notLoaded
                        return
                    }

                    if (loadTime >= timeout) {
                        LoadingDialogUtil.dismissLoadingDialog()
                        onCallback(
                            callback = failedCallBack ?: callback,
                            dismissAdCallback = dismissAdCallBack
                        )
                        adState = AdState.notLoaded
                        return
                    }
                    loadTime++
                    handler.postDelayed(this, 1000)
                }
            }
            handler.post(checkRunnable)

        } else {
            onCallback(
                callback = failedCallBack ?: callback,
                dismissAdCallback = dismissAdCallBack
            )
        }
    }

    /**
     * Shows an interstitial using waterfall holders or the pool holder.
     *
     * If no waterfall ad is currently available, it can show a loading dialog and poll
     * for availability until timeout.
     */
    fun showWaterfall(
        activity: Activity,
        adUnitKey: String,
        adUnitId: String,
        highAdUnitId: String?,
        callback: (() -> Unit)?,
        failedCallBack: (() -> Unit)?,
        dismissAdCallBack: (() -> Unit)?,
        loadingDialog: (() -> Unit)? = null,
        ignoreCooldown: Boolean
    ) {
        val availableHolder = getAvailableAdHolder() as? InterstitialAdHolder
        val availableAdPoolHolder = if (interstitialAdPoolHolder?.interstitialAd != null) interstitialAdPoolHolder else null

        if (availableHolder != null || availableAdPoolHolder != null) {
            if (adState == AdState.waiting) {
                LoadingDialogUtil.dismissLoadingDialog()
                adState = AdState.loaded
            }

            if (availableAdPoolHolder != null) {
                availableAdPoolHolder.showAd(activity, callback, failedCallBack, dismissAdCallBack)
            } else {
                availableHolder?.showAd(activity, callback, failedCallBack, dismissAdCallBack)
            }

        } else if (adState != AdState.waiting) {
            loadingDialog?.invoke() ?: LoadingDialogUtil.showLoadingDialog(activity, "")
            onAdLoadedCallback = callback
            adState = AdState.waiting

            var loadTime = 0
            val timeout = (configInterstitial["timeout"] as? Int) ?: 5
            val handler = Handler(Looper.getMainLooper())

            val checkRunnable = object : Runnable {
                override fun run() {
                    val currentAvailableHolder = getAvailableAdHolder() as? InterstitialAdHolder
                    val currentPoolHolder = if (interstitialAdPoolHolder?.interstitialAd != null) interstitialAdPoolHolder else null

                    if (currentAvailableHolder != null || currentPoolHolder != null) {
                        LoadingDialogUtil.dismissLoadingDialog()
                        show(
                            activity,
                            adUnitKey,
                            callback,
                            failedCallBack,
                            dismissAdCallBack,
                            loadingDialog,
                            ignoreCooldown
                        )
                        adState = AdState.notLoaded
                        return
                    }

                    if (loadTime >= timeout) {
                        LoadingDialogUtil.dismissLoadingDialog()
                        onCallback(failedCallBack ?: callback, dismissAdCallBack)
                        adState = AdState.notLoaded
                        return
                    }
                    loadTime++
                    handler.postDelayed(this, 1000)
                }
            }
            handler.post(checkRunnable)

        } else {
            onCallback(failedCallBack ?: callback, dismissAdCallBack)
        }
    }

    /**
     * Runs the provided callbacks safely (null-checks).
     *
     * @param callback primary callback to run.
     * @param dismissAdCallback secondary callback (often used for dismiss events).
     */
    fun onCallback(callback: (() -> Unit)?, dismissAdCallback: (() -> Unit)?) {
        callback?.invoke()
        dismissAdCallback?.invoke()
    }

    /**
     * Clears all cached ads and internal tracking collections.
     */
    override fun destroyAll() {
        interstitialAdMapper.clear()
        interstitialAdHolderUnitIds.clear()
        loadingAd.clear()
    }

    /**
     * Pauses waterfall holders so they stop retrying while the host app is backgrounded.
     */
    override fun pauseWaterFall() {
        for (holder in interstitialAdHolderUnitIds.values) {
            holder.pause = true
        }
        interstitialAdPoolHolder?.pause = true
    }

    /**
     * Resumes waterfall holders so they continue loading after being paused.
     */
    override fun resumeWaterFall() {
        for (holder in interstitialAdHolderUnitIds.values) {
            holder.resumeLoadAd()
        }
        interstitialAdPoolHolder?.resumeLoadAd()
    }

    /**
     * Starts the waterfall preloading strategy.
     *
     * It may create:
     * - Base holders (low/high)
     * - Per-placement holders (from [adUnitIdMapper])
     * - A pool holder for randomized selection (new loading mode)
     */
    override suspend fun loadWaterFall() {
        waterfallApply = false
        newLoadingMode = remoteConfig.getWaterfallLoadingMode() == 1

        if (waterfallApply && interstitialAdHolderUnitIds.isEmpty()) {
            if (newLoadingMode && interstitialAdPoolHolder == null) {
                maybeLoadAd("I-Base-Low", adClient.getLowAdId(AdType.interstitial))
                applyDelay()
                maybeLoadAd("I-Base-High", adClient.getHighAdId(AdType.interstitial))
                applyDelay()
                interstitialAdPoolHolder = InterstitialAdPoolHolder(
                    context,
                    this,
                    adUnitIdMapper.values.toList(),
                    adUnitIdMapper.keys.toList(),
                    adClient,
                )
                interstitialAdPoolHolder?.loadAd()
            } else {
                maybeLoadAd("I-Base-Low", adClient.getLowAdId(AdType.interstitial))
                applyDelay()
                maybeLoadAd("I-Base-High", adClient.getHighAdId(AdType.interstitial))
                for (entry in adUnitIdMapper) {
                    val key = entry.key
                    val value = entry.value
                    if (value.isNotEmpty()) {
                        applyDelay()
                        interstitialAdHolderUnitIds[key] = InterstitialAdHolder(
                            context,
                            this,
                            value,
                            remoteConfig,
                            adClient,
                        )
                        interstitialAdHolderUnitIds[key]?.loadAd()
                    }
                }

            }
        }
    }

    /**
     * Returns the first available waterfall holder following [priorityOrder].
     */
    override fun getAvailableAdHolder(): Any? {
        for (key in priorityOrder) {
            val holder = interstitialAdHolderUnitIds[key]
            if (holder != null && holder.interstitialAd != null) {
                return holder
            }
        }
        return null
    }

    /**
     * Returns the high-priority available holder.
     *
     * Currently same behavior as [getAvailableAdHolder].
     */
    override fun getHighAvailableAdHolder(): Any? {
        return getAvailableAdHolder()
    }

    /**
     * Creates and starts loading a waterfall holder for [adUnitId] if it's non-empty.
     *
     * @param name key used inside [interstitialAdHolderUnitIds].
     */
    private fun maybeLoadAd(name: String, adUnitId: String) {
        try {
            if (adUnitId.isNotEmpty()) {
                val holder = InterstitialAdHolder(
                    context,
                    this,
                    adUnitId,
                    remoteConfig,
                    adClient,
                )
                holder.loadAd()
                interstitialAdHolderUnitIds[name] = holder
            }
        } catch (e: Exception) {
            println(e)
        }

    }

    /**
     * Applies ramp-up delay between requests to avoid a burst of simultaneous ad loads.
     */
    private suspend fun applyDelay() {
        delay(remoteConfig.getAdsRampUpTime())
    }

}