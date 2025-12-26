package com.example.test_kotlin_compose.integration.init.di

import com.example.test_kotlin_compose.integration.init.api.AdsInitializer
import com.example.test_kotlin_compose.integration.init.impl.AdsInitializerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AdsInitModule {

    @Binds
    abstract fun bindAdsInitializer(impl: AdsInitializerImpl): AdsInitializer
}