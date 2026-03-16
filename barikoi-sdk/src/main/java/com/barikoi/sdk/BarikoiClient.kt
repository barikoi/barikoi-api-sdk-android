package com.barikoi.sdk

import com.barikoi.sdk.api.BarikoiApiService
import com.barikoi.sdk.models.FlexibleMapAdapter
import com.barikoi.sdk.models.GraphHopperPointsAdapter
import com.barikoi.sdk.models.StringOrDoubleAdapter
import com.barikoi.sdk.repository.BarikoiRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Main entry point for Barikoi SDK.
 *
 * ## Recommended usage (Application-level init)
 *
 * **Step 1 – initialize once in your `Application.onCreate()`:**
 * ```kotlin
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         BarikoiClient.init(
 *             apiKey      = BuildConfig.BARIKOI_API_KEY,
 *             enableLogging = BuildConfig.DEBUG
 *         )
 *     }
 * }
 * ```
 *
 * **Step 2 – obtain the singleton anywhere, no key required:**
 * ```kotlin
 * class MyActivity : AppCompatActivity() {
 *     // BarikoiClient() returns the same cached singleton every time.
 *     private val barikoi = BarikoiClient()
 *
 *     fun example() {
 *         lifecycleScope.launch {
 *             barikoi.reverseGeocode(23.8103, 90.4125)
 *                 .onSuccess { place -> ... }
 *                 .onFailure { error -> ... }
 *         }
 *     }
 * }
 * ```
 *
 * ## Advanced usage (Builder)
 * Use [Builder] when you need custom timeouts or a non-default base URL.
 * Note: Builder instances are **not** cached as the global singleton.
 */
