package com.pevplugshare.di;

import android.content.Context;
import com.pevplugshare.data.remote.CookieManager;
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
public final class NetworkModule_ProvideCookieManagerFactory implements Factory<CookieManager> {
  private final Provider<Context> contextProvider;

  public NetworkModule_ProvideCookieManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public CookieManager get() {
    return provideCookieManager(contextProvider.get());
  }

  public static NetworkModule_ProvideCookieManagerFactory create(
      Provider<Context> contextProvider) {
    return new NetworkModule_ProvideCookieManagerFactory(contextProvider);
  }

  public static CookieManager provideCookieManager(Context context) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideCookieManager(context));
  }
}
