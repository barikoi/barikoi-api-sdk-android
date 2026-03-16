package com.barikoi.sdk

import com.barikoi.sdk.api.BarikoiApiService
import com.barikoi.sdk.models.*
import com.barikoi.sdk.models.FlexibleMapAdapter
import com.barikoi.sdk.models.GraphHopperPointsAdapter
import com.barikoi.sdk.models.StringOrDoubleAdapter
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class BarikoiApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: BarikoiApiService

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Register both custom adapters – identical to the setup in BarikoiClient.init {}
        val moshi = Moshi.Builder()
            .add(GraphHopperPointsAdapter())
            .add(StringOrDoubleAdapter())
            .add(FlexibleMapAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BarikoiApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // ==================== Autocomplete ====================

    @Test
    fun `autocomplete returns places list`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("""
            {
                "places": [
                    {"id": 1, "address": "Gulshan 1, Dhaka", "latitude": 23.7808, "longitude": 90.4104, "area": "Gulshan"},
                    {"id": 2, "address": "Gulshan 2, Dhaka", "latitude": 23.7928, "longitude": 90.4069, "area": "Gulshan"}
                ],
                "status": 200
            }
        """.trimIndent()))

        val response = apiService.autocomplete("Gulshan")
        assertTrue(response.isSuccessful)
        assertEquals(2, response.body()?.places?.size)
        assertEquals("Gulshan 1, Dhaka", response.body()?.places?.get(0)?.address)
    }

    @Test
    fun `autocomplete with bangla parameter`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("""
            {"places": [{"id": 1, "address": "ঢাকা", "latitude": 23.8103, "longitude": 90.4125}], "status": 200}
        """.trimIndent()))

        val response = apiService.autocomplete("ঢাকা", bangla = true)
        assertTrue(response.isSuccessful)
        assertEquals(1, response.body()?.places?.size)
    }

    // ==================== Reverse Geocode ====================

    @Test
    fun `reverse geocode returns place with address`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("""
            {
                "place": {"id": 1, "address": "Gulshan, Dhaka", "latitude": 23.8103, "longitude": 90.4125, "area": "Gulshan", "city": "Dhaka"},
                "status": 200
            }
        """.trimIndent()))

        val response = apiService.reverseGeocode(23.8103, 90.4125)
        assertTrue(response.isSuccessful)
        assertNotNull(response.body()?.place)
        assertEquals("Gulshan, Dhaka", response.body()?.place?.address)
        assertEquals("Gulshan", response.body()?.place?.area)
    }

    @Test
    fun `reverse geocode with all optional parameters`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("""
            {"place": {"id": 1, "address": "Complete Address", "latitude": 23.8103, "longitude": 90.4125, "district": "Dhaka", "postCode": 1212}, "status": 200}
        """.trimIndent()))

        val response = apiService.reverseGeocode(
            latitude = 23.8103, longitude = 90.4125,
            district = "Dhaka", postCode = "1212", bangla = true
        )
        assertTrue(response.isSuccessful)
        assertEquals("Complete Address", response.body()?.place?.address)
    }

    // ==================== Rupantor Geocode ====================

    @Test
    fun `rupantor geocode returns formatted place`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("""
            {
                "given_address": "Gulshan 1",
                "fixed_address": "gulshan 1, dhaka",
                "bangla_address": "গুলশান ১, ঢাকা",
                "address_status": "complete",
                "confidence_score_percentage": 90,
                "geocoded_address": {
                    "id": 1,
                    "Address": "House 10, Road 5, Gulshan 1",
                    "address": "House 10, Road 5, Gulshan 1",
                    "latitude": "23.7808",
                    "longitude": "90.4104",
                    "thana": "Gulshan",
                    "district": "Dhaka"
                },
                "status": 200
            }
        """.trimIndent()))

        val response = apiService.rupantorGeocode(
            query = "Gulshan 1", thana = "yes", district = "yes", bangla = "no"
        )
        assertTrue(response.isSuccessful)
        assertEquals("House 10, Road 5, Gulshan 1", response.body()?.geocodedAddress?.address)
        assertEquals("Gulshan", response.body()?.geocodedAddress?.thana)
        assertEquals("gulshan 1, dhaka", response.body()?.fixedAddress)
        assertEquals(90, response.body()?.confidenceScorePercentage)
        assertEquals("complete", response.body()?.addressStatus)
        // latitude returned as string "23.7808" should be coerced to Double by @StringOrDouble
        assertEquals(23.7808, response.body()?.geocodedAddress?.latitude ?: 0.0, 0.0001)
    }

    // ==================== Nearby Places ====================

    @Test
    fun `nearby places returns places within radius`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("""
            {
                "places": [
                    {"id": 1, "address": "Place 1", "latitude": 23.8103, "longitude": 90.4125, "distance_within_meters": 500},
                    {"id": 2, "address": "Place 2", "latitude": 23.8110, "longitude": 90.4130, "distance_within_meters": 700}
                ],
                "status": 200
            }
        """.trimIndent()))

        val response = apiService.nearbyPlaces(
            radius = 1.0, limit = 10, latitude = 23.8103, longitude = 90.4125
        )
        assertTrue(response.isSuccessful)
        assertEquals(2, response.body()?.places?.size)
        assertEquals(500.0, response.body()?.places?.get(0)?.distanceWithinMeters)
    }

    // ==================== Route Overview ====================

    @Test
    fun `route overview returns route data`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("""
            {"routes": [{"distance": 5000.0, "duration": 600.0, "geometry": "encoded_polyline"}], "status": 200}
        """.trimIndent()))

        val response = apiService.routeOverview("90.4125,23.8103;90.4200,23.8200")
        assertTrue(response.isSuccessful)
        assertEquals(1, response.body()?.routes?.size)
        assertEquals(5000.0, response.body()?.routes?.get(0)?.distance)
    }

    // ==================== Calculate Route ====================

    @Test
    fun `calculate route returns GraphHopper response`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("""
            {
                "paths": [{"distance": 5500.0, "time": 650000, "instructions": [{"distance": 100.0, "time": 10000, "text": "Head north"}]}],
                "info": {"copyrights": ["Barikoi"]}
            }
        """.trimIndent()))

        val body = JSONObject()
            .put("start", JSONObject().put("latitude", 23.8103).put("longitude", 90.4125))
            .put("destination", JSONObject().put("latitude", 23.8200).put("longitude", 90.4200))
            .toString()
            .toRequestBody("application/json".toMediaType())

        val response = apiService.calculateRoute(body = body)
        assertTrue(response.isSuccessful)
        assertEquals(1, response.body()?.paths?.size)
        assertEquals(5500.0, response.body()?.paths?.get(0)?.distance)
    }

    @Test
    fun `optimizeRoute with empty array details and legs parses without crash`() = runTest {
        // The live API returns "legs":[] and "details":[] (empty arrays, not objects).
        // FlexibleMapAdapter must coerce [] to null instead of crashing with
        // "Expected BEGIN_OBJECT but was BEGIN_ARRAY".
        mockWebServer.enqueue(MockResponse().setBody("""
            {
                "hints": {},
                "info": {"copyrights": ["GraphHopper"]},
                "paths": [{
                    "distance": 12821.069,
                    "weight": 2633.8,
                    "time": 1479916,
                    "points_encoded": true,
                    "points": "encodedPolylineString",
                    "snapped_waypoints": "encodedWaypoints",
                    "legs": [],
                    "details": [],
                    "ascend": 0.0,
                    "descend": 0.0,
                    "instructions": [
                        {"distance": 56.1, "sign": 0, "text": "Continue onto Lane 11", "time": 20208}
                    ]
                }]
            }
        """.trimIndent()))

        val response = apiService.optimizeRoute(
            "{}".toRequestBody("application/json".toMediaType())
        )
        assertTrue("Response should be successful", response.isSuccessful)
        val path = response.body()?.paths?.firstOrNull()
        assertNotNull(path)
        // [] should be coerced to null – not a parse crash
        assertNull("details [] should be null after coercion", path?.details)
        assertNull("legs [] should be null after coercion", path?.legs)
        assertEquals(12821.069, path?.distance ?: 0.0, 0.01)
        assertEquals(1, path?.instructions?.size)
    }

    // ==================== Optimize Route ====================

    @Test
    fun `optimize route returns optimized route data`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("""
            {"code": 200, "trips": [{"distance": 8000.0, "duration": 900.0, "geometry": "encoded_polyline"}]}
        """.trimIndent()))

        val body = JSONObject()
            .put("source", "23.8103,90.4125")
            .put("destination", "23.8200,90.4200")
            .toString()
            .toRequestBody("application/json".toMediaType())

        val response = apiService.optimizeRoute(body)
        assertTrue(response.isSuccessful)
        assertEquals(200, response.body()?.code)
        assertEquals(1, response.body()?.trips?.size)
    }

    // ==================== Search Place ====================

    @Test
    fun `search place returns places with session ID`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("""
            {"places": [{"id": 1, "address": "Restaurant ABC", "name": "ABC Restaurant"}], "session_id": "session_12345", "status": 200}
        """.trimIndent()))

        val response = apiService.searchPlace("Restaurant")
        assertTrue(response.isSuccessful)
        assertEquals("session_12345", response.body()?.sessionId)
        assertEquals(1, response.body()?.places?.size)
    }

    // ==================== Place Details ====================

    @Test
    fun `place details returns detailed information`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("""
            {"place": {"id": 100, "place_code": "PLACE_001", "address": "Detailed Address", "name": "Place Name", "latitude": 23.7808, "longitude": 90.4104}, "status": 200}
        """.trimIndent()))

        val response = apiService.placeDetails("PLACE_001", "session_12345")
        assertTrue(response.isSuccessful)
        assertEquals("PLACE_001", response.body()?.place?.placeCode)
        assertEquals("Place Name", response.body()?.place?.name)
    }

    // ==================== Snap to Road ====================

    @Test
    fun `snap to road returns snapped coordinates`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("""
            {"snappedPoints": [{"latitude": 23.8105, "longitude": 90.4127, "distance": 12.5}], "status": 200}
        """.trimIndent()))

        val response = apiService.snapToRoad("23.8103,90.4125")
        assertTrue(response.isSuccessful)
        assertEquals(1, response.body()?.snappedPoints?.size)
        assertEquals(12.5, response.body()?.snappedPoints?.get(0)?.distance)
    }

    // ==================== Check Nearby (Geofence) ====================

    @Test
    fun `check nearby returns geofence status`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("""
            {"isWithinRadius": true, "distance": 450.0, "message": "Within radius"}
        """.trimIndent()))

        val response = apiService.checkNearby(23.8103, 90.4125, 23.8110, 90.4130, 500.0)
        assertTrue(response.isSuccessful)
        assertTrue(response.body()?.isWithinRadius == true)
        assertEquals(450.0, response.body()?.distance)
    }

    @Test
    fun `check nearby with geofence data returns typed doubles`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("""
            {
                "isWithinRadius": false,
                "distance": 1200.0,
                "message": "Outside radius",
                "data": {"id": "geo1", "name": "Test Fence", "radius": "1000.0", "latitude": "23.8103", "longitude": "90.4125", "user_id": 1}
            }
        """.trimIndent()))

        val response = apiService.checkNearby(23.8103, 90.4125, 23.8200, 90.4200, 1000.0)
        assertTrue(response.isSuccessful)
        // radius/lat/lon returned as strings must be coerced to Double by @StringOrDouble
        assertEquals(1000.0, response.body()?.data?.radius ?: 0.0, 0.01)
        assertEquals(23.8103, response.body()?.data?.latitude ?: 0.0, 0.0001)
    }

    // ==================== Error Responses ====================

    @Test
    fun `handles 401 unauthorized`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        val response = apiService.reverseGeocode(23.8103, 90.4125)
        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun `handles 404 not found`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        val response = apiService.reverseGeocode(23.8103, 90.4125)
        assertFalse(response.isSuccessful)
        assertEquals(404, response.code())
    }

    @Test
    fun `handles 500 server error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        val response = apiService.autocomplete("test")
        assertFalse(response.isSuccessful)
        assertEquals(500, response.code())
    }

    @Test
    fun `handles empty response body`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("{}"))
        val response = apiService.reverseGeocode(23.8103, 90.4125)
        assertTrue(response.isSuccessful)
        assertNull(response.body()?.place)
    }

    // ==================== Model Helpers ====================

    @Test
    fun `Place displayAddress prefers address over addressAlt`() {
        val place = Place(address = "Primary", addressAlt = "Alt")
        assertEquals("Primary", place.displayAddress)
    }

    @Test
    fun `Place displayAddress falls back to addressAlt when address is null`() {
        val place = Place(address = null, addressAlt = "Alt")
        assertEquals("Alt", place.displayAddress)
    }

    @Test
    fun `Place displayPostCode returns string postcode first`() {
        val place = Place(postcode = "1212", postCode = 1313)
        assertEquals("1212", place.displayPostCode)
    }

    @Test
    fun `Place displayPostCode falls back to int postCode`() {
        val place = Place(postcode = null, postCode = 1212)
        assertEquals("1212", place.displayPostCode)
    }

    @Test
    fun `GraphHopperPath distanceInKm converts correctly`() {
        val path = GraphHopperPath(distance = 5500.0)
        assertEquals(5.5, path.distanceInKm ?: 0.0, 0.001)
    }

    @Test
    fun `GraphHopperPath durationInMinutes converts correctly`() {
        val path = GraphHopperPath(time = 300_000L)
        assertEquals(5.0, path.durationInMinutes ?: 0.0, 0.001)
    }
}
