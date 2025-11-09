package com.pevplugshare.ui.settings;

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
public final class SettingsActivity_MembersInjector implements MembersInjector<SettingsActivity> {
  private final Provider<TokenManager> tokenManagerProvider;

  public SettingsActivity_MembersInjector(Provider<TokenManager> tokenManagerProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
  }

  public static MembersInjector<SettingsActivity> create(
      Provider<TokenManager> tokenManagerProvider) {
    return new SettingsActivity_MembersInjector(tokenManagerProvider);
  }

  @Override
  public void injectMembers(SettingsActivity instance) {
    injectTokenManager(instance, tokenManagerProvider.get());
  }

  @InjectedFieldSignature("com.pevplugshare.ui.settings.SettingsActivity.tokenManager")
  public static void injectTokenManager(SettingsActivity instance, TokenManager tokenManager) {
    instance.tokenManager = tokenManager;
  }
}
