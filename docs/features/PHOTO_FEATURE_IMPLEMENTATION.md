# Photo Feature Implementation Plan

## Overview
Add ability to capture, store, and view up to 5 images per location with camera integration, image optimization, and swipe-through gallery.

## Components Completed
✅ ImageUtils.kt - Utility for image resizing and storage
✅ modal_image_gallery.xml - Gallery overlay layout
✅ item_fullscreen_image.xml - ViewPager item layout
✅ Data models already support photos (Photo class exists)

## Implementation Steps

### 1. Update Database Schema (LocationEntity)
Add imagePaths field to LocationEntity in Entities.kt:
```kotlin
@ColumnInfo(name = "image_paths") var imagePaths: String? = null  // JSON array of paths
```

Add converter:
```kotlin
@TypeConverter
fun fromImagePathsList(paths: List<String>?): String? {
    return paths?.joinToString(",")
}

@TypeConverter
fun toImagePathsList(pathsString: String?): List<String>? {
    return pathsString?.split(",")?.filter { it.isNotBlank() }
}
```

### 2. Update Edit/Add Location Layout
Add image capture section after the Additional Info field:

```xml
<!-- Photo Section -->
<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="16dp"
    android:text="Photos (Optional)"
    android:textStyle="bold" />

<HorizontalScrollView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp">
    
    <LinearLayout
        android:id="@+id/photosContainer"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal">
        
        <!-- Add Photo Button -->
        <com.google.android.material.card.MaterialCardView
            android:id="@+id/addPhotoButton"
            android:layout_width="100dp"
            android:layout_height="100dp"
            android:layout_margin="4dp"
            android:clickable="true"
            android:focusable="true"
            app:cardCornerRadius="8dp">
            
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:orientation="vertical"
                android:gravity="center"
                android:background="?attr/selectableItemBackground">
                
                <ImageView
                    android:layout_width="48dp"
                    android:layout_height="48dp"
                    android:src="@drawable/ic_add"
                    android:tint="?attr/colorPrimary" />
                
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Add Photo"
                    android:textSize="12sp"
                    android:layout_marginTop="4dp" />
            </LinearLayout>
        </MaterialCardView>
        
        <!-- Photo thumbnails will be added here dynamically -->
    </LinearLayout>
</HorizontalScrollView>

<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="4dp"
    android:text="Add up to 5 photos to help identify the charging location"
    android:textSize="12sp"
    android:textStyle="italic"
    android:textColor="?android:attr/textColorSecondary" />
```

### 3. EditLocationActivity Changes

Add fields:
```kotlin
private val photoUris = mutableListOf<String>()
private val maxPhotos = 5

private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
    if (success) {
        currentPhotoUri?.let { uri ->
            processAndSavePhoto(uri)
        }
    }
}

private val cameraPermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { granted ->
    if (granted) {
        launchCamera()
    } else {
        Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
    }
}
```

Add methods:
```kotlin
private fun setupPhotoSection() {
    binding.addPhotoButton.setOnClickListener {
        if (photoUris.size < maxPhotos) {
            checkCameraPermissionAndLaunch()
        } else {
            Toast.makeText(this, "Maximum 5 photos allowed", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Load existing photos if in edit mode
    if (!isAddMode) {
        loadExistingPhotos()
    }
}

private fun checkCameraPermissionAndLaunch() {
    when {
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == 
            PackageManager.PERMISSION_GRANTED -> {
            launchCamera()
        }
        else -> {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }
}

private fun launchCamera() {
    val photoFile = createImageFile()
    photoFile?.let { file ->
        currentPhotoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )
        tempPhotoFile = file
        takePicture.launch(currentPhotoUri)
    }
}

private fun createImageFile(): File? {
    return try {
        val timestamp = System.currentTimeMillis()
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
    } catch (e: IOException) {
        null
    }
}

private fun processAndSavePhoto(uri: Uri) {
    lifecycleScope.launch(Dispatchers.IO) {
        tempPhotoFile?.let { sourceFile ->
            val resizedFile = ImageUtils.resizeAndSaveImage(
                this@EditLocationActivity,
                sourceFile,
                locationId ?: generateTempLocationId(),
                photoUris.size
            )
            
            withContext(Dispatchers.Main) {
                resizedFile?.let {
                    photoUris.add(it.absolutePath)
                    addPhotoThumbnail(it)
                    // Delete temp file
                    sourceFile.delete()
                    tempPhotoFile = null
                }
            }
        }
    }
}

private fun addPhotoThumbnail(file: File) {
    val thumbnailView = layoutInflater.inflate(R.layout.item_photo_thumbnail, null)
    val imageView = thumbnailView.findViewById<ImageView>(R.id.thumbnailImage)
    val removeButton = thumbnailView.findViewById<ImageView>(R.id.removePhotoButton)
    
    Glide.with(this)
        .load(file)
        .centerCrop()
        .into(imageView)
    
    val index = photoUris.size - 1
    removeButton.setOnClickListener {
        removePhoto(index)
    }
    
    thumbnailView.setOnClickListener {
        // Optional: Show full-size image
    }
    
    binding.photosContainer.addView(thumbnailView, binding.photosContainer.childCount - 1)
    updateAddButtonVisibility()
}

private fun removePhoto(index: Int) {
    if (index < photoUris.size) {
        val path = photoUris[index]
        File(path).delete()
        photoUris.removeAt(index)
        refreshPhotoThumbnails()
    }
}

private fun updateAddButtonVisibility() {
    binding.addPhotoButton.visibility = if (photoUris.size >= maxPhotos) {
        View.GONE
    } else {
        View.VISIBLE
    }
}
```

