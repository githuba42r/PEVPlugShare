package com.pevplugshare.ui.edit;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000v\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\b\n\u0000\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\b\u0007\u0018\u0000 62\u00020\u0001:\u00016B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010#\u001a\u00020\u001eH\u0002J\u0012\u0010$\u001a\u00020%2\b\u0010&\u001a\u0004\u0018\u00010\'H\u0016J$\u0010(\u001a\u00020)2\u0006\u0010*\u001a\u00020+2\b\u0010,\u001a\u0004\u0018\u00010-2\b\u0010&\u001a\u0004\u0018\u00010\'H\u0016J\b\u0010.\u001a\u00020\u001eH\u0016J\b\u0010/\u001a\u00020\u001eH\u0016J\u001a\u00100\u001a\u00020\u001e2\u0006\u00101\u001a\u00020)2\b\u0010&\u001a\u0004\u0018\u00010\'H\u0016J\u0010\u00102\u001a\u00020\u001e2\u0006\u00103\u001a\u00020\bH\u0002J\b\u00104\u001a\u00020\u001eH\u0002J\b\u00105\u001a\u00020\u001eH\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\t\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\n\u0010\u000bR\u0010\u0010\f\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R \u0010\u000f\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\b0\u00110\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u0013\u001a\u00020\u00148\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0015\u0010\u0016\"\u0004\b\u0017\u0010\u0018R\u000e\u0010\u0019\u001a\u00020\u001aX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001b\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u001c\u001a\u0010\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u001e\u0018\u00010\u001dX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001f\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010 \u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010!\u001a\b\u0012\u0004\u0012\u00020\b0\"X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u00067"}, d2 = {"Lcom/pevplugshare/ui/edit/AiDescriptionDialogFragment;", "Landroidx/fragment/app/DialogFragment;", "()V", "_binding", "Lcom/pevplugshare/databinding/DialogAiDescriptionBinding;", "accessType", "Lcom/pevplugshare/data/model/LocationAccessType;", "additionalInfo", "", "binding", "getBinding", "()Lcom/pevplugshare/databinding/DialogAiDescriptionBinding;", "capacity", "chargingPoints", "", "conversationHistory", "", "Lkotlin/Pair;", "currentDescription", "descriptionGenerator", "Lcom/pevplugshare/ai/LocationDescriptionGenerator;", "getDescriptionGenerator", "()Lcom/pevplugshare/ai/LocationDescriptionGenerator;", "setDescriptionGenerator", "(Lcom/pevplugshare/ai/LocationDescriptionGenerator;)V", "hasFee", "", "locationName", "onDescriptionAccepted", "Lkotlin/Function1;", "", "openingHours", "originalDescription", "socketTypes", "", "generateInitialDescription", "onCreateDialog", "Landroid/app/Dialog;", "savedInstanceState", "Landroid/os/Bundle;", "onCreateView", "Landroid/view/View;", "inflater", "Landroid/view/LayoutInflater;", "container", "Landroid/view/ViewGroup;", "onDestroyView", "onStart", "onViewCreated", "view", "refineDescription", "userMessage", "setupUI", "showContent", "Companion", "app_debug"})
public final class AiDescriptionDialogFragment extends androidx.fragment.app.DialogFragment {
    @javax.inject.Inject()
    public com.pevplugshare.ai.LocationDescriptionGenerator descriptionGenerator;
    @org.jetbrains.annotations.Nullable()
    private com.pevplugshare.databinding.DialogAiDescriptionBinding _binding;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String currentDescription = "";
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<kotlin.Pair<java.lang.String, java.lang.String>> conversationHistory = null;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String locationName = "";
    @org.jetbrains.annotations.NotNull()
    private com.pevplugshare.data.model.LocationAccessType accessType = com.pevplugshare.data.model.LocationAccessType.PUBLIC;
    private boolean hasFee = false;
    private int chargingPoints = 1;
    @org.jetbrains.annotations.NotNull()
    private java.util.List<java.lang.String> socketTypes;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String openingHours;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String capacity;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String additionalInfo;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String originalDescription;
    @org.jetbrains.annotations.Nullable()
    private kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onDescriptionAccepted;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "AiDescriptionDialog";
    @org.jetbrains.annotations.NotNull()
    public static final com.pevplugshare.ui.edit.AiDescriptionDialogFragment.Companion Companion = null;
    
    public AiDescriptionDialogFragment() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pevplugshare.ai.LocationDescriptionGenerator getDescriptionGenerator() {
        return null;
    }
    
    public final void setDescriptionGenerator(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.ai.LocationDescriptionGenerator p0) {
    }
    
    private final com.pevplugshare.databinding.DialogAiDescriptionBinding getBinding() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public android.view.View onCreateView(@org.jetbrains.annotations.NotNull()
    android.view.LayoutInflater inflater, @org.jetbrains.annotations.Nullable()
    android.view.ViewGroup container, @org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public android.app.Dialog onCreateDialog(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
        return null;
    }
    
    @java.lang.Override()
    public void onViewCreated(@org.jetbrains.annotations.NotNull()
    android.view.View view, @org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    public void onStart() {
    }
    
    private final void setupUI() {
    }
    
    private final void generateInitialDescription() {
    }
    
    private final void refineDescription(java.lang.String userMessage) {
    }
    
    private final void showContent() {
    }
    
    @java.lang.Override()
    public void onDestroyView() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010 \n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002Jp\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00040\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u00042\b\u0010\u0011\u001a\u0004\u0018\u00010\u00042\b\u0010\u0012\u001a\u0004\u0018\u00010\u00042\b\u0010\u0013\u001a\u0004\u0018\u00010\u00042\u0012\u0010\u0014\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00160\u0015R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0017"}, d2 = {"Lcom/pevplugshare/ui/edit/AiDescriptionDialogFragment$Companion;", "", "()V", "TAG", "", "newInstance", "Lcom/pevplugshare/ui/edit/AiDescriptionDialogFragment;", "locationName", "accessType", "Lcom/pevplugshare/data/model/LocationAccessType;", "hasFee", "", "chargingPoints", "", "socketTypes", "", "openingHours", "capacity", "additionalInfo", "originalDescription", "onAccept", "Lkotlin/Function1;", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pevplugshare.ui.edit.AiDescriptionDialogFragment newInstance(@org.jetbrains.annotations.NotNull()
        java.lang.String locationName, @org.jetbrains.annotations.NotNull()
        com.pevplugshare.data.model.LocationAccessType accessType, boolean hasFee, int chargingPoints, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.String> socketTypes, @org.jetbrains.annotations.Nullable()
        java.lang.String openingHours, @org.jetbrains.annotations.Nullable()
        java.lang.String capacity, @org.jetbrains.annotations.Nullable()
        java.lang.String additionalInfo, @org.jetbrains.annotations.Nullable()
        java.lang.String originalDescription, @org.jetbrains.annotations.NotNull()
        kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onAccept) {
            return null;
        }
    }
}