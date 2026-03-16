package com.barikoi.sdk.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Address components for detailed address information
 */
@JsonClass(generateAdapter = true)
data class AddressComponents(
    @Json(name = "place_name") val placeName: String? = null,
    @Json(name = "house") val house: String? = null,
    @Json(name = "road") val road: String? = null
)

/**
 * Area components for detailed area information
 */
@JsonClass(generateAdapter = true)
data class AreaComponents(
    @Json(name = "area") val area: String? = null,
    @Json(name = "sub_area") val subArea: String? = null
)

/**
 * Represents a place/location in Barikoi system.
 *
 * Different endpoints return different field names for the same concepts
 * (e.g. `"address"` vs `"Address"`, `"postcode"` vs `"postCode"`).
 * Use the convenience properties below instead of the raw fields:
 *  - [displayAddress] – best available address string
 *  - [displayPostCode] – best available postcode string
 */
@JsonClass(generateAdapter = true)
data class Place(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "place_code") val placeCode: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "Address") val addressAlt: String? = null, // Alternative field name used in some responses
    @Json(name = "address_short") val addressShort: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "area") val area: String? = null,
    @Json(name = "sub_area") val subArea: String? = null,
    @Json(name = "super_sub_area") val superSubArea: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "postcode") val postcode: String? = null,
    @Json(name = "postCode") val postCode: Int? = null, // Alternative naming
    @Json(name = "district") val district: String? = null,
    @Json(name = "sub_district") val subDistrict: String? = null,
    @Json(name = "thana") val thana: String? = null,
    @Json(name = "thana_bn") val thanaBn: String? = null,
    @Json(name = "union") val union: String? = null,
    @Json(name = "unions") val unions: String? = null,
    @Json(name = "pauroshova") val pauroshova: String? = null,
    @Json(name = "division") val division: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "latitude") @StringOrDouble val latitude: Double? = null,
    @Json(name = "longitude") @StringOrDouble val longitude: Double? = null,
    @Json(name = "holding_number") val holdingNumber: String? = null,
    @Json(name = "road_name_number") val roadNameNumber: String? = null,
    @Json(name = "address_bn") val addressBn: String? = null,
    @Json(name = "area_bn") val areaBn: String? = null,
    @Json(name = "city_bn") val cityBn: String? = null,
    @Json(name = "location_type") val locationType: String? = null,
    @Json(name = "distance_within_meters") val distanceWithinMeters: Double? = null,
    @Json(name = "distance_in_meters") val distanceInMeters: String? = null, // Alternative naming
    @Json(name = "pType") val pType: String? = null,
    @Json(name = "subType") val subType: String? = null,
    @Json(name = "uCode") val uCode: String? = null,
    @Json(name = "address_components") val addressComponents: AddressComponents? = null,
    @Json(name = "area_components") val areaComponents: AreaComponents? = null
) {
    /**
     * Returns the best available address string across the two naming variants used
     * by different Barikoi endpoints (`"address"` and `"Address"`).
     */
    val displayAddress: String?
        get() = address?.takeIf { it.isNotBlank() } ?: addressAlt?.takeIf { it.isNotBlank() }

    /**
     * Returns the best available postcode string across the two naming variants
     * (`"postcode"` string and `"postCode"` integer).
     */
    val displayPostCode: String?
        get() = postcode?.takeIf { it.isNotBlank() } ?: postCode?.toString()
}

/**
 * Represents geographic coordinates
 */
@JsonClass(generateAdapter = true)
data class Geometry(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double
)

/**
 * Response for autocomplete search
 */
@JsonClass(generateAdapter = true)
data class AutocompleteResponse(
    @Json(name = "places") val places: List<Place>? = null,
    @Json(name = "status") val status: Int? = null
)

/**
 * Response for reverse geocoding
 */
@JsonClass(generateAdapter = true)
data class ReverseGeocodeResponse(
    @Json(name = "place") val place: Place? = null,
    @Json(name = "status") val status: Int? = null
)

/**
 * Response for Rupantor geocoding
 *
 * The actual API response wraps the place data inside "geocoded_address" and includes
 * additional metadata fields such as given_address, fixed_address, bangla_address,
 * address_status, and confidence_score_percentage.
 */
@JsonClass(generateAdapter = true)
data class RupantorGeocodeResponse(
    @Json(name = "given_address") val givenAddress: String? = null,
    @Json(name = "fixed_address") val fixedAddress: String? = null,
    @Json(name = "bangla_address") val banglaAddress: String? = null,
    @Json(name = "address_status") val addressStatus: String? = null,
    @Json(name = "confidence_score_percentage") val confidenceScorePercentage: Int? = null,
    @Json(name = "geocoded_address") val geocodedAddress: Place? = null,
    @Json(name = "status") val status: Int? = null
)

/**
 * Response for nearby places search
 */
