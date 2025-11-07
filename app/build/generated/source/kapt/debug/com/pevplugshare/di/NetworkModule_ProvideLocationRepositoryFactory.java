package com.pevplugshare.di;

import com.pevplugshare.data.remote.OsmandApiClient;
import com.pevplugshare.data.remote.TokenManager;
import com.pevplugshare.domain.repository.LocationRepository;
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
public final class NetworkModule_ProvideLocationRepositoryFactory implements Factory<LocationRepository> {
  private final Provider<OsmandApiClient> apiClientProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public NetworkModule_ProvideLocationRepositoryFactory(Provider<OsmandApiClient> apiClientProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.apiClientProvider = apiClientProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public LocationRepository get() {
    return provideLocationRepository(apiClientProvider.get(), tokenManagerProvider.get());
  }

  public static NetworkModule_ProvideLocationRepositoryFactory create(
      Provider<OsmandApiClient> apiClientProvider, Provider<TokenManager> tokenManagerProvider) {
    return new NetworkModule_ProvideLocationRepositoryFactory(apiClientProvider, tokenManagerProvider);
  }

  public static LocationRepository provideLocationRepository(OsmandApiClient apiClient,
      TokenManager tokenManager) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideLocationRepository(apiClient, tokenManager));
  }
}
