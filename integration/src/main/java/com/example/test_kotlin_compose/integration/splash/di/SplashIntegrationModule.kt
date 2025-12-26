package com.example.test_kotlin_compose.integration.splash.di

import com.example.test_kotlin_compose.integration.platform.AndroidNotificationPermissionChecker
import com.example.test_kotlin_compose.integration.splash.api.NotificationPermissionChecker
import com.example.test_kotlin_compose.integration.splash.api.SplashAdCoordinator
import com.example.test_kotlin_compose.integration.splash.impl.SplashAdCoordinatorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SplashIntegrationModule {

    @Binds
    abstract fun bindSplashAdCoordinator(impl: SplashAdCoordinatorImpl): SplashAdCoordinator

    @Binds
    abstract fun bindNotificationPermissionChecker(
        impl: AndroidNotificationPermissionChecker,
    ): NotificationPermissionChecker
}

