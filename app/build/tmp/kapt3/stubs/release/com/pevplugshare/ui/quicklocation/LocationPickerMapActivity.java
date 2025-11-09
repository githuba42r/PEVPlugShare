package com.pevplugshare.ui.quicklocation;

/**
 * Map-based location picker for quick location workflow using OSMDroid
 */
@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000e\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u001cH\u0002J\u0012\u0010\u001d\u001a\u00020\u001a2\b\u0010\u001e\u001a\u0004\u0018\u00010\u001fH\u0014J\b\u0010 \u001a\u00020\u001aH\u0014J\b\u0010!\u001a\u00020\u001aH\u0014J\b\u0010\"\u001a\u00020\u001aH\u0014J\u0010\u0010#\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u001cH\u0002J\u0010\u0010$\u001a\u00020\u001a2\u0006\u0010%\u001a\u00020&H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u0016\u0010\r\u001a\n\u0012\u0004\u0012\u00020\u000f\u0018\u00010\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0012\u0010\u0012\u001a\u0004\u0018\u00010\u0013X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u0014R\u0012\u0010\u0015\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u0017R\u0012\u0010\u0018\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u0017\u00a8\u0006\'"}, d2 = {"Lcom/pevplugshare/ui/quicklocation/LocationPickerMapActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/pevplugshare/databinding/ActivityLocationPickerMapBinding;", "geocoder", "Landroid/location/Geocoder;", "job", "Lkotlinx/coroutines/CompletableJob;", "mapController", "Lorg/osmdroid/api/IMapController;", "mapView", "Lorg/osmdroid/views/MapView;", "markerOverlay", "Lorg/osmdroid/views/overlay/ItemizedIconOverlay;", "Lorg/osmdroid/views/overlay/OverlayItem;", "scope", "Lkotlinx/coroutines/CoroutineScope;", "selectedBearing", "", "Ljava/lang/Float;", "selectedLatitude", "", "Ljava/lang/Double;", "selectedLongitude", "addMarker", "", "geoPoint", "Lorg/osmdroid/util/GeoPoint;", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "onPause", "onResume", "reverseGeocode", "searchLocation", "query", "", "app_release"})
public final class LocationPickerMapActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.pevplugshare.databinding.ActivityLocationPickerMapBinding binding;
    private org.osmdroid.views.MapView mapView;
    private org.osmdroid.api.IMapController mapController;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Double selectedLatitude;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Double selectedLongitude;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Float selectedBearing;
    private android.location.Geocoder geocoder;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CompletableJob job = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.Nullable()
    private org.osmdroid.views.overlay.ItemizedIconOverlay<org.osmdroid.views.overlay.OverlayItem> markerOverlay;
    
    public LocationPickerMapActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void addMarker(org.osmdroid.util.GeoPoint geoPoint) {
    }
    
    private final void searchLocation(java.lang.String query) {
    }
    
    private final void reverseGeocode(org.osmdroid.util.GeoPoint geoPoint) {
    }
    
    @java.lang.Override()
    protected void onResume() {
    }
    
    @java.lang.Override()
    protected void onPause() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}