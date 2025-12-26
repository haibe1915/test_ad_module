// `integration/src/main/java/com/example/test_kotlin_compose/integration/adManager/di/AdManagerModule.kt`
package com.example.test_kotlin_compose.integration.adManager.di

import com.example.test_kotlin_compose.integration.adManager.AdClient
import com.example.test_kotlin_compose.integration.adManager.api.AdClientGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AdManagerModule {

    @Binds
    abstract fun bindAdGateway(impl: AdClient): AdClientGateway
}
