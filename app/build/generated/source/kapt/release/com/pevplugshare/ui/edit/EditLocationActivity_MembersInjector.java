package com.pevplugshare.ui.edit;

import com.pevplugshare.ai.PhotoAnalyzer;
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
public final class EditLocationActivity_MembersInjector implements MembersInjector<EditLocationActivity> {
  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<PhotoAnalyzer> photoAnalyzerProvider;

  public EditLocationActivity_MembersInjector(Provider<TokenManager> tokenManagerProvider,
      Provider<PhotoAnalyzer> photoAnalyzerProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
    this.photoAnalyzerProvider = photoAnalyzerProvider;
  }

  public static MembersInjector<EditLocationActivity> create(
      Provider<TokenManager> tokenManagerProvider, Provider<PhotoAnalyzer> photoAnalyzerProvider) {
    return new EditLocationActivity_MembersInjector(tokenManagerProvider, photoAnalyzerProvider);
  }

  @Override
  public void injectMembers(EditLocationActivity instance) {
    injectTokenManager(instance, tokenManagerProvider.get());
    injectPhotoAnalyzer(instance, photoAnalyzerProvider.get());
  }

  @InjectedFieldSignature("com.pevplugshare.ui.edit.EditLocationActivity.tokenManager")
  public static void injectTokenManager(EditLocationActivity instance, TokenManager tokenManager) {
    instance.tokenManager = tokenManager;
  }

  @InjectedFieldSignature("com.pevplugshare.ui.edit.EditLocationActivity.photoAnalyzer")
  public static void injectPhotoAnalyzer(EditLocationActivity instance,
      PhotoAnalyzer photoAnalyzer) {
    instance.photoAnalyzer = photoAnalyzer;
  }
}
