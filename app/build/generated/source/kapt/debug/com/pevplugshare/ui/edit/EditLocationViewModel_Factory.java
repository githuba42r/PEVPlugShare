package com.pevplugshare.ui.edit;

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
public final class EditLocationViewModel_Factory implements Factory<EditLocationViewModel> {
  private final Provider<LocationRepository> locationRepositoryProvider;

  public EditLocationViewModel_Factory(Provider<LocationRepository> locationRepositoryProvider) {
    this.locationRepositoryProvider = locationRepositoryProvider;
  }

  @Override
  public EditLocationViewModel get() {
    return newInstance(locationRepositoryProvider.get());
  }

  public static EditLocationViewModel_Factory create(
      Provider<LocationRepository> locationRepositoryProvider) {
    return new EditLocationViewModel_Factory(locationRepositoryProvider);
  }

  public static EditLocationViewModel newInstance(LocationRepository locationRepository) {
    return new EditLocationViewModel(locationRepository);
  }
}
