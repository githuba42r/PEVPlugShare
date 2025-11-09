package com.pevplugshare.di;

import com.pevplugshare.data.db.dao.LocationDao;
import com.pevplugshare.domain.repository.LocationRepository;
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
public final class NetworkModule_ProvideLocationRepositoryFactory implements Factory<LocationRepository> {
  private final Provider<LocationDao> locationDaoProvider;

  public NetworkModule_ProvideLocationRepositoryFactory(Provider<LocationDao> locationDaoProvider) {
    this.locationDaoProvider = locationDaoProvider;
  }

  @Override
  public LocationRepository get() {
    return provideLocationRepository(locationDaoProvider.get());
  }

  public static NetworkModule_ProvideLocationRepositoryFactory create(
      Provider<LocationDao> locationDaoProvider) {
    return new NetworkModule_ProvideLocationRepositoryFactory(locationDaoProvider);
  }

  public static LocationRepository provideLocationRepository(LocationDao locationDao) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideLocationRepository(locationDao));
  }
}
