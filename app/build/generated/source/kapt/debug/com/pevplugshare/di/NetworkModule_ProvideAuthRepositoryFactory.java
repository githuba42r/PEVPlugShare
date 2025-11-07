package com.pevplugshare.di;

import com.pevplugshare.data.remote.OsmandApiClient;
import com.pevplugshare.data.remote.TokenManager;
import com.pevplugshare.domain.repository.AuthRepository;
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
public final class NetworkModule_ProvideAuthRepositoryFactory implements Factory<AuthRepository> {
  private final Provider<OsmandApiClient> apiClientProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public NetworkModule_ProvideAuthRepositoryFactory(Provider<OsmandApiClient> apiClientProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.apiClientProvider = apiClientProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public AuthRepository get() {
    return provideAuthRepository(apiClientProvider.get(), tokenManagerProvider.get());
  }

  public static NetworkModule_ProvideAuthRepositoryFactory create(
      Provider<OsmandApiClient> apiClientProvider, Provider<TokenManager> tokenManagerProvider) {
    return new NetworkModule_ProvideAuthRepositoryFactory(apiClientProvider, tokenManagerProvider);
  }

  public static AuthRepository provideAuthRepository(OsmandApiClient apiClient,
      TokenManager tokenManager) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideAuthRepository(apiClient, tokenManager));
  }
}
