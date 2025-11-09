package com.pevplugshare.data.repository;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\b\u0007\u0018\u0000 \u00162\u00020\u0001:\u0001\u0016B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u001e\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0086@\u00a2\u0006\u0002\u0010\u000fJ6\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\n0\u00112\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\b\b\u0002\u0010\u0012\u001a\u00020\u0013H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0014\u0010\u0015R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u0017"}, d2 = {"Lcom/pevplugshare/data/repository/OsmandLocationSyncService;", "", "apiClient", "Lcom/pevplugshare/data/remote/OsmandApiClient;", "tokenManager", "Lcom/pevplugshare/data/remote/TokenManager;", "localRepository", "Lcom/pevplugshare/data/repository/LocalLocationRepository;", "(Lcom/pevplugshare/data/remote/OsmandApiClient;Lcom/pevplugshare/data/remote/TokenManager;Lcom/pevplugshare/data/repository/LocalLocationRepository;)V", "markLocationPendingSync", "", "location", "Lcom/pevplugshare/data/model/Location;", "categoryName", "", "(Lcom/pevplugshare/data/model/Location;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateLocationToOsmand", "Lkotlin/Result;", "index", "", "updateLocationToOsmand-BWLJW6A", "(Lcom/pevplugshare/data/model/Location;Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "app_debug"})
public final class OsmandLocationSyncService {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.remote.OsmandApiClient apiClient = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.remote.TokenManager tokenManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.repository.LocalLocationRepository localRepository = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "OsmandLocationSync";
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.data.repository.OsmandLocationSyncService.Companion Companion = null;
    
    @javax.inject.Inject()
    public OsmandLocationSyncService(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.OsmandApiClient apiClient, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.remote.TokenManager tokenManager, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.repository.LocalLocationRepository localRepository) {
        super();
    }
    
    /**
     * Mark a location as pending sync
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object markLocationPendingSync(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.model.Location location, @org.jetbrains.annotations.NotNull()
    java.lang.String categoryName, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/pevplugshare/data/repository/OsmandLocationSyncService$Companion;", "", "()V", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}