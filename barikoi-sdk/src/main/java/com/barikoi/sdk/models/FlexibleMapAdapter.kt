package com.barikoi.sdk.models

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * Marks a field whose JSON value may be either an object `{}` or an empty array `[]`.
 *
 * Some Barikoi / GraphHopper API fields (e.g. `details`, `legs`) are documented as
 * objects but the server returns an empty JSON array `[]` when there is no data.
 * Moshi throws "Expected BEGIN_OBJECT but was BEGIN_ARRAY" in that case.
 *
 * Fields annotated with [@FlexibleMap] are parsed as:
 * - `{}` or `{…}` → raw JSON string
 * - `[]`           → `null`  (empty array treated as absent)
 * - `null`         → `null`
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
@com.squareup.moshi.JsonQualifier
annotation class FlexibleMap

/**
 * Moshi adapter for [@FlexibleMap]-annotated `String?` fields.
 *
 * Handles the Barikoi/GraphHopper API quirk where `details` and `legs` are
 * returned as `[]` (empty JSON array) instead of `{}` when there is no data.
 * The standard Moshi `@JsonClass` KSP adapter would throw
 * "Expected BEGIN_OBJECT but was BEGIN_ARRAY" without this.
 *
 * Parsing rules:
 * - `null`    → `null`
 * - `[]`      → `null`   (empty array treated as absent)
 * - `[…]`     → `null`   (non-empty array also ignored; not a valid map)
 * - `{}`/`{…}`→ raw JSON string so callers can parse further if needed
 */
class FlexibleMapAdapter {

    @FromJson
    @FlexibleMap
    fun fromJson(reader: JsonReader): String? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                null
            }
            JsonReader.Token.BEGIN_ARRAY -> {
                // API returns [] (or sometimes a non-empty array) where an object
                // is expected – consume the entire array token and treat as absent.
                reader.skipValue()
                null
            }
            JsonReader.Token.BEGIN_OBJECT -> {
                // Capture the object as a raw JSON string for optional further parsing.
                reader.nextSource().readUtf8()
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, @FlexibleMap value: String?) {
        if (value == null) writer.nullValue() else writer.value(value)
    }
}
