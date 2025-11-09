package com.pevplugshare.di;

import android.content.Context;
import com.pevplugshare.data.remote.CookieManager;
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
public final class NetworkModule_ProvideAuthRepositoryFactory implements Factory<AuthRepository> {
  private final Provider<OsmandApiClient> apiClientProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<Context> contextProvider;

  private final Provider<CookieManager> cookieManagerProvider;

  public NetworkModule_ProvideAuthRepositoryFactory(Provider<OsmandApiClient> apiClientProvider,
      Provider<TokenManager> tokenManagerProvider, Provider<Context> contextProvider,
      Provider<CookieManager> cookieManagerProvider) {
    this.apiClientProvider = apiClientProvider;
    this.tokenManagerProvider = tokenManagerProvider;
    this.contextProvider = contextProvider;
    this.cookieManagerProvider = cookieManagerProvider;
  }

  @Override
  public AuthRepository get() {
    return provideAuthRepository(apiClientProvider.get(), tokenManagerProvider.get(), contextProvider.get(), cookieManagerProvider.get());
  }

  public static NetworkModule_ProvideAuthRepositoryFactory create(
      Provider<OsmandApiClient> apiClientProvider, Provider<TokenManager> tokenManagerProvider,
      Provider<Context> contextProvider, Provider<CookieManager> cookieManagerProvider) {
    return new NetworkModule_ProvideAuthRepositoryFactory(apiClientProvider, tokenManagerProvider, contextProvider, cookieManagerProvider);
  }

  public static AuthRepository provideAuthRepository(OsmandApiClient apiClient,
      TokenManager tokenManager, Context context, CookieManager cookieManager) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideAuthRepository(apiClient, tokenManager, context, cookieManager));
  }
}
