package com.example.test_kotlin_compose.integration.adManager;

import android.content.Context;
import com.example.test_kotlin_compose.integration.firebase.AdRemoteConfig;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class BannerAdManagerImpl_Factory implements Factory<BannerAdManagerImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<AdRemoteConfig> remoteConfigProvider;

  public BannerAdManagerImpl_Factory(Provider<Context> contextProvider,
      Provider<AdRemoteConfig> remoteConfigProvider) {
    this.contextProvider = contextProvider;
    this.remoteConfigProvider = remoteConfigProvider;
  }

  @Override
  public BannerAdManagerImpl get() {
    return newInstance(contextProvider.get(), remoteConfigProvider.get());
  }

  public static BannerAdManagerImpl_Factory create(Provider<Context> contextProvider,
      Provider<AdRemoteConfig> remoteConfigProvider) {
    return new BannerAdManagerImpl_Factory(contextProvider, remoteConfigProvider);
  }

  public static BannerAdManagerImpl newInstance(Context context, AdRemoteConfig remoteConfig) {
    return new BannerAdManagerImpl(context, remoteConfig);
  }
}
