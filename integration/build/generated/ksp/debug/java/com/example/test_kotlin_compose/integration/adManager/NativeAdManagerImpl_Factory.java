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
public final class NativeAdManagerImpl_Factory implements Factory<NativeAdManagerImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<RemoteConfigProvider> remoteConfigProvider;

  public NativeAdManagerImpl_Factory(Provider<Context> contextProvider,
      Provider<RemoteConfigProvider> remoteConfigProvider) {
    this.contextProvider = contextProvider;
    this.remoteConfigProvider = remoteConfigProvider;
  }

  @Override
  public NativeAdManagerImpl get() {
    return newInstance(contextProvider.get(), remoteConfigProvider.get());
  }

  public static NativeAdManagerImpl_Factory create(Provider<Context> contextProvider,
      Provider<RemoteConfigProvider> remoteConfigProvider) {
    return new NativeAdManagerImpl_Factory(contextProvider, remoteConfigProvider);
  }

  public static NativeAdManagerImpl newInstance(Context context,
      RemoteConfigProvider remoteConfig) {
    return new NativeAdManagerImpl(context, remoteConfig);
  }
}
