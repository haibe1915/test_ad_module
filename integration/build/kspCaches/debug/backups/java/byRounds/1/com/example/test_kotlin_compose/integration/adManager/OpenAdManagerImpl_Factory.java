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
public final class OpenAdManagerImpl_Factory implements Factory<OpenAdManagerImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<RemoteConfigProvider> remoteConfigProvider;

  public OpenAdManagerImpl_Factory(Provider<Context> contextProvider,
      Provider<RemoteConfigProvider> remoteConfigProvider) {
    this.contextProvider = contextProvider;
    this.remoteConfigProvider = remoteConfigProvider;
  }

  @Override
  public OpenAdManagerImpl get() {
    return newInstance(contextProvider.get(), remoteConfigProvider.get());
  }

  public static OpenAdManagerImpl_Factory create(Provider<Context> contextProvider,
      Provider<RemoteConfigProvider> remoteConfigProvider) {
    return new OpenAdManagerImpl_Factory(contextProvider, remoteConfigProvider);
  }

  public static OpenAdManagerImpl newInstance(Context context, RemoteConfigProvider remoteConfig) {
    return new OpenAdManagerImpl(context, remoteConfig);
  }
}
