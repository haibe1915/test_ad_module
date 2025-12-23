package com.example.test_kotlin_compose.di

import com.example.test_kotlin_compose.config.RemoteConfigProvider
import com.example.test_kotlin_compose.firebase.FirebaseRemoteConfigProvider
import com.example.test_kotlin_compose.integration.firebase.AdRemoteConfig
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteConfigModule {

    @Binds
    @Singleton
    abstract fun bindRemoteConfigProvider(
        impl: FirebaseRemoteConfigProvider
    ): RemoteConfigProvider

    @Binds
    @Singleton
    abstract fun bindAdRemoteConfig(
        impl: FirebaseRemoteConfigProvider
    ): AdRemoteConfig
}
