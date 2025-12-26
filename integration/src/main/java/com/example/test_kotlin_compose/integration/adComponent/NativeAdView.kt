package com.example.test_kotlin_compose.integration.adComponent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.test_kotlin_compose.integration.R
import com.example.test_kotlin_compose.integration.adManager.AdClient
import com.example.test_kotlin_compose.integration.adManager.impl.AdNativeHelper
import com.example.test_kotlin_compose.integration.adManager.impl.NativeAdManagerImpl
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NativeAdComposable(
    adUnitKey: String,
    factoryId: String,
    manager: NativeAdManagerImpl,
    adClient: AdClient,
    modifier: Modifier = Modifier,
    autoLoad: Boolean? = true,
    highFloor: Boolean? = false,
) {
    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }
    var nativeAd by remember { mutableStateOf(manager.getLoadedAd(adUnitKey)) }

    val isDispose = false
    var adUnitId = remember { adClient.getAdUnitId(adUnitKey) }
    var delayTime: Int = remember { 100 }
    var isHighFloor = remember { highFloor }
    val scope = rememberCoroutineScope()

    fun loadNativeAd() {
        try {
            val adLoader = AdLoader.Builder(context, adUnitId)
                .forNativeAd { ad: NativeAd ->
                    nativeAd = ad
                    isAdLoaded = true
                    ad.setOnPaidEventListener { /* optional */ }
                }
                .withAdListener(object : AdListener() {
                    override fun onAdClicked() {
                        super.onAdClicked()
                        adClient.notifyAdClick()
                        // Add per-placement click logic here if you need it.
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        super.onAdFailedToLoad(error)
                        if (isHighFloor ?: true) {
                            isHighFloor = false
                            scope.launch {
                                delay(delayTime.toLong())
                                adUnitId = adClient.getAdUnitId(adUnitKey)
                                loadNativeAd()
                            }
                        }
                    }
                })
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createNativeAd() {
        if (isDispose) return

        nativeAd = manager.getLoadedAd(adUnitKey)

        if (nativeAd != null) {
            isAdLoaded = true
        } else if (!adClient.canDismissAds() && !manager.waterfallApply) {
            loadNativeAd()
        } else {
            if (!manager.waterfallApply) {
                loadNativeAd()
            }
        }
    }

    LaunchedEffect(Unit) {
        delayTime = 100
        val highFloorAdUnitId = adClient.getHighFloor(adUnitKey)
        if (highFloor == true) {
            adUnitId = highFloorAdUnitId ?: adUnitId
        }
        if (autoLoad == true) {
            createNativeAd()
        }
    }

    if (isAdLoaded && nativeAd != null) {
        AndroidView(
            factory = { ctx ->
                val layoutId = AdNativeHelper.getLayoutIdFromFactory(factoryId)

                val parent = android.widget.FrameLayout(ctx)
                val adView = LayoutInflater.from(ctx)
                    .inflate(layoutId, parent, false) as NativeAdView

                adView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                populateNativeAdView(nativeAd!!, adView)
                adView
            },
            update = { adView ->
                adView.layoutParams = (adView.layoutParams ?: ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )).apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                }
            },
            modifier = modifier.fillMaxWidth()
        )
    } else {
        Box(modifier = modifier.fillMaxWidth()) { }
    }
}

// Helper to populate the NativeAdView
fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    try {
        // Set the media view.
        adView.mediaView = adView.findViewById(R.id.ad_media)

        // Set other asset views.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)

        // The headline and mediaContent are guaranteed to be in every NativeAd.
        (adView.headlineView as TextView).text = nativeAd.headline
        adView.mediaView?.mediaContent = nativeAd.mediaContent

        // These assets aren't guaranteed to be in every NativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }
        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as Button).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(nativeAd.icon?.drawable)
            adView.iconView?.visibility = View.VISIBLE
        }

//        if (nativeAd.starRating == null || adView.starRatingView == null) {
//            adView.starRatingView?.visibility = View.INVISIBLE
//        } else {
//            (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
//            adView.starRatingView?.visibility = View.VISIBLE
//        }

        if (nativeAd.advertiser == null) {
            adView.advertiserView?.visibility = View.INVISIBLE
        } else {
            (adView.advertiserView as TextView).text = nativeAd.advertiser
            adView.advertiserView?.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)
    } catch (e: Exception) {
        e.printStackTrace()
        // Optionally: clear or hide the ad view here if something goes wrong.
    }
}