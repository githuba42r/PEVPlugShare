package com.pevplugshare.ui.edit;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u00ca\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\b\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010%\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0012\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u001f\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u00012\u00020\u0002B\u0005\u00a2\u0006\u0002\u0010\u0003J\u0010\u0010C\u001a\u00020D2\u0006\u0010E\u001a\u000207H\u0002J\b\u0010F\u001a\u00020DH\u0002J\u0010\u0010G\u001a\u00020D2\u0006\u0010H\u001a\u00020IH\u0002J\b\u0010J\u001a\u00020DH\u0002J\n\u0010K\u001a\u0004\u0018\u000107H\u0002J\u0018\u0010L\u001a\u00020D2\u0006\u0010M\u001a\u00020\u000f2\u0006\u0010E\u001a\u000207H\u0002J\u0010\u0010N\u001a\u00020\b2\u0006\u0010O\u001a\u00020\u000bH\u0002J\b\u0010P\u001a\u00020DH\u0002J\u0010\u0010Q\u001a\u00020\u000f2\u0006\u0010R\u001a\u00020\bH\u0002J\b\u0010S\u001a\u00020DH\u0002J\b\u0010T\u001a\u00020DH\u0002J\b\u0010U\u001a\u00020DH\u0002J\b\u0010V\u001a\u00020DH\u0002J\u001a\u0010W\u001a\u00020D2\b\u0010X\u001a\u0004\u0018\u00010/2\u0006\u0010Y\u001a\u00020\u000fH\u0016J\u0012\u0010Z\u001a\u00020D2\b\u0010[\u001a\u0004\u0018\u00010\\H\u0014J\b\u0010]\u001a\u00020DH\u0014J\b\u0010^\u001a\u00020DH\u0014J\u0012\u0010_\u001a\u00020D2\b\u0010`\u001a\u0004\u0018\u00010aH\u0016J\b\u0010b\u001a\u00020DH\u0002J\u0010\u0010c\u001a\u00020D2\u0006\u0010d\u001a\u00020eH\u0002J\u0010\u0010f\u001a\u00020D2\u0006\u0010g\u001a\u00020\rH\u0002J\b\u0010h\u001a\u00020DH\u0002J\b\u0010i\u001a\u00020DH\u0002J\u0010\u0010j\u001a\u00020D2\u0006\u0010M\u001a\u00020\u000fH\u0002J\b\u0010k\u001a\u00020DH\u0002J\u0010\u0010l\u001a\u00020D2\u0006\u0010m\u001a\u00020\u000fH\u0002J\b\u0010n\u001a\u00020DH\u0002J\b\u0010o\u001a\u00020DH\u0002J\b\u0010p\u001a\u00020DH\u0002J\b\u0010q\u001a\u00020DH\u0002J\b\u0010r\u001a\u00020DH\u0002J\u0010\u0010s\u001a\u00020D2\u0006\u0010H\u001a\u00020IH\u0002J\b\u0010t\u001a\u00020DH\u0002J\b\u0010u\u001a\u00020DH\u0002J\b\u0010v\u001a\u00020DH\u0002J\b\u0010w\u001a\u00020DH\u0002J\u0010\u0010x\u001a\u00020D2\u0006\u0010y\u001a\u00020\u0015H\u0002J\b\u0010z\u001a\u00020DH\u0002J\b\u0010{\u001a\u00020DH\u0002J\u0010\u0010|\u001a\u00020D2\u0006\u0010}\u001a\u00020\u0015H\u0002J\u0010\u0010~\u001a\u00020D2\u0006\u0010\u007f\u001a\u00020\u0015H\u0002J\u001b\u0010\u0080\u0001\u001a\u00020D2\u0007\u0010\u0081\u0001\u001a\u0002012\u0007\u0010\u0082\u0001\u001a\u000201H\u0002J\u0013\u0010\u0083\u0001\u001a\u00020D2\b\u0010\u0084\u0001\u001a\u00030\u0085\u0001H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082.\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0006\u001a\u0010\u0012\f\u0012\n \t*\u0004\u0018\u00010\b0\b0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\f\u001a\u0004\u0018\u00010\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082.\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0012\u001a\u0010\u0012\f\u0012\n \t*\u0004\u0018\u00010\u00130\u00130\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0014\u001a\u00020\u00158BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0017\u0010\u0018\u001a\u0004\b\u0014\u0010\u0016R\u001d\u0010\u0019\u001a\u0004\u0018\u00010\b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001c\u0010\u0018\u001a\u0004\b\u001a\u0010\u001bR\u0010\u0010\u001d\u001a\u0004\u0018\u00010\u001eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001f\u001a\u0004\u0018\u00010 X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010!\u001a\u00020\u000fX\u0082D\u00a2\u0006\u0002\n\u0000R\u001e\u0010\"\u001a\u00020#8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b$\u0010%\"\u0004\b&\u0010\'R\u001a\u0010(\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020*0)X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010+\u001a\b\u0012\u0004\u0012\u00020\b0,X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001c\u0010-\u001a\u0010\u0012\f\u0012\n \t*\u0004\u0018\u00010\b0\b0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010.\u001a\u0004\u0018\u00010/X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00100\u001a\u000201X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00102\u001a\u000201X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00103\u001a\u000204X\u0082.\u00a2\u0006\u0002\n\u0000R\u001c\u00105\u001a\u0010\u0012\f\u0012\n \t*\u0004\u0018\u00010\r0\r0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u00106\u001a\u0004\u0018\u000107X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001e\u00108\u001a\u0002098\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b:\u0010;\"\u0004\b<\u0010=R\u001b\u0010>\u001a\u00020?8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\bB\u0010\u0018\u001a\u0004\b@\u0010A\u00a8\u0006\u0086\u0001"}, d2 = {"Lcom/pevplugshare/ui/edit/EditLocationActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "Landroid/hardware/SensorEventListener;", "()V", "binding", "Lcom/pevplugshare/databinding/ActivityEditLocationBinding;", "cameraPermission", "Landroidx/activity/result/ActivityResultLauncher;", "", "kotlin.jvm.PlatformType", "currentAzimuth", "", "currentPhotoUri", "Landroid/net/Uri;", "editingPhotoIndex", "", "fusedLocationClient", "Lcom/google/android/gms/location/FusedLocationProviderClient;", "imageEditor", "Landroid/content/Intent;", "isAddMode", "", "()Z", "isAddMode$delegate", "Lkotlin/Lazy;", "locationId", "getLocationId", "()Ljava/lang/String;", "locationId$delegate", "locationMarker", "Lorg/osmdroid/views/overlay/Marker;", "mapView", "Lorg/osmdroid/views/MapView;", "maxPhotos", "photoAnalyzer", "Lcom/pevplugshare/ai/PhotoAnalyzer;", "getPhotoAnalyzer", "()Lcom/pevplugshare/ai/PhotoAnalyzer;", "setPhotoAnalyzer", "(Lcom/pevplugshare/ai/PhotoAnalyzer;)V", "photoFocusAreas", "", "Landroid/graphics/RectF;", "photoUris", "", "pickImages", "rotationSensor", "Landroid/hardware/Sensor;", "selectedLatitude", "", "selectedLongitude", "sensorManager", "Landroid/hardware/SensorManager;", "takePicture", "tempPhotoFile", "Ljava/io/File;", "tokenManager", "Lcom/pevplugshare/data/remote/TokenManager;", "getTokenManager", "()Lcom/pevplugshare/data/remote/TokenManager;", "setTokenManager", "(Lcom/pevplugshare/data/remote/TokenManager;)V", "viewModel", "Lcom/pevplugshare/ui/edit/EditLocationViewModel;", "getViewModel", "()Lcom/pevplugshare/ui/edit/EditLocationViewModel;", "viewModel$delegate", "addPhotoThumbnail", "", "file", "analyzePhotosWithAI", "applyAnalysisToForm", "analysis", "Lcom/pevplugshare/ai/PhotoAnalysisResult;", "checkCameraPermissionAndLaunch", "createImageFile", "editPhoto", "index", "getCompassDirection", "azimuth", "getLocationAndPopulate", "getSocketTypeIndex", "socketType", "hideMapModal", "initializeUIForAddMode", "launchCamera", "loadExistingPhotos", "onAccuracyChanged", "sensor", "accuracy", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onPause", "onResume", "onSensorChanged", "event", "Landroid/hardware/SensorEvent;", "parseDescriptionToFields", "populateFormFromLocation", "location", "Lcom/pevplugshare/data/model/Location;", "processAndSaveGalleryPhoto", "uri", "processAndSavePhoto", "refreshPhotoThumbnails", "removePhoto", "requestLocationAndPopulate", "selectSocketCount", "count", "setupMapModal", "setupObservers", "setupPhotoSection", "setupUI", "showAiDescriptionDialog", "showAnalysisResults", "showMapModal", "showPhotoSourceDialog", "submitUpdates", "syncToOsmand", "updateAccessTypeIndicator", "isPublic", "updateAddButtonVisibility", "updateAnalyzeButtonVisibility", "updateFavoriteIndicator", "isFavorite", "updateFeeIndicator", "hasFee", "updateMapMarker", "latitude", "longitude", "updateUIFromFormData", "data", "Lcom/pevplugshare/ui/edit/EditLocationFormData;", "app_debug"})
public final class EditLocationActivity extends androidx.appcompat.app.AppCompatActivity implements android.hardware.SensorEventListener {
    private com.pevplugshare.databinding.ActivityEditLocationBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;
    @javax.inject.Inject()
    public com.pevplugshare.data.remote.TokenManager tokenManager;
    @javax.inject.Inject()
    public com.pevplugshare.ai.PhotoAnalyzer photoAnalyzer;
    @org.jetbrains.annotations.Nullable()
    private org.osmdroid.views.MapView mapView;
    @org.jetbrains.annotations.Nullable()
    private org.osmdroid.views.overlay.Marker locationMarker;
    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;
    private android.hardware.SensorManager sensorManager;
    @org.jetbrains.annotations.Nullable()
    private android.hardware.Sensor rotationSensor;
    private float currentAzimuth = 0.0F;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> photoUris = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, android.graphics.RectF> photoFocusAreas = null;
    private final int maxPhotos = 5;
    @org.jetbrains.annotations.Nullable()
    private android.net.Uri currentPhotoUri;
    @org.jetbrains.annotations.Nullable()
    private java.io.File tempPhotoFile;
    private int editingPhotoIndex = -1;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<android.net.Uri> takePicture = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String> pickImages = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String> cameraPermission = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> imageEditor = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy locationId$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy isAddMode$delegate = null;
    
    public EditLocationActivity() {
        super();
    }
    
    private final com.pevplugshare.ui.edit.EditLocationViewModel getViewModel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.remote.TokenManager getTokenManager() {
        return null;
    }
    
    public final void setTokenManager(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.TokenManager p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.ai.PhotoAnalyzer getPhotoAnalyzer() {
        return null;
    }
    
    public final void setPhotoAnalyzer(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.ai.PhotoAnalyzer p0) {
    }
    
    private final java.lang.String getLocationId() {
        return null;
    }
    
    private final boolean isAddMode() {
        return false;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void requestLocationAndPopulate() {
    }
    
    private final void getLocationAndPopulate() {
    }
    
    private final java.lang.String getCompassDirection(float azimuth) {
        return null;
    }
    
    private final void setupUI() {
    }
    
    private final void setupMapModal() {
    }
    
    private final void showMapModal() {
    }
    
    private final void hideMapModal() {
    }
    
    private final void updateMapMarker(double latitude, double longitude) {
    }
    
    private final void setupObservers() {
    }
    
    private final void populateFormFromLocation(com.pevplugshare.data.model.Location location) {
    }
    
    private final void initializeUIForAddMode() {
    }
    
    private final void updateFavoriteIndicator(boolean isFavorite) {
    }
    
    private final void updateFeeIndicator(boolean hasFee) {
    }
    
    private final void updateAccessTypeIndicator(boolean isPublic) {
    }
    
    private final void selectSocketCount(int count) {
    }
    
    private final void updateUIFromFormData(com.pevplugshare.ui.edit.EditLocationFormData data) {
    }
    
    private final void submitUpdates() {
    }
    
    private final void syncToOsmand() {
    }
    
    private final void showAiDescriptionDialog() {
    }
    
    private final void parseDescriptionToFields() {
    }
    
    private final int getSocketTypeIndex(java.lang.String socketType) {
        return 0;
    }
    
    private final void setupPhotoSection() {
    }
    
    private final void updateAnalyzeButtonVisibility() {
    }
    
    private final void showPhotoSourceDialog() {
    }
    
    private final void checkCameraPermissionAndLaunch() {
    }
    
    private final void launchCamera() {
    }
    
    private final java.io.File createImageFile() {
        return null;
    }
    
    private final void processAndSavePhoto() {
    }
    
    private final void processAndSaveGalleryPhoto(android.net.Uri uri) {
    }
    
    private final void addPhotoThumbnail(java.io.File file) {
    }
    
    private final void removePhoto(int index) {
    }
    
    private final void editPhoto(int index, java.io.File file) {
    }
    
    private final void refreshPhotoThumbnails() {
    }
    
    private final void updateAddButtonVisibility() {
    }
    
    private final void loadExistingPhotos() {
    }
    
    private final void analyzePhotosWithAI() {
    }
    
    private final void showAnalysisResults(com.pevplugshare.ai.PhotoAnalysisResult analysis) {
    }
    
    private final void applyAnalysisToForm(com.pevplugshare.ai.PhotoAnalysisResult analysis) {
    }
    
    @java.lang.Override()
    protected void onResume() {
    }
    
    @java.lang.Override()
    protected void onPause() {
    }
    
    @java.lang.Override()
    public void onSensorChanged(@org.jetbrains.annotations.Nullable()
    android.hardware.SensorEvent event) {
    }
    
    @java.lang.Override()
    public void onAccuracyChanged(@org.jetbrains.annotations.Nullable()
    android.hardware.Sensor sensor, int accuracy) {
    }
}