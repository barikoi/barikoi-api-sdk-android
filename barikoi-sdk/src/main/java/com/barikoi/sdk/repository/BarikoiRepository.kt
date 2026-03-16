package com.barikoi.sdk.repository

import com.barikoi.sdk.api.BarikoiApiService
import com.barikoi.sdk.errors.BarikoiError
import com.barikoi.sdk.models.*
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.util.Locale

/**
 * Repository layer that handles all API calls and maps HTTP/network errors to
 * [BarikoiError] subclasses. The API key is no longer needed here – it is
 * injected at the OkHttp layer inside [com.barikoi.sdk.BarikoiClient].
 */
class BarikoiRepository(
    private val apiService: BarikoiApiService
) {

    // ─── Geocoding ────────────────────────────────────────────────────────────

    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
        district: String? = null,
        postCode: String? = null,
        country: String? = null,
        subDistrict: String? = null,
        union: String? = null,
        pauroshova: String? = null,
        locationType: String? = null,
        division: String? = null,
        address: String? = null,
        area: String? = null,
        bangla: Boolean? = null
    ): Result<Place> = safeApiCall {
        val body = handleHttpResponse(
            apiService.reverseGeocode(
                latitude, longitude, district, postCode, country,
                subDistrict, union, pauroshova, locationType, division, address, area, bangla
            )
        )
        body.place ?: throw BarikoiError.ParseError("No place data in response")
    }

    suspend fun autocomplete(
        query: String,
        bangla: Boolean? = true
    ): Result<List<Place>> = safeApiCall {
        val body = handleHttpResponse(apiService.autocomplete(query, bangla))
        body.places ?: emptyList()
    }

    suspend fun rupantorGeocode(
        query: String,
        thana: Boolean = false,
        district: Boolean = false,
        bangla: Boolean = false
    ): Result<RupantorGeocodeResponse> = safeApiCall {
        handleHttpResponse(
            apiService.rupantorGeocode(
                query = query,
                thana = if (thana) "yes" else "no",
                district = if (district) "yes" else "no",
                bangla = if (bangla) "yes" else "no"
            )
        )
    }

    // ─── Routing ──────────────────────────────────────────────────────────────

    suspend fun routeOverview(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        profile: String? = null,
        geometries: String? = null
    ): Result<List<Route>> = safeApiCall {
        // Use Locale.US to guarantee decimal point (.) regardless of device locale.
        // Without this, devices with comma-decimal locales (e.g. German, French) produce
        // "23,81,90,41" instead of "23.81,90.41", causing a silent wrong-route or 400.
        val coordinates = String.format(
            Locale.US, "%.6f,%.6f;%.6f,%.6f",
            startLon, startLat, endLon, endLat
        )
        val body = handleHttpResponse(apiService.routeOverview(coordinates, profile, geometries))
        body.routes ?: emptyList()
    }

    suspend fun calculateRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        profile: String? = null
    ): Result<GraphHopperRouteResponse> = safeApiCall {
        val requestBody = try {
            JSONObject()
                .put(
                    "data", JSONObject()
                        .put(
                            "start", JSONObject()
                                .put("latitude", startLat)
                                .put("longitude", startLon)
                        )
                        .put(
                            "destination", JSONObject()
                                .put("latitude", endLat)
                                .put("longitude", endLon)
                        )
                )
                .toString()
                .toRequestBody("application/json".toMediaType())
        } catch (e: JSONException) {
            throw BarikoiError.UnknownError("Failed to build route request body: ${e.message}", e)
        }

        handleHttpResponse(apiService.calculateRoute(profile = profile, body = requestBody))
    }

    suspend fun optimizeRoute(
        sourceLat: Double,
        sourceLon: Double,
        destinationLat: Double,
        destinationLon: Double,
        waypoints: List<Pair<Double, Double>>,
        profile: String? = null
    ): Result<OptimizedRouteResponse> = safeApiCall {
        val requestBody = try {
            val geoPoints = JSONArray().apply {
                waypoints.forEachIndexed { index, (lat, lon) ->
                    // Locale.US ensures "." decimal separator on all devices
                    put(
                        JSONObject()
                            .put("id", index + 1)
                            .put("point", String.format(Locale.US, "%.6f,%.6f", lat, lon))
                    )
                }
            }
            JSONObject()
                .put("source", String.format(Locale.US, "%.6f,%.6f", sourceLat, sourceLon))
                .put("destination", String.format(Locale.US, "%.6f,%.6f", destinationLat, destinationLon))
                .put("geo_points", geoPoints)
                .apply { profile?.let { put("profile", it) } }
                .toString()
                .toRequestBody("application/json".toMediaType())
        } catch (e: JSONException) {
            throw BarikoiError.UnknownError("Failed to build optimize-route request body: ${e.message}", e)
        }

        handleHttpResponse(apiService.optimizeRoute(requestBody))
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    suspend fun searchPlace(query: String): Result<SearchPlaceResponse> = safeApiCall {
        handleHttpResponse(apiService.searchPlace(query))
    }

    suspend fun placeDetails(
        placeCode: String,
        sessionId: String
    ): Result<Place> = safeApiCall {
        val body = handleHttpResponse(apiService.placeDetails(placeCode, sessionId))
        body.place ?: throw BarikoiError.ParseError("No place data in response")
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    suspend fun snapToRoad(
        latitude: Double,
        longitude: Double
    ): Result<SnapToRoadResponse> = safeApiCall {
        // Locale.US prevents comma-decimal bug on non-English locales
        val point = String.format(Locale.US, "%.6f,%.6f", latitude, longitude)
        handleHttpResponse(apiService.snapToRoad(point))
    }

    suspend fun nearbyPlaces(
        latitude: Double,
        longitude: Double,
        radius: Double,
        limit: Int = 10
    ): Result<List<Place>> = safeApiCall {
        val body = handleHttpResponse(apiService.nearbyPlaces(radius, limit, latitude, longitude))
        body.places ?: emptyList()
    }

    suspend fun checkNearby(
        currentLat: Double,
        currentLon: Double,
        destinationLat: Double,
        destinationLon: Double,
        radiusMeters: Double
    ): Result<GeofenceResponse> = safeApiCall {
        handleHttpResponse(
            apiService.checkNearby(currentLat, currentLon, destinationLat, destinationLon, radiusMeters)
        )
    }

    // ─── Internals ────────────────────────────────────────────────────────────

    /**
     * Wraps any suspend API call, catching errors into typed [BarikoiError] subclasses.
     *
     * **IMPORTANT:** [CancellationException] is explicitly re-thrown so that Kotlin
     * structured concurrency is not broken – a cancelled coroutine must stay cancelled.
     *
     * Mapping rules:
     * - [CancellationException]           → re-thrown (never swallowed)
     * - [BarikoiError]                    → returned as [Result.failure] as-is
     * - [IllegalArgumentException]        → [BarikoiError.ValidationError]
     * - [JsonDataException]               → [BarikoiError.ParseError] (Moshi field mismatch)
     * - [JsonEncodingException]           → [BarikoiError.ParseError] (Moshi malformed JSON)
     * - [retrofit2.HttpException]         → [BarikoiError.HttpError] (non-Response<T> calls)
     * - [IOException]                     → [BarikoiError.NetworkError]
     * - everything else                   → [BarikoiError.UnknownError]
     */
    private suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            // Must never be caught – re-throw to keep structured concurrency intact
            throw e
        } catch (e: BarikoiError) {
            Result.failure(e)
        } catch (e: IllegalArgumentException) {
            // Comes from require() / check() in BarikoiClient validation helpers
            Result.failure(BarikoiError.ValidationError(e.message ?: "Invalid argument"))
        } catch (e: JsonDataException) {
            // Moshi structural mismatch (e.g. expected object but got array)
            Result.failure(BarikoiError.ParseError("Response parse error: ${e.message}", e))
        } catch (e: JsonEncodingException) {
            // Malformed / invalid JSON received from server
            Result.failure(BarikoiError.ParseError("Malformed JSON in response: ${e.message}", e))
        } catch (e: retrofit2.HttpException) {
            // Thrown by Retrofit when not using Response<T> wrapper (defensive)
            val code = e.code()
            val msg = e.message() ?: "HTTP $code"
            Result.failure(
                when (code) {
                    400 -> BarikoiError.BadRequestError(msg)
                    401, 403 -> BarikoiError.UnauthorizedError(msg)
                    402 -> BarikoiError.QuotaExceededError(msg)
                    429 -> BarikoiError.RateLimitError(msg)
                    in 500..599 -> BarikoiError.HttpError(code, "Server error ($code): $msg")
                    else -> BarikoiError.HttpError(code, "HTTP $code: $msg")
                }
            )
        } catch (e: IOException) {
            Result.failure(BarikoiError.NetworkError("Network error: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(BarikoiError.UnknownError("Unexpected error: ${e.message}", e))
        }
    }

    /**
     * Reads the response body or maps the HTTP status code to a typed [BarikoiError].
     *
     * [Response.errorBody] is consumed **once** here to avoid the silent-empty-string
     * bug that occurs when the stream is read more than once.
     */
    private fun <T> handleHttpResponse(response: Response<T>): T {
        if (response.isSuccessful) {
            return response.body()
                ?: throw BarikoiError.ParseError("Response body is null (HTTP ${response.code()})")
        }

        // Read the error body exactly once; a failure here just loses the body text.
        val errorMessage = try {
            response.errorBody()?.string()?.takeIf { it.isNotBlank() }
        } catch (_: IOException) {
            // Swallow – we still throw the right BarikoiError subclass below
            null
        }

        throw when (response.code()) {
            400 -> BarikoiError.BadRequestError(errorMessage ?: "Bad request")
            401 -> BarikoiError.UnauthorizedError(errorMessage ?: "Unauthorized – invalid API key")
            402 -> BarikoiError.QuotaExceededError(errorMessage ?: "Payment required – API quota exceeded")
            403 -> BarikoiError.UnauthorizedError(errorMessage ?: "Forbidden – access denied")
            404 -> BarikoiError.HttpError(404, errorMessage ?: "Resource not found")
            429 -> BarikoiError.RateLimitError(errorMessage ?: "Too many requests – please retry later")

            in 500..599 -> BarikoiError.HttpError(
                response.code(),
                "Server error (${response.code()}): ${errorMessage ?: "unknown"}"
            )

            else -> BarikoiError.HttpError(
                response.code(),
                "HTTP ${response.code()}: ${errorMessage ?: "unknown error"}"
            )
        }
    }
}
