package com.pevplugshare.ui.common;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006H\u0016J\u000e\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u0006H\u0016J\u000e\u0010\n\u001a\b\u0012\u0004\u0012\u00020\t0\u0006H\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000b"}, d2 = {"Lcom/pevplugshare/ui/common/OfflineStatusProviderImpl;", "Lcom/pevplugshare/ui/common/OfflineStatusProvider;", "connectivityObserver", "Lcom/pevplugshare/data/network/ConnectivityObserver;", "(Lcom/pevplugshare/data/network/ConnectivityObserver;)V", "getNetworkStatus", "Lkotlinx/coroutines/flow/Flow;", "Lcom/pevplugshare/data/network/NetworkStatus;", "isOffline", "", "isOnline", "app_release"})
public final class OfflineStatusProviderImpl implements com.pevplugshare.ui.common.OfflineStatusProvider {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.data.network.ConnectivityObserver connectivityObserver = null;
    
    public OfflineStatusProviderImpl(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.data.network.ConnectivityObserver connectivityObserver) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<com.pevplugshare.data.network.NetworkStatus> getNetworkStatus() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.lang.Boolean> isOffline() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.lang.Boolean> isOnline() {
        return null;
    }
}