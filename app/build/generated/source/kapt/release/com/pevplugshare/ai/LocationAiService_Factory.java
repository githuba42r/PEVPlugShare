package com.pevplugshare.ai;

import com.pevplugshare.data.db.dao.LocationDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class LocationAiService_Factory implements Factory<LocationAiService> {
  private final Provider<LocationDescriptionParser> parserProvider;

  private final Provider<LocationDao> locationDaoProvider;

  public LocationAiService_Factory(Provider<LocationDescriptionParser> parserProvider,
      Provider<LocationDao> locationDaoProvider) {
    this.parserProvider = parserProvider;
    this.locationDaoProvider = locationDaoProvider;
  }

  @Override
  public LocationAiService get() {
    return newInstance(parserProvider.get(), locationDaoProvider.get());
  }

  public static LocationAiService_Factory create(Provider<LocationDescriptionParser> parserProvider,
      Provider<LocationDao> locationDaoProvider) {
    return new LocationAiService_Factory(parserProvider, locationDaoProvider);
  }

  public static LocationAiService newInstance(LocationDescriptionParser parser,
      LocationDao locationDao) {
    return new LocationAiService(parser, locationDao);
  }
}
