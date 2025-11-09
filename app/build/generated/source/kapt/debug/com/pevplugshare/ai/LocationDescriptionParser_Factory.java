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
public final class LocationDescriptionParser_Factory implements Factory<LocationDescriptionParser> {
  private final Provider<TokenManager> tokenManagerProvider;

  public LocationDescriptionParser_Factory(Provider<TokenManager> tokenManagerProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public LocationDescriptionParser get() {
    return newInstance(tokenManagerProvider.get());
  }

  public static LocationDescriptionParser_Factory create(
      Provider<TokenManager> tokenManagerProvider) {
    return new LocationDescriptionParser_Factory(tokenManagerProvider);
  }

  public static LocationDescriptionParser newInstance(TokenManager tokenManager) {
    return new LocationDescriptionParser(tokenManager);
  }
}
