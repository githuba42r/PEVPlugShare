package com.pevplugshare.ui.main;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0006\u0010\u001e\u001a\u00020\u001fJ\u000e\u0010 \u001a\u00020\u001f2\u0006\u0010!\u001a\u00020\u000fJ\u0006\u0010\"\u001a\u00020\u001fJ\u000e\u0010#\u001a\u00020\u001f2\u0006\u0010!\u001a\u00020\u000fJ\u001c\u0010$\u001a\b\u0012\u0004\u0012\u00020&0%2\f\u0010\'\u001a\b\u0012\u0004\u0012\u00020&0%H\u0002J\u0006\u0010(\u001a\u00020\u001fJ\u000e\u0010)\u001a\u00020\u001f2\u0006\u0010*\u001a\u00020&J\u0016\u0010+\u001a\u00020\u001f2\u0006\u0010,\u001a\u00020\u00122\u0006\u0010-\u001a\u00020\u0012R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u000b0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u000e\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\"\u0010\u0010\u001a\u0016\u0012\u0012\u0012\u0010\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u0012\u0018\u00010\u00110\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\t0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0017\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0016R\u0017\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\r0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u001a\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0016R%\u0010\u001c\u001a\u0016\u0012\u0012\u0012\u0010\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u0012\u0018\u00010\u00110\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0016\u00a8\u0006."}, d2 = {"Lcom/pevplugshare/ui/main/MainViewModel;", "Landroidx/lifecycle/ViewModel;", "locationRepository", "Lcom/pevplugshare/domain/repository/LocationRepository;", "authRepository", "Lcom/pevplugshare/domain/repository/AuthRepository;", "(Lcom/pevplugshare/domain/repository/LocationRepository;Lcom/pevplugshare/domain/repository/AuthRepository;)V", "_categoryState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/pevplugshare/ui/main/CategoryState;", "_isOffline", "", "_locationListState", "Lcom/pevplugshare/ui/main/LocationListState;", "_selectedCategory", "", "_userLocation", "Lkotlin/Pair;", "", "categoryState", "Lkotlinx/coroutines/flow/StateFlow;", "getCategoryState", "()Lkotlinx/coroutines/flow/StateFlow;", "isOffline", "locationListState", "getLocationListState", "selectedCategory", "getSelectedCategory", "userLocation", "getUserLocation", "loadCategories", "", "loadLocations", "categoryName", "logout", "selectCategory", "sortLocationsByDistance", "", "Lcom/pevplugshare/data/model/Location;", "locations", "syncLocations", "toggleFavourite", "location", "updateUserLocation", "latitude", "longitude", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class MainViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.domain.repository.LocationRepository locationRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.domain.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pevplugshare.ui.main.LocationListState> _locationListState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.main.LocationListState> locationListState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pevplugshare.ui.main.CategoryState> _categoryState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.main.CategoryState> categoryState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _selectedCategory = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> selectedCategory = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<kotlin.Pair<java.lang.Double, java.lang.Double>> _userLocation = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<kotlin.Pair<java.lang.Double, java.lang.Double>> userLocation = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isOffline = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isOffline = null;
    
    @javax.inject.Inject()
    public MainViewModel(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.domain.repository.LocationRepository locationRepository, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.domain.repository.AuthRepository authRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.main.LocationListState> getLocationListState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.main.CategoryState> getCategoryState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getSelectedCategory() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<kotlin.Pair<java.lang.Double, java.lang.Double>> getUserLocation() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isOffline() {
        return null;
    }
    
    public final void loadCategories() {
    }
    
    public final void selectCategory(@org.jetbrains.annotations.NotNull()
    java.lang.String categoryName) {
    }
    
    public final void loadLocations(@org.jetbrains.annotations.NotNull()
    java.lang.String categoryName) {
    }
    
    public final void updateUserLocation(double latitude, double longitude) {
    }
    
    public final void toggleFavourite(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.model.Location location) {
    }
    
    public final void syncLocations() {
    }
    
    public final void logout() {
    }
    
    private final java.util.List<com.pevplugshare.data.model.Location> sortLocationsByDistance(java.util.List<com.pevplugshare.data.model.Location> locations) {
        return null;
    }
}