package com.pevplugshare.di;

import com.pevplugshare.data.db.PEVPlugShareDatabase;
import com.pevplugshare.data.db.dao.PhotoDao;
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
public final class DatabaseModule_ProvidePhotoDaoFactory implements Factory<PhotoDao> {
  private final Provider<PEVPlugShareDatabase> databaseProvider;

  public DatabaseModule_ProvidePhotoDaoFactory(Provider<PEVPlugShareDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public PhotoDao get() {
    return providePhotoDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvidePhotoDaoFactory create(
      Provider<PEVPlugShareDatabase> databaseProvider) {
    return new DatabaseModule_ProvidePhotoDaoFactory(databaseProvider);
  }

  public static PhotoDao providePhotoDao(PEVPlugShareDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePhotoDao(database));
  }
}
