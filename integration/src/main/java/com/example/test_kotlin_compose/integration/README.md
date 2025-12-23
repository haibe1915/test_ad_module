# integration module

This Gradle module is intended to be reusable across multiple apps.

## Design goals
- No direct dependency on Firebase: host app provides `RemoteConfigProvider` implementation.
- Ads logic is separated into:
  - `adManager/` (loading/showing/caching/waterfall)
  - `adComponent/` (Compose UI wrappers)

## Initialization
The host app must:
1. Provide an implementation of `RemoteConfigProvider` (typically via Hilt).
2. Call `AdClient.setRemoteConfigProvider(...)` once at app startup.
3. Call `AdClient.initialize()` after remote config defaults are ready.

## Recommended API usage
Prefer injecting `NativeAdManagerInterface`, `InterstitialAdManagerInterface`, ... instead of concrete `*Impl` classes.

