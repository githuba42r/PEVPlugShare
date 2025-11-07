package com.pevplugshare.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.pevplugshare.data.repository.SyncManager;
import dagger.internal.DaggerGenerated;
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
public final class AutoSyncWorker_Factory {
  private final Provider<SyncManager> syncManagerProvider;

  public AutoSyncWorker_Factory(Provider<SyncManager> syncManagerProvider) {
    this.syncManagerProvider = syncManagerProvider;
  }

  public AutoSyncWorker get(Context appContext, WorkerParameters params) {
    return newInstance(appContext, params, syncManagerProvider.get());
  }

  public static AutoSyncWorker_Factory create(Provider<SyncManager> syncManagerProvider) {
    return new AutoSyncWorker_Factory(syncManagerProvider);
  }

  public static AutoSyncWorker newInstance(Context appContext, WorkerParameters params,
      SyncManager syncManager) {
    return new AutoSyncWorker(appContext, params, syncManager);
  }
}
