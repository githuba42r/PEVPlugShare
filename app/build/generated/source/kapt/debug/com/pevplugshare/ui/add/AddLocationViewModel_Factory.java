package com.pevplugshare.ui.add;

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
public final class AddLocationViewModel_Factory implements Factory<AddLocationViewModel> {
  private final Provider<LocationRepository> locationRepositoryProvider;

  public AddLocationViewModel_Factory(Provider<LocationRepository> locationRepositoryProvider) {
    this.locationRepositoryProvider = locationRepositoryProvider;
  }

  @Override
  public AddLocationViewModel get() {
    return newInstance(locationRepositoryProvider.get());
  }

  public static AddLocationViewModel_Factory create(
      Provider<LocationRepository> locationRepositoryProvider) {
    return new AddLocationViewModel_Factory(locationRepositoryProvider);
  }

  public static AddLocationViewModel newInstance(LocationRepository locationRepository) {
    return new AddLocationViewModel(locationRepository);
  }
}
