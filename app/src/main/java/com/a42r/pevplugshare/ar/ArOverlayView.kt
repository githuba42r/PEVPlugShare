package com.a42r.pevplugshare.ar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Overlay view for rendering AR target circle and directional indicators
 */
class ArOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.argb(200, 0, 255, 100)
        strokeCap = Paint.Cap.ROUND
    }
    
    private val targetFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(50, 0, 255, 100)
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }
    
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(200, 0, 255, 100)
    }
    
    private val pulseAnimator = ValueAnimatorWrapper()
    
    // Target position on screen (null if off-screen)
    var targetX: Float? = null
    var targetY: Float? = null
    
    // Distance and height info
    var distance: String? = null
    var heightInfo: String? = null
    
    // Off-screen arrow direction (in degrees, 0 = up)
    var offScreenDirection: Float? = null
    
    // Pulse animation phase (0.0 to 1.0)
    private var pulsePhase = 0f
    
    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        startPulseAnimation()
    }
    
    private fun startPulseAnimation() {
        pulseAnimator.start(
            duration = 2000L,
            update = { value ->
                pulsePhase = value
                invalidate()
            }
        )
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val tx = targetX
        val ty = targetY
        
        if (tx != null && ty != null) {
            // Target is on screen - draw glowing circle
            drawTargetCircle(canvas, tx, ty)
            drawTargetInfo(canvas, tx, ty)
        } else if (offScreenDirection != null) {
            // Target is off-screen - draw directional arrow
            drawOffScreenArrow(canvas, offScreenDirection!!)
        }
    }
    
    private fun drawTargetCircle(canvas: Canvas, x: Float, y: Float) {
        // Pulsing effect: vary radius
        val baseRadius = 60f
        val pulseRadius = baseRadius + (20f * pulsePhase)
        
        // Outer glow
        val glowPaint = Paint(targetPaint).apply {
            strokeWidth = 12f
            alpha = (100 * (1f - pulsePhase)).toInt()
        }
        canvas.drawCircle(x, y, pulseRadius + 10f, glowPaint)
        
        // Main circle
        canvas.drawCircle(x, y, baseRadius, targetFillPaint)
        canvas.drawCircle(x, y, baseRadius, targetPaint)
        
        // Crosshair
        val crossSize = 15f
        canvas.drawLine(x - crossSize, y, x + crossSize, y, targetPaint)
        canvas.drawLine(x, y - crossSize, x, y + crossSize, targetPaint)
        
        // Inner ring (pulsing)
        val innerRadius = 30f + (10f * pulsePhase)
        canvas.drawCircle(x, y, innerRadius, targetPaint)
    }
    
    private fun drawTargetInfo(canvas: Canvas, x: Float, y: Float) {
        var offsetY = y + 100f
        
        // Draw distance
        distance?.let {
            canvas.drawText(it, x, offsetY, textPaint)
            offsetY += 45f
        }
        
        // Draw height info
        heightInfo?.let {
            canvas.drawText(it, x, offsetY, textPaint)
        }
    }
    
    private fun drawOffScreenArrow(canvas: Canvas, direction: Float) {
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Position arrow at edge of screen in the direction of target
        val edgeDistance = min(width, height) / 2f - 80f
        val angleRad = Math.toRadians(direction.toDouble())
        
        val arrowX = centerX + (edgeDistance * Math.sin(angleRad)).toFloat()
        val arrowY = centerY - (edgeDistance * Math.cos(angleRad)).toFloat()
        
        canvas.save()
        canvas.translate(arrowX, arrowY)
        canvas.rotate(direction)
        
        // Draw arrow pointing towards target
        val path = Path().apply {
            moveTo(0f, -40f)
            lineTo(-25f, 20f)
            lineTo(0f, 10f)
            lineTo(25f, 20f)
            close()
        }
        
        canvas.drawPath(path, arrowPaint)
        canvas.restore()
        
        // Draw distance below arrow
        distance?.let {
            canvas.drawText(it, arrowX, arrowY + 60f, textPaint)
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator.cancel()
    }
    
    /**
     * Simple wrapper for value animation without depending on Android animator
     */
    private class ValueAnimatorWrapper {
        private var running = false
        private var startTime = 0L
        private var duration = 1000L
        private var updateCallback: ((Float) -> Unit)? = null
        private val handler = android.os.Handler(android.os.Looper.getMainLooper())
        
        private val animationRunnable = object : Runnable {
            override fun run() {
                if (!running) return
                
                val elapsed = System.currentTimeMillis() - startTime
                val fraction = ((elapsed % duration).toFloat() / duration)
                
                updateCallback?.invoke(fraction)
                
                // Schedule next frame
                handler.postDelayed(this, 16) // ~60fps
            }
        }
        
        fun start(duration: Long, update: (Float) -> Unit) {
            this.duration = duration
            this.updateCallback = update
            this.startTime = System.currentTimeMillis()
            this.running = true
            handler.post(animationRunnable)
        }
        
        fun cancel() {
            running = false
            handler.removeCallbacks(animationRunnable)
            updateCallback = null
        }
    }
}
