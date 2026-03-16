package com.barikoi.sdk

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Test suite for Location APIs
 * - Nearby Places
 * - Snap to Road
 * - Check Nearby (Geofence)
 */
class LocationApiTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: BarikoiClient

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        client = BarikoiClient.Builder()
            .apiKey("test_api_key")
            .baseUrl(mockWebServer.url("/").toString())
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // ==================== Nearby Places Tests ====================

    @Test
    fun `nearbyPlaces returns places within radius`() = runTest {
        val mockResponse = """
            {
                "places": [
                    {
                        "id": 1,
                        "address": "Place 1",
                        "latitude": 23.7808,
                        "longitude": 90.4104,
                        "distance_within_meters": 100.5
                    },
                    {
                        "id": 2,
                        "address": "Place 2",
                        "latitude": 23.7815,
                        "longitude": 90.4110,
                        "distance_within_meters": 250.8
                    },
                    {
                        "id": 3,
                        "address": "Place 3",
                        "latitude": 23.7820,
                        "longitude": 90.4115,
                        "distance_within_meters": 450.2
                    }
                ],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.nearbyPlaces(23.7808, 90.4104, 1.0, 10)

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()?.size)
        assertEquals(100.5, result.getOrNull()?.get(0)?.distanceWithinMeters)
        assertEquals(250.8, result.getOrNull()?.get(1)?.distanceWithinMeters)
    }

    @Test
    fun `nearbyPlaces with small radius returns fewer places`() = runTest {
        val mockResponse = """
            {
                "places": [
                    {
                        "id": 1,
                        "address": "Very Close Place",
                        "distance_within_meters": 50.0
                    }
                ],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.nearbyPlaces(23.7808, 90.4104, 0.1, 10)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `nearbyPlaces with custom limit`() = runTest {
        val mockResponse = """
            {
                "places": [
                    {"id": 1, "address": "Place 1", "distance_within_meters": 100},
                    {"id": 2, "address": "Place 2", "distance_within_meters": 200},
                    {"id": 3, "address": "Place 3", "distance_within_meters": 300}
                ],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.nearbyPlaces(23.7808, 90.4104, 1.0, 3)

        assertTrue(result.isSuccess)
        assertTrue((result.getOrNull()?.size ?: 0) <= 3)
    }

    @Test
    fun `nearbyPlaces with no places returns empty list`() = runTest {
        val mockResponse = """
            {
                "places": [],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.nearbyPlaces(0.0, 0.0, 1.0, 10)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `nearbyPlaces sorted by distance`() = runTest {
        val mockResponse = """
            {
                "places": [
                    {"id": 1, "address": "Nearest", "distance_within_meters": 50.0},
                    {"id": 2, "address": "Middle", "distance_within_meters": 150.0},
                    {"id": 3, "address": "Farthest", "distance_within_meters": 300.0}
                ],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.nearbyPlaces(23.7808, 90.4104, 1.0, 10)

        assertTrue(result.isSuccess)
        val places = result.getOrNull() ?: emptyList()
        // Verify distances are in ascending order
        for (i in 0 until places.size - 1) {
            val current = places[i].distanceWithinMeters ?: 0.0
            val next = places[i + 1].distanceWithinMeters ?: 0.0
            assertTrue(current <= next)
        }
    }

    // ==================== Snap to Road Tests ====================

    @Test
    fun `snapToRoad returns nearest road point`() = runTest {
        val mockResponse = """
            {
                "snappedPoints": [
                    {
                        "latitude": 23.8105,
                        "longitude": 90.4127,
                        "distance": 12.5
                    }
                ],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.snapToRoad(23.8103, 90.4125)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.snappedPoints?.size)
        assertEquals(23.8105, result.getOrNull()?.snappedPoints?.get(0)?.latitude)
        assertEquals(90.4127, result.getOrNull()?.snappedPoints?.get(0)?.longitude)
        assertEquals(12.5, result.getOrNull()?.snappedPoints?.get(0)?.distance ?: 0.0, 0.01)
    }

    @Test
    fun `snapToRoad when already on road`() = runTest {
        val mockResponse = """
            {
                "snappedPoints": [
                    {
                        "latitude": 23.8103,
                        "longitude": 90.4125,
                        "distance": 0.0
                    }
                ],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.snapToRoad(23.8103, 90.4125)

        assertTrue(result.isSuccess)
        // Distance should be very small or zero
        assertTrue((result.getOrNull()?.snappedPoints?.get(0)?.distance ?: 100.0) < 1.0)
    }

    @Test
    fun `snapToRoad with coordinates far from roads`() = runTest {
        val mockResponse = """
            {
                "snappedPoints": [
                    {
                        "latitude": 23.8150,
                        "longitude": 90.4200,
                        "distance": 500.0
                    }
                ],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.snapToRoad(23.8100, 90.4100)

        assertTrue(result.isSuccess)
        // Large distance indicates point is far from roads
        assertTrue((result.getOrNull()?.snappedPoints?.get(0)?.distance ?: 0.0) > 100.0)
    }

    // ==================== Check Nearby (Geofence) Tests ====================

    @Test
    fun `checkNearby when destination is inside radius`() = runTest {
        val mockResponse = """
            {
                "isWithinRadius": true,
                "distance": 450.0,
                "message": "Destination is within the specified radius"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.checkNearby(
            currentLat = 23.8103,
            currentLon = 90.4125,
            destinationLat = 23.8110,
            destinationLon = 90.4130,
            radiusMeters = 500.0
        )

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isWithinRadius == true)
        assertEquals(450.0, result.getOrNull()?.distance ?: 0.0, 0.01)
        assertTrue(result.getOrNull()?.isInside == true)
    }

    @Test
    fun `checkNearby when destination is outside radius`() = runTest {
        val mockResponse = """
            {
                "isWithinRadius": false,
                "distance": 750.0,
                "message": "Destination is outside the specified radius"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.checkNearby(
            currentLat = 23.8103,
            currentLon = 90.4125,
            destinationLat = 23.8200,
            destinationLon = 90.4200,
            radiusMeters = 500.0
        )

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()?.isWithinRadius == true)
        assertEquals(750.0, result.getOrNull()?.distance ?: 0.0, 0.01)
        assertFalse(result.getOrNull()?.isInside == true)
    }

    @Test
    fun `checkNearby at exact radius boundary`() = runTest {
        val mockResponse = """
            {
                "isWithinRadius": true,
                "distance": 500.0,
                "message": "Destination is at the boundary"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.checkNearby(
            currentLat = 23.8103,
            currentLon = 90.4125,
            destinationLat = 23.8145,
            destinationLon = 90.4125,
            radiusMeters = 500.0
        )

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isWithinRadius == true)
        assertEquals(500.0, result.getOrNull()?.distance ?: 0.0, 0.01)
    }

    @Test
    fun `checkNearby with same current and destination`() = runTest {
        val mockResponse = """
            {
                "isWithinRadius": true,
                "distance": 0.0,
                "message": "Same location"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.checkNearby(
            currentLat = 23.8103,
            currentLon = 90.4125,
            destinationLat = 23.8103,
            destinationLon = 90.4125,
            radiusMeters = 100.0
        )

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isWithinRadius == true)
        assertEquals(0.0, result.getOrNull()?.distance ?: 100.0, 0.01)
    }

    @Test
    fun `checkNearby with different radius values`() = runTest {
        val mockResponses = listOf(
            """{"isWithinRadius": false, "distance": 750.0, "message": "Outside"}""",
            """{"isWithinRadius": true, "distance": 750.0, "message": "Inside"}"""
        )

        mockResponses.forEach { response ->
            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(response))
        }

        // Small radius - should be outside
        val result1 = client.checkNearby(23.8103, 90.4125, 23.8200, 90.4200, 500.0)
        // Large radius - should be inside
        val result2 = client.checkNearby(23.8103, 90.4125, 23.8200, 90.4200, 1000.0)

        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertFalse(result1.getOrNull()?.isWithinRadius == true)
        assertTrue(result2.getOrNull()?.isWithinRadius == true)
    }

    @Test
    fun `checkNearby with geofence data`() = runTest {
        val mockResponse = """
            {
                "isWithinRadius": true,
                "distance": 300.0,
                "message": "Inside geofence",
                "data": {
                    "id": "geofence_1",
                    "name": "Home Geofence",
                    "radius": "500",
                    "latitude": "23.8103",
                    "longitude": "90.4125",
                    "user_id": 123
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.checkNearby(23.8103, 90.4125, 23.8110, 90.4130, 500.0)

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull()?.data)
        assertEquals("geofence_1", result.getOrNull()?.data?.id)
        assertEquals("Home Geofence", result.getOrNull()?.data?.name)
        // data.radius is returned as a JSON string "500" – @StringOrDouble coerces it to Double
        assertEquals(500.0, result.getOrNull()?.data?.radius ?: 0.0, 0.01)
    }
}
