package com.pevplugshare.ui.edit;

import com.pevplugshare.ai.LocationDescriptionParser;
import com.pevplugshare.data.remote.TokenManager;
import com.pevplugshare.data.repository.OsmandLocationSyncService;
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

  private final Provider<OsmandLocationSyncService> osmandSyncServiceProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<LocationDescriptionParser> descriptionParserProvider;

  public EditLocationViewModel_Factory(Provider<LocationRepository> locationRepositoryProvider,
      Provider<OsmandLocationSyncService> osmandSyncServiceProvider,
      Provider<TokenManager> tokenManagerProvider,
      Provider<LocationDescriptionParser> descriptionParserProvider) {
    this.locationRepositoryProvider = locationRepositoryProvider;
    this.osmandSyncServiceProvider = osmandSyncServiceProvider;
    this.tokenManagerProvider = tokenManagerProvider;
    this.descriptionParserProvider = descriptionParserProvider;
  }

  @Override
  public EditLocationViewModel get() {
    return newInstance(locationRepositoryProvider.get(), osmandSyncServiceProvider.get(), tokenManagerProvider.get(), descriptionParserProvider.get());
  }

  public static EditLocationViewModel_Factory create(
      Provider<LocationRepository> locationRepositoryProvider,
      Provider<OsmandLocationSyncService> osmandSyncServiceProvider,
      Provider<TokenManager> tokenManagerProvider,
      Provider<LocationDescriptionParser> descriptionParserProvider) {
    return new EditLocationViewModel_Factory(locationRepositoryProvider, osmandSyncServiceProvider, tokenManagerProvider, descriptionParserProvider);
  }

  public static EditLocationViewModel newInstance(LocationRepository locationRepository,
      OsmandLocationSyncService osmandSyncService, TokenManager tokenManager,
      LocationDescriptionParser descriptionParser) {
    return new EditLocationViewModel(locationRepository, osmandSyncService, tokenManager, descriptionParser);
  }
}
