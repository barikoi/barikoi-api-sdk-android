package com.barikoi.sdk.models

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * Moshi adapter that coerces a JSON string OR number to a Double (nullable).
 *
 * Some Barikoi API endpoints (e.g. Rupantor geocode) return latitude/longitude
 * as JSON strings instead of numbers.  This adapter handles both formats
 * transparently so that the [Place] model always exposes a [Double?].
 *
 * Usage: annotate fields with @StringOrDouble and register this adapter.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
@com.squareup.moshi.JsonQualifier
annotation class StringOrDouble

class StringOrDoubleAdapter {

    @FromJson
    @StringOrDouble
    fun fromJson(reader: JsonReader): Double? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                null
            }
            JsonReader.Token.NUMBER -> reader.nextDouble()
            JsonReader.Token.STRING -> reader.nextString().toDoubleOrNull()
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, @StringOrDouble value: Double?) {
        if (value == null) writer.nullValue() else writer.value(value)
    }
}