### 4. Create Photo Thumbnail Layout

Create `item_photo_thumbnail.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="100dp"
    android:layout_height="100dp"
    android:layout_margin="4dp"
    app:cardCornerRadius="8dp">
    
    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">
        
        <ImageView
            android:id="@+id/thumbnailImage"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:scaleType="centerCrop" />
        
        <ImageView
            android:id="@+id/removePhotoButton"
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:layout_gravity="top|end"
            android:layout_margin="4dp"
            android:src="@drawable/ic_clear"
            android:background="@drawable/circle_background_red"
            android:padding="4dp"
            android:tint="@android:color/white" />
    </FrameLayout>
</com.google.android.material.card.MaterialCardView>
```

### 5. Location List Item - Add Photo Indicator

Update item_location.xml to show photo icon if photos exist:
```xml
<ImageView
    android:id="@+id/hasPhotosIndicator"
    android:layout_width="20dp"
    android:layout_height="20dp"
    android:src="@drawable/ic_photo"
    android:visibility="gone"
    android:tint="?attr/colorPrimary" />
```

In LocationAdapter:
```kotlin
holder.hasPhotosIndicator.visibility = if (location.photos.isNotEmpty()) {
    View.VISIBLE
} else {
    View.GONE
}

holder.hasPhotosIndicator.setOnClickListener {
    onPhotoClick(location)
}
```

### 6. Image Gallery Fragment/Dialog

Create ImageGalleryAdapter:
```kotlin
class ImageGalleryAdapter(private val imagePaths: List<String>) : 
    RecyclerView.Adapter<ImageGalleryAdapter.ImageViewHolder>() {
    
    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.fullscreenImage)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fullscreen_image, parent, false)
        return ImageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(File(imagePaths[position]))
            .into(holder.imageView)
    }
    
    override fun getItemCount() = imagePaths.size
}
```

Show gallery in MainActivity:
```kotlin
private fun showImageGallery(location: Location, startPosition: Int = 0) {
    val imagePaths = ImageUtils.getLocationImages(this, location.id).map { it.absolutePath }
    if (imagePaths.isEmpty()) return
    
    binding.imageGalleryOverlay.visibility = View.VISIBLE
    
    val adapter = ImageGalleryAdapter(imagePaths)
    binding.imageViewPager.adapter = adapter
    binding.imageViewPager.setCurrentItem(startPosition, false)
    
    binding.imageCounter.text = "${startPosition + 1} / ${imagePaths.size}"
    
    binding.imageViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            binding.imageCounter.text = "${position + 1} / ${imagePaths.size}"
        }
    })
    
    binding.galleryToolbar.setNavigationOnClickListener {
        binding.imageGalleryOverlay.visibility = View.GONE
    }
}
```

### 7. FileProvider Configuration

Add to AndroidManifest.xml:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.provider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

Create res/xml/file_paths.xml:
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path name="pictures" path="Pictures/" />
    <files-path name="location_images" path="location_images/" />
</paths>
```

### 8. Dependencies
Add to build.gradle.kts if not present:
```kotlin
implementation("com.github.bumptech.glide:glide:4.16.0")
implementation("androidx.viewpager2:viewpager2:1.0.0")
```

## Testing Checklist
- [ ] Camera permission flow
- [ ] Photo capture works
- [ ] Images are resized (check file sizes)
- [ ] Maximum 5 photos enforced
- [ ] Photos persist across app restarts
- [ ] Photo deletion works
- [ ] Gallery swipe navigation works
- [ ] Photos sync to OSMAnd (optional)
- [ ] Photos deleted when location deleted

## Future Enhancements
- Add photo captions
- Pinch-to-zoom in gallery
- Share photos
- Cloud backup of photos
- Photo editing (crop, rotate)
