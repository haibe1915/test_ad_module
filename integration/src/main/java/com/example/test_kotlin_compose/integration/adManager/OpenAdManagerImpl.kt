package com.example.test_kotlin_compose.integration.adManager

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.test_kotlin_compose.integration.firebase.AdRemoteConfig
//import com.example.test_kotlin_compose.util.LoadingDialogUtil
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
    private val remoteConfig: AdRemoteConfig
) : OpenAdManagerInterface {

    private val appOpenAdMapper = mutableMapOf<String, AppOpenAd?>()
    private val loadingAd = mutableSetOf<String>()
    private var adUnitIdMapper = mapOf<String, String>()
    private var configAppOpen = mapOf<String, Any>()
    private var showedTime: Date? = null
    private var cooldownTime: Int = 0
    private var delayTime: Int = 100
    var isDisable: Boolean = false
    private var isShowingAd: Boolean = false

    private var loadTime: Date? = null
    private var cachedNextAd: Boolean = false
    private val cachedInterstitialAds = mutableListOf<AppOpenAd>()
    private var doneGetPermission: Boolean = false
    private var isLoading: Boolean = false
    private var parallelLoadingMode: Boolean = false
    private val adStatus = mutableMapOf<String, AdLoadState>()
    private var waitingCallback: (() -> Unit)? = null
    private var waitingAdUnitName: AdUnitName? = null

    override suspend fun init(context: Context) {
        configAppOpen = remoteConfig.getConfigAdAppOpen()
        cooldownTime = (configAppOpen["time interval"] as? Number)?.toInt() ?: cooldownTime
        adUnitIdMapper = AdClient.getAdUnitId(AdUnitName.globalOpen).let {
            mapOf(AdUnitName.globalOpen.name to it)
        }
        delayTime = AdClient.getOpenAppFailReloadTime()
        parallelLoadingMode = (configAppOpen["ads_pair_load_mode"] as? Int) == 1
    }

    override fun getLoadedAd(adUnitName: AdUnitName): Any? {
        val adUnitId = AdClient.getAdUnitId(adUnitName)
        return appOpenAdMapper[adUnitId]
    }

    override fun isAdLoaded(adUnitName: AdUnitName): Boolean {
        val adUnitId = AdClient.getAdUnitId(adUnitName)
        return appOpenAdMapper.containsKey(adUnitId)
    }

    override fun destroyAd(adUnitName: AdUnitName) {
        val adUnitId = AdClient.getAdUnitId(adUnitName)
        appOpenAdMapper.remove(adUnitId)
    }

    override fun destroyAll() {
        appOpenAdMapper.clear()
    }

    fun cacheNextAd() {
        cachedNextAd = true
    }

    fun updateDoneGetPermission(value: Boolean) {
        doneGetPermission = value
    }

    fun loadAdInSplash(
        adUnitName: AdUnitName,
        isHighFloor: Boolean? = true,
        callback: (() -> Unit)? = null,
        timeoutCallback: (() -> Unit)? = null,
        duration: Int = 5000

    ) {
        if (parallelLoadingMode) {
            // Implement parallel loading logic if needed
        } else {
            loadOpenAdSequential(
                adUnitName,
                isHighFloor,
                callback,
                timeoutCallback,
                duration
            )
        }
    }


    override fun preloadAd(adUnitName: AdUnitName) {
        if (AdClient.canDismissAds()) return

        val adUnitId = AdClient.getAdUnitId(adUnitName)

        if (loadingAd.contains(adUnitId) || appOpenAdMapper.containsKey(adUnitId)) {
            return
        }

        loadingAd.add(adUnitId)

        load(adUnitName, true)
    }

    private fun load(
        adUnitName: AdUnitName,
        isHighFloor: Boolean? = true
    ) {
        if (AdClient.canDismissAds()) return
        val adUnitId = AdClient.getAdUnitId(adUnitName)
        val highFloorAdUnitId = AdClient.getHighFloor(adUnitName)

        val currentAdUnitId =
            if (isHighFloor == true && highFloorAdUnitId != null) highFloorAdUnitId else adUnitId

        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            currentAdUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    loadingAd.remove(adUnitId)
                    appOpenAdMapper[adUnitId] = ad
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (isHighFloor == true && highFloorAdUnitId != null) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            load(adUnitName, false)
                        }, delayTime.toLong())
                    } else {
                        loadingAd.remove(adUnitId)
                    }
                }
            }
        )
    }

    private fun loadOpenAdSequential(
        adUnitName: AdUnitName,
        isHighFloor: Boolean? = true,
        callback: (() -> Unit)? = null,
        timeoutCallback: (() -> Unit)? = null,
        duration: Int = 5000
    ) {
        if (AdClient.canDismissAds()) return

        var adComplete = false

        if (timeoutCallback != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (!adComplete) {
                    timeoutCallback.invoke()
                }
            }, duration.toLong())
        }

        val adUnitId = AdClient.getAdUnitId(adUnitName)
        val highFloorAdUnitId = AdClient.getHighFloor(adUnitName)

        val currentAdUnitId =
            if (isHighFloor == true && highFloorAdUnitId != null) highFloorAdUnitId else adUnitId

        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            context,
            currentAdUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    loadingAd.remove(adUnitId)

                    if (cachedNextAd) {
                        cachedInterstitialAds.add(ad)
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

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    adComplete = true
                    appOpenAdMapper[adUnitId] = null
                    if (!cachedNextAd && isHighFloor == true) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadAdInSplash(
                                adUnitName,
                                false,
                                callback,
                                timeoutCallback,
                                duration
                            )
                        }, delayTime.toLong())
                    }
                }
            }
        )
    }

    fun showAdIfAvailable(
        activity: Activity,
        adUnitName: AdUnitName,
        onShowAdComplete: () -> Unit
    ) {
        if (isShowingAd) {
            return
        }

        if (AdClient.canDismissAds()) {
            onShowAdComplete()
            return
        }

        if (!isAdAvailable(adUnitName)) {
            onShowAdComplete()
            load(adUnitName)
            return
        }

        val adUnitId = AdClient.getAdUnitId(adUnitName)
        val highAdUnitId = AdClient.getHighFloor(adUnitName)
        var appOpenAd = appOpenAdMapper[highAdUnitId]
        if (appOpenAd == null) {
            appOpenAd = appOpenAdMapper[adUnitId]
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowingAd = false
                appOpenAdMapper.remove(adUnitId)
                onShowAdComplete()
                load(adUnitName)
                showedTime = Date()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isShowingAd = false
                appOpenAdMapper.remove(adUnitId)
                onShowAdComplete()
                load(adUnitName)
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }
        }
        isShowingAd = true
        appOpenAd?.show(activity)
    }

    private fun isAdAvailable(adUnitName: AdUnitName): Boolean {
        val adUnitId = AdClient.getAdUnitId(adUnitName)
        val highAdUnitId = AdClient.getHighFloor(adUnitName)
        return appOpenAdMapper.containsKey(adUnitId) || appOpenAdMapper.containsKey(highAdUnitId) // && wasLoadTimeLessThanNHoursAgo(4)
    }

    fun loadAdAndShow(
        activity: Activity,
        adUnitName: AdUnitName,
        callback: (() -> Unit)? = null,
        timeoutCallback: (() -> Unit)? = null,
        duration: Int = 5000,
        isHighFloor: Boolean = true
    ) {
        if (AdClient.canDismissAds() || availableCooldowmTime()) {
            callback?.invoke()
            return
        }

//        LoadingDialogUtil.showLoadingDialog(activity)
        var adComplete = false

        if (timeoutCallback != null) {
            Handler(Looper.getMainLooper()).postDelayed({
//                LoadingDialogUtil.dismissLoadingDialog()
                if (!adComplete) {
                    timeoutCallback.invoke()
                }
            }, duration.toLong())
        }

        val adUnitId = AdClient.getAdUnitId(adUnitName)
        val highFloorId = AdClient.getHighFloor(adUnitName)
        val currentAdUnitId = if (isHighFloor && highFloorId != null) highFloorId else adUnitId

        AppOpenAd.load(
            context,
            currentAdUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
//                    LoadingDialogUtil.dismissLoadingDialog()
                    adComplete = true
                    appOpenAdMapper[adUnitId] = ad
                    showAdIfAvailable(activity, adUnitName) {
                        callback?.invoke()
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (isHighFloor && highFloorId != null) {
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
                    } else {
                        adComplete = true
//                        LoadingDialogUtil.dismissLoadingDialog()
                        callback?.invoke()
                    }
                }
            }
        )
    }

    private fun availableCooldowmTime(): Boolean {
        if (showedTime == null) return false
        val diff = Date().time - showedTime!!.time
        return diff < cooldownTime * 1000
    }
}
