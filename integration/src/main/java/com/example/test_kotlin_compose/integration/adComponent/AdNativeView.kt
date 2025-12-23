package com.example.test_kotlin_compose.integration.adComponent

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.test_kotlin_compose.integration.R
import com.example.test_kotlin_compose.integration.adManager.AdClient
import com.example.test_kotlin_compose.integration.adManager.AdNativeHelper
import com.example.test_kotlin_compose.integration.adManager.AdUnitName
import com.example.test_kotlin_compose.integration.adManager.NativeAdManagerImpl
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.text.get
import kotlin.text.toFloat
import kotlin.text.toLong

@Composable
fun NativeAdComposable(
    adUnitName: AdUnitName,
    factoryId: String, // Maps to a layout ID
    manager: NativeAdManagerImpl,
    modifier: Modifier = Modifier,
    showLoadingCard: Boolean? = true,
    keepSize: Boolean? = false,
    autoLoad: Boolean? = true,
    highFloor: Boolean? = false,
) {
    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) };
    var nativeAd by remember { mutableStateOf(manager.getLoadedAd(adUnitName)) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val nativeWidth = remember { screenWidth } // Or passed param
    val nativeHeight = remember(factoryId) {
        AdNativeHelper.getAdNativeHeight(factoryId, configuration.screenHeightDp.dp)
    }
    var isDispose = false
    var adUnitId = remember { AdClient.getAdUnitId(adUnitName) }
    var failCounter = remember { 0 }
    var isCta = remember { false }
    var maxLoadFailCounter = remember { 3 }
    var delayTime: Int = remember { 100 }
    var saved = remember { false }
    var isHighFloor = remember { highFloor }
    val scope = rememberCoroutineScope()

    fun loadNativeAd() {
        try {
            val adLoader = AdLoader.Builder(context, adUnitId)
                .forNativeAd { ad: NativeAd ->
                    nativeAd = ad
                    failCounter = 0
                    isAdLoaded = true
                    ad.setOnPaidEventListener { adValue ->
                        // handle paid event if needed
                    }
                }
                .withAdListener(object : AdListener() {
                    override fun onAdClicked() {
                        super.onAdClicked()
                        AdClient.notifyAdClick()
                        if (adUnitName == AdUnitName.languageNative) {
                            // language native specific logic
                        }

                        if (adUnitName == AdUnitName.onboardNative || adUnitName == AdUnitName.onboardNative2) {
                            // onboard specific click behavior if needed
                        }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        super.onAdFailedToLoad(error)
                        if (isHighFloor ?: true) {
                            isHighFloor = false
                            scope.launch {
                                delay(delayTime.toLong())
                                adUnitId = AdClient.getAdUnitId(adUnitName)
                                loadNativeAd()
                            }
                        } else {
                            return
                        }
                    }
                })
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
            // Optionally: handle error state, e.g.
            // isAdLoaded = false
        }
    }


    fun creatNativeAd(autoLoad: Boolean) {
        if (isDispose) return;

        nativeAd = manager.getLoadedAd(adUnitName)

        if (nativeAd != null) {
            isAdLoaded = true

        } else if (!AdClient.canDismissAds() && !manager.waterfallApply) {
            loadNativeAd()
        } else {
            if (!manager.waterfallApply) {
                loadNativeAd()
            }
        }

    }

    LaunchedEffect(Unit) {
        maxLoadFailCounter = manager.maxLoadFailCounter
        delayTime = 100;
        val highFloorAdUnitId = AdClient.getHighFloor(adUnitName)
        if (highFloor == true) {
            adUnitId = highFloorAdUnitId ?: adUnitId
        }
        creatNativeAd(autoLoad == true)

    }

    if (isAdLoaded && nativeAd != null) {
        AndroidView(
            factory = { ctx ->
                // 1. Inflate the XML Layout based on factoryId
                val layoutId = AdNativeHelper.getLayoutIdFromFactory(factoryId)
                val adView = LayoutInflater.from(ctx).inflate(layoutId, null) as NativeAdView

                // 2. Populate the View
                populateNativeAdView(nativeAd!!, adView)

                adView
            },
            update = { adView ->
                // Update logic if needed
            },
            modifier = modifier
        )
    } else {
        Box {  }
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