class BarikoiClient private constructor(
    private val apiKey: String,
    private val baseUrl: String,
    private val enableLogging: Boolean,
    private val connectTimeoutSeconds: Long,
    private val readTimeoutSeconds: Long,
    private val writeTimeoutSeconds: Long
) {
    private val repository: BarikoiRepository

    init {
        val moshi = Moshi.Builder()
            .add(GraphHopperPointsAdapter())
            .add(StringOrDoubleAdapter())
            .add(FlexibleMapAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()

        // Inject an API key into every request automatically – callers never touch it directly.
        val apiKeyInterceptor = Interceptor { chain ->
            val original = chain.request()
            val url = original.url.newBuilder()
                .addQueryParameter("api_key", apiKey)
                .build()
            chain.proceed(original.newBuilder().url(url).build())
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(apiKeyInterceptor)
            .apply {
                if (enableLogging) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val apiService = retrofit.create(BarikoiApiService::class.java)
        repository = BarikoiRepository(apiService)
    }

    // ─── Geocoding ────────────────────────────────────────────────────────────

    /**
     * Reverse geocode: convert [latitude]/[longitude] to a human-readable address.
     */
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
    ) = validated(latitude, longitude) {
        repository.reverseGeocode(
            latitude, longitude, district, postCode, country, subDistrict,
            union, pauroshova, locationType, division, address, area, bangla
        )
    }

    /** Autocomplete: return place suggestions for [query]. */
    suspend fun autocomplete(
        query: String,
        bangla: Boolean? = true
    ) = validatedQuery(query) {
        repository.autocomplete(query, bangla)
    }

    /** Rupantor geocode: format and geocode a free-text [query] address. */
    suspend fun rupantorGeocode(
        query: String,
        thana: Boolean = false,
        district: Boolean = false,
        bangla: Boolean = false
    ) = validatedQuery(query) {
        repository.rupantorGeocode(query, thana, district, bangla)
    }

    // ─── Routing ──────────────────────────────────────────────────────────────

    /** Route overview: get a route summary (distance + duration) between two points. */
    suspend fun routeOverview(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        profile: String? = null,
        geometries: String? = null
    ) = validated(startLat, startLon, endLat, endLon) {
        repository.routeOverview(startLat, startLon, endLat, endLon, profile, geometries)
    }

    /** Calculate route: get turn-by-turn instructions via GraphHopper. */
    suspend fun calculateRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        profile: String? = null
    ) = validated(startLat, startLon, endLat, endLon) {
        repository.calculateRoute(startLat, startLon, endLat, endLon, profile)
    }

    /**
     * Optimize route: find the most efficient order to visit all [waypoints].
     *
     * @param waypoints List of (latitude, longitude) pairs to visit. Must not be empty.
     */
    suspend fun optimizeRoute(
        sourceLat: Double,
        sourceLon: Double,
        destinationLat: Double,
        destinationLon: Double,
        waypoints: List<Pair<Double, Double>>,
        profile: String? = null
    ) = validated(sourceLat, sourceLon, destinationLat, destinationLon) {
        require(waypoints.isNotEmpty()) { "waypoints must not be empty" }
        repository.optimizeRoute(sourceLat, sourceLon, destinationLat, destinationLon, waypoints, profile)
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    /**
     * Search place: full-text search. Returns a session ID required for [placeDetails].
     */
    suspend fun searchPlace(query: String) = validatedQuery(query) {
        repository.searchPlace(query)
    }

    /**
     * Place details: fetch full information for [placeCode] from [searchPlace].
     *
     * @param sessionId The session ID returned by [searchPlace].
     */
    suspend fun placeDetails(
        placeCode: String,
        sessionId: String
    ): Result<com.barikoi.sdk.models.Place> {
        require(placeCode.isNotBlank()) { "placeCode must not be blank" }
        require(sessionId.isNotBlank()) { "sessionId must not be blank – obtain it from searchPlace()" }
        return repository.placeDetails(placeCode, sessionId)
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    /** Snap to a road: find the nearest road point for [latitude]/[longitude]. */
    suspend fun snapToRoad(
        latitude: Double,
        longitude: Double
    ) = validated(latitude, longitude) {
        repository.snapToRoad(latitude, longitude)
    }

    /**
     * Nearby places: find places within [radius] km of [latitude]/[longitude].
     *
     * @param radius Search radius in kilometres (must be > 0).
     * @param limit  Maximum number of results (default 10).
     */
    suspend fun nearbyPlaces(
        latitude: Double,
        longitude: Double,
        radius: Double,
        limit: Int = 10
    ) = validated(latitude, longitude) {
        require(radius > 0) { "radius must be > 0, was $radius" }
        require(limit > 0) { "limit must be > 0, was $limit" }
        repository.nearbyPlaces(latitude, longitude, radius, limit)
    }

    /**
     * Check nearby (geofence): check whether [destinationLat]/[destinationLon] is
     * within [radiusMeters] metres of [currentLat]/[currentLon].
     */
    suspend fun checkNearby(
        currentLat: Double,
        currentLon: Double,
        destinationLat: Double,
        destinationLon: Double,
        radiusMeters: Double
    ) = validated(currentLat, currentLon, destinationLat, destinationLon) {
        require(radiusMeters > 0) { "radiusMeters must be > 0, was $radiusMeters" }
        repository.checkNearby(currentLat, currentLon, destinationLat, destinationLon, radiusMeters)
    }

    // ─── Validation helpers ───────────────────────────────────────────────────

    private inline fun <T> validated(lat: Double, lon: Double, block: () -> T): T {
        require(lat in -90.0..90.0) { "latitude must be in [-90, 90], was $lat" }
        require(lon in -180.0..180.0) { "longitude must be in [-180, 180], was $lon" }
        return block()
    }

    private inline fun <T> validated(
        lat1: Double, lon1: Double, lat2: Double, lon2: Double, block: () -> T
    ): T {
        require(lat1 in -90.0..90.0) { "startLat must be in [-90, 90], was $lat1" }
        require(lon1 in -180.0..180.0) { "startLon must be in [-180, 180], was $lon1" }
        require(lat2 in -90.0..90.0) { "endLat must be in [-90, 90], was $lat2" }
        require(lon2 in -180.0..180.0) { "endLon must be in [-180, 180], was $lon2" }
        return block()
    }

    private inline fun <T> validatedQuery(query: String, block: () -> T): T {
        require(query.isNotBlank()) { "query must not be blank" }
        return block()
    }

    // ─── Companion: global init + no-arg factory ──────────────────────────────

    companion object {

        @Volatile private var config: Config? = null
        @Volatile private var defaultInstance: BarikoiClient? = null

        /**
         * Initialize the SDK once – call this in `Application.onCreate()`.
         *
         * ```kotlin
         * BarikoiClient.init(
         *     apiKey        = BuildConfig.BARIKOI_API_KEY,
         *     enableLogging = BuildConfig.DEBUG
         * )
         * ```
         *
         * After calling [init], create client instances anywhere with just `BarikoiClient()`.
         *
         * @param apiKey        Your Barikoi API key (required).
         * @param enableLogging Set `true` to print full HTTP logs (use only in debug builds).
         * @param baseUrl       Override the base URL (optional).
         * @param connectTimeoutSeconds TCP connect timeout in seconds (default 30).
         * @param readTimeoutSeconds    Socket read timeout in seconds (default 30).
         * @param writeTimeoutSeconds   Socket write timeout in seconds (default 30).
         */
        @JvmStatic
        @JvmOverloads
        fun init(
            apiKey: String,
            enableLogging: Boolean = false,
            baseUrl: String = "https://barikoi.xyz/",
            connectTimeoutSeconds: Long = 30,
            readTimeoutSeconds: Long = 30,
            writeTimeoutSeconds: Long = 30
        ) {
            require(apiKey.isNotBlank()) { "API key must not be blank." }
            val newConfig = Config(
                apiKey = apiKey,
                baseUrl = baseUrl,
                enableLogging = enableLogging,
                connectTimeoutSeconds = connectTimeoutSeconds,
                readTimeoutSeconds = readTimeoutSeconds,
                writeTimeoutSeconds = writeTimeoutSeconds
            )
            // Invalidate a cached instance whenever config changes
            synchronized(this) {
                config = newConfig
                defaultInstance = null
            }
        }

        /**
         * Returns the shared [BarikoiClient] singleton created from the config set by [init].
         *
         * The instance is created **lazily** on the first call and reused for all subsequent
         * calls – no new `OkHttpClient` or `Retrofit` is allocated on each use.
         *
         * ```kotlin
         * private val barikoi = BarikoiClient()
         * ```
         *
         * @throws IllegalStateException if [init] has not been called yet.
         */
        @JvmStatic
        operator fun invoke(): BarikoiClient {
            // Fast path – already initialized
            defaultInstance?.let { return it }
            return synchronized(this) {
                // Double-checked locking
                defaultInstance ?: run {
                    val cfg = config
                        ?: error("BarikoiClient is not initialised. Call BarikoiClient.init() in your Application.onCreate().")
                    BarikoiClient(
                        apiKey                = cfg.apiKey,
                        baseUrl               = cfg.baseUrl,
                        enableLogging         = cfg.enableLogging,
                        connectTimeoutSeconds = cfg.connectTimeoutSeconds,
                        readTimeoutSeconds    = cfg.readTimeoutSeconds,
                        writeTimeoutSeconds   = cfg.writeTimeoutSeconds
                    ).also { defaultInstance = it }
                }
            }
        }

        /** Holds the global SDK configuration set by [init]. */
        private data class Config(
            val apiKey: String,
            val baseUrl: String,
            val enableLogging: Boolean,
            val connectTimeoutSeconds: Long,
            val readTimeoutSeconds: Long,
            val writeTimeoutSeconds: Long
        )
    }

    // ─── Builder (advanced use) ───────────────────────────────────────────────

    /**
     * Fluent builder for [BarikoiClient] – use when you need custom timeouts or
     * a non-default base URL. For the common case prefer [init] + `BarikoiClient()`.
     */
    class Builder {
        private var apiKey: String = ""
        private var baseUrl: String = "https://barikoi.xyz/"
        private var enableLogging: Boolean = false
        private var connectTimeoutSeconds: Long = 30
        private var readTimeoutSeconds: Long = 30
        private var writeTimeoutSeconds: Long = 30

        /** Set the Barikoi API key (required). */
        fun apiKey(key: String) = apply { this.apiKey = key }

        /** Override the base URL (optional, defaults to https://barikoi.xyz/). */
        private fun baseUrl(url: String) = apply { this.baseUrl = url }

        /** Enable full HTTP body logging. Use only in debug builds. */
        fun enableLogging(enable: Boolean) = apply { this.enableLogging = enable }

        /** TCP connection timeout in **seconds** (default 30). */
        fun connectTimeoutSeconds(seconds: Long) = apply { this.connectTimeoutSeconds = seconds }

        /** Socket read timeout in **seconds** (default 30). */
        fun readTimeoutSeconds(seconds: Long) = apply { this.readTimeoutSeconds = seconds }

        /** Socket write timeout in **seconds** (default 30). */
        fun writeTimeoutSeconds(seconds: Long) = apply { this.writeTimeoutSeconds = seconds }

        /**
         * Build and return a [BarikoiClient].
         * @throws IllegalArgumentException if [apiKey] is blank.
         */
        fun build(): BarikoiClient {
            require(apiKey.isNotBlank()) { "API key is required. Call apiKey(\"...\") on the Builder." }
            return BarikoiClient(
                apiKey                = apiKey,
                baseUrl               = baseUrl,
                enableLogging         = enableLogging,
                connectTimeoutSeconds = connectTimeoutSeconds,
                readTimeoutSeconds    = readTimeoutSeconds,
                writeTimeoutSeconds   = writeTimeoutSeconds
            )
        }
    }
}
