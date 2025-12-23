package com.example.test_kotlin_compose

import android.app.Application
import com.example.test_kotlin_compose.integration.adManager.AdClient
import com.example.test_kotlin_compose.integration.firebase.RemoteConfigProvider
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TestKotlinComposeApplication : Application() {

    @Inject lateinit var remoteConfigProvider: RemoteConfigProvider

    override fun onCreate() {
        super.onCreate()

        // Initialize remote config default values early.
        remoteConfigProvider.init()

        // Bridge integration module to the app's remote config implementation.
        AdClient.setRemoteConfigProvider(remoteConfigProvider)
        AdClient.initialize()
    }
}

