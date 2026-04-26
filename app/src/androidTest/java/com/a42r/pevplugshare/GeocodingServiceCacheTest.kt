package com.a42r.pevplugshare

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a42r.pevplugshare.data.remote.GeocodingResult
import com.a42r.pevplugshare.data.remote.GeocodingService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeocodingServiceCacheTest {

    private lateinit var service: GeocodingService

    @Before
    fun setUp() {
        service = GeocodingService()
    }

    @Test
    fun testCachingReturnsSameResultForSameCoordinates() = runBlocking {
        val lat = 37.7749
        val lon = -122.4194

        // First call - should hit the network
        val firstResult = service.reverseGeocode(lat, lon)

        // Second call - should return cached result immediately
        val secondResult = service.reverseGeocode(lat, lon)

        // Both results should be identical (from cache)
        assertEquals(firstResult, secondResult)
        assertTrue(secondResult is GeocodingResult.Success)
    }

    @Test
    fun testRateLimitingWithParallelCalls() = runBlocking {
        val lat = 37.7749
        val lon = -122.4194

        val startTime = System.currentTimeMillis()
        val results = mutableListOf<GeocodingResult>()
        val callTimes = mutableListOf<Long>()

        // Launch 3 parallel coroutines that each call reverseGeocode
        launch {
            callTimes.add(System.currentTimeMillis() - startTime)
            results.add(service.reverseGeocode(lat, lon))
        }
        launch {
            callTimes.add(System.currentTimeMillis() - startTime)
            results.add(service.reverseGeocode(lat, lon + 0.0001)) // Different coords to bypass cache
        }
        launch {
            callTimes.add(System.currentTimeMillis() - startTime)
            results.add(service.reverseGeocode(lat + 0.0001, lon)) // Different coords to bypass cache
        }

        val endTime = System.currentTimeMillis() - startTime

        // All three calls should complete
        assertEquals(3, results.size)

        // Total time should be at least 2.2 seconds (1.1s per request * 2 rate-limited calls)
        // First call starts immediately, second starts after 1.1s, third after 2.2s
        assert(endTime >= 2200) {
            "Expected at least 2200ms elapsed time, but got ${endTime}ms"
        }
    }
}
