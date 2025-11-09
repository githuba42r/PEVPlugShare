package com.pevplugshare.data.repository;

import com.pevplugshare.data.remote.OsmandApiClient;
import com.pevplugshare.data.remote.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class OsmandLocationSyncService_Factory implements Factory<OsmandLocationSyncService> {
  private final Provider<OsmandApiClient> apiClientProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<LocalLocationRepository> localRepositoryProvider;

  public OsmandLocationSyncService_Factory(Provider<OsmandApiClient> apiClientProvider,
      Provider<TokenManager> tokenManagerProvider,
      Provider<LocalLocationRepository> localRepositoryProvider) {
    this.apiClientProvider = apiClientProvider;
    this.tokenManagerProvider = tokenManagerProvider;
    this.localRepositoryProvider = localRepositoryProvider;
  }

  @Override
  public OsmandLocationSyncService get() {
    return newInstance(apiClientProvider.get(), tokenManagerProvider.get(), localRepositoryProvider.get());
  }

  public static OsmandLocationSyncService_Factory create(
      Provider<OsmandApiClient> apiClientProvider, Provider<TokenManager> tokenManagerProvider,
      Provider<LocalLocationRepository> localRepositoryProvider) {
    return new OsmandLocationSyncService_Factory(apiClientProvider, tokenManagerProvider, localRepositoryProvider);
  }

  public static OsmandLocationSyncService newInstance(OsmandApiClient apiClient,
      TokenManager tokenManager, LocalLocationRepository localRepository) {
    return new OsmandLocationSyncService(apiClient, tokenManager, localRepository);
  }
}
