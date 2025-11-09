package com.pevplugshare;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.pevplugshare.ai.LocationDescriptionGenerator;
import com.pevplugshare.ai.LocationDescriptionParser;
import com.pevplugshare.ai.PhotoAnalyzer;
import com.pevplugshare.data.db.PEVPlugShareDatabase;
import com.pevplugshare.data.db.dao.LocationDao;
import com.pevplugshare.data.db.dao.PendingOperationDao;
import com.pevplugshare.data.network.ConnectivityObserver;
import com.pevplugshare.data.remote.CookieManager;
import com.pevplugshare.data.remote.OsmandApiClient;
import com.pevplugshare.data.remote.TokenManager;
import com.pevplugshare.data.repository.LocalLocationRepository;
import com.pevplugshare.data.repository.OsmandLocationSyncService;
import com.pevplugshare.data.repository.PendingOperationRepository;
import com.pevplugshare.data.repository.SyncManager;
import com.pevplugshare.data.storage.PhotoStorageManager;
import com.pevplugshare.di.DatabaseModule_ProvideDatabaseFactory;
import com.pevplugshare.di.DatabaseModule_ProvideLocationDaoFactory;
import com.pevplugshare.di.DatabaseModule_ProvidePendingOperationDaoFactory;
import com.pevplugshare.di.NetworkModule_ProvideAuthRepositoryFactory;
import com.pevplugshare.di.NetworkModule_ProvideConnectivityObserverFactory;
import com.pevplugshare.di.NetworkModule_ProvideCookieManagerFactory;
import com.pevplugshare.di.NetworkModule_ProvideLocationRepositoryFactory;
import com.pevplugshare.di.NetworkModule_ProvideOsmandApiClientFactory;
import com.pevplugshare.di.NetworkModule_ProvidePhotoStorageManagerFactory;
import com.pevplugshare.di.NetworkModule_ProvideTokenManagerFactory;
import com.pevplugshare.di.RepositoryModule_ProvideLocalLocationRepositoryFactory;
import com.pevplugshare.di.RepositoryModule_ProvidePendingOperationRepositoryFactory;
import com.pevplugshare.domain.repository.AuthRepository;
import com.pevplugshare.domain.repository.LocationRepository;
import com.pevplugshare.ui.add.AddLocationActivity;
import com.pevplugshare.ui.add.AddLocationViewModel;
import com.pevplugshare.ui.add.AddLocationViewModel_HiltModules_KeyModule_ProvideFactory;
import com.pevplugshare.ui.auth.LoginActivity;
import com.pevplugshare.ui.auth.LoginViewModel;
import com.pevplugshare.ui.auth.LoginViewModel_HiltModules_KeyModule_ProvideFactory;
import com.pevplugshare.ui.auth.SplashActivity;
import com.pevplugshare.ui.auth.SplashViewModel;
import com.pevplugshare.ui.auth.SplashViewModel_HiltModules_KeyModule_ProvideFactory;
import com.pevplugshare.ui.edit.AiDescriptionDialogFragment;
import com.pevplugshare.ui.edit.AiDescriptionDialogFragment_MembersInjector;
import com.pevplugshare.ui.edit.EditLocationActivity;
import com.pevplugshare.ui.edit.EditLocationActivity_MembersInjector;
import com.pevplugshare.ui.edit.EditLocationViewModel;
import com.pevplugshare.ui.edit.EditLocationViewModel_HiltModules_KeyModule_ProvideFactory;
import com.pevplugshare.ui.main.MainActivity;
import com.pevplugshare.ui.main.MainViewModel;
import com.pevplugshare.ui.main.MainViewModel_HiltModules_KeyModule_ProvideFactory;
import com.pevplugshare.ui.photo.PhotoCaptureActivity;
import com.pevplugshare.ui.photo.PhotoCaptureViewModel;
import com.pevplugshare.ui.photo.PhotoCaptureViewModel_HiltModules_KeyModule_ProvideFactory;
import com.pevplugshare.ui.quicklocation.LocationPickerMapActivity;
import com.pevplugshare.ui.quicklocation.PhotoGalleryEditorActivity;
import com.pevplugshare.ui.quicklocation.QuickLocationActivity;
import com.pevplugshare.ui.quicklocation.QuickLocationActivity_MembersInjector;
import com.pevplugshare.ui.settings.SettingsActivity;
import com.pevplugshare.ui.settings.SettingsActivity_MembersInjector;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SetBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerPEVPlugShareApplication_HiltComponents_SingletonC {
  private DaggerPEVPlugShareApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public PEVPlugShareApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements PEVPlugShareApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public PEVPlugShareApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements PEVPlugShareApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public PEVPlugShareApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements PEVPlugShareApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public PEVPlugShareApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements PEVPlugShareApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public PEVPlugShareApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements PEVPlugShareApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public PEVPlugShareApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements PEVPlugShareApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public PEVPlugShareApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements PEVPlugShareApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public PEVPlugShareApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends PEVPlugShareApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends PEVPlugShareApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public void injectAiDescriptionDialogFragment(
        AiDescriptionDialogFragment aiDescriptionDialogFragment) {
      injectAiDescriptionDialogFragment2(aiDescriptionDialogFragment);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }

    @CanIgnoreReturnValue
    private AiDescriptionDialogFragment injectAiDescriptionDialogFragment2(
        AiDescriptionDialogFragment instance) {
      AiDescriptionDialogFragment_MembersInjector.injectDescriptionGenerator(instance, singletonCImpl.locationDescriptionGeneratorProvider.get());
      return instance;
    }
  }

  private static final class ViewCImpl extends PEVPlugShareApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends PEVPlugShareApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectAddLocationActivity(AddLocationActivity addLocationActivity) {
    }

    @Override
    public void injectLoginActivity(LoginActivity loginActivity) {
    }

    @Override
    public void injectSplashActivity(SplashActivity splashActivity) {
    }

    @Override
    public void injectEditLocationActivity(EditLocationActivity editLocationActivity) {
      injectEditLocationActivity2(editLocationActivity);
    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public void injectPhotoCaptureActivity(PhotoCaptureActivity photoCaptureActivity) {
    }

    @Override
    public void injectLocationPickerMapActivity(
        LocationPickerMapActivity locationPickerMapActivity) {
    }

    @Override
    public void injectPhotoGalleryEditorActivity(
        PhotoGalleryEditorActivity photoGalleryEditorActivity) {
    }

    @Override
    public void injectQuickLocationActivity(QuickLocationActivity quickLocationActivity) {
      injectQuickLocationActivity2(quickLocationActivity);
    }

    @Override
    public void injectSettingsActivity(SettingsActivity settingsActivity) {
      injectSettingsActivity2(settingsActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Set<String> getViewModelKeys() {
      return SetBuilder.<String>newSetBuilder(6).add(AddLocationViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(EditLocationViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(LoginViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(MainViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(PhotoCaptureViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(SplashViewModel_HiltModules_KeyModule_ProvideFactory.provide()).build();
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @CanIgnoreReturnValue
    private EditLocationActivity injectEditLocationActivity2(EditLocationActivity instance) {
      EditLocationActivity_MembersInjector.injectTokenManager(instance, singletonCImpl.provideTokenManagerProvider.get());
      EditLocationActivity_MembersInjector.injectPhotoAnalyzer(instance, singletonCImpl.photoAnalyzerProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private QuickLocationActivity injectQuickLocationActivity2(QuickLocationActivity instance) {
      QuickLocationActivity_MembersInjector.injectTokenManager(instance, singletonCImpl.provideTokenManagerProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private SettingsActivity injectSettingsActivity2(SettingsActivity instance) {
      SettingsActivity_MembersInjector.injectTokenManager(instance, singletonCImpl.provideTokenManagerProvider.get());
      return instance;
    }
  }

  private static final class ViewModelCImpl extends PEVPlugShareApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AddLocationViewModel> addLocationViewModelProvider;

    private Provider<EditLocationViewModel> editLocationViewModelProvider;

    private Provider<LoginViewModel> loginViewModelProvider;

    private Provider<MainViewModel> mainViewModelProvider;

    private Provider<PhotoCaptureViewModel> photoCaptureViewModelProvider;

    private Provider<SplashViewModel> splashViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.addLocationViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.editLocationViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.loginViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.mainViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.photoCaptureViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.splashViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
    }

    @Override
    public Map<String, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(6).put("com.pevplugshare.ui.add.AddLocationViewModel", ((Provider) addLocationViewModelProvider)).put("com.pevplugshare.ui.edit.EditLocationViewModel", ((Provider) editLocationViewModelProvider)).put("com.pevplugshare.ui.auth.LoginViewModel", ((Provider) loginViewModelProvider)).put("com.pevplugshare.ui.main.MainViewModel", ((Provider) mainViewModelProvider)).put("com.pevplugshare.ui.photo.PhotoCaptureViewModel", ((Provider) photoCaptureViewModelProvider)).put("com.pevplugshare.ui.auth.SplashViewModel", ((Provider) splashViewModelProvider)).build();
    }

    @Override
    public Map<String, Object> getHiltViewModelAssistedMap() {
      return Collections.<String, Object>emptyMap();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.pevplugshare.ui.add.AddLocationViewModel 
          return (T) new AddLocationViewModel(singletonCImpl.provideLocationRepositoryProvider.get());

          case 1: // com.pevplugshare.ui.edit.EditLocationViewModel 
          return (T) new EditLocationViewModel(singletonCImpl.provideLocationRepositoryProvider.get(), singletonCImpl.osmandLocationSyncServiceProvider.get(), singletonCImpl.provideTokenManagerProvider.get(), singletonCImpl.locationDescriptionParserProvider.get());

          case 2: // com.pevplugshare.ui.auth.LoginViewModel 
          return (T) new LoginViewModel(singletonCImpl.provideAuthRepositoryProvider.get());

          case 3: // com.pevplugshare.ui.main.MainViewModel 
          return (T) new MainViewModel(singletonCImpl.provideLocationRepositoryProvider.get(), singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideConnectivityObserverProvider.get(), singletonCImpl.syncManagerProvider.get(), singletonCImpl.provideTokenManagerProvider.get());

          case 4: // com.pevplugshare.ui.photo.PhotoCaptureViewModel 
          return (T) new PhotoCaptureViewModel(singletonCImpl.providePhotoStorageManagerProvider.get());

          case 5: // com.pevplugshare.ui.auth.SplashViewModel 
          return (T) new SplashViewModel(singletonCImpl.provideAuthRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends PEVPlugShareApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends PEVPlugShareApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends PEVPlugShareApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<TokenManager> provideTokenManagerProvider;

    private Provider<PhotoAnalyzer> photoAnalyzerProvider;

    private Provider<LocationDescriptionGenerator> locationDescriptionGeneratorProvider;

    private Provider<PEVPlugShareDatabase> provideDatabaseProvider;

    private Provider<LocationDao> provideLocationDaoProvider;

    private Provider<LocationRepository> provideLocationRepositoryProvider;

    private Provider<CookieManager> provideCookieManagerProvider;

    private Provider<OsmandApiClient> provideOsmandApiClientProvider;

    private Provider<LocalLocationRepository> provideLocalLocationRepositoryProvider;

    private Provider<OsmandLocationSyncService> osmandLocationSyncServiceProvider;

    private Provider<LocationDescriptionParser> locationDescriptionParserProvider;

    private Provider<AuthRepository> provideAuthRepositoryProvider;

    private Provider<ConnectivityObserver> provideConnectivityObserverProvider;

    private Provider<PendingOperationDao> providePendingOperationDaoProvider;

    private Provider<PendingOperationRepository> providePendingOperationRepositoryProvider;

    private Provider<SyncManager> syncManagerProvider;

    private Provider<PhotoStorageManager> providePhotoStorageManagerProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideTokenManagerProvider = DoubleCheck.provider(new SwitchingProvider<TokenManager>(singletonCImpl, 0));
      this.photoAnalyzerProvider = DoubleCheck.provider(new SwitchingProvider<PhotoAnalyzer>(singletonCImpl, 1));
      this.locationDescriptionGeneratorProvider = DoubleCheck.provider(new SwitchingProvider<LocationDescriptionGenerator>(singletonCImpl, 2));
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<PEVPlugShareDatabase>(singletonCImpl, 5));
      this.provideLocationDaoProvider = DoubleCheck.provider(new SwitchingProvider<LocationDao>(singletonCImpl, 4));
      this.provideLocationRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<LocationRepository>(singletonCImpl, 3));
      this.provideCookieManagerProvider = DoubleCheck.provider(new SwitchingProvider<CookieManager>(singletonCImpl, 8));
      this.provideOsmandApiClientProvider = DoubleCheck.provider(new SwitchingProvider<OsmandApiClient>(singletonCImpl, 7));
      this.provideLocalLocationRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<LocalLocationRepository>(singletonCImpl, 9));
      this.osmandLocationSyncServiceProvider = DoubleCheck.provider(new SwitchingProvider<OsmandLocationSyncService>(singletonCImpl, 6));
      this.locationDescriptionParserProvider = DoubleCheck.provider(new SwitchingProvider<LocationDescriptionParser>(singletonCImpl, 10));
      this.provideAuthRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<AuthRepository>(singletonCImpl, 11));
      this.provideConnectivityObserverProvider = DoubleCheck.provider(new SwitchingProvider<ConnectivityObserver>(singletonCImpl, 12));
      this.providePendingOperationDaoProvider = DoubleCheck.provider(new SwitchingProvider<PendingOperationDao>(singletonCImpl, 15));
      this.providePendingOperationRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<PendingOperationRepository>(singletonCImpl, 14));
      this.syncManagerProvider = DoubleCheck.provider(new SwitchingProvider<SyncManager>(singletonCImpl, 13));
      this.providePhotoStorageManagerProvider = DoubleCheck.provider(new SwitchingProvider<PhotoStorageManager>(singletonCImpl, 16));
    }

    @Override
    public void injectPEVPlugShareApplication(PEVPlugShareApplication pEVPlugShareApplication) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.pevplugshare.data.remote.TokenManager 
          return (T) NetworkModule_ProvideTokenManagerFactory.provideTokenManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // com.pevplugshare.ai.PhotoAnalyzer 
          return (T) new PhotoAnalyzer(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideTokenManagerProvider.get());

          case 2: // com.pevplugshare.ai.LocationDescriptionGenerator 
          return (T) new LocationDescriptionGenerator(singletonCImpl.provideTokenManagerProvider.get());

          case 3: // com.pevplugshare.domain.repository.LocationRepository 
          return (T) NetworkModule_ProvideLocationRepositoryFactory.provideLocationRepository(singletonCImpl.provideLocationDaoProvider.get());

          case 4: // com.pevplugshare.data.db.dao.LocationDao 
          return (T) DatabaseModule_ProvideLocationDaoFactory.provideLocationDao(singletonCImpl.provideDatabaseProvider.get());

          case 5: // com.pevplugshare.data.db.PEVPlugShareDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 6: // com.pevplugshare.data.repository.OsmandLocationSyncService 
          return (T) new OsmandLocationSyncService(singletonCImpl.provideOsmandApiClientProvider.get(), singletonCImpl.provideTokenManagerProvider.get(), singletonCImpl.provideLocalLocationRepositoryProvider.get());

          case 7: // com.pevplugshare.data.remote.OsmandApiClient 
          return (T) NetworkModule_ProvideOsmandApiClientFactory.provideOsmandApiClient(singletonCImpl.provideTokenManagerProvider.get(), singletonCImpl.provideCookieManagerProvider.get());

          case 8: // com.pevplugshare.data.remote.CookieManager 
          return (T) NetworkModule_ProvideCookieManagerFactory.provideCookieManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 9: // com.pevplugshare.data.repository.LocalLocationRepository 
          return (T) RepositoryModule_ProvideLocalLocationRepositoryFactory.provideLocalLocationRepository(singletonCImpl.provideLocationDaoProvider.get());

          case 10: // com.pevplugshare.ai.LocationDescriptionParser 
          return (T) new LocationDescriptionParser(singletonCImpl.provideTokenManagerProvider.get());

          case 11: // com.pevplugshare.domain.repository.AuthRepository 
          return (T) NetworkModule_ProvideAuthRepositoryFactory.provideAuthRepository(singletonCImpl.provideOsmandApiClientProvider.get(), singletonCImpl.provideTokenManagerProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideCookieManagerProvider.get());

          case 12: // com.pevplugshare.data.network.ConnectivityObserver 
          return (T) NetworkModule_ProvideConnectivityObserverFactory.provideConnectivityObserver(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 13: // com.pevplugshare.data.repository.SyncManager 
          return (T) new SyncManager(singletonCImpl.providePendingOperationRepositoryProvider.get(), singletonCImpl.provideLocationRepositoryProvider.get());

          case 14: // com.pevplugshare.data.repository.PendingOperationRepository 
          return (T) RepositoryModule_ProvidePendingOperationRepositoryFactory.providePendingOperationRepository(singletonCImpl.providePendingOperationDaoProvider.get());

          case 15: // com.pevplugshare.data.db.dao.PendingOperationDao 
          return (T) DatabaseModule_ProvidePendingOperationDaoFactory.providePendingOperationDao(singletonCImpl.provideDatabaseProvider.get());

          case 16: // com.pevplugshare.data.storage.PhotoStorageManager 
          return (T) NetworkModule_ProvidePhotoStorageManagerFactory.providePhotoStorageManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
