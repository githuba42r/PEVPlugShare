package com.pevplugshare.data.gpx;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001:\u0001\bB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\u0006\u001a\u00020\u0007\u00a8\u0006\t"}, d2 = {"Lcom/pevplugshare/data/gpx/GpxParser;", "", "()V", "parse", "", "Lcom/pevplugshare/data/model/Location;", "inputStream", "Ljava/io/InputStream;", "LocationBuilder", "app_debug"})
public final class GpxParser {
    
    public GpxParser() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pevplugshare.data.model.Location> parse(@org.jetbrains.annotations.NotNull()
    java.io.InputStream inputStream) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u000e\n\u0002\u0018\u0002\n\u0000\b\u0002\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0005J\b\u0010\u0015\u001a\u0004\u0018\u00010\u0016R\u001c\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\rR\u001c\u0010\u000f\u001a\u0004\u0018\u00010\u0007X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0010\u0010\t\"\u0004\b\u0011\u0010\u000bR\u001c\u0010\u0012\u001a\u0004\u0018\u00010\u0007X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0013\u0010\t\"\u0004\b\u0014\u0010\u000b\u00a8\u0006\u0017"}, d2 = {"Lcom/pevplugshare/data/gpx/GpxParser$LocationBuilder;", "", "latitude", "", "longitude", "(DD)V", "description", "", "getDescription", "()Ljava/lang/String;", "setDescription", "(Ljava/lang/String;)V", "getLatitude", "()D", "getLongitude", "name", "getName", "setName", "timestamp", "getTimestamp", "setTimestamp", "build", "Lcom/pevplugshare/data/model/Location;", "app_debug"})
    static final class LocationBuilder {
        private final double latitude = 0.0;
        private final double longitude = 0.0;
        @org.jetbrains.annotations.Nullable()
        private java.lang.String name;
        @org.jetbrains.annotations.Nullable()
        private java.lang.String description;
        @org.jetbrains.annotations.Nullable()
        private java.lang.String timestamp;
        
        public LocationBuilder(double latitude, double longitude) {
            super();
        }
        
        public final double getLatitude() {
            return 0.0;
        }
        
        public final double getLongitude() {
            return 0.0;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getName() {
            return null;
        }
        
        public final void setName(@org.jetbrains.annotations.Nullable()
        java.lang.String p0) {
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getDescription() {
            return null;
        }
        
        public final void setDescription(@org.jetbrains.annotations.Nullable()
        java.lang.String p0) {
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getTimestamp() {
            return null;
        }
        
        public final void setTimestamp(@org.jetbrains.annotations.Nullable()
        java.lang.String p0) {
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.pevplugshare.data.model.Location build() {
            return null;
        }
    }
}