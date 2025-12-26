package com.example.test_kotlin_compose.integration.adManager.waterFallAdHolder

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.test_kotlin_compose.integration.adManager.AdClient
import com.example.test_kotlin_compose.integration.adManager.impl.NativeAdManagerImpl
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import java.util.Random

class NativeAdHolder(
    private val context: Context,
    private val manager: NativeAdManagerImpl,
    val adUnitId: String,
    private val adClient: AdClient,
) {
    var nativeAd: NativeAd? = null
    private var expireHandler: Handler? = null
    private var expireRunnable: Runnable? = null
    var retryTime = 0
    var pause = false

    fun resumeLoadAd() {
        pause = false
        if (nativeAd == null) {
            loadAd()
        }
    }

    fun loadAd() {
        if (adClient.canDismissAds()) {
            return
        }

        manager.addLoadingAd(adUnitId)

        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                nativeAd = ad
                manager.removeLoadingAd(adUnitId)
                retryTime = 0

                resetExpireTimer()
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (pause) return

                    val maxRetry = 3
                    if (retryTime >= maxRetry) {
                        manager.removeLoadingAd(adUnitId)
                        return
                    }
                    retryTime++

                    val config = manager.getWaterfallReloadTime()
                    val baseTime = (config["base_time"] as? Number)?.toInt() ?: 1000
                    val addTime = (config["add_time"] as? Number)?.toInt() ?: 1000
                    val maxTime = (config["max"] as? Number)?.toInt() ?: 10000

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

                override fun onAdClicked() {
                    adClient.notifyAdClick()
                }

                override fun onAdImpression() {
                    cancelExpireTimer()
                    loadAd()
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun resetExpireTimer() {
        cancelExpireTimer()
        expireRunnable = Runnable {
            nativeAd?.destroy()
            nativeAd = null
            expireHandler = null
            loadAd()
        }
        expireHandler = Handler(Looper.getMainLooper())
        expireHandler?.postDelayed(expireRunnable!!, 3600000) // 1 hour
    }

    private fun cancelExpireTimer() {
        expireHandler?.removeCallbacksAndMessages(null)
        expireHandler = null
        expireRunnable = null
    }
}