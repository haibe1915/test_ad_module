package com.example.test_kotlin_compose.integration.adManager

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.Random

class InterstitialAdPoolHolder(
    private val context: Context,
    private val manager: InterstialAdManagerImpl,
    val adUnitIds: List<String>,
    val adUnitNames: List<String>
) {
    var interstitialAd: InterstitialAd? = null
    var chosenIndex = -1

    private var expireHandler: Handler? = null
    private var expireRunnable: Runnable? = null

    var retryTime = 0
    var pause = false

    fun resumeLoadAd() {
        pause = false
        if (interstitialAd == null) {
            loadAd()
        }
    }

    fun loadAd() {
        if (AdClient.canDismissAds()) {
            return
        }

        val index = Random().nextInt(adUnitNames.size)
        val adUnitId = adUnitIds[index]
        // val adUnitName = adUnitNames[index]

        manager.addLoadingAd(adUnitId)

        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    chosenIndex = index
                    manager.removeLoadingAd(adUnitId)
                    retryTime = 0

                    expireHandler?.removeCallbacksAndMessages(null)
                    expireRunnable = Runnable {
                        interstitialAd = null
                        expireHandler = null
                        loadAd()
                    }
                    expireHandler = Handler(Looper.getMainLooper())
                    expireHandler?.postDelayed(expireRunnable!!, 3600000) // 1 hour
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (pause) return

                    val maxRetry = 3
                    if (retryTime >= maxRetry) {
                        manager.removeLoadingAd(adUnitId)
                        return
                    }
                    retryTime++

                    val config = manager.getWaterfallReloadTime()
                    val baseTime = (config["base_time"] as? Int) ?: 1000
                    val addTime = (config["add_time"] as? Int) ?: 1000
                    val maxTime = (config["max"] as? Int) ?: 10000

                    val reloadTime = baseTime + retryTime * addTime
                    val finalDelay = if (reloadTime > maxTime) {
                        maxTime - 5 + Random().nextInt(10)
                    } else {
                        reloadTime
                    }

                    Handler(Looper.getMainLooper()).postDelayed({
                        loadAd()
                    }, finalDelay.toLong())
                }
            }
        )
    }

    fun showAd(
        activity: Activity,
        callback: (() -> Unit)? = null,
        failedCallBack: (() -> Unit)? = null,
        dismissAdCallback: (() -> Unit)? = null
    ) {
        if (interstitialAd == null) return

        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                // Log
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                manager.onCallback(failedCallBack ?: callback, dismissAdCallback)
                interstitialAd = null
                loadAd()
            }

            override fun onAdDismissedFullScreenContent() {
                dismissAdCallback?.invoke()
                manager.onCallback(callback, dismissAdCallback)
                manager.updateShowedTime()
                interstitialAd = null
                loadAd()
            }

            override fun onAdClicked() {
                manager.onCallback(callback, dismissAdCallback)
                AdClient.notifyAdClick()
            }
        }

        interstitialAd?.show(activity)
    }
}