package com.pevplugshare.ui.edit;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\f\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u000b\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u0013J\u0006\u0010\u0014\u001a\u00020\u0011J\u000e\u0010\u0015\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u0013J\u000e\u0010\u0016\u001a\u00020\u00112\u0006\u0010\u0017\u001a\u00020\u0013J\u000e\u0010\u0018\u001a\u00020\u00112\u0006\u0010\u0019\u001a\u00020\u0013J\u000e\u0010\u001a\u001a\u00020\u00112\u0006\u0010\u001b\u001a\u00020\u0013J\u000e\u0010\u001c\u001a\u00020\u00112\u0006\u0010\u001d\u001a\u00020\u0013J\u000e\u0010\u001e\u001a\u00020\u00112\u0006\u0010\u001f\u001a\u00020 J\u000e\u0010!\u001a\u00020\u00112\u0006\u0010\"\u001a\u00020#J\u0016\u0010$\u001a\u00020\u00112\u0006\u0010%\u001a\u00020&2\u0006\u0010\'\u001a\u00020&J\u000e\u0010(\u001a\u00020\u00112\u0006\u0010)\u001a\u00020\u0013J\u000e\u0010*\u001a\u00020\u00112\u0006\u0010+\u001a\u00020\u0013J\u000e\u0010,\u001a\u00020\u00112\u0006\u0010)\u001a\u00020\u0013J\u000e\u0010-\u001a\u00020\u00112\u0006\u0010.\u001a\u00020\u0013J\u000e\u0010/\u001a\u00020\u00112\u0006\u00100\u001a\u00020\u0013R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00070\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\t0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\rR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00061"}, d2 = {"Lcom/pevplugshare/ui/edit/EditLocationViewModel;", "Landroidx/lifecycle/ViewModel;", "locationRepository", "Lcom/pevplugshare/domain/repository/LocationRepository;", "(Lcom/pevplugshare/domain/repository/LocationRepository;)V", "_editState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/pevplugshare/ui/edit/EditLocationState;", "_formData", "Lcom/pevplugshare/ui/edit/EditLocationFormData;", "editState", "Lkotlinx/coroutines/flow/StateFlow;", "getEditState", "()Lkotlinx/coroutines/flow/StateFlow;", "formData", "getFormData", "loadLocation", "", "locationId", "", "resetForm", "submitUpdates", "updateAccessType", "accessType", "updateAdditionalInfo", "info", "updateChargingCosts", "costs", "updateDescription", "description", "updateGpoCount", "count", "", "updateHasFee", "hasFee", "", "updateLocation", "latitude", "", "longitude", "updateName", "name", "updateOpeningHours", "hours", "updateProviderName", "updateProviderWebsite", "website", "updateSocketTypes", "types", "app_release"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class EditLocationViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.domain.repository.LocationRepository locationRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pevplugshare.ui.edit.EditLocationState> _editState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.edit.EditLocationState> editState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pevplugshare.ui.edit.EditLocationFormData> _formData = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.edit.EditLocationFormData> formData = null;
    
    @javax.inject.Inject()
    public EditLocationViewModel(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.domain.repository.LocationRepository locationRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.edit.EditLocationState> getEditState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.edit.EditLocationFormData> getFormData() {
        return null;
    }
    
    public final void loadLocation(@org.jetbrains.annotations.NotNull()
    java.lang.String locationId) {
    }
    
    public final void updateName(@org.jetbrains.annotations.NotNull()
    java.lang.String name) {
    }
    
    public final void updateGpoCount(int count) {
    }
    
    public final void updateSocketTypes(@org.jetbrains.annotations.NotNull()
    java.lang.String types) {
    }
    
    public final void updateAccessType(@org.jetbrains.annotations.NotNull()
    java.lang.String accessType) {
    }
    
    public final void updateHasFee(boolean hasFee) {
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
    
    public final void updateDescription(@org.jetbrains.annotations.NotNull()
    java.lang.String description) {
    }
    
    public final void updateOpeningHours(@org.jetbrains.annotations.NotNull()
    java.lang.String hours) {
    }
    
    public final void updateAdditionalInfo(@org.jetbrains.annotations.NotNull()
    java.lang.String info) {
    }
    
    public final void updateLocation(double latitude, double longitude) {
    }
    
    public final void submitUpdates(@org.jetbrains.annotations.NotNull()
    java.lang.String locationId) {
    }
    
    public final void resetForm() {
    }
}