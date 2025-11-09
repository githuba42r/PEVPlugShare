package com.pevplugshare.ui.settings.model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001\u001cBE\u0012\f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u0012\u0012\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\n0\t\u0012\u0012\u0010\u000b\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\n0\t\u00a2\u0006\u0002\u0010\fJ\u0018\u0010\u000e\u001a\u00020\n2\u0006\u0010\u000f\u001a\u00020\u00072\b\b\u0002\u0010\u0010\u001a\u00020\u0011J\b\u0010\u0012\u001a\u00020\u0013H\u0016J\b\u0010\u0014\u001a\u0004\u0018\u00010\u0005J\u0018\u0010\u0015\u001a\u00020\n2\u0006\u0010\u0016\u001a\u00020\u00022\u0006\u0010\u0017\u001a\u00020\u0013H\u0016J\u0018\u0010\u0018\u001a\u00020\u00022\u0006\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u0013H\u0016R\u0014\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\n0\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000b\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\n0\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001d"}, d2 = {"Lcom/pevplugshare/ui/settings/model/ModelSelectionAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Lcom/pevplugshare/ui/settings/model/ModelSelectionAdapter$ViewHolder;", "models", "", "Lcom/pevplugshare/ui/settings/model/AiModel;", "selectedModelId", "", "onModelSelected", "Lkotlin/Function1;", "", "onViewModelLink", "(Ljava/util/List;Ljava/lang/String;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;)V", "filteredModels", "filter", "query", "freeOnly", "", "getItemCount", "", "getSelectedModel", "onBindViewHolder", "holder", "position", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "ViewHolder", "app_debug"})
public final class ModelSelectionAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.pevplugshare.ui.settings.model.ModelSelectionAdapter.ViewHolder> {
    @org.jetbrains.annotations.NotNull()
    private java.util.List<com.pevplugshare.ui.settings.model.AiModel> models;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String selectedModelId;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<com.pevplugshare.ui.settings.model.AiModel, kotlin.Unit> onModelSelected = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<java.lang.String, kotlin.Unit> onViewModelLink = null;
    @org.jetbrains.annotations.NotNull()
    private java.util.List<com.pevplugshare.ui.settings.model.AiModel> filteredModels;
    
    public ModelSelectionAdapter(@org.jetbrains.annotations.NotNull()
    java.util.List<com.pevplugshare.ui.settings.model.AiModel> models, @org.jetbrains.annotations.Nullable()
    java.lang.String selectedModelId, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.pevplugshare.ui.settings.model.AiModel, kotlin.Unit> onModelSelected, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onViewModelLink) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public com.pevplugshare.ui.settings.model.ModelSelectionAdapter.ViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.ui.settings.model.ModelSelectionAdapter.ViewHolder holder, int position) {
    }
    
    @java.lang.Override()
    public int getItemCount() {
        return 0;
    }
    
    public final void filter(@org.jetbrains.annotations.NotNull()
    java.lang.String query, boolean freeOnly) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.pevplugshare.ui.settings.model.AiModel getSelectedModel() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\t\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\bR\u0011\u0010\u000b\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\bR\u0011\u0010\r\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\bR\u0011\u0010\u000f\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\bR\u0011\u0010\u0011\u001a\u00020\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014\u00a8\u0006\u0015"}, d2 = {"Lcom/pevplugshare/ui/settings/model/ModelSelectionAdapter$ViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "view", "Landroid/view/View;", "(Landroid/view/View;)V", "modelCost", "Landroid/widget/TextView;", "getModelCost", "()Landroid/widget/TextView;", "modelDetails", "getModelDetails", "modelLink", "getModelLink", "modelName", "getModelName", "modelProvider", "getModelProvider", "radioButton", "Landroid/widget/RadioButton;", "getRadioButton", "()Landroid/widget/RadioButton;", "app_debug"})
    public static final class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull()
        private final android.widget.RadioButton radioButton = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView modelName = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView modelProvider = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView modelCost = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView modelDetails = null;
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView modelLink = null;
        
        public ViewHolder(@org.jetbrains.annotations.NotNull()
        android.view.View view) {
            super(null);
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.RadioButton getRadioButton() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.TextView getModelName() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.TextView getModelProvider() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.TextView getModelCost() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.TextView getModelDetails() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.TextView getModelLink() {
            return null;
        }
    }
}