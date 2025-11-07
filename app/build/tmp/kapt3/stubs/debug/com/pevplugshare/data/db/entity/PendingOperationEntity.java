package com.pevplugshare.data.db.entity;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u001f\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0087\b\u0018\u00002\u00020\u0001B]\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\u0005\u0012\b\b\u0002\u0010\t\u001a\u00020\u0003\u0012\b\b\u0002\u0010\n\u001a\u00020\u000b\u0012\b\b\u0002\u0010\f\u001a\u00020\u000b\u0012\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\u0002\u0010\u000fJ\t\u0010\u001f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010 \u001a\u00020\u0005H\u00c6\u0003J\t\u0010!\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\"\u001a\u00020\u0005H\u00c6\u0003J\t\u0010#\u001a\u00020\u0003H\u00c6\u0003J\t\u0010$\u001a\u00020\u000bH\u00c6\u0003J\t\u0010%\u001a\u00020\u000bH\u00c6\u0003J\u0010\u0010&\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003\u00a2\u0006\u0002\u0010\u0015J\u000b\u0010\'\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003Jl\u0010(\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00052\b\b\u0002\u0010\t\u001a\u00020\u00032\b\b\u0002\u0010\n\u001a\u00020\u000b2\b\b\u0002\u0010\f\u001a\u00020\u000b2\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u0005H\u00c6\u0001\u00a2\u0006\u0002\u0010)J\u0013\u0010*\u001a\u00020+2\b\u0010,\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010-\u001a\u00020\u000bH\u00d6\u0001J\t\u0010.\u001a\u00020\u0005H\u00d6\u0001R\u0013\u0010\u000e\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0015\u0010\r\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\n\n\u0002\u0010\u0016\u001a\u0004\b\u0014\u0010\u0015R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0011R\u0011\u0010\f\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0011\u0010\b\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0011R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u0011\u0010\n\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0019R\u0011\u0010\t\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u0013\u00a8\u0006/"}, d2 = {"Lcom/pevplugshare/data/db/entity/PendingOperationEntity;", "", "id", "", "locationId", "", "operationType", "Lcom/pevplugshare/data/db/entity/PendingOperationType;", "operationData", "timestamp", "retryCount", "", "maxRetries", "lastRetryTime", "errorMessage", "(JLjava/lang/String;Lcom/pevplugshare/data/db/entity/PendingOperationType;Ljava/lang/String;JIILjava/lang/Long;Ljava/lang/String;)V", "getErrorMessage", "()Ljava/lang/String;", "getId", "()J", "getLastRetryTime", "()Ljava/lang/Long;", "Ljava/lang/Long;", "getLocationId", "getMaxRetries", "()I", "getOperationData", "getOperationType", "()Lcom/pevplugshare/data/db/entity/PendingOperationType;", "getRetryCount", "getTimestamp", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "(JLjava/lang/String;Lcom/pevplugshare/data/db/entity/PendingOperationType;Ljava/lang/String;JIILjava/lang/Long;Ljava/lang/String;)Lcom/pevplugshare/data/db/entity/PendingOperationEntity;", "equals", "", "other", "hashCode", "toString", "app_debug"})
@androidx.room.Entity(tableName = "pending_operations", foreignKeys = {@androidx.room.ForeignKey(entity = com.pevplugshare.data.db.entity.LocationEntity.class, parentColumns = {"id"}, childColumns = {"locationId"}, onDelete = 5)})
public final class PendingOperationEntity {
    @androidx.room.PrimaryKey(autoGenerate = true)
    private final long id = 0L;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String locationId = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.db.entity.PendingOperationType operationType = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String operationData = null;
    private final long timestamp = 0L;
    private final int retryCount = 0;
    private final int maxRetries = 0;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Long lastRetryTime = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String errorMessage = null;
    
    public PendingOperationEntity(long id, @org.jetbrains.annotations.NotNull()
    java.lang.String locationId, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationType operationType, @org.jetbrains.annotations.NotNull()
    java.lang.String operationData, long timestamp, int retryCount, int maxRetries, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastRetryTime, @org.jetbrains.annotations.Nullable()
    java.lang.String errorMessage) {
        super();
    }
    
    public final long getId() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getLocationId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.db.entity.PendingOperationType getOperationType() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getOperationData() {
        return null;
    }
    
    public final long getTimestamp() {
        return 0L;
    }
    
    public final int getRetryCount() {
        return 0;
    }
    
    public final int getMaxRetries() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getLastRetryTime() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getErrorMessage() {
        return null;
    }
    
    public final long component1() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.db.entity.PendingOperationType component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component4() {
        return null;
    }
    
    public final long component5() {
        return 0L;
    }
    
    public final int component6() {
        return 0;
    }
    
    public final int component7() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long component8() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.data.db.entity.PendingOperationEntity copy(long id, @org.jetbrains.annotations.NotNull()
    java.lang.String locationId, @org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.db.entity.PendingOperationType operationType, @org.jetbrains.annotations.NotNull()
    java.lang.String operationData, long timestamp, int retryCount, int maxRetries, @org.jetbrains.annotations.Nullable()
    java.lang.Long lastRetryTime, @org.jetbrains.annotations.Nullable()
    java.lang.String errorMessage) {
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