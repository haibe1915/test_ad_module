package com.example.test_kotlin_compose.integration.adManager

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.test_kotlin_compose.integration.firebase.RemoteConfigProvider
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.Random

class InterstitialAdHolder(
    private val context: Context,
    private val manager: InterstialAdManagerImpl,
    val adUnitId: String,
    val adUnitName: String,
    private val remoteConfig: RemoteConfigProvider
) {
    var interstitialAd: InterstitialAd? = null
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

        // Premium logic placeholder
        // val prem = AdClient.getInterstitialPremium().contains(adUnitName.name)

        // Assuming manager has a method to track loading ads
        manager.addLoadingAd(adUnitId)

        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    manager.removeLoadingAd(adUnitId)
                    retryTime = 0

                    // Cancel existing expiration timer
                    expireHandler?.removeCallbacksAndMessages(null)

                    // Set new expiration timer (1 hour)
                    expireRunnable = Runnable {
                        interstitialAd = null
                        expireHandler = null
                        loadAd()
                    }
                    expireHandler = Handler(Looper.getMainLooper())
                    expireHandler?.postDelayed(expireRunnable!!, 3600000)

                    // Flash Offer / Premium Logic would go here
                    /*
                    if (prem && ...) {
                         // Show premium dialog
                    }
                    */
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (pause) return

                    if (remoteConfig.getWaterfallDebug()) {
                        // Log the error for debugging
                    }

                    // Assuming RemoteConfig has maxRetry, defaulting to 3
                    val maxRetry = remoteConfig.getMaxRetry()
                    if (retryTime >= maxRetry) {
                        manager.removeLoadingAd(adUnitId)
                        return
                    }

                    retryTime++

                    // Calculate backoff time based on manager's config
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
                // Log ad showed
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                manager.onCallback(failedCallBack ?: callback, dismissAdCallback)
                interstitialAd = null
                loadAd()
            }

            override fun onAdDismissedFullScreenContent() {
                // AdmodAppOpenAdManager.isDisable = true // Handle Open Ad logic if needed
                dismissAdCallback?.invoke()
                manager.onCallback(callback, dismissAdCallback)
                manager.updateShowedTime()
                interstitialAd = null
                loadAd()
            }

            override fun onAdClicked() {
                // AdmodAppOpenAdManager.isDisable = true
                manager.onCallback(callback, dismissAdCallback)
                AdClient.notifyAdClick()
            }
        }

        interstitialAd?.setOnPaidEventListener { adValue ->
            val valueInCurrency = adValue.valueMicros / 1e6
            // manager.onPaidEvent(interstitialAd, valueInCurrency, adValue.currencyCode, adUnitName)
        }

        interstitialAd?.show(activity)
    }
}
