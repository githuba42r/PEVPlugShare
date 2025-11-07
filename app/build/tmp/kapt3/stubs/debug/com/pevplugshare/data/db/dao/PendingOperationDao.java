package com.pevplugshare.data.db.dao;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0002\b\u000b\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0016\u0010\u000b\u001a\u00020\u00032\u0006\u0010\f\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u000eJ\u001e\u0010\u000f\u001a\u00020\u00032\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0012J\u0014\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\t0\u0014H\u00a7@\u00a2\u0006\u0002\u0010\u0015J\u000e\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00180\u0017H\'J\u0014\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\t0\u0014H\u00a7@\u00a2\u0006\u0002\u0010\u0015J\u001c\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\t0\u00142\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u001b\u001a\u0004\u0018\u00010\t2\u0006\u0010\f\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u000eJ\u001c\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\t0\u00142\u0006\u0010\u0010\u001a\u00020\u0011H\u00a7@\u00a2\u0006\u0002\u0010\u001dJ\u000e\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u00180\u0017H\'J\u0014\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\t0\u0014H\u00a7@\u00a2\u0006\u0002\u0010\u0015J\u001c\u0010 \u001a\b\u0012\u0004\u0012\u00020\t0\u00142\u0006\u0010\u0010\u001a\u00020\u0011H\u00a7@\u00a2\u0006\u0002\u0010\u001dJ\u0016\u0010!\u001a\u00020\r2\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0016\u0010\"\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\n\u00a8\u0006#"}, d2 = {"Lcom/pevplugshare/data/db/dao/PendingOperationDao;", "", "deleteLocationOperations", "", "locationId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteOperation", "operation", "Lcom/pevplugshare/data/db/entity/PendingOperationEntity;", "(Lcom/pevplugshare/data/db/entity/PendingOperationEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteOperationById", "operationId", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteOperationsByTypeAndLocation", "operationType", "Lcom/pevplugshare/data/db/entity/PendingOperationType;", "(Lcom/pevplugshare/data/db/entity/PendingOperationType;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllPendingOperations", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getFailedOperationCount", "Lkotlinx/coroutines/flow/Flow;", "", "getFailedOperations", "getLocationOperations", "getOperationById", "getOperationsByType", "(Lcom/pevplugshare/data/db/entity/PendingOperationType;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getPendingOperationCount", "getRetryableOperations", "getRetryableOperationsByType", "insertOperation", "updateOperation", "app_debug"})
@androidx.room.Dao()
public abstract interface PendingOperationDao {
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertOperation(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationEntity operation, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion);
    
    @androidx.room.Update()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateOperation(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationEntity operation, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteOperation(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationEntity operation, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM pending_operations WHERE id = :operationId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteOperationById(long operationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM pending_operations WHERE id = :operationId LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getOperationById(long operationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pevplugshare.data.db.entity.PendingOperationEntity> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM pending_operations WHERE locationId = :locationId ORDER BY timestamp ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLocationOperations(@org.jetbrains.annotations.NotNull()
    java.lang.String locationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pevplugshare.data.db.entity.PendingOperationEntity>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM pending_operations ORDER BY timestamp ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAllPendingOperations(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pevplugshare.data.db.entity.PendingOperationEntity>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM pending_operations WHERE retryCount < maxRetries ORDER BY timestamp ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getRetryableOperations(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pevplugshare.data.db.entity.PendingOperationEntity>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM pending_operations WHERE retryCount >= maxRetries ORDER BY timestamp DESC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getFailedOperations(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pevplugshare.data.db.entity.PendingOperationEntity>> $completion);
    
    @androidx.room.Query(value = "SELECT COUNT(*) FROM pending_operations")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.lang.Integer> getPendingOperationCount();
    
    @androidx.room.Query(value = "SELECT COUNT(*) FROM pending_operations WHERE retryCount >= maxRetries")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.lang.Integer> getFailedOperationCount();
    
    @androidx.room.Query(value = "DELETE FROM pending_operations WHERE locationId = :locationId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteLocationOperations(@org.jetbrains.annotations.NotNull()
    java.lang.String locationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM pending_operations WHERE operationType = :operationType AND locationId = :locationId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteOperationsByTypeAndLocation(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationType operationType, @org.jetbrains.annotations.NotNull()
    java.lang.String locationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM pending_operations WHERE operationType = :operationType ORDER BY timestamp ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getOperationsByType(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationType operationType, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pevplugshare.data.db.entity.PendingOperationEntity>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM pending_operations WHERE operationType = :operationType AND retryCount < maxRetries ORDER BY timestamp ASC")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getRetryableOperationsByType(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationType operationType, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pevplugshare.data.db.entity.PendingOperationEntity>> $completion);
}