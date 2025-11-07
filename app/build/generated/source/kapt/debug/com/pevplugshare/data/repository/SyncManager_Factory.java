package com.pevplugshare.data.repository;

import com.pevplugshare.domain.repository.LocationRepository;
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
public final class SyncManager_Factory implements Factory<SyncManager> {
  private final Provider<PendingOperationRepository> pendingOperationRepositoryProvider;

  private final Provider<LocationRepository> locationRepositoryProvider;

  public SyncManager_Factory(
      Provider<PendingOperationRepository> pendingOperationRepositoryProvider,
      Provider<LocationRepository> locationRepositoryProvider) {
    this.pendingOperationRepositoryProvider = pendingOperationRepositoryProvider;
    this.locationRepositoryProvider = locationRepositoryProvider;
  }

  @Override
  public SyncManager get() {
    return newInstance(pendingOperationRepositoryProvider.get(), locationRepositoryProvider.get());
  }

  public static SyncManager_Factory create(
      Provider<PendingOperationRepository> pendingOperationRepositoryProvider,
      Provider<LocationRepository> locationRepositoryProvider) {
    return new SyncManager_Factory(pendingOperationRepositoryProvider, locationRepositoryProvider);
  }

  public static SyncManager newInstance(PendingOperationRepository pendingOperationRepository,
      LocationRepository locationRepository) {
    return new SyncManager(pendingOperationRepository, locationRepository);
  }
}
