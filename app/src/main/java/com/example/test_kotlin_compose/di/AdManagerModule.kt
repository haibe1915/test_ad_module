//package com.example.test_kotlin_compose.di
//
//import com.example.test_kotlin_compose.config.RemoteConfigProvider
//import com.example.test_kotlin_compose.firebase.FirebaseRemoteConfigProvider
//import com.example.test_kotlin_compose.integration.adManager.BannerAdManagerImpl
//import com.example.test_kotlin_compose.integration.adManager.BannerAdManagerInterface
//import com.example.test_kotlin_compose.integration.adManager.InterstialAdManagerImpl
//import com.example.test_kotlin_compose.integration.adManager.InterstitialAdManagerInterface
//import com.example.test_kotlin_compose.integration.adManager.NativeAdManagerImpl
//import com.example.test_kotlin_compose.integration.adManager.NativeAdManagerInterface
//import com.example.test_kotlin_compose.integration.adManager.OpenAdManagerImpl
//import com.example.test_kotlin_compose.integration.adManager.OpenAdManagerInterface
//import com.example.test_kotlin_compose.integration.adManager.RewardAdManagerImpl
//import com.example.test_kotlin_compose.integration.adManager.RewardAdManagerInterface
//import com.example.test_kotlin_compose.integration.firebase.AdRemoteConfig
//import dagger.Binds
//import dagger.Module
//import dagger.hilt.InstallIn
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton
//
//@Module
//@InstallIn(SingletonComponent::class)
//abstract class AdsManagerModule {
//
//    @Binds
//    @Singleton
//    abstract fun bindNativeAdManager(
//        impl: NativeAdManagerImpl
//    ): NativeAdManagerInterface
//
//    @Binds
//    @Singleton
//    abstract fun bindInterstitialAdManager(
//        impl: InterstialAdManagerImpl
//    ): InterstitialAdManagerInterface
//
//    @Binds
//    @Singleton
//    abstract fun bindOpenAdManager(
//        impl: OpenAdManagerImpl
//    ): OpenAdManagerInterface
//
//    @Binds
//    @Singleton
//    abstract fun bindRewardAdManager(
//        impl: RewardAdManagerImpl
//    ): RewardAdManagerInterface
//
//    @Binds
//    @Singleton
//    abstract fun bindBannerAdManager(
//        impl: BannerAdManagerImpl
//    ): BannerAdManagerInterface
//}
