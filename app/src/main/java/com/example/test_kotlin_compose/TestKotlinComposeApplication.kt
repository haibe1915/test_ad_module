package com.example.test_kotlin_compose

import android.app.Application
import com.example.test_kotlin_compose.config.RemoteConfigProvider
import com.example.test_kotlin_compose.firebase.FirebaseRemoteConfigProvider
import com.example.test_kotlin_compose.integration.adManager.AdClient
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TestKotlinComposeApplication : Application() {

    @Inject
    lateinit var firebaseRemoteConfigProvider: FirebaseRemoteConfigProvider

    override fun onCreate() {
        super.onCreate()

        // Initialize remote config default values early.
        firebaseRemoteConfigProvider.init()

        // Bridge integration module to the app's remote config implementation.
        AdClient.setRemoteConfigProvider(firebaseRemoteConfigProvider)
        AdClient.initialize()
    }
}

