package com.barikoi.sdk.models

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * Represents GraphHopper points / snapped_waypoints which can be either:
 * - A GeoJSON LineString object (when points_encoded = false)
 * - An encoded polyline string (when points_encoded = true)
 */
sealed class GraphHopperPoints {
    data class GeoJson(val lineString: GeoJsonLineString) : GraphHopperPoints()
    data class Encoded(val polyline: String) : GraphHopperPoints()

    /** Convenience: get coordinates list if GeoJSON, null otherwise */
    fun coordinatesOrNull(): List<List<Double>>? =
        (this as? GeoJson)?.lineString?.coordinates

    /** Convenience: get encoded polyline if encoded, null otherwise */
    fun encodedOrNull(): String? =
        (this as? Encoded)?.polyline
}

/**
 * Moshi adapter that handles both GeoJSON object and encoded string for GraphHopper points fields.
 */
class GraphHopperPointsAdapter {

    @FromJson
    fun fromJson(reader: JsonReader, geoJsonAdapter: JsonAdapter<GeoJsonLineString>): GraphHopperPoints? {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> {
                val s = reader.nextString()
                GraphHopperPoints.Encoded(s)
            }
            JsonReader.Token.BEGIN_OBJECT -> {
                val lineString = geoJsonAdapter.fromJson(reader)
                lineString?.let { GraphHopperPoints.GeoJson(it) }
            }
            JsonReader.Token.BEGIN_ARRAY -> {
                // Some API responses return [] instead of a valid points object; skip safely.
                reader.skipValue()
                null
            }
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                null
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: GraphHopperPoints?, geoJsonAdapter: JsonAdapter<GeoJsonLineString>) {
        when (value) {
            is GraphHopperPoints.Encoded -> writer.value(value.polyline)
            is GraphHopperPoints.GeoJson -> geoJsonAdapter.toJson(writer, value.lineString)
            null -> writer.nullValue()
        }
    }
}

