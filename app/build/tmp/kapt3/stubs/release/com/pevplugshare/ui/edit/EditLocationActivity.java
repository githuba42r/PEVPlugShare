package com.pevplugshare.ui.edit;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0015\u001a\u00020\u0016H\u0002J\u0010\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u0007H\u0002J\u0012\u0010\u001a\u001a\u00020\u00162\b\u0010\u001b\u001a\u0004\u0018\u00010\u001cH\u0014J\u0010\u0010\u001d\u001a\u00020\u00162\u0006\u0010\u001e\u001a\u00020\u001fH\u0002J\b\u0010 \u001a\u00020\u0016H\u0002J\b\u0010!\u001a\u00020\u0016H\u0002J\b\u0010\"\u001a\u00020\u0016H\u0002J\b\u0010#\u001a\u00020\u0016H\u0002J\u0010\u0010$\u001a\u00020\u00162\u0006\u0010%\u001a\u00020&H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0005\u001a\u0010\u0012\f\u0012\n \b*\u0004\u0018\u00010\u00070\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u000b\u001a\u00020\u00078BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\u000f\u001a\u0004\b\f\u0010\rR\u001b\u0010\u0010\u001a\u00020\u00118BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0014\u0010\u000f\u001a\u0004\b\u0012\u0010\u0013\u00a8\u0006\'"}, d2 = {"Lcom/pevplugshare/ui/edit/EditLocationActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/pevplugshare/databinding/ActivityEditLocationBinding;", "fineLocationLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "", "kotlin.jvm.PlatformType", "fusedLocationClient", "Lcom/google/android/gms/location/FusedLocationProviderClient;", "locationId", "getLocationId", "()Ljava/lang/String;", "locationId$delegate", "Lkotlin/Lazy;", "viewModel", "Lcom/pevplugshare/ui/edit/EditLocationViewModel;", "getViewModel", "()Lcom/pevplugshare/ui/edit/EditLocationViewModel;", "viewModel$delegate", "getCurrentLocation", "", "getSocketTypeIndex", "", "socketType", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "populateFormFromLocation", "location", "Lcom/pevplugshare/data/model/Location;", "requestLocationPermission", "setupObservers", "setupUI", "submitUpdates", "updateUIFromFormData", "data", "Lcom/pevplugshare/ui/edit/EditLocationFormData;", "app_release"})
public final class EditLocationActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.pevplugshare.databinding.ActivityEditLocationBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy locationId$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String> fineLocationLauncher = null;
    
    public EditLocationActivity() {
        super();
    }
    
    private final com.pevplugshare.ui.edit.EditLocationViewModel getViewModel() {
        return null;
    }
    
    private final java.lang.String getLocationId() {
        return null;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupUI() {
    }
    
    private final void setupObservers() {
    }
    
    private final void populateFormFromLocation(com.pevplugshare.data.model.Location location) {
    }
    
    private final void updateUIFromFormData(com.pevplugshare.ui.edit.EditLocationFormData data) {
    }
    
    private final void requestLocationPermission() {
    }
    
    private final void getCurrentLocation() {
    }
    
    private final void submitUpdates() {
    }
    
    private final int getSocketTypeIndex(java.lang.String socketType) {
        return 0;
    }
}