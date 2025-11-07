package com.pevplugshare.di;

import android.content.Context;
import com.pevplugshare.data.network.ConnectivityObserver;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
    "KotlinInternalInJava"
})
public final class NetworkModule_ProvideConnectivityObserverFactory implements Factory<ConnectivityObserver> {
  private final Provider<Context> contextProvider;

  public NetworkModule_ProvideConnectivityObserverFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ConnectivityObserver get() {
    return provideConnectivityObserver(contextProvider.get());
  }

  public static NetworkModule_ProvideConnectivityObserverFactory create(
      Provider<Context> contextProvider) {
    return new NetworkModule_ProvideConnectivityObserverFactory(contextProvider);
  }

  public static ConnectivityObserver provideConnectivityObserver(Context context) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideConnectivityObserver(context));
  }
}
