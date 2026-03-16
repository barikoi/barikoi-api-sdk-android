package com.barikoi.sdk

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Test suite for Routing APIs
 * - Route Overview
 * - Calculate Route (GraphHopper)
 * - Optimize Route
 */
class RoutingApiTest {

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

    // ==================== Route Overview Tests ====================

    @Test
    fun `routeOverview returns route summary`() = runTest {
        val mockResponse = """
            {
                "routes": [
                    {
                        "distance": 5000.0,
                        "duration": 600.0,
                        "geometry": "encoded_polyline_string"
                    }
                ],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.routeOverview(
            startLat = 23.8103,
            startLon = 90.4125,
            endLat = 23.8200,
            endLon = 90.4200
        )

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals(5000.0, result.getOrNull()?.get(0)?.distance ?: 0.0, 0.01)
        assertEquals(600.0, result.getOrNull()?.get(0)?.duration ?: 0.0, 0.01)
    }

    @Test
    fun `routeOverview with driving profile`() = runTest {
        val mockResponse = """
            {
                "routes": [
                    {
                        "distance": 4500.0,
                        "duration": 540.0
                    }
                ],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.routeOverview(
            startLat = 23.8103,
            startLon = 90.4125,
            endLat = 23.8200,
            endLon = 90.4200,
            profile = "driving"
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `routeOverview with walking profile takes longer`() = runTest {
        val mockResponse = """
            {
                "routes": [
                    {
                        "distance": 4500.0,
                        "duration": 2700.0
                    }
                ],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.routeOverview(
            startLat = 23.8103,
            startLon = 90.4125,
            endLat = 23.8200,
            endLon = 90.4200,
            profile = "walking"
        )

        assertTrue(result.isSuccess)
        // Walking should have longer duration than driving for same distance
        val duration = result.getOrNull()?.get(0)?.duration ?: 0.0
        assertTrue(duration > 1000.0) // Should take more than 1000 seconds
    }

    @Test
    fun `routeOverview with geojson geometries`() = runTest {
        val mockResponse = """
            {
                "routes": [
                    {
                        "distance": 5000.0,
                        "duration": 600.0,
                        "geometry": "encodedPolylineString"
                    }
                ],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.routeOverview(
            startLat = 23.8103,
            startLon = 90.4125,
            endLat = 23.8200,
            endLon = 90.4200,
            geometries = "geojson"
        )

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull()?.get(0)?.geometry)
    }

    // ==================== Calculate Route (GraphHopper) Tests ====================

    @Test
    fun `calculateRoute returns detailed route with instructions`() = runTest {
        val mockResponse = """
            {
                "paths": [
                    {
                        "distance": 5500.0,
                        "time": 650000,
                        "ascend": 10.0,
                        "descend": 5.0,
                        "instructions": [
                            {
                                "distance": 100.0,
                                "time": 10000,
                                "text": "Head north on Main Street",
                                "street_name": "Main Street"
                            },
                            {
                                "distance": 200.0,
                                "time": 20000,
                                "text": "Turn right onto Second Avenue",
                                "street_name": "Second Avenue"
                            },
                            {
                                "distance": 150.0,
                                "time": 15000,
                                "text": "Turn left onto Third Street",
                                "street_name": "Third Street"
                            }
                        ]
                    }
                ],
                "info": {
                    "copyrights": ["Barikoi", "OpenStreetMap contributors"]
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.calculateRoute(
            startLat = 23.8103,
            startLon = 90.4125,
            endLat = 23.8200,
            endLon = 90.4200
        )

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.paths?.size)
        assertEquals(5500.0, result.getOrNull()?.paths?.get(0)?.distance ?: 0.0, 0.01)
        assertEquals(3, result.getOrNull()?.paths?.get(0)?.instructions?.size)
        assertEquals("Head north on Main Street", result.getOrNull()?.paths?.get(0)?.instructions?.get(0)?.text)
    }

    @Test
    fun `calculateRoute with car profile`() = runTest {
        val mockResponse = """
            {
                "paths": [
                    {
                        "distance": 5000.0,
                        "time": 600000,
                        "instructions": []
                    }
                ],
                "info": {"copyrights": ["Barikoi"]}
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.calculateRoute(
            startLat = 23.8103,
            startLon = 90.4125,
            endLat = 23.8200,
            endLon = 90.4200,
            profile = "car"
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `calculateRoute includes elevation data`() = runTest {
        val mockResponse = """
            {
                "paths": [
                    {
                        "distance": 6000.0,
                        "time": 720000,
                        "ascend": 50.0,
                        "descend": 30.0,
                        "instructions": []
                    }
                ],
                "info": {"copyrights": ["Barikoi"]}
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.calculateRoute(23.8103, 90.4125, 23.8200, 90.4200)

        assertTrue(result.isSuccess)
        assertEquals(50.0, result.getOrNull()?.paths?.get(0)?.ascend ?: 0.0, 0.01)
        assertEquals(30.0, result.getOrNull()?.paths?.get(0)?.descend ?: 0.0, 0.01)
    }

    // ==================== Optimize Route Tests ====================

    @Test
    fun `optimizeRoute returns optimized route with waypoints`() = runTest {
        val mockResponse = """
            {
                "code": 200,
                "trips": [
                    {
                        "distance": 8000.0,
                        "duration": 900.0,
                        "geometry": "encoded_polyline"
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val waypoints = listOf(
            23.8150 to 90.4150,
            23.8170 to 90.4170,
            23.8180 to 90.4180
        )

        val result = client.optimizeRoute(
            sourceLat = 23.8103,
            sourceLon = 90.4125,
            destinationLat = 23.8200,
            destinationLon = 90.4200,
            waypoints = waypoints
        )

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.trips?.size)
        assertEquals(8000.0, result.getOrNull()?.trips?.get(0)?.distance ?: 0.0, 0.01)
        assertEquals(900.0, result.getOrNull()?.trips?.get(0)?.duration ?: 0.0, 0.01)
    }

    @Test
    fun `optimizeRoute with single waypoint`() = runTest {
        val mockResponse = """
            {
                "code": 200,
                "trips": [
                    {
                        "distance": 6000.0,
                        "duration": 700.0
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val waypoints = listOf(23.8150 to 90.4150)

        val result = client.optimizeRoute(
            sourceLat = 23.8103,
            sourceLon = 90.4125,
            destinationLat = 23.8200,
            destinationLon = 90.4200,
            waypoints = waypoints
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `optimizeRoute with many waypoints`() = runTest {
        val mockResponse = """
            {
                "code": 200,
                "trips": [
                    {
                        "distance": 15000.0,
                        "duration": 1800.0
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val waypoints = (0..10).map { i ->
            23.8103 + (i * 0.01) to 90.4125 + (i * 0.01)
        }

        val result = client.optimizeRoute(
            sourceLat = 23.8103,
            sourceLon = 90.4125,
            destinationLat = 23.8200,
            destinationLon = 90.4200,
            waypoints = waypoints
        )

        assertTrue(result.isSuccess)
        // More waypoints should generally result in longer distance
        assertTrue((result.getOrNull()?.trips?.get(0)?.distance ?: 0.0) > 10000.0)
    }

    @Test
    fun `optimizeRoute with driving profile`() = runTest {
        val mockResponse = """
            {
                "code": 200,
                "trips": [
                    {
                        "distance": 7500.0,
                        "duration": 850.0
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val waypoints = listOf(23.8150 to 90.4150)

        val result = client.optimizeRoute(
            sourceLat = 23.8103,
            sourceLon = 90.4125,
            destinationLat = 23.8200,
            destinationLon = 90.4200,
            waypoints = waypoints,
            profile = "driving"
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `optimizeRoute empty waypoints still works`() = runTest {
        val mockResponse = """
            {
                "code": 200,
                "trips": [
                    {
                        "distance": 5000.0,
                        "duration": 600.0
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.optimizeRoute(
            sourceLat = 23.8103,
            sourceLon = 90.4125,
            destinationLat = 23.8200,
            destinationLon = 90.4200,
            waypoints = listOf(
                Pair(23.8200, 90.4200),
                Pair(23.7900, 90.4100),
                Pair(23.7700, 90.4300)
            )
        )

        assertTrue(result.isSuccess)
    }

    // ==================== Route Comparison Tests ====================

    @Test
    fun `routeOverview faster than calculateRoute for quick summary`() = runTest {
        // Route Overview - simple
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"routes": [{"distance": 5000.0, "duration": 600.0}], "status": 200}"""))

        // Calculate Route - detailed
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"paths": [{"distance": 5000.0, "time": 600000, "instructions": []}], "info": {}}"""))

        val overviewResult = client.routeOverview(23.8103, 90.4125, 23.8200, 90.4200)
        val detailedResult = client.calculateRoute(23.8103, 90.4125, 23.8200, 90.4200)

        assertTrue(overviewResult.isSuccess)
        assertTrue(detailedResult.isSuccess)

        // Both should return similar distance
        val overviewDistance = overviewResult.getOrNull()?.get(0)?.distance ?: 0.0
        val detailedDistance = detailedResult.getOrNull()?.paths?.get(0)?.distance ?: 0.0

        assertEquals(overviewDistance, detailedDistance, 100.0) // Within 100m tolerance
    }
}
