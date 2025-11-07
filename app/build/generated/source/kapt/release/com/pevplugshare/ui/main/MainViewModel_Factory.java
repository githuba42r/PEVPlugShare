package com.pevplugshare.ui.main;

import com.pevplugshare.data.network.ConnectivityObserver;
import com.pevplugshare.data.repository.SyncManager;
import com.pevplugshare.domain.repository.AuthRepository;
import com.pevplugshare.domain.repository.LocationRepository;
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
public final class MainViewModel_Factory implements Factory<MainViewModel> {
  private final Provider<LocationRepository> locationRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<ConnectivityObserver> connectivityObserverProvider;

  private final Provider<SyncManager> syncManagerProvider;

  public MainViewModel_Factory(Provider<LocationRepository> locationRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<ConnectivityObserver> connectivityObserverProvider,
      Provider<SyncManager> syncManagerProvider) {
    this.locationRepositoryProvider = locationRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.connectivityObserverProvider = connectivityObserverProvider;
    this.syncManagerProvider = syncManagerProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(locationRepositoryProvider.get(), authRepositoryProvider.get(), connectivityObserverProvider.get(), syncManagerProvider.get());
  }

  public static MainViewModel_Factory create(
      Provider<LocationRepository> locationRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<ConnectivityObserver> connectivityObserverProvider,
      Provider<SyncManager> syncManagerProvider) {
    return new MainViewModel_Factory(locationRepositoryProvider, authRepositoryProvider, connectivityObserverProvider, syncManagerProvider);
  }

  public static MainViewModel newInstance(LocationRepository locationRepository,
      AuthRepository authRepository, ConnectivityObserver connectivityObserver,
      SyncManager syncManager) {
    return new MainViewModel(locationRepository, authRepository, connectivityObserver, syncManager);
  }
}
