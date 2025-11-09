package com.pevplugshare.ui.auth;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0005\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0005\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010\u0007J\u000e\u0010\b\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010\u0007J\u000e\u0010\t\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010\u0007J\u000e\u0010\n\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010\u0007R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000b"}, d2 = {"Lcom/pevplugshare/ui/auth/SplashViewModel;", "Landroidx/lifecycle/ViewModel;", "authRepository", "Lcom/pevplugshare/domain/repository/AuthRepository;", "(Lcom/pevplugshare/domain/repository/AuthRepository;)V", "attemptAutoLoginInBackground", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "hasSessionCookie", "hasStoredCredentials", "validateSessionInBackground", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class SplashViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.domain.repository.AuthRepository authRepository = null;
    
    @javax.inject.Inject()
    public SplashViewModel(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.domain.repository.AuthRepository authRepository) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object hasSessionCookie(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object hasStoredCredentials(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object validateSessionInBackground(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object attemptAutoLoginInBackground(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
}