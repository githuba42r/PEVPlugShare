package com.pevplugshare.ai;

import android.content.Context;
import com.pevplugshare.data.remote.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class PhotoAnalyzer_Factory implements Factory<PhotoAnalyzer> {
  private final Provider<Context> contextProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public PhotoAnalyzer_Factory(Provider<Context> contextProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.contextProvider = contextProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public PhotoAnalyzer get() {
    return newInstance(contextProvider.get(), tokenManagerProvider.get());
  }

  public static PhotoAnalyzer_Factory create(Provider<Context> contextProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new PhotoAnalyzer_Factory(contextProvider, tokenManagerProvider);
  }

  public static PhotoAnalyzer newInstance(Context context, TokenManager tokenManager) {
    return new PhotoAnalyzer(context, tokenManager);
  }
}
