package com.pevplugshare.di;

import com.pevplugshare.data.db.dao.PendingOperationDao;
import com.pevplugshare.data.repository.PendingOperationRepository;
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
public final class RepositoryModule_ProvidePendingOperationRepositoryFactory implements Factory<PendingOperationRepository> {
  private final Provider<PendingOperationDao> pendingOperationDaoProvider;

  public RepositoryModule_ProvidePendingOperationRepositoryFactory(
      Provider<PendingOperationDao> pendingOperationDaoProvider) {
    this.pendingOperationDaoProvider = pendingOperationDaoProvider;
  }

  @Override
  public PendingOperationRepository get() {
    return providePendingOperationRepository(pendingOperationDaoProvider.get());
  }

  public static RepositoryModule_ProvidePendingOperationRepositoryFactory create(
      Provider<PendingOperationDao> pendingOperationDaoProvider) {
    return new RepositoryModule_ProvidePendingOperationRepositoryFactory(pendingOperationDaoProvider);
  }

  public static PendingOperationRepository providePendingOperationRepository(
      PendingOperationDao pendingOperationDao) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.providePendingOperationRepository(pendingOperationDao));
  }
}