@JsonClass(generateAdapter = true)
data class NearbyResponse(
    @Json(name = "places") val places: List<Place>? = null,
    @Json(name = "status") val status: Int? = null
)

/**
 * Response for place search with session
 */
@JsonClass(generateAdapter = true)
data class SearchPlaceResponse(
    @Json(name = "places") val places: List<Place>? = null,
    @Json(name = "session_id") val sessionId: String? = null,
    @Json(name = "status") val status: Int? = null
)

/**
 * Response for place details
 */
@JsonClass(generateAdapter = true)
data class PlaceDetailsResponse(
    @Json(name = "place") val place: Place? = null,
    @Json(name = "status") val status: Int? = null
)

/**
 * Route waypoint/point
 */
@JsonClass(generateAdapter = true)
data class RoutePoint(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double
)

/**
 * Route overview response
 */
@JsonClass(generateAdapter = true)
data class RouteOverviewResponse(
    @Json(name = "routes") val routes: List<Route>? = null,
    @Json(name = "status") val status: Int? = null
)

/**
 * Route information
 */
@JsonClass(generateAdapter = true)
data class Route(
    @Json(name = "distance") val distance: Double? = null, // in meters
    @Json(name = "duration") val duration: Double? = null, // in seconds
    @Json(name = "geometry") val geometry: String? = null, // encoded polyline
    @Json(name = "legs") val legs: List<RouteLeg>? = null,
    @Json(name = "weight") val weight: Double? = null
)

/**
 * Route leg (segment)
 */
@JsonClass(generateAdapter = true)
data class RouteLeg(
    @Json(name = "distance") val distance: Double? = null,
    @Json(name = "duration") val duration: Double? = null,
    @Json(name = "steps") val steps: List<RouteStep>? = null
)

/**
 * Route step (instruction)
 */
@JsonClass(generateAdapter = true)
data class RouteStep(
    @Json(name = "distance") val distance: Double? = null,
    @Json(name = "duration") val duration: Double? = null,
    @Json(name = "instruction") val instruction: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "mode") val mode: String? = null
)

/**
 * GeoJSON LineString geometry returned by GraphHopper when points_encoded = false
 */
@JsonClass(generateAdapter = true)
data class GeoJsonLineString(
    @Json(name = "type") val type: String? = null,
    @Json(name = "coordinates") val coordinates: List<List<Double>>? = null
)

/**
 * GraphHopper route response
 */
@JsonClass(generateAdapter = true)
data class GraphHopperRouteResponse(
    @Json(name = "paths") val paths: List<GraphHopperPath>? = null,
    @Json(name = "info") val info: GraphHopperInfo? = null
)

/**
 * GraphHopper path.
 *
 * When points_encoded = false (default for Barikoi API), both [points] and
 * [snappedWaypoints] are GeoJSON LineString objects ([GraphHopperPoints.GeoJson]).
 * When points_encoded = true, [points] is an encoded polyline string
 * ([GraphHopperPoints.Encoded]).
 */
@JsonClass(generateAdapter = true)
data class GraphHopperPath(
    @Json(name = "distance") val distance: Double? = null,
    @Json(name = "weight") val weight: Double? = null,
    @Json(name = "time") val time: Long? = null,
    @Json(name = "transfers") val transfers: Int? = null,
    @Json(name = "points_encoded") val pointsEncoded: Boolean? = null,
    @Json(name = "points_encoded_multiplier") val pointsEncodedMultiplier: Int? = null,
    @Json(name = "bbox") val bbox: List<Double?>? = null,
    /** GeoJSON LineString when points_encoded = false, or encoded polyline string when points_encoded = true */
    @Json(name = "points") val points: GraphHopperPoints? = null,
    /** GeoJSON LineString when points_encoded = false, or encoded polyline string when points_encoded = true */
    @Json(name = "snapped_waypoints") val snappedWaypoints: GraphHopperPoints? = null,
    @Json(name = "ascend") val ascend: Double? = null,
    @Json(name = "descend") val descend: Double? = null,
    /**
     * Route legs. The API returns `[]` when empty instead of `{}`, so
     * [@FlexibleMap][FlexibleMap] coerces `[]` to `null` and stores any
     * populated object as a raw JSON string.
     */
    @Json(name = "legs") @FlexibleMap val legs: String? = null,
    /**
     * Route detail segments (e.g. street_name, time, distance ranges).
     * The API returns `[]` when empty instead of `{}`, so
     * [@FlexibleMap][FlexibleMap] coerces `[]` to `null` and stores any
     * populated object as a raw JSON string.
     */
    @Json(name = "details") @FlexibleMap val details: String? = null,
    @Json(name = "instructions") val instructions: List<GraphHopperInstruction>? = null
) {
    /** Distance in kilometres (convenience wrapper around the raw metres value). */
    val distanceInKm: Double? get() = distance?.let { it / 1000.0 }

    /** Duration in minutes (convenience wrapper around the raw milliseconds value). */
    val durationInMinutes: Double? get() = time?.let { it / 60_000.0 }
}

