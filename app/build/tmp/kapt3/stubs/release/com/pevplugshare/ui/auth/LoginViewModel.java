package com.pevplugshare.ui.auth;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0007\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\f\u001a\u00020\rJ \u0010\u000e\u001a\u00020\r2\u0018\u0010\u000f\u001a\u0014\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00120\u0011\u0012\u0004\u0012\u00020\r0\u0010J\u000e\u0010\u0013\u001a\u00020\u0014H\u0086@\u00a2\u0006\u0002\u0010\u0015J\u000e\u0010\u0016\u001a\u00020\u0014H\u0086@\u00a2\u0006\u0002\u0010\u0015J\u001e\u0010\u0017\u001a\u00020\r2\u0006\u0010\u0018\u001a\u00020\u00122\u0006\u0010\u0019\u001a\u00020\u00122\u0006\u0010\u001a\u001a\u00020\u0014R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u001b"}, d2 = {"Lcom/pevplugshare/ui/auth/LoginViewModel;", "Landroidx/lifecycle/ViewModel;", "authRepository", "Lcom/pevplugshare/domain/repository/AuthRepository;", "(Lcom/pevplugshare/domain/repository/AuthRepository;)V", "_loginState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/pevplugshare/ui/auth/LoginState;", "loginState", "Lkotlinx/coroutines/flow/StateFlow;", "getLoginState", "()Lkotlinx/coroutines/flow/StateFlow;", "attemptAutoLogin", "", "getFavoritesCategories", "onResult", "Lkotlin/Function1;", "", "", "hasStoredCredentials", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "hasValidSessionToken", "loginWithCredentials", "username", "password", "rememberMe", "app_release"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class LoginViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.pevplugshare.domain.repository.AuthRepository authRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pevplugshare.ui.auth.LoginState> _loginState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.auth.LoginState> loginState = null;
    
    @javax.inject.Inject()
    public LoginViewModel(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.domain.repository.AuthRepository authRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pevplugshare.ui.auth.LoginState> getLoginState() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object hasStoredCredentials(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object hasValidSessionToken(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    public final void attemptAutoLogin() {
    }
    
    public final void loginWithCredentials(@org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    java.lang.String password, boolean rememberMe) {
    }
    
    public final void getFavoritesCategories(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.util.List<java.lang.String>, kotlin.Unit> onResult) {
    }
}