package com.pevplugshare.ui.main;

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

  public MainViewModel_Factory(Provider<LocationRepository> locationRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.locationRepositoryProvider = locationRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(locationRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static MainViewModel_Factory create(
      Provider<LocationRepository> locationRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new MainViewModel_Factory(locationRepositoryProvider, authRepositoryProvider);
  }

  public static MainViewModel newInstance(LocationRepository locationRepository,
      AuthRepository authRepository) {
    return new MainViewModel(locationRepository, authRepository);
  }
}
