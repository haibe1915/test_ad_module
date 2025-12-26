package com.example.test_kotlin_compose.integration.splash.api

import android.content.Context

/**
 * Platform abstraction so integration code can check notification permission without depending
 * on the app module.
 */
fun interface NotificationPermissionChecker {
    fun isComplete(context: Context): Boolean
}

