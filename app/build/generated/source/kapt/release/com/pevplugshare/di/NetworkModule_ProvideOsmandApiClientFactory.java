package com.pevplugshare.di;

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

  public NetworkModule_ProvideOsmandApiClientFactory(Provider<TokenManager> tokenManagerProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public OsmandApiClient get() {
    return provideOsmandApiClient(tokenManagerProvider.get());
  }

  public static NetworkModule_ProvideOsmandApiClientFactory create(
      Provider<TokenManager> tokenManagerProvider) {
    return new NetworkModule_ProvideOsmandApiClientFactory(tokenManagerProvider);
  }

  public static OsmandApiClient provideOsmandApiClient(TokenManager tokenManager) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideOsmandApiClient(tokenManager));
  }
}
