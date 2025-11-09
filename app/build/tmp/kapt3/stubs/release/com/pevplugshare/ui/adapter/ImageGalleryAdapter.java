package com.pevplugshare.ui.adapter;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001:\u0001\u0011B\u0013\u0012\f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\u0002\u0010\u0006J\b\u0010\u0007\u001a\u00020\bH\u0016J\u0018\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u00022\u0006\u0010\f\u001a\u00020\bH\u0016J\u0018\u0010\r\u001a\u00020\u00022\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\bH\u0016R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Lcom/pevplugshare/ui/adapter/ImageGalleryAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Lcom/pevplugshare/ui/adapter/ImageGalleryAdapter$ImageViewHolder;", "imagePaths", "", "", "(Ljava/util/List;)V", "getItemCount", "", "onBindViewHolder", "", "holder", "position", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "ImageViewHolder", "app_release"})
public final class ImageGalleryAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.pevplugshare.ui.adapter.ImageGalleryAdapter.ImageViewHolder> {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> imagePaths = null;
    
    public ImageGalleryAdapter(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> imagePaths) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public com.pevplugshare.ui.adapter.ImageGalleryAdapter.ImageViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    com.pevplugshare.ui.adapter.ImageGalleryAdapter.ImageViewHolder holder, int position) {
    }
    
    @java.lang.Override()
    public int getItemCount() {
        return 0;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u000b"}, d2 = {"Lcom/pevplugshare/ui/adapter/ImageGalleryAdapter$ImageViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "binding", "Lcom/pevplugshare/databinding/ItemFullscreenImageBinding;", "(Lcom/pevplugshare/databinding/ItemFullscreenImageBinding;)V", "getBinding", "()Lcom/pevplugshare/databinding/ItemFullscreenImageBinding;", "bind", "", "imagePath", "", "app_release"})
    public static final class ImageViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull()
        private final com.pevplugshare.databinding.ItemFullscreenImageBinding binding = null;
        
        public ImageViewHolder(@org.jetbrains.annotations.NotNull()
        com.pevplugshare.databinding.ItemFullscreenImageBinding binding) {
            super(null);
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pevplugshare.databinding.ItemFullscreenImageBinding getBinding() {
            return null;
        }
        
        public final void bind(@org.jetbrains.annotations.NotNull()
        java.lang.String imagePath) {
        }
    }
}