package com.pevplugshare.ui.add;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0005\u0003\u0004\u0005\u0006\u0007B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0005\b\t\n\u000b\f\u00a8\u0006\r"}, d2 = {"Lcom/pevplugshare/ui/add/AddLocationState;", "", "()V", "Error", "Idle", "Loading", "NearbyLocationsFound", "Success", "Lcom/pevplugshare/ui/add/AddLocationState$Error;", "Lcom/pevplugshare/ui/add/AddLocationState$Idle;", "Lcom/pevplugshare/ui/add/AddLocationState$Loading;", "Lcom/pevplugshare/ui/add/AddLocationState$NearbyLocationsFound;", "Lcom/pevplugshare/ui/add/AddLocationState$Success;", "app_debug"})
public abstract class AddLocationState {
    
    private AddLocationState() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/pevplugshare/ui/add/AddLocationState$Error;", "Lcom/pevplugshare/ui/add/AddLocationState;", "message", "", "(Ljava/lang/String;)V", "getMessage", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class Error extends com.pevplugshare.ui.add.AddLocationState {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String message = null;
        
        public Error(@org.jetbrains.annotations.NotNull()
        java.lang.String message) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getMessage() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pevplugshare.ui.add.AddLocationState.Error copy(@org.jetbrains.annotations.NotNull()
        java.lang.String message) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/pevplugshare/ui/add/AddLocationState$Idle;", "Lcom/pevplugshare/ui/add/AddLocationState;", "()V", "app_debug"})
    public static final class Idle extends com.pevplugshare.ui.add.AddLocationState {
        @org.jetbrains.annotations.NotNull()
        public static final com.pevplugshare.ui.add.AddLocationState.Idle INSTANCE = null;
        
        private Idle() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/pevplugshare/ui/add/AddLocationState$Loading;", "Lcom/pevplugshare/ui/add/AddLocationState;", "()V", "app_debug"})
    public static final class Loading extends com.pevplugshare.ui.add.AddLocationState {
        @org.jetbrains.annotations.NotNull()
        public static final com.pevplugshare.ui.add.AddLocationState.Loading INSTANCE = null;
        
        private Loading() {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u001f\u0012\u0018\u0010\u0002\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00060\u00040\u0003\u00a2\u0006\u0002\u0010\u0007J\u001b\u0010\n\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00060\u00040\u0003H\u00c6\u0003J%\u0010\u000b\u001a\u00020\u00002\u001a\b\u0002\u0010\u0002\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00060\u00040\u0003H\u00c6\u0001J\u0013\u0010\f\u001a\u00020\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u00d6\u0003J\t\u0010\u0010\u001a\u00020\u0011H\u00d6\u0001J\t\u0010\u0012\u001a\u00020\u0013H\u00d6\u0001R#\u0010\u0002\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00060\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\t\u00a8\u0006\u0014"}, d2 = {"Lcom/pevplugshare/ui/add/AddLocationState$NearbyLocationsFound;", "Lcom/pevplugshare/ui/add/AddLocationState;", "locations", "", "Lkotlin/Pair;", "Lcom/pevplugshare/data/model/Location;", "", "(Ljava/util/List;)V", "getLocations", "()Ljava/util/List;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
    public static final class NearbyLocationsFound extends com.pevplugshare.ui.add.AddLocationState {
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<kotlin.Pair<com.pevplugshare.data.model.Location, java.lang.Double>> locations = null;
        
        public NearbyLocationsFound(@org.jetbrains.annotations.NotNull()
        java.util.List<kotlin.Pair<com.pevplugshare.data.model.Location, java.lang.Double>> locations) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<kotlin.Pair<com.pevplugshare.data.model.Location, java.lang.Double>> getLocations() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<kotlin.Pair<com.pevplugshare.data.model.Location, java.lang.Double>> component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pevplugshare.ui.add.AddLocationState.NearbyLocationsFound copy(@org.jetbrains.annotations.NotNull()
        java.util.List<kotlin.Pair<com.pevplugshare.data.model.Location, java.lang.Double>> locations) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u0011\u0012\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\u0004J\u000b\u0010\u0007\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u0015\u0010\b\u001a\u00020\u00002\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0013\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/pevplugshare/ui/add/AddLocationState$Success;", "Lcom/pevplugshare/ui/add/AddLocationState;", "location", "Lcom/pevplugshare/data/model/Location;", "(Lcom/pevplugshare/data/model/Location;)V", "getLocation", "()Lcom/pevplugshare/data/model/Location;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
    public static final class Success extends com.pevplugshare.ui.add.AddLocationState {
        @org.jetbrains.annotations.Nullable()
        private final com.pevplugshare.data.model.Location location = null;
        
        public Success(@org.jetbrains.annotations.Nullable()
        com.pevplugshare.data.model.Location location) {
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.pevplugshare.data.model.Location getLocation() {
            return null;
        }
        
        public Success() {
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.pevplugshare.data.model.Location component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pevplugshare.ui.add.AddLocationState.Success copy(@org.jetbrains.annotations.Nullable()
        com.pevplugshare.data.model.Location location) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}