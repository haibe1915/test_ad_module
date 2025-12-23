package com.example.test_kotlin_compose.integration.adManager

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import java.util.Random

class NativeAdPoolHolder(
    private val context: Context,
    private val manager: NativeAdManagerImpl,
    val adUnitIds: List<String>,
    val adUnitNames: List<String>,
//    val factoryId: String
) {
    var nativeAd: NativeAd? = null
    var chosenIndex = -1

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
        if (AdClient.canDismissAds()) {
            return
        }

        val index = Random().nextInt(adUnitNames.size)
        val adUnitId = adUnitIds[index]

        manager.addLoadingAd(adUnitId)

        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                nativeAd = ad
                chosenIndex = index
                manager.removeLoadingAd(adUnitId)
                retryTime = 0

                expireHandler?.removeCallbacksAndMessages(null)
                expireRunnable = Runnable {
                    nativeAd?.destroy()
                    nativeAd = null
                    expireHandler = null
                    loadAd()
                }
                expireHandler = Handler(Looper.getMainLooper())
                expireHandler?.postDelayed(expireRunnable!!, 3600000) // 1 hour
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

                override fun onAdClicked() {
                    AdClient.notifyAdClick()
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }
}