## Image Editor Refactoring To-Do List

This list outlines the incremental steps required to refactor the image editor as per the requirements.

### 1. Initial Setup & UI Structure (Verification/Correction)

*   **Verify `app/src/main/res/layout/activity_image_editor.xml`:**
    *   Ensure `TextView` with `id="@+id/instructionTextView"` is present at the top.
    *   Confirm `HorizontalScrollView` with `id="@+id/mainControlsScrollView"` containing `LinearLayout` with `id="@+id/mainControlsLayout"` for main buttons.
    *   Confirm `LinearLayout` with `id="@+id/cropControlsLayout"` for crop sub-menu buttons, with `android:visibility="gone"`.
    *   Confirm `LinearLayout` with `id="@+id/focusControlsLayout"` for focus sub-menu buttons, with `android:visibility="gone"`.
    *   Verify all new buttons are present with correct IDs:
        *   Main: `saveNewButton`, `cancelButton`
        *   Crop Sub-menu: `cropCancelButton`, `cropResetButton`, `cropApplyButton`
        *   Focus Sub-menu: `focusCancelButton`, `focusResetButton`, `focusApplyButton`
        *   (Note: `toggleFocusButton` and `clearFocusButton` should be replaced by `focusButton` in main controls).
*   **Verify `app/src/main/java/com/a42r/pevplugshare/ui/edit/ImageEditorActivity.kt`:**
    *   Ensure all new UI elements (`instructionTextView`, `mainControlsLayout`, `cropControlsLayout`, `focusControlsLayout`, `saveNewButton`, `cancelButton`, `cropCancelButton`, `cropResetButton`, `cropApplyButton`, `focusCancelButton`, `focusResetButton`, `focusApplyButton`) are declared as member variables.
    *   Confirm `EditorMode` enum (`MAIN`, `CROP`, `FOCUS`) is defined.
    *   Confirm `currentMode: EditorMode` state variable is declared and initialized to `EditorMode.MAIN`.
    *   Verify `onCreate` method correctly finds all new views by their IDs using `findViewById`.
    *   Ensure correct imports are present: `android.view.View`, `android.graphics.PointF`, `android.content.Intent`.
    *   Confirm removal of old unused variables: `isDrawingFocus`, `focusStartX`, `focusStartY`.

### 2. Core Logic - Mode Management & UI Updates

*   **Implement `updateUIForMode(mode: EditorMode)` function:**
    *   Set visibility of `mainControlsLayout`, `cropControlsLayout`, `focusControlsLayout` based on the `mode`.
    *   Update `instructionTextView.text` with appropriate guidance messages for each `EditorMode`.
*   **Modify `setupUI()` function:**
    *   Attach `OnClickListener`s to all main menu buttons (`zoomInButton`, `zoomOutButton`, `rotateLeftButton`, `rotateRightButton`, `focusButton`, `cropButton`, `resetButton`, `saveButton`, `saveNewButton`, `cancelButton`).
    *   Attach `OnClickListener`s to all crop sub-menu buttons (`cropCancelButton`, `cropResetButton`, `cropApplyButton`).
    *   Attach `OnClickListener`s to all focus sub-menu buttons (`focusCancelButton`, `focusResetButton`, `focusApplyButton`).
    *   Ensure `imageView.setOnTouchListener` correctly dispatches touch events to `handleRectDrawing` based on `currentMode`.

### 3. Drawing & Interaction Logic - Crop & Focus Regions

*   **Implement `getDefaultRect(): RectF`:**
    *   Calculates and returns a default `RectF` (e.g., 70% of image size, centered) for initial crop/focus regions.
*   **Implement `imageViewToBitmapCoords(x: Float, y: Float): PointF`:**
    *   Converts screen touch coordinates (`x`, `y`) on the `ImageView` to corresponding coordinates on the underlying `editBitmap`.
*   **Implement `handleRectDrawing(event: MotionEvent, type: RectType): Boolean`:**
    *   Manages touch events (`ACTION_DOWN`, `ACTION_MOVE`, `ACTION_UP`) for drawing/resizing either `cropRect` or `focusRect`.
    *   Calls `getActiveHandle` to identify interaction point.
    *   Calls `adjustRect` to modify the rectangle.
    *   Triggers `refreshImageWithOverlay` to update the UI.
*   **Implement `getActiveHandle(x: Float, y: Float, rect: RectF, handleSize: Float): Int`:**
    *   Determines if a touch event is on a corner handle (0-3) or inside the rectangle (4 for move). Returns -1 if no active handle.
*   **Implement `adjustRect(originalRect: RectF, handle: Int, newX: Float, newY: Float): RectF`:**
    *   Modifies the `originalRect` based on the `handle` being dragged and the `newX`, `newY` coordinates.
    *   Ensures the rectangle stays within `editBitmap` bounds and maintains a minimum size.
