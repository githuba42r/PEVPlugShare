package com.pevplugshare.di;

import com.pevplugshare.data.db.PEVPlugShareDatabase;
import com.pevplugshare.data.db.dao.SyncStateDao;
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
public final class DatabaseModule_ProvideSyncStateDaoFactory implements Factory<SyncStateDao> {
  private final Provider<PEVPlugShareDatabase> databaseProvider;

  public DatabaseModule_ProvideSyncStateDaoFactory(
      Provider<PEVPlugShareDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public SyncStateDao get() {
    return provideSyncStateDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideSyncStateDaoFactory create(
      Provider<PEVPlugShareDatabase> databaseProvider) {
    return new DatabaseModule_ProvideSyncStateDaoFactory(databaseProvider);
  }

  public static SyncStateDao provideSyncStateDao(PEVPlugShareDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSyncStateDao(database));
  }
}
