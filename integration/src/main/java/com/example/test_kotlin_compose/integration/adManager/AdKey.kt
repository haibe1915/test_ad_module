//package com.example.test_kotlin_compose.integration.adManager
//
///**
// * Open ad placement identifier.
// *
// * Why not enum?
// * - Enums are closed: other apps/modules cannot add new placements.
// * - This value class is open: callers can create new keys freely.
// */
//@JvmInline
//value class AdUnitKey(val value: String) {
//    init {
//        require(value.isNotBlank()) { "AdUnitKey must not be blank" }
//    }
//
//    override fun toString(): String = value
//}
//
///**
// * Canonical built-in keys shipped with the integration module.
// *
// * Apps can add their own keys:
// * `val MyNative = AdUnitKey("my_native")`
// */
////object AdUnitKeys {
//////    val HomeBanner = AdUnitKey("homeBanner")
//////    val LanguageBanner = AdUnitKey("languageBanner")
//////    val MapBanner = AdUnitKey("mapBanner")
//////    val ScanBanner = AdUnitKey("scanBanner")
//////
//////    val ConvertNative = AdUnitKey("convertNative")
//////    val ResultNative = AdUnitKey("resultNative")
//////    val HistoryNative = AdUnitKey("historyNative")
//////    val LanguageNative = AdUnitKey("languageNative")
//////
//////    val OnboardNative = AdUnitKey("onboardNative")
//////    val OnboardNative2 = AdUnitKey("onboardNative2")
//////    val OnboardFullNative = AdUnitKey("onboardFullNative")
//////
//////    val SettingNative = AdUnitKey("settingNative")
//////    val ScanSuccessNative = AdUnitKey("scanSuccessNative")
//////    val CustomizeNative = AdUnitKey("customizeNative")
//////    val UninstallNative = AdUnitKey("uninstallNative")
//////    val BusinessNative = AdUnitKey("businessNative")
//////
//////    val ResultReward = AdUnitKey("resultReward")
//////
//////    val ResultInterstitial = AdUnitKey("resultInterstitial")
//////    val ConvertInterstitial = AdUnitKey("convertInterstitial")
//////    val OpenInterstitial = AdUnitKey("openInterstitial")
//////    val UninstallInterstitial = AdUnitKey("uninstallInterstitial")
//////
//////    val PremiumReward = AdUnitKey("premiumReward")
//////
//////    val GlobalOpen = AdUnitKey("globalOpen")
//////    val Back = AdUnitKey("back")
////}
