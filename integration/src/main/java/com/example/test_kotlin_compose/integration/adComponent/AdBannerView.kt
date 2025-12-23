package com.example.test_kotlin_compose.integration.adComponent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.test_kotlin_compose.integration.adManager.AdClient
import com.example.test_kotlin_compose.integration.adManager.AdUnitName
import com.example.test_kotlin_compose.integration.adManager.BannerAdManagerImpl
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.delay

@Composable
fun BannerAdComposable(
    adUnitName: AdUnitName,
    showLoadingCard: Boolean? = true,
    keepSize: Boolean? = false,
    retryNumber: Int? = 3,
    adManager: BannerAdManagerImpl // Inject or pass this
){
    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }
    var failCounter by remember { mutableIntStateOf(0) }
    var adView by remember { mutableStateOf<AdView?>(null) }
    val maxLoadFailCounter = retryNumber ?: adManager.maxLoadFailCounter
    val reloadTime = adManager.delayTime.toLong()

    // Check if we can dismiss ads
    if (AdClient.canDismissAds()) {
        return
    }

    LaunchedEffect(adUnitName) {
        if (AdClient.isDisablePreload(adUnitName)) {
             // Handle self load logic if needed, similar to Dart's selfLoad
        }

        val existingAd = adManager.get(adUnitName)
        if (existingAd != null) {
            adView = existingAd
            isAdLoaded = true
        } else {
            // Create and load new ad
            val newAdView = AdView(context)
            newAdView.setAdSize(adManager.createAdSize(adUnitName))
            newAdView.adUnitId = AdClient.getAdUnitId(adUnitName)

            val extras = adManager.getCollapsibleExtras(adUnitName)
            val adRequestBuilder = AdRequest.Builder()
            if (extras != null) {
                adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            }

            newAdView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    isAdLoaded = true
                    adManager.save(adUnitName, newAdView)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    failCounter++
                    if (failCounter <= maxLoadFailCounter) {
                        // Retry logic handled by LaunchedEffect re-trigger or separate coroutine if needed
                        // For simplicity in Compose, we might need a more robust retry mechanism
                        // Here we just log or ignore for now, or trigger a state change to retry
                    }
                }

                override fun onAdClicked() {
                    AdClient.notifyAdClick()
                    // LoggerManager logic
                }

                override fun onAdImpression() {
                    // LoggerManager logic
                }
            }

            newAdView.loadAd(adRequestBuilder.build())
            adView = newAdView
        }
    }

    // Retry Logic (Simplified)
    LaunchedEffect(failCounter) {
        if (failCounter > 0 && failCounter <= maxLoadFailCounter) {
            delay(reloadTime)
            adView?.loadAd(AdRequest.Builder().build()) // Re-load
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Handle disposal if needed.
            // Note: If we are caching ads in AdManager, we might NOT want to destroy them here
            // unless the manager says so or we are destroying the whole screen/app.
            // The Dart code had `isDispose = true` but commented out `_bannerAd?.dispose()`.
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        if (isAdLoaded && adView != null) {
            AndroidView(
                factory = { adView!! },
                modifier = Modifier.fillMaxWidth()
            )
        } else if (showLoadingCard == true) {
             // Show Loading Card
             // BannerLoadingCard(...)
             Text("Loading Ad...") // Placeholder
        } else if (keepSize == true) {
             // Transparent placeholder
             Box(modifier = Modifier.height(50.dp).fillMaxWidth()) // Height should be dynamic based on ad size
        }
    }
}