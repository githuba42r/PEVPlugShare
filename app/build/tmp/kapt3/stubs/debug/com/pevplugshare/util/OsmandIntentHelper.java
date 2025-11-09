package com.pevplugshare.util;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000bJ\u0016\u0010\f\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000bJ\u0010\u0010\r\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u000bH\u0002J\u0010\u0010\u000e\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tH\u0002J\u0010\u0010\u000f\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tH\u0002J\u0010\u0010\u0010\u001a\u0004\u0018\u00010\u00042\u0006\u0010\b\u001a\u00020\tJ\u000e\u0010\u0011\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tJ\u0018\u0010\u0012\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\u0013\u001a\u00020\u0004H\u0002J \u0010\u0014\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\b\b\u0002\u0010\u0015\u001a\u00020\u0004J\u000e\u0010\u0016\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tJ\u000e\u0010\u0017\u001a\u00020\u00182\u0006\u0010\b\u001a\u00020\tJ\u0016\u0010\u0019\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000bR\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001a"}, d2 = {"Lcom/pevplugshare/util/OsmandIntentHelper;", "", "()V", "OSMAND_FREE_PACKAGE", "", "OSMAND_PACKAGE", "addFavorite", "", "context", "Landroid/content/Context;", "location", "Lcom/pevplugshare/data/model/Location;", "addMapMarker", "buildDescription", "canLaunchOsmandScheme", "canResolveOsmandIntent", "getOsmandPackage", "isOsmandInstalled", "isPackageInstalled", "packageName", "navigateToLocation", "profile", "openOsmand", "openPlayStoreForOsmand", "", "showLocationOnMap", "app_debug"})
public final class OsmandIntentHelper {
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String OSMAND_PACKAGE = "net.osmand.plus";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String OSMAND_FREE_PACKAGE = "net.osmand";
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.util.OsmandIntentHelper INSTANCE = null;
    
    private OsmandIntentHelper() {
        super();
    }
    
    /**
     * Check if OSMAnd (Plus or Free) is installed
     */
    public final boolean isOsmandInstalled(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
    
    /**
     * Get the installed OSMAnd package name
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getOsmandPackage(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    private final boolean isPackageInstalled(android.content.Context context, java.lang.String packageName) {
        return false;
    }
    
    /**
     * Alternative check using resolveActivity (more reliable for Intent-based apps)
     */
    private final boolean canResolveOsmandIntent(android.content.Context context) {
        return false;
    }
    
    /**
     * Test if we can actually launch an OSMAnd scheme (without actually launching it)
     */
    private final boolean canLaunchOsmandScheme(android.content.Context context) {
        return false;
    }
    
    /**
     * Add a charging location as a favorite in OSMAnd
     * @return true if successful, false if OSMAnd not installed or error
     */
    public final boolean addFavorite(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.model.Location location) {
        return false;
    }
    
    /**
     * Navigate to a charging location using OSMAnd
     * @param profile Navigation profile: "bicycle", "car", "pedestrian"
     * @return true if successful, false if OSMAnd not installed or error
     */
    public final boolean navigateToLocation(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.model.Location location, @org.jetbrains.annotations.NotNull()
    java.lang.String profile) {
        return false;
    }
    
    /**
     * Show a charging location on OSMAnd map
     * @return true if successful, false if OSMAnd not installed or error
     */
    public final boolean showLocationOnMap(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.model.Location location) {
        return false;
    }
    
    /**
     * Add a map marker in OSMAnd
     * @return true if successful, false if OSMAnd not installed or error
     */
    public final boolean addMapMarker(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.model.Location location) {
        return false;
    }
    
    /**
     * Open OSMAnd app
     * @return true if successful, false if not installed
     */
    public final boolean openOsmand(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
    
    /**
     * Open Play Store to install OSMAnd Plus
     */
    public final void openPlayStoreForOsmand(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    /**
     * Build a description string for OSMAnd favorite from location data
     */
    private final java.lang.String buildDescription(com.pevplugshare.data.model.Location location) {
        return null;
    }
}