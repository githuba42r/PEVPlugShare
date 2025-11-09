package com.pevplugshare.data.remote;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class SessionRefreshWorker_AssistedFactory_Impl implements SessionRefreshWorker_AssistedFactory {
  private final SessionRefreshWorker_Factory delegateFactory;

  SessionRefreshWorker_AssistedFactory_Impl(SessionRefreshWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public SessionRefreshWorker create(Context arg0, WorkerParameters arg1) {
    return delegateFactory.get(arg0, arg1);
  }

  public static Provider<SessionRefreshWorker_AssistedFactory> create(
      SessionRefreshWorker_Factory delegateFactory) {
    return InstanceFactory.create(new SessionRefreshWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<SessionRefreshWorker_AssistedFactory> createFactoryProvider(
      SessionRefreshWorker_Factory delegateFactory) {
    return InstanceFactory.create(new SessionRefreshWorker_AssistedFactory_Impl(delegateFactory));
  }
}
