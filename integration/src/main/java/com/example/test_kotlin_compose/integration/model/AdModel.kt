package com.example.test_kotlin_compose.integration.model

enum class CallToActionStyle {
    Fill,
    Stroke,
}

data class NativeConfig(
    val hideMedia: Boolean? = null,
    val space: Int? = null,
    val hideIcon: Boolean? = null,
    val smallFont: Boolean? = null,
    val hideBody: Boolean? = null,
    val hideButton: Boolean? = null,
    val reverse: Boolean? = null,
    val hideTagLine: Boolean? = null,
    val callToActionStyle: CallToActionStyle? = null,
    val disableExpand: Boolean? = null,
    val delayExpandTime: Int? = null,
)

data class LayoutNative(
    val id: Int,
    val customLayout: NativeConfig,
)

data class NativeRemoteConfig(
    val showAds: Boolean? = null,
    val highEcpm: Boolean? = null,
    val layout: List<LayoutNative>? = null,
    val random: List<Int>? = null,
    val layoutAfterClick: LayoutNative? = null,
    val action: Int? = null,
    val preload: Boolean? = null,
    val timeoutUseCommon: Int? = null,
    val timeRefreshInterval: Int? = null,
)

data class AdsIdRemoteConfig(
    val referenceConfigKey: String? = null,
    val lowIdName: String? = null,
    val lowId: String? = null,
    val highId: String? = null,
    val highIdName: String? = null,
)

data class AdsIdType(
    val lowId: String,
    val highId: String,
)

data class NativeAdConfig(
    val showAds: Boolean,
    val highEcpm: Boolean,
    val layout: List<LayoutNative>,
    val random: List<Int>? = null,
    val layoutAfterClick: LayoutNative? = null,
    val action: Int,
    val preload: Boolean,
    val timeoutUseCommon: Int? = null,
    val nativeId: AdsIdType,
    val timeRefreshInterval: Int? = null,
)

data class InterAdConfig(
    val showAds: Boolean,
    val highEcpm: Boolean,
    val interId: AdsIdType,
    val preload: Boolean,
    val timeRefreshInterval: Int? = null,
    val rateRequestHighId: List<List<Any>>? = null,
)

data class OpenAppAdConfig(
    val showAds: Boolean,
    val highEcpm: Boolean,
    val openAppId: AdsIdType,
    val preload: Boolean,
    val timeRefreshInterval: Int? = null,
    val rateRequestHighId: List<List<Any>>? = null,
)
