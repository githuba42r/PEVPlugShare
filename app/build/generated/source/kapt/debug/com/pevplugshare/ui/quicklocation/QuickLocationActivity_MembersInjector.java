package com.pevplugshare.ui.quicklocation;

import com.pevplugshare.data.remote.TokenManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class QuickLocationActivity_MembersInjector implements MembersInjector<QuickLocationActivity> {
  private final Provider<TokenManager> tokenManagerProvider;

  public QuickLocationActivity_MembersInjector(Provider<TokenManager> tokenManagerProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
  }

  public static MembersInjector<QuickLocationActivity> create(
      Provider<TokenManager> tokenManagerProvider) {
    return new QuickLocationActivity_MembersInjector(tokenManagerProvider);
  }

  @Override
  public void injectMembers(QuickLocationActivity instance) {
    injectTokenManager(instance, tokenManagerProvider.get());
  }

  @InjectedFieldSignature("com.pevplugshare.ui.quicklocation.QuickLocationActivity.tokenManager")
  public static void injectTokenManager(QuickLocationActivity instance, TokenManager tokenManager) {
    instance.tokenManager = tokenManager;
  }
}
