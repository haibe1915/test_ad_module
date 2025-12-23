package com.example.test_kotlin_compose.integration.adManager

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.test_kotlin_compose.integration.firebase.RemoteConfigProvider
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
    private val remoteConfig: RemoteConfigProvider
) : RewardAdManagerInterface {

    private val rewardedAdMapper = mutableMapOf<String, RewardedAd>()
    private val loadingAd = mutableSetOf<String>()
    // private var adUnitIdMapper = mapOf<String, String>()
    private var configReward = mapOf<String, Any>()
    private var showedTime: Date? = null
    private var cooldownTime: Int = 90
    private var delayTime: Int = 100
    private var isShowingAd: Boolean = false

    override suspend fun init(context: Context) {
        configReward = remoteConfig.getConfigAdReward()
        cooldownTime = (configReward["time interval"] as? Number)?.toInt() ?: cooldownTime
        delayTime = AdClient.getRewardAdFailReloadTime()
    }

    override fun getLoadedAd(adUnitName: AdUnitName): Any? {
        val adUnitId = AdClient.getAdUnitId(adUnitName)
        return rewardedAdMapper[adUnitId]
    }

    override fun isAdLoaded(adUnitName: AdUnitName): Boolean {
        val adUnitId = AdClient.getAdUnitId(adUnitName)
        return rewardedAdMapper.containsKey(adUnitId)
    }

    override fun destroyAd(adUnitName: AdUnitName) {
        val adUnitId = AdClient.getAdUnitId(adUnitName)
        rewardedAdMapper.remove(adUnitId)
    }

    override fun destroyAll() {
        rewardedAdMapper.clear()
    }

    fun loadAd(adUnitName: AdUnitName) {
        if (AdClient.canDismissAds()) return

        val adUnitId = AdClient.getAdUnitId(adUnitName)
        val highFloorAdUnitId = AdClient.getHighFloor(adUnitName)

        if (loadingAd.contains(adUnitId) || rewardedAdMapper.containsKey(adUnitId)) {
            return
        }

        loadingAd.add(adUnitId)
        load(adUnitName, adUnitId, highFloorAdUnitId, true)
    }

    private fun load(
        adUnitName: AdUnitName,
        adUnitId: String,
        highFloorAdUnitId: String?,
        isHighFloor: Boolean
    ) {
        val currentAdUnitId = if (isHighFloor && highFloorAdUnitId != null) highFloorAdUnitId else adUnitId

        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            currentAdUnitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loadingAd.remove(adUnitId)
                    rewardedAdMapper[adUnitId] = ad
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (isHighFloor && highFloorAdUnitId != null) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            load(adUnitName, adUnitId, highFloorAdUnitId, false)
                        }, delayTime.toLong())
                    } else {
                        loadingAd.remove(adUnitId)
                    }
                }
            }
        )
    }

    fun showAd(
        activity: Activity,
        adUnitName: AdUnitName,
        onUserEarnedReward: (Any) -> Unit, // Adjust type as needed
        onAdDismissed: (() -> Unit)? = null,
        onAdFailedToShow: (() -> Unit)? = null
    ) {
        if (isShowingAd) return

        if (AdClient.canDismissAds()) {
            onAdDismissed?.invoke()
            return
        }

        val adUnitId = AdClient.getAdUnitId(adUnitName)
        val rewardedAd = rewardedAdMapper[adUnitId]

        if (rewardedAd != null) {
            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    isShowingAd = false
                    rewardedAdMapper.remove(adUnitId)
                    onAdDismissed?.invoke()
                    loadAd(adUnitName)
                    showedTime = Date()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    isShowingAd = false
                    rewardedAdMapper.remove(adUnitId)
                    onAdFailedToShow?.invoke()
                    loadAd(adUnitName)
                }

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
            loadAd(adUnitName)
        }
    }

    fun loadAdAndShow(
        activity: Activity,
        adUnitName: AdUnitName,
        onUserEarnedReward: (Any) -> Unit,
        onAdDismissed: (() -> Unit)? = null,
        onAdFailedToShow: (() -> Unit)? = null,
        timeoutCallback: (() -> Unit)? = null,
        duration: Int = 5000,
        isHighFloor: Boolean = true
    ) {
        if (AdClient.canDismissAds()) {
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

        val adUnitId = AdClient.getAdUnitId(adUnitName)
        val highFloorId = AdClient.getHighFloor(adUnitName)
        val currentAdUnitId = if (isHighFloor && highFloorId != null) highFloorId else adUnitId

        RewardedAd.load(
            context,
            currentAdUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    LoadingDialogUtil.dismissLoadingDialog()
                    adComplete = true
                    rewardedAdMapper[adUnitId] = ad
                    showAd(activity, adUnitName, onUserEarnedReward, onAdDismissed, onAdFailedToShow)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (isHighFloor && highFloorId != null) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadAdAndShow(
                                activity,
                                adUnitName,
                                onUserEarnedReward,
                                onAdDismissed,
                                onAdFailedToShow,
                                timeoutCallback,
                                duration,
                                false
                            )
                        }, 1000)
                    } else {
                        adComplete = true
                        LoadingDialogUtil.dismissLoadingDialog()
                        onAdFailedToShow?.invoke()
                    }
                }
            }
        )
    }
}
