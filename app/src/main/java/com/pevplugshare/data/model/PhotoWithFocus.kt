package com.pevplugshare.data.model

import android.graphics.RectF

/**
 * Represents a photo with optional focus area for AI analysis
 */
data class PhotoWithFocus(
    val filePath: String,
    val focusRect: RectF? = null
) {
    fun hasFocus(): Boolean = focusRect != null
}
