package com.barikoi.sdk

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Test suite for Search APIs
 * - Autocomplete
 * - Search Place
 * - Place Details
 */
class SearchApiTest {

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

    // ==================== Autocomplete Tests ====================

    @Test
    fun `autocomplete returns matching places`() = runTest {
        val mockResponse = """
            {
                "places": [
                    {
                        "id": 1,
                        "address": "Gulshan 1, Dhaka",
                        "area": "Gulshan",
                        "latitude": 23.7808,
                        "longitude": 90.4104
                    },
                    {
                        "id": 2,
                        "address": "Gulshan 2, Dhaka",
                        "area": "Gulshan",
                        "latitude": 23.7928,
                        "longitude": 90.4069
                    }
                ],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.autocomplete("Gulshan")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("Gulshan 1, Dhaka", result.getOrNull()?.get(0)?.address)
        assertEquals("Gulshan 2, Dhaka", result.getOrNull()?.get(1)?.address)
    }

    @Test
    fun `autocomplete with single character query`() = runTest {
        val mockResponse = """
            {
                "places": [
                    {"id": 1, "address": "Dhaka"},
                    {"id": 2, "address": "Dhanmondi"}
                ],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.autocomplete("D")

        assertTrue(result.isSuccess)
        assertTrue((result.getOrNull()?.size ?: 0) > 0)
    }

    @Test
    fun `autocomplete with bangla query`() = runTest {
        val mockResponse = """
            {
                "places": [
                    {
                        "id": 1,
                        "address": "ঢাকা",
                        "address_bn": "ঢাকা",
                        "area_bn": "ঢাকা"
                    }
                ],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.autocomplete("ঢাকা", bangla = true)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `autocomplete with no matches returns empty list`() = runTest {
        val mockResponse = """
            {
                "places": [],
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.autocomplete("XYZ_NONEXISTENT")

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `autocomplete with empty query throws IllegalArgumentException`() = runTest {
        // Validation happens before any network call, so no mock needed
        var threw = false
        try {
            client.autocomplete("")
        } catch (e: IllegalArgumentException) {
            threw = true
            assertTrue(e.message?.contains("blank") == true)
        }
        assertTrue("Expected IllegalArgumentException for blank query", threw)
    }

    // ==================== Search Place Tests ====================

    @Test
    fun `searchPlace returns places with session ID`() = runTest {
        val mockResponse = """
            {
                "places": [
                    {
                        "id": 1,
                        "place_code": "PLACE_001",
                        "address": "Restaurant ABC",
                        "name": "ABC Restaurant",
                        "pType": "Restaurant"
                    },
                    {
                        "id": 2,
                        "place_code": "PLACE_002",
                        "address": "Restaurant XYZ",
                        "name": "XYZ Restaurant",
                        "pType": "Restaurant"
                    }
                ],
                "session_id": "session_abc123",
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.searchPlace("Restaurant")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.places?.size)
        assertEquals("session_abc123", result.getOrNull()?.sessionId)
        assertEquals("PLACE_001", result.getOrNull()?.places?.get(0)?.placeCode)
    }

    @Test
    fun `searchPlace generates unique session IDs`() = runTest {
        val mockResponse1 = """{"places": [], "session_id": "session_1", "status": 200}"""
        val mockResponse2 = """{"places": [], "session_id": "session_2", "status": 200}"""

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse1))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse2))

        val result1 = client.searchPlace("Query1")
        val result2 = client.searchPlace("Query2")

        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertNotEquals(result1.getOrNull()?.sessionId, result2.getOrNull()?.sessionId)
    }

    @Test
    fun `searchPlace with specific category`() = runTest {
        val mockResponse = """
            {
                "places": [
                    {
                        "id": 1,
                        "address": "Hospital ABC",
                        "pType": "Hospital"
                    }
                ],
                "session_id": "session_123",
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.searchPlace("Hospital")

        assertTrue(result.isSuccess)
        assertEquals("Hospital", result.getOrNull()?.places?.get(0)?.pType)
    }

    // ==================== Place Details Tests ====================

    @Test
    fun `placeDetails returns detailed information`() = runTest {
        val mockResponse = """
            {
                "place": {
                    "id": 100,
                    "place_code": "PLACE_001",
                    "address": "123 Main Street, Gulshan 1, Dhaka",
                    "name": "ABC Restaurant",
                    "latitude": 23.7808,
                    "longitude": 90.4104,
                    "area": "Gulshan",
                    "city": "Dhaka",
                    "pType": "Restaurant",
                    "subType": "Fine Dining",
                    "address_components": {
                        "place_name": "ABC Restaurant",
                        "house": "123",
                        "road": "Main Street"
                    }
                },
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.placeDetails("PLACE_001", "session_123")

        assertTrue(result.isSuccess)
        assertEquals("PLACE_001", result.getOrNull()?.placeCode)
        assertEquals("ABC Restaurant", result.getOrNull()?.name)
        assertEquals("Restaurant", result.getOrNull()?.pType)
        assertEquals("123", result.getOrNull()?.addressComponents?.house)
    }

    @Test
    fun `placeDetails with invalid place code returns error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Place not found"))

        val result = client.placeDetails("INVALID_CODE", "session_123")

        assertTrue(result.isFailure)
    }

    @Test
    fun `placeDetails with invalid session ID returns error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody("Invalid session"))

        val result = client.placeDetails("PLACE_001", "invalid_session")

        assertTrue(result.isFailure)
    }

    @Test
    fun `placeDetails includes all optional fields`() = runTest {
        val mockResponse = """
            {
                "place": {
                    "id": 100,
                    "place_code": "PLACE_001",
                    "address": "Complete Address",
                    "name": "Place Name",
                    "latitude": 23.7808,
                    "longitude": 90.4104,
                    "area": "Area",
                    "city": "City",
                    "postCode": 1212,
                    "district": "District",
                    "division": "Division",
                    "country": "Bangladesh",
                    "pType": "Type",
                    "subType": "SubType",
                    "uCode": "UCODE123"
                },
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.placeDetails("PLACE_001", "session_123")

        assertTrue(result.isSuccess)
        val place = result.getOrNull()
        assertNotNull(place?.district)
        assertNotNull(place?.division)
        assertNotNull(place?.country)
        assertNotNull(place?.postCode)
        assertNotNull(place?.uCode)
    }

    @Test
    fun `searchPlace and placeDetails workflow`() = runTest {
        // First search
        val searchResponse = """
            {
                "places": [
                    {"id": 1, "place_code": "PLACE_001", "address": "Place 1"}
                ],
                "session_id": "session_abc",
                "status": 200
            }
        """.trimIndent()

        // Then get details
        val detailsResponse = """
            {
                "place": {
                    "id": 1,
                    "place_code": "PLACE_001",
                    "address": "Detailed Address",
                    "name": "Place Name"
                },
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(searchResponse))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(detailsResponse))

        val searchResult = client.searchPlace("test")
        assertTrue(searchResult.isSuccess)

        val placeCode = searchResult.getOrNull()?.places?.get(0)?.placeCode
        val sessionId = searchResult.getOrNull()?.sessionId

        assertNotNull(placeCode)
        assertNotNull(sessionId)

        val detailsResult = client.placeDetails(placeCode!!, sessionId!!)
        assertTrue(detailsResult.isSuccess)
        assertEquals("Detailed Address", detailsResult.getOrNull()?.address)
    }
}
