package com.pevplugshare.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0002\b\t\n\u0002\u0010\u000b\n\u0002\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0086@\u00a2\u0006\u0002\u0010\tJ\u0016\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\rH\u0086@\u00a2\u0006\u0002\u0010\u000eJ\u0016\u0010\u000f\u001a\u00020\u000b2\u0006\u0010\u0007\u001a\u00020\bH\u0086@\u00a2\u0006\u0002\u0010\tJ\u0016\u0010\u0010\u001a\u00020\u000b2\u0006\u0010\u0011\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010\u0012J\u001e\u0010\u0013\u001a\u00020\u000b2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\f\u001a\u00020\rH\u0086@\u00a2\u0006\u0002\u0010\u0016J\u0014\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\b0\u0018H\u0086@\u00a2\u0006\u0002\u0010\u0019J\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u001c0\u001bJ\u0014\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\b0\u0018H\u0086@\u00a2\u0006\u0002\u0010\u0019J\u001c\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\b0\u00182\u0006\u0010\f\u001a\u00020\rH\u0086@\u00a2\u0006\u0002\u0010\u000eJ\u0018\u0010\u001f\u001a\u0004\u0018\u00010\b2\u0006\u0010\u0011\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010\u0012J\u001c\u0010 \u001a\b\u0012\u0004\u0012\u00020\b0\u00182\u0006\u0010\u0014\u001a\u00020\u0015H\u0086@\u00a2\u0006\u0002\u0010!J\f\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u001c0\u001bJ\u0014\u0010#\u001a\b\u0012\u0004\u0012\u00020\b0\u0018H\u0086@\u00a2\u0006\u0002\u0010\u0019J\u001c\u0010$\u001a\b\u0012\u0004\u0012\u00020\b0\u00182\u0006\u0010\u0014\u001a\u00020\u0015H\u0086@\u00a2\u0006\u0002\u0010!J\u000e\u0010%\u001a\u00020&H\u0086@\u00a2\u0006\u0002\u0010\u0019J\u000e\u0010\'\u001a\u00020&H\u0086@\u00a2\u0006\u0002\u0010\u0019J\"\u0010(\u001a\u00020\u000b2\u0006\u0010\u0007\u001a\u00020\b2\n\b\u0002\u0010)\u001a\u0004\u0018\u00010\rH\u0086@\u00a2\u0006\u0002\u0010*J\u0016\u0010+\u001a\u00020\u000b2\u0006\u0010\u0007\u001a\u00020\bH\u0086@\u00a2\u0006\u0002\u0010\tJ\u0016\u0010,\u001a\u00020\u000b2\u0006\u0010\u0007\u001a\u00020\bH\u0086@\u00a2\u0006\u0002\u0010\tR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006-"}, d2 = {"Lcom/pevplugshare/data/repository/PendingOperationRepository;", "", "pendingOperationDao", "Lcom/pevplugshare/data/db/dao/PendingOperationDao;", "(Lcom/pevplugshare/data/db/dao/PendingOperationDao;)V", "addOperation", "", "operation", "Lcom/pevplugshare/data/db/entity/PendingOperationEntity;", "(Lcom/pevplugshare/data/db/entity/PendingOperationEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteLocationOperations", "", "locationId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteOperation", "deleteOperationById", "operationId", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteOperationsByTypeAndLocation", "operationType", "Lcom/pevplugshare/data/db/entity/PendingOperationType;", "(Lcom/pevplugshare/data/db/entity/PendingOperationType;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllPendingOperations", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getFailedOperationCount", "Lkotlinx/coroutines/flow/Flow;", "", "getFailedOperations", "getLocationOperations", "getOperationById", "getOperationsByType", "(Lcom/pevplugshare/data/db/entity/PendingOperationType;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getPendingOperationCount", "getRetryableOperations", "getRetryableOperationsByType", "hasFailedOperations", "", "hasRetryableOperations", "incrementRetryCount", "errorMessage", "(Lcom/pevplugshare/data/db/entity/PendingOperationEntity;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "markOperationAsSuccessful", "updateOperation", "app_release"})
public final class PendingOperationRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.db.dao.PendingOperationDao pendingOperationDao = null;
    
    @javax.inject.Inject()
    public PendingOperationRepository(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.dao.PendingOperationDao pendingOperationDao) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object addOperation(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationEntity operation, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object updateOperation(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationEntity operation, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object deleteOperation(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationEntity operation, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object deleteOperationById(long operationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getOperationById(long operationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pevplugshare.data.db.entity.PendingOperationEntity> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getLocationOperations(@org.jetbrains.annotations.NotNull()
    java.lang.String locationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pevplugshare.data.db.entity.PendingOperationEntity>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getAllPendingOperations(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pevplugshare.data.db.entity.PendingOperationEntity>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getRetryableOperations(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pevplugshare.data.db.entity.PendingOperationEntity>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getFailedOperations(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pevplugshare.data.db.entity.PendingOperationEntity>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.lang.Integer> getPendingOperationCount() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.lang.Integer> getFailedOperationCount() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object deleteLocationOperations(@org.jetbrains.annotations.NotNull()
    java.lang.String locationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object deleteOperationsByTypeAndLocation(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationType operationType, @org.jetbrains.annotations.NotNull()
    java.lang.String locationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getOperationsByType(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationType operationType, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pevplugshare.data.db.entity.PendingOperationEntity>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getRetryableOperationsByType(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationType operationType, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pevplugshare.data.db.entity.PendingOperationEntity>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object incrementRetryCount(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationEntity operation, @org.jetbrains.annotations.Nullable()
    java.lang.String errorMessage, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object markOperationAsSuccessful(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationEntity operation, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object hasFailedOperations(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object hasRetryableOperations(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
}