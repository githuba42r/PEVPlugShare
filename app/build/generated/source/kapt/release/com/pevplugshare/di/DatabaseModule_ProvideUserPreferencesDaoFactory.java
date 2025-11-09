package com.pevplugshare.di;

import com.pevplugshare.data.db.PEVPlugShareDatabase;
import com.pevplugshare.data.db.dao.UserPreferencesDao;
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
public final class DatabaseModule_ProvideUserPreferencesDaoFactory implements Factory<UserPreferencesDao> {
  private final Provider<PEVPlugShareDatabase> databaseProvider;

  public DatabaseModule_ProvideUserPreferencesDaoFactory(
      Provider<PEVPlugShareDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public UserPreferencesDao get() {
    return provideUserPreferencesDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideUserPreferencesDaoFactory create(
      Provider<PEVPlugShareDatabase> databaseProvider) {
    return new DatabaseModule_ProvideUserPreferencesDaoFactory(databaseProvider);
  }

  public static UserPreferencesDao provideUserPreferencesDao(PEVPlugShareDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideUserPreferencesDao(database));
  }
}
