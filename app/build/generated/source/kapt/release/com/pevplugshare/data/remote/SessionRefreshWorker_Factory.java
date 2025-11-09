package com.pevplugshare.data.remote;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.pevplugshare.domain.repository.AuthRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SessionRefreshWorker_Factory {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public SessionRefreshWorker_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  public SessionRefreshWorker get(Context context, WorkerParameters params) {
    return newInstance(context, params, authRepositoryProvider.get(), tokenManagerProvider.get());
  }

  public static SessionRefreshWorker_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new SessionRefreshWorker_Factory(authRepositoryProvider, tokenManagerProvider);
  }

  public static SessionRefreshWorker newInstance(Context context, WorkerParameters params,
      AuthRepository authRepository, TokenManager tokenManager) {
    return new SessionRefreshWorker(context, params, authRepository, tokenManager);
  }
}
