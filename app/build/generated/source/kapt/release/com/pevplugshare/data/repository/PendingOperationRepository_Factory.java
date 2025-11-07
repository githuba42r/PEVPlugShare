package com.pevplugshare.data.repository;

import com.pevplugshare.data.db.dao.PendingOperationDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class PendingOperationRepository_Factory implements Factory<PendingOperationRepository> {
  private final Provider<PendingOperationDao> pendingOperationDaoProvider;

  public PendingOperationRepository_Factory(
      Provider<PendingOperationDao> pendingOperationDaoProvider) {
    this.pendingOperationDaoProvider = pendingOperationDaoProvider;
  }

  @Override
  public PendingOperationRepository get() {
    return newInstance(pendingOperationDaoProvider.get());
  }

  public static PendingOperationRepository_Factory create(
      Provider<PendingOperationDao> pendingOperationDaoProvider) {
    return new PendingOperationRepository_Factory(pendingOperationDaoProvider);
  }

  public static PendingOperationRepository newInstance(PendingOperationDao pendingOperationDao) {
    return new PendingOperationRepository(pendingOperationDao);
  }
}
