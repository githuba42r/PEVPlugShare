package com.pevplugshare.ui.photo;

import com.pevplugshare.data.storage.PhotoStorageManager;
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
public final class PhotoCaptureViewModel_Factory implements Factory<PhotoCaptureViewModel> {
  private final Provider<PhotoStorageManager> photoStorageManagerProvider;

  public PhotoCaptureViewModel_Factory(Provider<PhotoStorageManager> photoStorageManagerProvider) {
    this.photoStorageManagerProvider = photoStorageManagerProvider;
  }

  @Override
  public PhotoCaptureViewModel get() {
    return newInstance(photoStorageManagerProvider.get());
  }

  public static PhotoCaptureViewModel_Factory create(
      Provider<PhotoStorageManager> photoStorageManagerProvider) {
    return new PhotoCaptureViewModel_Factory(photoStorageManagerProvider);
  }

  public static PhotoCaptureViewModel newInstance(PhotoStorageManager photoStorageManager) {
    return new PhotoCaptureViewModel(photoStorageManager);
  }
}
