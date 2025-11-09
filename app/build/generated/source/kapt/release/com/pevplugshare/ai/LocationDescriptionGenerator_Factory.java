package com.pevplugshare.ai;

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
public final class LocationDescriptionGenerator_Factory implements Factory<LocationDescriptionGenerator> {
  private final Provider<TokenManager> tokenManagerProvider;

  public LocationDescriptionGenerator_Factory(Provider<TokenManager> tokenManagerProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public LocationDescriptionGenerator get() {
    return newInstance(tokenManagerProvider.get());
  }

  public static LocationDescriptionGenerator_Factory create(
      Provider<TokenManager> tokenManagerProvider) {
    return new LocationDescriptionGenerator_Factory(tokenManagerProvider);
  }

  public static LocationDescriptionGenerator newInstance(TokenManager tokenManager) {
    return new LocationDescriptionGenerator(tokenManager);
  }
}
