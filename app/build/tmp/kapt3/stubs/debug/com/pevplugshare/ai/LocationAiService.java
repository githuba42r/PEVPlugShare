package com.pevplugshare.ai;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\b\u0007\u0018\u0000 \u00162\u00020\u0001:\u0001\u0016B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u001e\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\bH\u0086@\u00a2\u0006\u0002\u0010\fJ.\u0010\r\u001a\b\u0012\u0004\u0012\u00020\b0\u000e2\u0018\u0010\u000f\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\b0\u00100\u000eH\u0086@\u00a2\u0006\u0002\u0010\u0011J\u000e\u0010\u0012\u001a\u00020\u0013H\u0086@\u00a2\u0006\u0002\u0010\u0014J\f\u0010\u0015\u001a\u00020\n*\u00020\bH\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0017"}, d2 = {"Lcom/pevplugshare/ai/LocationAiService;", "", "parser", "Lcom/pevplugshare/ai/LocationDescriptionParser;", "locationDao", "Lcom/pevplugshare/data/db/dao/LocationDao;", "(Lcom/pevplugshare/ai/LocationDescriptionParser;Lcom/pevplugshare/data/db/dao/LocationDao;)V", "processLocation", "Lcom/pevplugshare/data/db/entity/LocationEntity;", "location", "Lcom/pevplugshare/data/model/Location;", "entity", "(Lcom/pevplugshare/data/model/Location;Lcom/pevplugshare/data/db/entity/LocationEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "processLocations", "", "locations", "Lkotlin/Pair;", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "reprocessAllLocations", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "toLocation", "Companion", "app_debug"})
public final class LocationAiService {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.ai.LocationDescriptionParser parser = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.db.dao.LocationDao locationDao = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "LocationAiService";
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.ai.LocationAiService.Companion Companion = null;
    
    @javax.inject.Inject()
    public LocationAiService(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.ai.LocationDescriptionParser parser, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.dao.LocationDao locationDao) {
        super();
    }
    
    /**
     * Process a location and parse its description with AI if needed
     * Checks if description has changed before parsing
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object processLocation(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.model.Location location, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.LocationEntity entity, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pevplugshare.data.db.entity.LocationEntity> $completion) {
        return null;
    }
    
    /**
     * Process a batch of locations
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object processLocations(@org.jetbrains.annotations.NotNull()
    java.util.List<kotlin.Pair<com.pevplugshare.data.model.Location, com.pevplugshare.data.db.entity.LocationEntity>> locations, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pevplugshare.data.db.entity.LocationEntity>> $completion) {
        return null;
    }
    
    /**
     * Reprocess all locations with changed descriptions
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object reprocessAllLocations(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final com.pevplugshare.data.model.Location toLocation(com.pevplugshare.data.db.entity.LocationEntity $this$toLocation) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/pevplugshare/ai/LocationAiService$Companion;", "", "()V", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}