package com.pevplugshare.ui.add;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000d\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0010\u0006\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\t\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\u0010\u001a\u00020\u0011J\u0010\u0010\u0012\u001a\u00020\u00112\u0006\u0010\u0013\u001a\u00020\tH\u0002J\u0006\u0010\u0014\u001a\u00020\u0011J\u0016\u0010\u0015\u001a\u00020\u00112\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0017J\u0006\u0010\u0019\u001a\u00020\u0011J\u000e\u0010\u001a\u001a\u00020\u00112\u0006\u0010\u001b\u001a\u00020\u001cJ\u000e\u0010\u001d\u001a\u00020\u00112\u0006\u0010\u001e\u001a\u00020\u001fJ\u000e\u0010 \u001a\u00020\u00112\u0006\u0010!\u001a\u00020\u001fJ\u000e\u0010\"\u001a\u00020\u00112\u0006\u0010#\u001a\u00020\u001fJ\u000e\u0010$\u001a\u00020\u00112\u0006\u0010\u0013\u001a\u00020\tJ\u000e\u0010%\u001a\u00020\u00112\u0006\u0010&\u001a\u00020\'J\u000e\u0010(\u001a\u00020\u00112\u0006\u0010)\u001a\u00020*J\u000e\u0010+\u001a\u00020\u00112\u0006\u0010,\u001a\u00020\u001fJ\u000e\u0010-\u001a\u00020\u00112\u0006\u0010.\u001a\u00020\u001fJ\u000e\u0010/\u001a\u00020\u00112\u0006\u0010,\u001a\u00020\u001fJ\u000e\u00100\u001a\u00020\u00112\u0006\u00101\u001a\u00020\u001fJ\u0014\u00102\u001a\u00020\u00112\f\u00103\u001a\b\u0012\u0004\u0012\u00020504R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00070\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\t0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\rR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00066"}, d2 = {"Lcom/pevplugshare/ui/add/AddLocationViewModel;", "Landroidx/lifecycle/ViewModel;", "locationRepository", "Lcom/pevplugshare/domain/repository/LocationRepository;", "(Lcom/pevplugshare/domain/repository/LocationRepository;)V", "_addLocationState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/pevplugshare/ui/add/AddLocationState;", "_formData", "Lcom/pevplugshare/ui/add/LocationFormData;", "addLocationState", "Lkotlinx/coroutines/flow/StateFlow;", "getAddLocationState", "()Lkotlinx/coroutines/flow/StateFlow;", "formData", "getFormData", "resetForm", "", "saveLocation", "data", "saveLocationAnyway", "setCurrentLocation", "latitude", "", "longitude", "submitLocation", "updateAccessType", "type", "Lcom/pevplugshare/data/model/LocationAccessType;", "updateAdditionalInfo", "info", "", "updateChargingCosts", "costs", "updateDescription", "description", "updateFormData", "updateGpoCount", "count", "", "updateHasFee", "hasFee", "", "updateName", "name", "updateOpeningHours", "hours", "updateProviderName", "updateProviderWebsite", "website", "updateSocketTypes", "types", "", "Lcom/pevplugshare/data/model/SocketType;", "app_release"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class AddLocationViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.domain.repository.LocationRepository locationRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pevplugshare.ui.add.AddLocationState> _addLocationState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.add.AddLocationState> addLocationState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pevplugshare.ui.add.LocationFormData> _formData = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.add.LocationFormData> formData = null;
    
    @javax.inject.Inject()
    public AddLocationViewModel(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.domain.repository.LocationRepository locationRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.add.AddLocationState> getAddLocationState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.add.LocationFormData> getFormData() {
        return null;
    }
    
    public final void updateFormData(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.ui.add.LocationFormData data) {
    }
    
    public final void updateName(@org.jetbrains.annotations.NotNull()
    java.lang.String name) {
    }
    
    public final void updateGpoCount(int count) {
    }
    
    public final void updateSocketTypes(@org.jetbrains.annotations.NotNull()
    java.util.List<? extends com.pevplugshare.data.model.SocketType> types) {
    }
    
    public final void updateAccessType(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.model.LocationAccessType type) {
    }
    
    public final void updateHasFee(boolean hasFee) {
    }
    
    public final void updateDescription(@org.jetbrains.annotations.NotNull()
    java.lang.String description) {
    }
    
    public final void updateOpeningHours(@org.jetbrains.annotations.NotNull()
    java.lang.String hours) {
    }
    
    public final void updateAdditionalInfo(@org.jetbrains.annotations.NotNull()
    java.lang.String info) {
    }
    
    public final void updateProviderName(@org.jetbrains.annotations.NotNull()
    java.lang.String name) {
    }
    
    public final void updateProviderWebsite(@org.jetbrains.annotations.NotNull()
    java.lang.String website) {
    }
    
    public final void updateChargingCosts(@org.jetbrains.annotations.NotNull()
    java.lang.String costs) {
    }
    
    public final void setCurrentLocation(double latitude, double longitude) {
    }
    
    public final void submitLocation() {
    }
    
    /**
     * Force save location even if duplicates exist (user confirmed)
     */
    public final void saveLocationAnyway() {
    }
    
    private final void saveLocation(com.pevplugshare.ui.add.LocationFormData data) {
    }
    
    public final void resetForm() {
    }
}