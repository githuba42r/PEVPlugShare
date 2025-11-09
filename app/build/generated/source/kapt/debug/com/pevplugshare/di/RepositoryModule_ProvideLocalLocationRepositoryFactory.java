package com.pevplugshare.di;

import com.pevplugshare.data.db.dao.LocationDao;
import com.pevplugshare.data.repository.LocalLocationRepository;
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
public final class RepositoryModule_ProvideLocalLocationRepositoryFactory implements Factory<LocalLocationRepository> {
  private final Provider<LocationDao> locationDaoProvider;

  public RepositoryModule_ProvideLocalLocationRepositoryFactory(
      Provider<LocationDao> locationDaoProvider) {
    this.locationDaoProvider = locationDaoProvider;
  }

  @Override
  public LocalLocationRepository get() {
    return provideLocalLocationRepository(locationDaoProvider.get());
  }

  public static RepositoryModule_ProvideLocalLocationRepositoryFactory create(
      Provider<LocationDao> locationDaoProvider) {
    return new RepositoryModule_ProvideLocalLocationRepositoryFactory(locationDaoProvider);
  }

  public static LocalLocationRepository provideLocalLocationRepository(LocationDao locationDao) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideLocalLocationRepository(locationDao));
  }
}
