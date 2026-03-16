package com.barikoi.sdk.api

import com.barikoi.sdk.models.*
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for Barikoi Location APIs.
 *
 * The `api_key` query parameter is injected automatically for every request by the
 * OkHttp interceptor configured in [com.barikoi.sdk.BarikoiClient] – it does NOT
 * appear in any method signature here.
 */
interface BarikoiApiService {

    /** Reverse Geocoding – convert lat/lng to address. */
    @GET("v2/api/search/reverse/geocode")
    suspend fun reverseGeocode(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("district") district: String? = null,
        @Query("post_code") postCode: String? = null,
        @Query("country") country: String? = null,
        @Query("sub_district") subDistrict: String? = null,
        @Query("union") union: String? = null,
        @Query("pauroshova") pauroshova: String? = null,
        @Query("location_type") locationType: String? = null,
        @Query("division") division: String? = null,
        @Query("address") address: String? = null,
        @Query("area") area: String? = null,
        @Query("bangla") bangla: Boolean? = null
    ): Response<ReverseGeocodeResponse>

    /** Autocomplete – get place suggestions. */
    @GET("v2/api/search/autocomplete/place")
    suspend fun autocomplete(
        @Query("q") query: String,
        @Query("bangla") bangla: Boolean? = true
    ): Response<AutocompleteResponse>

    /**
     * Rupantor Geocode – format and geocode address.
     * Requires application/x-www-form-urlencoded (FormData), not JSON.
     */
    @FormUrlEncoded
    @POST("v2/api/search/rupantor/geocode")
    suspend fun rupantorGeocode(
        @Field("q") query: String,
        @Field("thana") thana: String? = null,
        @Field("district") district: String? = null,
        @Field("bangla") bangla: String? = null
    ): Response<RupantorGeocodeResponse>

    /** Route Overview – get route summary between two points. */
    @GET("v2/api/route/{coordinates}")
    suspend fun routeOverview(
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("profile") profile: String? = null,
        @Query("geometries") geometries: String? = null
    ): Response<RouteOverviewResponse>

    /** Calculate Route – get detailed route with GraphHopper. */
    @POST("v2/api/routing")
    suspend fun calculateRoute(
        @Query("type") type: String = "gh",
        @Query("profile") profile: String? = null,
        @Body body: RequestBody
    ): Response<GraphHopperRouteResponse>

    /** Route Optimization – optimise route with multiple waypoints. */
    @POST("v2/api/route/optimized")
    suspend fun optimizeRoute(
        @Body body: RequestBody
    ): Response<OptimizedRouteResponse>

    /** Search Place – full-text search for places; returns a session ID. */
    @GET("api/v2/search-place")
    suspend fun searchPlace(
        @Query("q") query: String
    ): Response<SearchPlaceResponse>

    /** Place Details – fetch full information for a given place code. */
    @GET("api/v2/places")
    suspend fun placeDetails(
        @Query("place_code") placeCode: String,
        @Query("session_id") sessionId: String
    ): Response<PlaceDetailsResponse>

    /** Snap to Road – find the nearest road point. */
    @GET("v2/api/routing/nearest")
    suspend fun snapToRoad(
        @Query("point") point: String   // "lat,lon"
    ): Response<SnapToRoadResponse>

    /** Nearby Places – find places within a radius. */
    @GET("v2/api/search/nearby/{radius}/{limit}")
    suspend fun nearbyPlaces(
        @Path("radius") radius: Double,
        @Path("limit") limit: Int,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): Response<NearbyResponse>

    /** Check Nearby (Geofence) – check if a destination is within a radius. */
    @GET("v2/api/check/nearby")
    suspend fun checkNearby(
        @Query("current_latitude") currentLatitude: Double,
        @Query("current_longitude") currentLongitude: Double,
        @Query("destination_latitude") destinationLatitude: Double,
        @Query("destination_longitude") destinationLongitude: Double,
        @Query("radius") radius: Double     // in metres
    ): Response<GeofenceResponse>
}
