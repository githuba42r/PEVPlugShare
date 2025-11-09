package com.pevplugshare.di;

import com.pevplugshare.data.remote.CookieManager;
import com.pevplugshare.data.remote.OsmandApiClient;
import com.pevplugshare.data.remote.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class NetworkModule_ProvideOsmandApiClientFactory implements Factory<OsmandApiClient> {
  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<CookieManager> cookieManagerProvider;

  public NetworkModule_ProvideOsmandApiClientFactory(Provider<TokenManager> tokenManagerProvider,
      Provider<CookieManager> cookieManagerProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
    this.cookieManagerProvider = cookieManagerProvider;
  }

  @Override
  public OsmandApiClient get() {
    return provideOsmandApiClient(tokenManagerProvider.get(), cookieManagerProvider.get());
  }

  public static NetworkModule_ProvideOsmandApiClientFactory create(
      Provider<TokenManager> tokenManagerProvider, Provider<CookieManager> cookieManagerProvider) {
    return new NetworkModule_ProvideOsmandApiClientFactory(tokenManagerProvider, cookieManagerProvider);
  }

  public static OsmandApiClient provideOsmandApiClient(TokenManager tokenManager,
      CookieManager cookieManager) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideOsmandApiClient(tokenManager, cookieManager));
  }
}