/**
 * GraphHopper instruction
 */
@JsonClass(generateAdapter = true)
data class GraphHopperInstruction(
    @Json(name = "distance") val distance: Double? = null,
    @Json(name = "time") val time: Long? = null,
    @Json(name = "text") val text: String? = null,
    @Json(name = "street_name") val streetName: String? = null,
    @Json(name = "sign") val sign: Int? = null,          // -2=left,-1=slight-left,0=straight,1=slight-right,2=right,4=arrive,5=waypoint,-98=u-turn
    @Json(name = "heading") val heading: Double? = null,
    @Json(name = "last_heading") val lastHeading: Double? = null,
    @Json(name = "exit_number") val exitNumber: Int? = null,
    @Json(name = "exited") val exited: Boolean? = null,
    @Json(name = "turn_angle") val turnAngle: Double? = null,
    @Json(name = "interval") val interval: List<Int>? = null
)

/**
 * GraphHopper info
 */
@JsonClass(generateAdapter = true)
data class GraphHopperInfo(
    @Json(name = "copyrights") val copyrights: List<String>? = null,
    @Json(name = "took") val took: Long? = null
)


/**
 * Snap point for snap-to-road response
 */
@JsonClass(generateAdapter = true)
data class SnapPoint(
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "distance") val distance: Double? = null // distance from original point
)

/**
 * Snap to road response
 */
@JsonClass(generateAdapter = true)
data class SnapToRoadResponse(
    @Json(name = "snappedPoints") val snappedPoints: List<SnapPoint>? = null,
    @Json(name = "coordinates") val coordinates: List<Double>? = null, // [longitude, latitude]
    @Json(name = "distance") val distance: Double? = null, // distance from original point
    @Json(name = "type") val type: String? = null, // "Point"
    @Json(name = "status") val status: Int? = null
) {
    // Helper properties for easy access
    val snappedLongitude: Double? get() = coordinates?.getOrNull(0)
    val snappedLatitude: Double? get() = coordinates?.getOrNull(1)
}

/**
 * Geofence data details.
 *
 * The API returns [radius], [latitude] and [longitude] as JSON strings,
 * so [@StringOrDouble][StringOrDouble] is used to coerce them transparently to [Double].
 */
@JsonClass(generateAdapter = true)
data class GeofenceData(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "radius") @StringOrDouble val radius: Double? = null,
    @Json(name = "latitude") @StringOrDouble val latitude: Double? = null,
    @Json(name = "longitude") @StringOrDouble val longitude: Double? = null,
    @Json(name = "user_id") val userId: Int? = null
)

/**
 * Geofence check response
 */
@JsonClass(generateAdapter = true)
data class GeofenceResponse(
    @Json(name = "isWithinRadius") val isWithinRadius: Boolean? = null,
    @Json(name = "distance") val distance: Double? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: Int? = null,
    @Json(name = "data") val data: GeofenceData? = null
) {
    // Helper property to check if point is inside geofence
    val isInside: Boolean
        get() = isWithinRadius == true || message?.contains("Inside", ignoreCase = true) == true

    // Helper property to get distance if needed (calculate from data)
    val radiusMeters: Double?
        get() = distance ?: data?.radius
}

/**
 * Optimized trip in optimized route response (legacy - kept for compatibility)
 */
@JsonClass(generateAdapter = true)
data class OptimizedTrip(
    @Json(name = "distance") val distance: Double? = null,
    @Json(name = "duration") val duration: Double? = null,
    @Json(name = "geometry") val geometry: String? = null,
    @Json(name = "legs") val legs: List<RouteLeg>? = null
)

/**
 * Hints from optimized route response
 */
@JsonClass(generateAdapter = true)
data class RouteHints(
    @Json(name = "visited_nodes.sum") val visitedNodesSum: Int? = null,
    @Json(name = "visited_nodes.average") val visitedNodesAverage: Double? = null
)

/**
 * Optimized route response
 * The Barikoi optimize route API returns the same GraphHopper structure as calculateRoute,
 * with top-level: hints, info, paths[].
 */
@JsonClass(generateAdapter = true)
data class OptimizedRouteResponse(
    @Json(name = "hints") val hints: RouteHints? = null,
    @Json(name = "info") val info: GraphHopperInfo? = null,
    @Json(name = "paths") val paths: List<GraphHopperPath>? = null,
    // Legacy fields kept for backward compatibility
    @Json(name = "code") val code: Int? = null,
    @Json(name = "trips") val trips: List<OptimizedTrip>? = null,
    @Json(name = "routes") val routes: List<Route>? = null,
    @Json(name = "waypoints") val waypoints: List<RoutePoint>? = null,
    @Json(name = "status") val status: Int? = null
)

