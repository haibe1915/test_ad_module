package com.example.test_kotlin_compose.integration.adManager

/**
 * Keeps module behavior configurable per host app.
 *
 * - [isDebug] controls using test ad unit ids.
 * - [forceDisableAds] can be used by host app (premium user, etc.).
 */
data class AdsEnvironment(
    val isDebug: Boolean = false,
    val forceDisableAds: Boolean = false,
)

