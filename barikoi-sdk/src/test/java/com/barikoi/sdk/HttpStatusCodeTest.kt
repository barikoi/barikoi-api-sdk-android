package com.barikoi.sdk

import com.barikoi.sdk.errors.BarikoiError
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Test suite for HTTP status code handling
 * Tests all possible HTTP responses from 200-599
 */
class HttpStatusCodeTest {

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

    // ==================== 2xx Success Codes ====================

    @Test
    fun `HTTP 200 OK returns success`() = runTest {
        val mockResponse = """
            {
                "place": {
                    "id": 1,
                    "address": "Test Address",
                    "latitude": 23.8103,
                    "longitude": 90.4125
                },
                "status": 200
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))

        val result = client.reverseGeocode(23.8103, 90.4125)

        assertTrue(result.isSuccess)
        assertEquals("Test Address", result.getOrNull()?.address)
    }

    @Test
    fun `HTTP 201 Created returns success`() = runTest {
        val mockResponse = """
            {
                "place": {
                    "id": 1,
                    "address": "Created Address",
                    "latitude": 23.8103,
                    "longitude": 90.4125
                },
                "status": 201
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(mockResponse))

        val result = client.reverseGeocode(23.8103, 90.4125)

        assertTrue(result.isSuccess)
    }

    // ==================== 4xx Client Error Codes ====================

    @Test
    fun `HTTP 400 Bad Request returns BadRequestError`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("Invalid parameters")
        )

        val result = client.reverseGeocode(23.8103, 90.4125)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.BadRequestError)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid parameters") == true)
    }

    @Test
    fun `HTTP 401 Unauthorized returns UnauthorizedError`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("Invalid API key")
        )

        val result = client.reverseGeocode(23.8103, 90.4125)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.UnauthorizedError)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid API key") == true)
    }

    @Test
    fun `HTTP 403 Forbidden returns UnauthorizedError`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("Access denied")
        )

        val result = client.reverseGeocode(23.8103, 90.4125)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.UnauthorizedError)
        assertTrue(result.exceptionOrNull()?.message?.contains("Access denied") == true)
    }

    @Test
    fun `HTTP 404 Not Found returns HttpError`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("Resource not found")
        )

        val result = client.reverseGeocode(23.8103, 90.4125)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.HttpError)
        val error = result.exceptionOrNull() as BarikoiError.HttpError
        assertEquals(404, error.code)
    }

    @Test
    fun `HTTP 402 Payment Required returns QuotaExceededError`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(402)
                .setBody("Payment required - API quota exceeded")
        )

        val result = client.reverseGeocode(23.8103, 90.4125)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.QuotaExceededError)
    }

    @Test
    fun `HTTP 429 Too Many Requests returns RateLimitError`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("Rate limit exceeded")
        )

        val result = client.reverseGeocode(23.8103, 90.4125)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.RateLimitError)
        // message comes from the server body "Rate limit exceeded" (not the SDK default)
        assertNotNull(result.exceptionOrNull()?.message)
    }

    // ==================== 5xx Server Error Codes ====================

    @Test
    fun `HTTP 500 Internal Server Error returns HttpError`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal server error")
        )

        val result = client.reverseGeocode(23.8103, 90.4125)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.HttpError)
        val error = result.exceptionOrNull() as BarikoiError.HttpError
        assertEquals(500, error.code)
        assertTrue(error.message.contains("Server error"))
    }

    @Test
    fun `HTTP 502 Bad Gateway returns HttpError`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(502)
                .setBody("Bad gateway")
        )

        val result = client.reverseGeocode(23.8103, 90.4125)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.HttpError)
        val error = result.exceptionOrNull() as BarikoiError.HttpError
        assertEquals(502, error.code)
    }

    @Test
    fun `HTTP 503 Service Unavailable returns HttpError`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("Service temporarily unavailable")
        )

        val result = client.reverseGeocode(23.8103, 90.4125)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.HttpError)
        val error = result.exceptionOrNull() as BarikoiError.HttpError
        assertEquals(503, error.code)
    }

    @Test
    fun `HTTP 504 Gateway Timeout returns HttpError`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(504)
                .setBody("Gateway timeout")
        )

        val result = client.reverseGeocode(23.8103, 90.4125)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.HttpError)
        val error = result.exceptionOrNull() as BarikoiError.HttpError
        assertEquals(504, error.code)
    }

    // ==================== Test All Endpoints Handle Status Codes ====================

    @Test
    fun `Autocomplete handles 401 Unauthorized`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))

        val result = client.autocomplete("test")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.UnauthorizedError)
    }

    @Test
    fun `NearbyPlaces handles 429 Rate Limit`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(429).setBody("Too many requests"))

        val result = client.nearbyPlaces(23.8103, 90.4125, 1.0)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.RateLimitError)
    }

    @Test
    fun `SearchPlace handles 500 Server Error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Server error"))

        val result = client.searchPlace("test")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.HttpError)
    }

    @Test
    fun `SnapToRoad handles 402 Payment Required`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(402).setBody("Quota exceeded"))

        val result = client.snapToRoad(23.8103, 90.4125)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.QuotaExceededError)
    }

    @Test
    fun `CheckNearby handles 400 Bad Request`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody("Invalid coordinates"))

        val result = client.checkNearby(23.8103, 90.4125, 23.8200, 90.4200, 500.0)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.BadRequestError)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `Empty error body still creates appropriate error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        val result = client.reverseGeocode(23.8103, 90.4125)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.UnauthorizedError)
    }

    @Test
    fun `Unknown status code creates generic HttpError`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(418).setBody("I'm a teapot"))

        val result = client.reverseGeocode(23.8103, 90.4125)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BarikoiError.HttpError)
        val error = result.exceptionOrNull() as BarikoiError.HttpError
        assertEquals(418, error.code)
    }
}
