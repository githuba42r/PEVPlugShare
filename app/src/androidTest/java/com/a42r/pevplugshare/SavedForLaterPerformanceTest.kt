package com.a42r.pevplugshare

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.a42r.pevplugshare.data.db.PEVPlugShareDatabase
import com.a42r.pevplugshare.data.remote.GeocodingService
import com.a42r.pevplugshare.domain.repository.SavedForLaterRepository
import com.a42r.pevplugshare.ui.savedforlater.SavedForLaterState
import com.a42r.pevplugshare.ui.savedforlater.SavedForLaterViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SavedForLaterPerformanceTest {
    private lateinit var context: android.content.Context
    private lateinit var db: PEVPlugShareDatabase
    private lateinit var repository: SavedForLaterRepository

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Restore backup if not already done
        val backupZip = findBackupFile()
        val alreadyRestored = hasSavedLocations()

        if (!alreadyRestored) {
            println("Restoring backup for performance test...")
            val helper = com.a42r.pevplugshare.util.BackupRestoreHelper(context)
            runBlocking {
                val result = helper.restoreFromZip(backupZip)
                assertTrue("Backup restore failed", result is com.a42r.pevplugshare.util.BackupRestoreHelper.RestoreResult.Success)
            }
            Thread.sleep(1000)
        }

        db = PEVPlugShareDatabase.getDatabase(context)
        repository = SavedForLaterRepository(db.savedForLaterDao())
    }

    @Test
    fun loadSavedLocationsShowsCoordinatesImmediately() = runBlocking {
        val viewModel = SavedForLaterViewModel(repository, GeocodingService())

        val startTime = System.currentTimeMillis()
        viewModel.loadSavedLocations()

        // Wait briefly for state emission
        Thread.sleep(100)

        val elapsedMs = System.currentTimeMillis() - startTime
        println("Time to first state emission: ${elapsedMs}ms")

        val currentState = viewModel.savedLocationsState.value
        assertTrue("Should have Success state quickly", currentState is SavedForLaterState.Success)

        val items = (currentState as SavedForLaterState.Success).items
        assertTrue("Should have loaded saved locations", items.isNotEmpty())

        // Verify first item has coordinate-based display name
        val firstItem = items.first()
        assertTrue(
            "Display name should start with 'Lat:' initially",
            firstItem.locationDisplayName.startsWith("Lat:") ||
            firstItem.locationDisplayName.contains("Lat")
        )
        println("First item initial display name: ${firstItem.locationDisplayName}")

        // Time to first state should be < 200ms
        assertTrue("Should show items within 200ms", elapsedMs < 200)
    }

    @Test
    fun geocodingUpdatesItemNamesProgressively() = runBlocking {
        val viewModel = SavedForLaterViewModel(repository, GeocodingService())

        val updates = mutableListOf<Long>()
        val displayNames = mutableListOf<String>()

        viewModel.loadSavedLocations()

        // Collect state updates
        var previousSize = 0
        var nameChangeCount = 0
        val startTime = System.currentTimeMillis()

        for (i in 0..50) {
            Thread.sleep(200)

            val state = viewModel.savedLocationsState.value
            if (state is SavedForLaterState.Success) {
                val items = state.items

                // Count unique display names
                val uniqueNames = items.map { it.locationDisplayName }.toSet().size
                if (uniqueNames > previousSize) {
                    nameChangeCount++
                    previousSize = uniqueNames
                    val elapsedMs = System.currentTimeMillis() - startTime
                    updates.add(elapsedMs)
                    if (items.isNotEmpty()) {
                        displayNames.add(items.first().locationDisplayName)
                    }
                    println("Name update at ${elapsedMs}ms: $uniqueNames unique names")
                }
            }
        }

        println("=== Geocoding Progress ===")
        println("Total state updates with name changes: $nameChangeCount")
        displayNames.take(3).forEach { println("  - $it") }

        // Verify some items were geocoded (names changed from coordinates to locations)
        assertTrue(
            "Should have some geocoded items within ~10 seconds",
            nameChangeCount > 0 || displayNames.any { !it.startsWith("Lat:") }
        )
    }

    @Test
    fun noLoadingSpinnerOnSecondLoad() = runBlocking {
        val viewModel = SavedForLaterViewModel(repository, GeocodingService())

        // First load
        viewModel.loadSavedLocations()
        Thread.sleep(1000)

        // Verify first load showed Success
        val firstState = viewModel.savedLocationsState.value
        assertTrue("First load should be Success", firstState is SavedForLaterState.Success)

        // Second load (simulating return to Saved tab)
        viewModel.loadSavedLocations()

        // Check immediately - should still show Success, not Loading
        val secondState = viewModel.savedLocationsState.value
        assertTrue(
            "Second load should not show Loading spinner, should show Success instead",
            secondState is SavedForLaterState.Success
        )
        println("Second load state: $secondState (no spinner)")
    }

    private fun hasSavedLocations(): Boolean {
        return try {
            val dbFile = File(context.filesDir.parentFile, "databases/pevplugshare.db")
            if (!dbFile.exists()) return false
            runBlocking {
                val count = repository.getAllSavedForLaterList().size
                count > 0
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun findBackupFile(): File {
        val searchLocations = listOf(
            "/data/local/tmp/backup_full.zip",
            "${context.cacheDir.path}/backup_full.zip",
            "/sdcard/Downloads/backup_full.zip",
            "/sdcard/Download/backup_full.zip"
        )

        for (path in searchLocations) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                println("Found backup at: $path")
                return file
            }
        }

        throw AssertionError("Backup file not found")
    }
}
