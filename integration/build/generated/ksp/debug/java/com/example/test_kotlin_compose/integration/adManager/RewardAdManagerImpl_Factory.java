package com.example.test_kotlin_compose.integration.adManager;

import android.content.Context;
import com.example.test_kotlin_compose.integration.firebase.RemoteConfigProvider;
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
public final class RewardAdManagerImpl_Factory implements Factory<RewardAdManagerImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<RemoteConfigProvider> remoteConfigProvider;

  public RewardAdManagerImpl_Factory(Provider<Context> contextProvider,
      Provider<RemoteConfigProvider> remoteConfigProvider) {
    this.contextProvider = contextProvider;
    this.remoteConfigProvider = remoteConfigProvider;
  }

  @Override
  public RewardAdManagerImpl get() {
    return newInstance(contextProvider.get(), remoteConfigProvider.get());
  }

  public static RewardAdManagerImpl_Factory create(Provider<Context> contextProvider,
      Provider<RemoteConfigProvider> remoteConfigProvider) {
    return new RewardAdManagerImpl_Factory(contextProvider, remoteConfigProvider);
  }

  public static RewardAdManagerImpl newInstance(Context context,
      RemoteConfigProvider remoteConfig) {
    return new RewardAdManagerImpl(context, remoteConfig);
  }
}
