package com.pevplugshare.di;

import com.pevplugshare.data.db.PEVPlugShareDatabase;
import com.pevplugshare.data.db.dao.PendingLocationDao;
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
public final class DatabaseModule_ProvidePendingLocationDaoFactory implements Factory<PendingLocationDao> {
  private final Provider<PEVPlugShareDatabase> databaseProvider;

  public DatabaseModule_ProvidePendingLocationDaoFactory(
      Provider<PEVPlugShareDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public PendingLocationDao get() {
    return providePendingLocationDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvidePendingLocationDaoFactory create(
      Provider<PEVPlugShareDatabase> databaseProvider) {
    return new DatabaseModule_ProvidePendingLocationDaoFactory(databaseProvider);
  }

  public static PendingLocationDao providePendingLocationDao(PEVPlugShareDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePendingLocationDao(database));
  }
}
