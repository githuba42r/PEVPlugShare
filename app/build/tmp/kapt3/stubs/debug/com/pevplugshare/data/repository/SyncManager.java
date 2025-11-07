package com.pevplugshare.data.repository;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\\\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u0014\u001a\u00020\u0015H\u0086@\u00a2\u0006\u0002\u0010\u0016J\u000e\u0010\u0017\u001a\u00020\u000bH\u0086@\u00a2\u0006\u0002\u0010\u0016J\u000e\u0010\u0018\u001a\u00020\u0019H\u0086@\u00a2\u0006\u0002\u0010\u0016J\u000e\u0010\u001a\u001a\u00020\u0019H\u0086@\u00a2\u0006\u0002\u0010\u0016J\u001e\u0010\u001b\u001a\u00020\u00152\u0006\u0010\u001c\u001a\u00020\u001d2\u0006\u0010\u001e\u001a\u00020\u001fH\u0086@\u00a2\u0006\u0002\u0010 J\u0006\u0010!\u001a\u00020\u0015J\u0016\u0010\"\u001a\u00020\u00152\u0006\u0010#\u001a\u00020$H\u0082@\u00a2\u0006\u0002\u0010%J\u0016\u0010&\u001a\u00020\u00152\u0006\u0010#\u001a\u00020$H\u0082@\u00a2\u0006\u0002\u0010%J\u0016\u0010\'\u001a\u00020\u00152\u0006\u0010#\u001a\u00020$H\u0082@\u00a2\u0006\u0002\u0010%J\u0016\u0010(\u001a\u00020\u00152\u0006\u0010#\u001a\u00020$H\u0082@\u00a2\u0006\u0002\u0010%J\u000e\u0010)\u001a\u00020\u0019H\u0086@\u00a2\u0006\u0002\u0010\u0016J\u0016\u0010*\u001a\u00020\u00152\u0006\u0010#\u001a\u00020$H\u0082@\u00a2\u0006\u0002\u0010%R\u000e\u0010\u0007\u001a\u00020\bX\u0082D\u00a2\u0006\u0002\n\u0000R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000b0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0017\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\r0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0011\u00a8\u0006+"}, d2 = {"Lcom/pevplugshare/data/repository/SyncManager;", "", "pendingOperationRepository", "Lcom/pevplugshare/data/repository/PendingOperationRepository;", "locationRepository", "Lcom/pevplugshare/domain/repository/LocationRepository;", "(Lcom/pevplugshare/data/repository/PendingOperationRepository;Lcom/pevplugshare/domain/repository/LocationRepository;)V", "TAG", "", "_syncProgress", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_syncState", "Lcom/pevplugshare/data/repository/SyncState;", "syncProgress", "Lkotlinx/coroutines/flow/StateFlow;", "getSyncProgress", "()Lkotlinx/coroutines/flow/StateFlow;", "syncState", "getSyncState", "clearSyncState", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getPendingOperationCount", "hasFailedOperations", "", "hasPendingOperations", "queueLocationOperation", "location", "Lcom/pevplugshare/data/model/Location;", "operationType", "Lcom/pevplugshare/data/db/entity/PendingOperationType;", "(Lcom/pevplugshare/data/model/Location;Lcom/pevplugshare/data/db/entity/PendingOperationType;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "resetSyncState", "syncAddLocation", "operation", "Lcom/pevplugshare/data/db/entity/PendingOperationEntity;", "(Lcom/pevplugshare/data/db/entity/PendingOperationEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "syncAddPhoto", "syncDeleteLocation", "syncDeletePhoto", "syncPendingOperations", "syncUpdateLocation", "app_debug"})
public final class SyncManager {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.repository.PendingOperationRepository pendingOperationRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.domain.repository.LocationRepository locationRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pevplugshare.data.repository.SyncState> _syncState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.data.repository.SyncState> syncState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Integer> _syncProgress = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> syncProgress = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String TAG = "SyncManager";
    
    @javax.inject.Inject()
    public SyncManager(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.repository.PendingOperationRepository pendingOperationRepository, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.domain.repository.LocationRepository locationRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.data.repository.SyncState> getSyncState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> getSyncProgress() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object syncPendingOperations(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    private final java.lang.Object syncAddLocation(com.pevplugshare.data.db.entity.PendingOperationEntity operation, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object syncUpdateLocation(com.pevplugshare.data.db.entity.PendingOperationEntity operation, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object syncDeleteLocation(com.pevplugshare.data.db.entity.PendingOperationEntity operation, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object syncAddPhoto(com.pevplugshare.data.db.entity.PendingOperationEntity operation, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object syncDeletePhoto(com.pevplugshare.data.db.entity.PendingOperationEntity operation, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object queueLocationOperation(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.model.Location location, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationType operationType, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object hasPendingOperations(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object hasFailedOperations(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getPendingOperationCount(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object clearSyncState(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    public final void resetSyncState() {
    }
}