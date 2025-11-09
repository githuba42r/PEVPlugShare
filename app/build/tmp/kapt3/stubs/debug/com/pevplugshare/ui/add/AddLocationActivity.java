package com.pevplugshare.ui.add;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000N\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0011\u001a\u00020\u0012H\u0002J\b\u0010\u0013\u001a\u00020\u0012H\u0002J\u0012\u0010\u0014\u001a\u00020\u00122\b\u0010\u0015\u001a\u0004\u0018\u00010\u0016H\u0014J\b\u0010\u0017\u001a\u00020\u0012H\u0002J\b\u0010\u0018\u001a\u00020\u0012H\u0002J\b\u0010\u0019\u001a\u00020\u0012H\u0002J\"\u0010\u001a\u001a\u00020\u00122\u0018\u0010\u001b\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u001e\u0012\u0004\u0012\u00020\u001f0\u001d0\u001cH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0007\u001a\u0010\u0012\f\u0012\n \n*\u0004\u0018\u00010\t0\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u000b\u001a\u00020\f8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000f\u0010\u0010\u001a\u0004\b\r\u0010\u000e\u00a8\u0006 "}, d2 = {"Lcom/pevplugshare/ui/add/AddLocationActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/pevplugshare/databinding/ActivityAddLocationBinding;", "fusedLocationClient", "Lcom/google/android/gms/location/FusedLocationProviderClient;", "locationPermissionLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "", "kotlin.jvm.PlatformType", "viewModel", "Lcom/pevplugshare/ui/add/AddLocationViewModel;", "getViewModel", "()Lcom/pevplugshare/ui/add/AddLocationViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "collectFormData", "", "getLocationAndPopulate", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "requestLocationAndPopulate", "setupObservers", "setupUI", "showNearbyLocationsDialog", "nearbyLocations", "", "Lkotlin/Pair;", "Lcom/pevplugshare/data/model/Location;", "", "app_debug"})
public final class AddLocationActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.pevplugshare.databinding.ActivityAddLocationBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String> locationPermissionLauncher = null;
    
    public AddLocationActivity() {
        super();
    }
    
    private final com.pevplugshare.ui.add.AddLocationViewModel getViewModel() {
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
    
    private final void showNearbyLocationsDialog(java.util.List<kotlin.Pair<com.pevplugshare.data.model.Location, java.lang.Double>> nearbyLocations) {
    }
    
    private final void requestLocationAndPopulate() {
    }
    
    private final void getLocationAndPopulate() {
    }
    
    private final void collectFormData() {
    }
}