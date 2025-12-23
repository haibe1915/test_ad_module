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
public final class InterstialAdManagerImpl_Factory implements Factory<InterstialAdManagerImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<AdRemoteConfig> remoteConfigProvider;

  public InterstialAdManagerImpl_Factory(Provider<Context> contextProvider,
      Provider<AdRemoteConfig> remoteConfigProvider) {
    this.contextProvider = contextProvider;
    this.remoteConfigProvider = remoteConfigProvider;
  }

  @Override
  public InterstialAdManagerImpl get() {
    return newInstance(contextProvider.get(), remoteConfigProvider.get());
  }

  public static InterstialAdManagerImpl_Factory create(Provider<Context> contextProvider,
      Provider<AdRemoteConfig> remoteConfigProvider) {
    return new InterstialAdManagerImpl_Factory(contextProvider, remoteConfigProvider);
  }

  public static InterstialAdManagerImpl newInstance(Context context, AdRemoteConfig remoteConfig) {
    return new InterstialAdManagerImpl(context, remoteConfig);
  }
}
