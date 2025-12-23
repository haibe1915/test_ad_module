package com.example.test_kotlin_compose.di

import com.example.test_kotlin_compose.firebase.FirebaseRemoteConfigProvider
import com.example.test_kotlin_compose.integration.firebase.RemoteConfigProvider
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
}

