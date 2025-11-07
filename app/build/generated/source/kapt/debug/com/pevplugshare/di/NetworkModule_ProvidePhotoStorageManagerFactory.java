package com.pevplugshare.di;

import android.content.Context;
import com.pevplugshare.data.storage.PhotoStorageManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class NetworkModule_ProvidePhotoStorageManagerFactory implements Factory<PhotoStorageManager> {
  private final Provider<Context> contextProvider;

  public NetworkModule_ProvidePhotoStorageManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public PhotoStorageManager get() {
    return providePhotoStorageManager(contextProvider.get());
  }

  public static NetworkModule_ProvidePhotoStorageManagerFactory create(
      Provider<Context> contextProvider) {
    return new NetworkModule_ProvidePhotoStorageManagerFactory(contextProvider);
  }

  public static PhotoStorageManager providePhotoStorageManager(Context context) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.providePhotoStorageManager(context));
  }
}
