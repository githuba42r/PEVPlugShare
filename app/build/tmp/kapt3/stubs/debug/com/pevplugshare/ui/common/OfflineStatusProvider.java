package com.pevplugshare.ui.common;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\bf\u0018\u0000 \b2\u00020\u0001:\u0001\bJ\u000e\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H&J\u000e\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00060\u0003H&J\u000e\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00060\u0003H&\u00a8\u0006\t"}, d2 = {"Lcom/pevplugshare/ui/common/OfflineStatusProvider;", "", "getNetworkStatus", "Lkotlinx/coroutines/flow/Flow;", "Lcom/pevplugshare/data/network/NetworkStatus;", "isOffline", "", "isOnline", "Companion", "app_debug"})
public abstract interface OfflineStatusProvider {
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.ui.common.OfflineStatusProvider.Companion Companion = null;
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.pevplugshare.data.network.NetworkStatus> getNetworkStatus();
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.lang.Boolean> isOffline();
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.lang.Boolean> isOnline();
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/pevplugshare/ui/common/OfflineStatusProvider$Companion;", "", "()V", "create", "Lcom/pevplugshare/ui/common/OfflineStatusProvider;", "connectivityObserver", "Lcom/pevplugshare/data/network/ConnectivityObserver;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pevplugshare.ui.common.OfflineStatusProvider create(@org.jetbrains.annotations.NotNull()
        com.pevplugshare.data.network.ConnectivityObserver connectivityObserver) {
            return null;
        }
    }
}