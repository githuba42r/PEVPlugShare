package com.pevplugshare.ui.auth;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0011\u001a\u00020\u00122\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u0014J\b\u0010\u0015\u001a\u00020\u0012H\u0014J\b\u0010\u0016\u001a\u00020\u0012H\u0002J\b\u0010\u0017\u001a\u00020\u0012H\u0002J\b\u0010\u0018\u001a\u00020\u0012H\u0002J\b\u0010\u0019\u001a\u00020\u0012H\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u000b\u001a\u00020\f8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000f\u0010\u0010\u001a\u0004\b\r\u0010\u000e\u00a8\u0006\u001a"}, d2 = {"Lcom/pevplugshare/ui/auth/LoginActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "authErrorDialog", "Landroidx/appcompat/app/AlertDialog;", "authErrorDismissTimer", "Lkotlinx/coroutines/Job;", "binding", "Lcom/pevplugshare/databinding/ActivityLoginBinding;", "rememberMeConfirmed", "", "viewModel", "Lcom/pevplugshare/ui/auth/LoginViewModel;", "getViewModel", "()Lcom/pevplugshare/ui/auth/LoginViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "showAutoLoginState", "showBackgroundAuthenticationFailedModal", "showLoginForm", "showRememberMeWarning", "app_debug"})
public final class LoginActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.pevplugshare.databinding.ActivityLoginBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    private boolean rememberMeConfirmed = false;
    @org.jetbrains.annotations.Nullable()
    private androidx.appcompat.app.AlertDialog authErrorDialog;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job authErrorDismissTimer;
    
    public LoginActivity() {
        super();
    }
    
    private final com.pevplugshare.ui.auth.LoginViewModel getViewModel() {
        return null;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
    
    private final void showRememberMeWarning() {
    }
    
    private final void showBackgroundAuthenticationFailedModal() {
    }
    
    private final void showAutoLoginState() {
    }
    
    private final void showLoginForm() {
    }
}