package com.example.test_kotlin_compose

import android.app.Application
import androidx.compose.runtime.rememberCoroutineScope
import com.example.test_kotlin_compose.firebase.FirebaseRemoteConfigProvider
import com.example.test_kotlin_compose.integration.adManager.AdClient
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TestKotlinComposeApplication : Application() {

    @Inject
    lateinit var firebaseRemoteConfigProvider: FirebaseRemoteConfigProvider
    @Inject
    lateinit var adClient: AdClient

    override fun onCreate() {
        super.onCreate()

        firebaseRemoteConfigProvider.init()
        adClient.initialize()

    }
}
