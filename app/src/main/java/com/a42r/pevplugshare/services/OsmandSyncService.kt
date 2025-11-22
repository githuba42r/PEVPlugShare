package com.a42r.pevplugshare.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.a42r.pevplugshare.domain.repository.LocationRepository
import com.a42r.pevplugshare.data.repository.OsmandLocationSyncService
import com.a42r.pevplugshare.ai.LocationDescriptionGenerator
import com.a42r.pevplugshare.data.remote.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import javax.inject.Inject

@AndroidEntryPoint
class OsmandBackgroundSyncService : Service() {

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var osmandSyncService: OsmandLocationSyncService

    @Inject
    lateinit var descriptionGenerator: LocationDescriptionGenerator

    @Inject
    lateinit var tokenManager: TokenManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "OsmandBackgroundSyncService started")

        // Validate intent
        if (intent == null) {
            Log.e(TAG, "Intent is null")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        // Validate location ID
        val locationId = intent.getStringExtra(EXTRA_LOCATION_ID)
        if (locationId.isNullOrBlank()) {
            Log.e(TAG, "Invalid or missing location ID")
            broadcastResult(false, "Invalid location ID")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        // Validate location ID format (should be a UUID)
        try {
            java.util.UUID.fromString(locationId)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid location ID format: $locationId")
            broadcastResult(false, "Invalid location ID format")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val description = intent.getStringExtra(EXTRA_DESCRIPTION)
        val shouldGenerateDescription = intent.getBooleanExtra(EXTRA_GENERATE_DESCRIPTION, false)

        // Validate that we have either a description OR we're generating one
        if (!shouldGenerateDescription && description.isNullOrBlank()) {
            Log.e(TAG, "No description provided and generation not requested")
            broadcastResult(false, "Invalid sync configuration")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        serviceScope.launch {
            try {
                val category = tokenManager.getSelectedCategory() ?: "Charging Locations"
                Log.d(TAG, "━━━ Starting OsmAnd Sync ━━━")
                Log.d(TAG, "Location ID: $locationId")
                Log.d(TAG, "Generate description: $shouldGenerateDescription")

                // Get location from DB
                val result = locationRepository.getLocationsByCategory(category)

                result.onSuccess { locations ->
                    val location = locations.find { it.id == locationId }
                    if (location != null) {
                        Log.d(TAG, "Found location: ${location.name}")

                        // Determine description to use
                        val finalDescription = if (shouldGenerateDescription) {
                            Log.d(TAG, "Generating AI description from DB values...")
                            val socketTypeNames = location.socketTypes.map { it.name }
                            val accessType = location.publicPrivate
                            val hasFee = location.hasFee
                            val gpoCount = location.gpoCount
                            val openingHours = location.openingHours
                            val capacity = location.chargingCosts
                            val additionalInfo = location.additionalInfo
                            val currentDescription = location.description ?: ""
                            val locationName = location.name

                            val generationResult = descriptionGenerator.generateDescription(
                                locationName = locationName,
                                accessType = accessType,
                                hasFee = hasFee,
                                chargingPoints = gpoCount,
                                socketTypes = socketTypeNames,
                                openingHours = openingHours,
                                capacity = capacity,
                                additionalInfo = additionalInfo,
                                description = currentDescription
                            )

                            generationResult.getOrElse { error ->
                                Log.e(TAG, "Failed to generate description: ${error.message}")
                                broadcastResult(false, "Failed to generate description: ${error.message}")
                                stopSelf(startId)
                                return@launch
                            }
                        } else {
                            description ?: location.description ?: ""
                        }

                        // Validate description is not empty
                        if (finalDescription.isBlank()) {
                            Log.e(TAG, "Final description is empty")
                            broadcastResult(false, "Cannot sync with empty description")
                            stopSelf(startId)
                            return@launch
                        }

                        // Validate location has required metadata
                        if (location.name.isBlank()) {
                            Log.e(TAG, "Location name is blank")
                            broadcastResult(false, "Invalid location: missing name")
                            stopSelf(startId)
                            return@launch
                        }

                        if (location.latitude == 0.0 && location.longitude == 0.0) {
                            Log.e(TAG, "Location has invalid coordinates: ${location.latitude}, ${location.longitude}")
                            broadcastResult(false, "Invalid location: missing coordinates")
                            stopSelf(startId)
                            return@launch
                        }

                        Log.d(TAG, "Using description (${finalDescription.length} chars): $finalDescription")
                        Log.d(TAG, "Location validated: name='${location.name}', coords=(${location.latitude}, ${location.longitude})")

                        // Create copy with final description for OsmAnd
                        val locationWithDescription = location.copy(description = finalDescription)

                        // Perform sync
                        osmandSyncService.smartSync(locationWithDescription, category)
                            .onSuccess {
                                Log.d(TAG, "✓ Smart sync SUCCESS")
                                broadcastResult(true, "Successfully synced to OsmAnd")
                            }
                            .onFailure { e ->
                                Log.e(TAG, "✗ Smart sync FAILED: ${e.message}", e)
                                broadcastResult(false, "Sync failed: ${e.message}")
                            }
                    } else {
                        Log.e(TAG, "Location not found in DB")
                        broadcastResult(false, "Location not found")
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Failed to get locations: ${e.message}", e)
                    broadcastResult(false, "Failed to get locations: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in sync: ${e.message}", e)
                broadcastResult(false, "Sync failed: ${e.message}")
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    private fun broadcastResult(success: Boolean, message: String) {
        // Validate message before broadcasting
        val validMessage = if (message.isBlank()) {
            Log.w(TAG, "Attempted to broadcast empty message, using default")
            if (success) "Sync completed" else "Sync failed"
        } else {
            message
        }

        // Create intent with validated data
        val intent = Intent(ACTION_SYNC_RESULT).apply {
            putExtra(EXTRA_SYNC_SUCCESS, success)
            putExtra(EXTRA_SYNC_MESSAGE, validMessage)
            // Set package to ensure it's only received by our app
            setPackage(applicationContext.packageName)
        }

        // Use LocalBroadcastManager for internal app-only communication
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        Log.d(TAG, "Broadcast result: success=$success, message=$validMessage")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "OsmandBackgroundSyncService destroyed")
    }

    companion object {
        private const val TAG = "OsmandBackgroundSync"
        const val EXTRA_LOCATION_ID = "location_id"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_GENERATE_DESCRIPTION = "generate_description"
        const val ACTION_SYNC_RESULT = "com.a42r.pevplugshare.OSMAND_SYNC_RESULT"
        const val EXTRA_SYNC_SUCCESS = "sync_success"
        const val EXTRA_SYNC_MESSAGE = "sync_message"
    }
}