*   **Implement `refreshImageWithOverlay(isActive: Boolean = false, type: RectType? = null)`:**
    *   Creates a mutable copy of `editBitmap`.
    *   Draws `cropRect` (red borders, square handles) if `cropRect` is not null. Changes color to brighter red if `isActive` and `type == RectType.CROP`.
    *   Draws `focusRect` (yellow borders, square handles) if `focusRect` is not null. Changes color to brighter yellow if `isActive` and `type == RectType.FOCUS`.
    *   Sets the modified bitmap to `imageView`.
*   **Implement `drawHandles(canvas: Canvas, rect: RectF, color: Int)`:**
    *   Helper function to draw the four square handles on the corners of a given `RectF`.

### 4. Functionality - Crop Operation

*   **`cropButton.setOnClickListener`:**
    *   Set `currentMode = EditorMode.CROP`.
    *   Call `updateUIForMode(currentMode)`.
    *   Initialize `cropRect` with `getDefaultRect()`.
    *   Call `refreshImageWithOverlay()`.
*   **`cropCancelButton.setOnClickListener`:**
    *   Set `currentMode = EditorMode.MAIN`.
    *   Set `cropRect = null` (clear crop selection).
    *   Call `updateUIForMode(currentMode)`.
    *   Call `refreshImageWithOverlay()`.
*   **`cropResetButton.setOnClickListener`:**
    *   Reset `cropRect` to `getDefaultRect()`.
    *   Call `refreshImageWithOverlay()`.
*   **`cropApplyButton.setOnClickListener`:**
    *   Call `applyCrop()`.
    *   Set `currentMode = EditorMode.MAIN`.
    *   Call `updateUIForMode(currentMode)`.
    *   Call `refreshImageWithOverlay()`.
*   **Implement `applyCrop()`:**
    *   Perform the actual bitmap cropping using the coordinates from `cropRect`.
    *   Set `cropRect = null` after applying.
    *   Reset `currentZoom` and `imageView.scaleX/Y` to 1.0f.
    *   Display a "Image cropped" `Toast` message.

### 5. Functionality - Focus Operation

*   **`focusButton.setOnClickListener`:**
    *   Set `currentMode = EditorMode.FOCUS`.
    *   Call `updateUIForMode(currentMode)`.
    *   Initialize `focusRect` with `getDefaultRect()`.
    *   Call `refreshImageWithOverlay()`.
*   **`focusCancelButton.setOnClickListener`:**
    *   Set `currentMode = EditorMode.MAIN`.
    *   Set `focusRect = null` (clear focus selection).
    *   Call `updateUIForMode(currentMode)`.
    *   Call `refreshImageWithOverlay()`.
*   **`focusResetButton.setOnClickListener`:**
    *   Reset `focusRect` to `getDefaultRect()`.
    *   Call `refreshImageWithOverlay()`.
*   **`focusApplyButton.setOnClickListener`:**
    *   Set `currentMode = EditorMode.MAIN`.
    *   Call `updateUIForMode(currentMode)`.
    *   Call `refreshImageWithOverlay()`.
    *   Display a "Focus area saved" `Toast` message. (The `focusRect` is already stored as a member variable for later use).

### 6. Functionality - Main Menu Operations

*   **`resetButton.setOnClickListener`:**
    *   Reset `editBitmap` to a copy of `originalBitmap`.
    *   Set `focusRect = null` and `cropRect = null`.
    *   Reset `currentZoom` and `imageView.scaleX/Y` to 1.0f.
    *   Call `refreshImageWithOverlay()`.
    *   Display an "All edits reset" `Toast` message.
*   **`saveButton.setOnClickListener`:**
    *   Call `saveImage(false)`.
*   **`saveNewButton.setOnClickListener`:**
    *   Call `saveImage(true)`.
*   **`cancelButton.setOnClickListener`:**
    *   Set `setResult(RESULT_CANCELED)`.
    *   Call `finish()`.
*   **Modify `saveImage(saveAsNew: Boolean)`:**
    *   If `saveAsNew` is `true`, generate a new unique filename (e.g., by appending a timestamp before the extension).
    *   Ensure the `Intent` returned with `RESULT_OK` includes `EXTRA_OUTPUT_PATH` with the *actual* path of the saved file (especially important for "Save New").

### 7. Clean up

*   Remove any remaining unused old code (e.g., `handleFocusDrawing`, `refreshImageWithFocus`, the old `cropImage` function if it still exists as a separate function).
*   Review and ensure all `Log.d` and `Log.e` statements are appropriate and helpful for debugging.
*   Ensure all `Toast` messages are user-friendly.
*   Verify that the `addRotationControls()` is still correctly called and functional.
*   Check for any potential memory leaks related to Bitmaps (e.g., ensuring `recycle()` is called when appropriate, though modern Android often handles this better).
*   Consider adding comments for complex logic, especially for the `handleRectDrawing` and `adjustRect` functions.
