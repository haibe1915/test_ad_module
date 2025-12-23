package com.example.test_kotlin_compose.integration.adManager

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.test_kotlin_compose.integration.firebase.AdRemoteConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.test_kotlin_compose.util.LoadingDialogUtil
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.containsKey


@Singleton
class InterstialAdManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfig: AdRemoteConfig
) : InterstitialAdManagerInterface {
    // Reactive State for Compose
    val adStatus = MutableStateFlow<Map<String, AdLoadState>>(emptyMap())

    // Internal State
    private var waitingCallback: (() -> Unit)? = null
    private var waitingAdUnitName: AdUnitName? = null

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
    private var showNextAd: Boolean = false
    private var savedAdCallback: (() -> Unit)? = null

    private var priorityOrder = listOf<String>()
    private var interstitialAdPoolHolder: InterstitialAdPoolHolder? = null
    private var parallelLoadingMode: Boolean = false


    override fun getLoadedAd(adUnitName: AdUnitName): Any? {
        TODO("Not yet implemented")
    }

    override suspend fun init(context: Context) {
        configInterstitial = remoteConfig.getConfigAdInterstitial()
        waterfallReloadTime = remoteConfig.getReloadTime()
        cooldownTime = configInterstitial["time interval"] as Int? ?: 60
        parallelLoadingMode = configInterstitial["ads pair load mode"] as Boolean? ?: false
        adUnitIdMapper = AdClient.getInterstitialId()
        delayTime = AdClient.getInterstitialAdFailReloadTime()
        adInterPremium = AdClient.getInterstitialPremium()

        waterfallApply = false
        newLoadingMode = false

        priorityOrder = mutableListOf<String>().apply {
            addAll(adUnitIdMapper.keys)
            add("I-Base-High")
            add("I-Base-Low")
        }
        loadWaterFall()
    }

    fun addLoadingAd(adUnitId: String) {
        loadingAd.add(adUnitId)
    }

    fun removeLoadingAd(adUnitId: String) {
        loadingAd.remove(adUnitId)
    }

    fun cacheNextAd() {
        cachedNextAd = true
    }

    fun getWaterfallReloadTime(): Map<String, Any> {
        return waterfallReloadTime
    }

    override fun preloadAd(adUnitName: AdUnitName, ignoreCooldown: Boolean?) {
        if (!waterfallApply) {
            load(adUnitName, isHighFloor = true, ignoreCooldown = ignoreCooldown)
        }
    }

    fun updateShowedTime() {
        showedTime = Date()
    }

    fun updateDoneGetPermission(value: Boolean) {
        doneGetPermission = value
    }

    private fun availableCooldowmTime(adUnitName: AdUnitName): Boolean {
        if (showedTime == null) return false
        val diff = Date().time - showedTime!!.time
        return diff < cooldownTime * 1000
    }

    fun loadAdAndShow(
        activity: Activity,
        adUnitName: AdUnitName,
        callback: (() -> Unit)? = null,
        timeoutCallback: (() -> Unit)? = null,
        duration: Int = 5000,
        isHighFloor: Boolean? = true
    ) {
        if (AdClient.canDismissAds() || availableCooldowmTime(adUnitName)) {
            callback?.invoke()
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

        var adUnitId = AdClient.getAdUnitId(adUnitName)
        val highFloorId = AdClient.getHighFloor(adUnitName)

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
                            adUnitName,
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
                                adUnitName,
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

    fun loadAdInSplash(
        adUnitName: AdUnitName,
        isHighFloor: Boolean?,
        callback: (() -> Unit)? = null,
        timeoutCallback: (() -> Unit)? = null,
        duration: Int = 5000,
    ) {
        if (parallelLoadingMode) {

        } else {
            loadAdSequential(
                adUnitName,
                isHighFloor,
                callback,
                timeoutCallback,
                duration
            )
        }
    }

    fun showAd(
        activity: Activity,
        adUnitName: AdUnitName,
        callback: (() -> Unit)? = null
    ) {
        if (parallelLoadingMode) {

        } else {
            showSavedAdSequential(
                activity,
                adUnitName,
                callback
            )
        }
    }

    private fun showSavedAdSequential(
        activity: Activity,
        adUnitName: AdUnitName,
        callback: (() -> Unit)? = null
    ) {
        var adUnitId = AdClient.getAdUnitId(adUnitName)
        val highFloorAdUnitId = AdClient.getHighFloor(adUnitName)

        // Determine Ad Unit ID based on floor
        if (interstitialAdMapper.containsKey(highFloorAdUnitId)) {
            adUnitId = highFloorAdUnitId ?: adUnitId
        }

        if (interstitialAdMapper.containsKey(adUnitId)) {
            show(
                activity,
                adUnitName,
                callback,
                null,
                null,
                null,
                false
            )
        } else {
            return
        }
    }

    private fun loadAdSequential(
        adUnitName: AdUnitName,
        isHighFloor: Boolean?,
        callback: (() -> Unit)? = null,
        timeoutCallback: (() -> Unit)? = null,
        duration: Int = 5000,
    ) {
        if (AdClient.canDismissAds()) return

        var adComplete = false
        var adUnitId = AdClient.getAdUnitId(adUnitName)
        val highFloorAdUnitId = AdClient.getHighFloor(adUnitName)

        // Determine Ad Unit ID based on floor
        if (isHighFloor ?: true) {
            adUnitId = highFloorAdUnitId ?: adUnitId
        }

        // Check Cache
        if (cachedInterstitialAds.isNotEmpty()) {
            val cachedAd = cachedInterstitialAds.removeAt(0)
            interstitialAdMapper[adUnitId] = cachedAd
            return
        }

        // Check if already loading or loaded
        if (loadingAd.contains(adUnitId) || interstitialAdMapper.containsKey(adUnitId)) {
            return
        }

        loadingAd.add(adUnitId)

        // Timeout Logic
        if (timeoutCallback != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (!adComplete) {
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
                        // Retry with low floor after 1 second
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadAdSequential(adUnitName, false, callback, timeoutCallback, duration)
                        }, 1000)
                        return
                    }
                    callback?.invoke()
                }
            }
        )
    }

    private fun load(
        adUnitName: AdUnitName,
        isHighFloor: Boolean?,
        ignoreCooldown: Boolean? = false
    ) {
        if (AdClient.canDismissAds()) {
            return
        }

        var adUnitId = AdClient.getAdUnitId(adUnitName)

        var highFloorAdUnitId = AdClient.getHighFloor(adUnitName)
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
                            load(adUnitName, false)
                        }, delayTime.toLong())
                    } else {
                        adState = AdState.notLoaded
                    }
                }
            }
        )
    }

    fun show(
        activity: Activity,
        adUnitName: AdUnitName,
        callback: (() -> Unit)? = null,
        failedCallBack: (() -> Unit)? = null,
        dismissAdCallBack: (() -> Unit)? = null,
        loadingDialog: (() -> Unit)? = null,
        ignoreCooldown: Boolean
    ) {
        var adUnitId = AdClient.getAdUnitId(adUnitName)
        val highAdUnitId = AdClient.getHighFloor(adUnitName)
        if (interstitialAdMapper.containsKey(highAdUnitId)) {
            adUnitId = highAdUnitId ?: adUnitId
        }
        if (waterfallApply) {
            showWaterfall(
                activity,
                adUnitName,
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
                adUnitName,
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

    fun showNonWaterfall(
        activity: Activity,
        adUnitName: AdUnitName,
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
            // AdmodAppOpenAdManager.isDisable = true
            // disableUIMode()
            if (adState == AdState.waiting) {
                LoadingDialogUtil.dismissLoadingDialog()
                adState = AdState.loaded
            }

            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    // onAdShowedFullScreenContent
                }

                override fun onAdImpression() {
                    // onAdImpression
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    onCallback(
                        callback = failedCallBack ?: callback,
                        dismissAdCallback = dismissAdCallBack
                    )
                    showedTime = Date()
                }

                override fun onAdDismissedFullScreenContent() {
                    // AdmodAppOpenAdManager.isDisable = true
                    onCallback(
                        callback = failedCallBack ?: callback,
                        dismissAdCallback = dismissAdCallBack
                    )
                    showedTime = Date()
                    preloadAd(adUnitName)
                }

                override fun onAdClicked() {
                    // AdmodAppOpenAdManager.isDisable = true
                    AdClient.notifyAdClick()
                    onCallback(
                        callback = failedCallBack ?: callback,
                        dismissAdCallback = dismissAdCallBack
                    )
                    // LoggerManager().logAdsAction(adType = AdType.interstitial, action = AdAction.click, adUnitName = adUnitName)
                }
            }

            interstitialAd?.setOnPaidEventListener { adValue ->
                val valueInCurrency = adValue.valueMicros / 1e6
                // onPaidEvent(interstitialAd, valueInCurrency, adValue.currencyCode, adUnitName)
            }

            interstitialAd?.show(activity)

            // enableUIMode()
            interstitialAdMapper.remove(adUnitId)

            // Check if auto load is enabled (Logic commented out as AdsClient/map structure is unknown)
            // if (AdsClient.adInterPlaces[adUnitName]?["is auto load after dismiss"] == true) {
            //     load(adUnitName, false)
            // }

        } else if ((loadingAd.contains(adUnitId) || (highAdUnitId != null && loadingAd.contains(
                highAdUnitId
            ))) && adState != AdState.waiting
        ) {
            loadingDialog?.invoke() ?: LoadingDialogUtil.showLoadingDialog(activity, "")
            onAdLoadedCallback = callback
            adState = AdState.waiting
            var loadTime = 0
            val timeout = (configInterstitial["timeout"] as? Int) ?: 5

            val handler = Handler(Looper.getMainLooper())
            val checkRunnable = object : Runnable {
                override fun run() {
                    // if (LocalStorage.openOffer.value) return

                    if (interstitialAdMapper.containsKey(adUnitId) /* || check high floor key */) {
                        LoadingDialogUtil.dismissLoadingDialog()
                        show(activity, adUnitName, callback, ignoreCooldown = ignoreCooldown)
                        adState = AdState.notLoaded
                        return
                    }

                    if (loadTime >= timeout) {
                        if (adState == AdState.waiting || adState == AdState.notLoaded) {
                            LoadingDialogUtil.dismissLoadingDialog()
                            onCallback(
                                callback = failedCallBack ?: callback,
                                dismissAdCallback = dismissAdCallBack
                            )
                            adState = AdState.notLoaded
                        } else {
                            adState = AdState.notLoaded
                        }
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
            // if (AdsClient.adInterPlaces[adUnitName]?["is auto load after dismiss"] == true) {
//             load(adUnitName, false)
            // }
        }
    }

    fun showWaterfall(
        activity: Activity,
        adUnitName: AdUnitName,
        adUnitId: String,
        highAdUnitId: String?,
        callback: (() -> Unit)?,
        failedCallBack: (() -> Unit)?,
        dismissAdCallBack: (() -> Unit)?,
        loadingDialog: (() -> Unit)? = null,
        ignoreCooldown: Boolean
    ) {
        val availableHolder = getAvailableAdHolder() as? InterstitialAdHolder
        var availableAdPoolHolder: InterstitialAdPoolHolder? = null

        if (interstitialAdPoolHolder != null && interstitialAdPoolHolder!!.interstitialAd != null) {
            availableAdPoolHolder = interstitialAdPoolHolder
        }

        if (availableHolder != null || availableAdPoolHolder != null) {
            // AdmodAppOpenAdManager.isDisable = true
            // disableUIMode()

            if (adState == AdState.waiting) {
                LoadingDialogUtil.dismissLoadingDialog()
                adState = AdState.loaded
            }

            if (availableAdPoolHolder != null) {
                availableAdPoolHolder.showAd(activity, callback, failedCallBack, dismissAdCallBack)
            } else {
                availableHolder?.showAd(activity, callback, failedCallBack, dismissAdCallBack)
            }

            // enableUIMode()
            // if (AdsClient.adInterPlaces[adUnitName]?["is_auto_load_after_dismiss"] == true) {
            //     load(adUnitName, false)
            // }

        } else if (adState != AdState.waiting) {
            loadingDialog?.invoke() ?: LoadingDialogUtil.showLoadingDialog(activity, "")
            onAdLoadedCallback = callback
            adState = AdState.waiting

            var loadTime = 0
            val timeout = (configInterstitial["timeout"] as? Int) ?: 5
            val handler = Handler(Looper.getMainLooper())

            val checkRunnable = object : Runnable {
                override fun run() {
                    // if (LocalStorage.openOffer.value) return

                    val currentAvailableHolder = getAvailableAdHolder() as? InterstitialAdHolder
                    val currentPoolHolder = if (interstitialAdPoolHolder?.interstitialAd != null) interstitialAdPoolHolder else null

                    if (currentAvailableHolder != null || currentPoolHolder != null) {
                        LoadingDialogUtil.dismissLoadingDialog()
                        show(
                            activity,
                            adUnitName,
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
                        if (adState == AdState.waiting || adState == AdState.notLoaded) {
                            LoadingDialogUtil.dismissLoadingDialog()
                            onCallback(failedCallBack ?: callback, dismissAdCallBack)
                            adState = AdState.notLoaded
                        } else {
                            adState = AdState.notLoaded
                        }
                        return
                    }
                    loadTime++
                    handler.postDelayed(this, 1000)
                }
            }
            handler.post(checkRunnable)

        } else {
            onCallback(failedCallBack ?: callback, dismissAdCallBack)
            // if (AdsClient.adInterPlaces[adUnitName]?["is_auto_load_after_dismiss"] == true) {
            //     load(adUnitName, false)
            // }
        }
    }


    fun onCallback(callback: (() -> Unit)?, dismissAdCallback: (() -> Unit)?) {
        callback?.invoke()
        dismissAdCallback?.invoke()
    }


    override fun isAdLoaded(adUnitName: AdUnitName): Boolean {
        TODO("Not yet implemented")
    }

    override fun destroyAd(adUnitName: AdUnitName) {
        TODO("Not yet implemented")
    }

    override fun destroyAll() {
        TODO("Not yet implemented")
    }

    override fun pauseWaterFall() {
        for (holder in interstitialAdHolderUnitIds.values) {
            holder.pause = true
        }
        interstitialAdPoolHolder?.pause = true
    }

    override fun resumeWaterFall() {
        for (holder in interstitialAdHolderUnitIds.values) {
            holder.resumeLoadAd()
            interstitialAdPoolHolder?.resumeLoadAd()
        }
    }

    override suspend fun loadWaterFall() {
        waterfallApply = remoteConfig.getWaterfallApply();
        newLoadingMode = remoteConfig.getWaterfallLoadingMode() == 1;

        if (waterfallApply && interstitialAdHolderUnitIds.isEmpty()) {
            if (newLoadingMode && interstitialAdPoolHolder == null) {
                maybeLoadAd("I-Base-Low", AdClient.getLowAdId(AdType.interstitial))
                applyDelay()
                maybeLoadAd("I-Base-High", AdClient.getHighAdId(AdType.interstitial))
                applyDelay()
                interstitialAdPoolHolder = InterstitialAdPoolHolder(
                    context,
                    this,
                    adUnitIdMapper.values.toList(),
                    adUnitIdMapper.keys.toList()
                )
                interstitialAdPoolHolder?.loadAd()
            } else {
                maybeLoadAd("I-Base-Low", AdClient.getLowAdId(AdType.interstitial))
                applyDelay()
                maybeLoadAd("I-Base-High", AdClient.getHighAdId(AdType.interstitial))
                for (entry in adUnitIdMapper) {
                    val key = entry.key
                    val value = entry.value
                    if (value.isNotEmpty()) {
                        applyDelay()
                        interstitialAdHolderUnitIds[key] = InterstitialAdHolder(
                            context,
                            this,
                            value,
                            key,
                            remoteConfig
                        )
                        interstitialAdHolderUnitIds[key]?.loadAd()
                    }
                }

            }
        }
    }

    override fun getAvailableAdHolder(): Any? {
        for (key in priorityOrder) {
            val holder = interstitialAdHolderUnitIds[key]
            if (holder != null && holder.interstitialAd != null) {
                return holder
            }
        }
        return null
    }

    override fun getHighAvailableAdHolder(): Any? {
        for (key in priorityOrder) {
            val holder = interstitialAdHolderUnitIds[key]
            if (holder != null && holder.interstitialAd != null) {
                return holder
            }
        }
        return null
    }

    private fun maybeLoadAd(name: String, adUnitId: String) {
        try {
            if (adUnitId.isNotEmpty()) {
                val holder = InterstitialAdHolder(
                    context,
                    this,
                    adUnitId,
                    name,
                    remoteConfig
                )
                holder.loadAd()
                interstitialAdHolderUnitIds[name] = holder
            }
        } catch (e: Exception) {
            println(e)
        }

    }

    private suspend fun applyDelay() {
        kotlinx.coroutines.delay(remoteConfig.getAdsRampUpTime())
    }

}