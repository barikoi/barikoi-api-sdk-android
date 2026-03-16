package com.barikoi.barikoiapis

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.barikoi.barikoiapis.databinding.ActivityMainBinding
import com.barikoi.sdk.BarikoiClient
import com.barikoi.sdk.models.Place
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val barikoiClient = BarikoiClient.Builder()
        .apiKey(BuildConfig.BARIKOI_API_KEY)
        .enableLogging(true)
        .build()


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    // Permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted
                getCurrentLocation()
            }

            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Approximate location access granted
                getCurrentLocation()
            }

            else -> {
                // No location access granted
                showToast("❌ Location permission denied")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Request location permission on start
        checkAndRequestLocationPermission()

        // Setup button click listeners
        setupButtonListeners()
    }


    private fun setupButtonListeners() {
        // Geocoding APIs
        binding.btnReverseGeocode.setOnClickListener {
            showToast("Running Reverse Geocode...")
            lifecycleScope.launch { reverseGeocodeExample() }
        }

        binding.btnRupantorGeocode.setOnClickListener {
            showToast("Running Rupantor Geocode...")
            lifecycleScope.launch { rupantorGeocodeExample() }
        }

        // Search APIs
        binding.btnAutocomplete.setOnClickListener {
            showToast("Running Autocomplete...")
            lifecycleScope.launch { autocompleteExample() }
        }

        binding.btnSearchPlace.setOnClickListener {
            showToast("Running Search Place...")
            lifecycleScope.launch { searchPlaceExample() }
        }

        binding.btnNearbyPlaces.setOnClickListener {
            showToast("Running Nearby Places...")
            lifecycleScope.launch { nearbyPlacesExample() }
        }

        // Open Search Page
        binding.btnOpenSearchPage.setOnClickListener {
            SearchActivity.start(this)
        }

        // Routing APIs
        binding.btnRouteOverview.setOnClickListener {
            showToast("Running Route Overview...")
            lifecycleScope.launch { routeOverviewExample() }
        }

        binding.btnCalculateRoute.setOnClickListener {
            showToast("Running Calculate Route...")
            lifecycleScope.launch { calculateRouteExample() }
        }

        binding.btnOptimizeRoute.setOnClickListener {
            showToast("Running Optimize Route...")
            lifecycleScope.launch { optimizeRouteExample() }
        }

        // Utility APIs
        binding.btnSnapToRoad.setOnClickListener {
            showToast("Running Snap to Road...")
            lifecycleScope.launch { snapToRoadExample() }
        }

        binding.btnCheckNearby.setOnClickListener {
            showToast("Running Geofence Check...")
            lifecycleScope.launch { checkNearbyExample() }
        }

        // Run all tests
        binding.btnRunAllTests.setOnClickListener {
            showToast("Running All API Tests...")
            lifecycleScope.launch { runAllTests() }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private suspend fun runAllTests() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "RUNNING ALL API TESTS")
        Log.d(TAG, "========================================")

        reverseGeocodeExample()
        autocompleteExample()
        nearbyPlacesExample()
        routeOverviewExample()
        searchPlaceExample()
        snapToRoadExample()
        checkNearbyExample()
        rupantorGeocodeExample()
        calculateRouteExample()
        optimizeRouteExample()

        Log.d(TAG, "========================================")
        Log.d(TAG, "ALL TESTS COMPLETED")
        Log.d(TAG, "========================================")
        runOnUiThread {
            showToast("All tests completed! Check Logcat.")
        }
    }

    private suspend fun reverseGeocodeExample() {
        Log.d(TAG, "=== Reverse Geocode Example ===")

        // Get current location first
        val location = currentLocation ?: run {
            runOnUiThread {
                showToast("❌ Please wait, getting location...")
            }
            getCurrentLocation()
            currentLocation
        }

        if (location == null) {
            runOnUiThread {
                showToast("❌ Could not get current location")
            }
            return
        }

        val lat = location.latitude
        val lon = location.longitude

        barikoiClient.reverseGeocode(
            latitude = lat,
            longitude = lon
        ).onSuccess { place ->
            Log.d(TAG, "Address: ${place.address}")
            Log.d(TAG, "Area: ${place.area}")
            Log.d(TAG, "City: ${place.city}")
            runOnUiThread {
                showReverseGeocodeDialog(place, lat, lon)
            }
        }.onFailure { error ->
            Log.e(TAG, "Error: ${error.message}")
            runOnUiThread {
                showToast("❌ Error: ${error.message}")
            }
        }
    }

    private suspend fun autocompleteExample() {
        Log.d(TAG, "=== Autocomplete Example ===")
        barikoiClient.autocomplete(
            query = "Gulshan"
        ).onSuccess { places ->
            Log.d(TAG, "Found ${places.size} places")
            places.forEach { place ->
                Log.d(TAG, "Place: ${place.address}")
            }
            runOnUiThread {
                showToast("✅ Found ${places.size} places for 'Gulshan'")
            }
        }.onFailure { error ->
            Log.e(TAG, "Error: ${error.message}")
            runOnUiThread {
                showToast("❌ Error: ${error.message}")
            }
        }
    }

    private suspend fun nearbyPlacesExample() {
        Log.d(TAG, "=== Nearby Places Example ===")

        // Get current location first
        val location = currentLocation ?: run {
            runOnUiThread {
                showToast("❌ Please wait, getting location...")
            }
            getCurrentLocation()
            currentLocation
        }

        if (location == null) {
            runOnUiThread {
                showToast("❌ Could not get current location")
            }
            return
        }

        val lat = location.latitude
        val lon = location.longitude
        val radius = 1.0 // 1 km

        barikoiClient.nearbyPlaces(
            latitude = lat,
            longitude = lon,
            radius = radius,
            limit = 10
        ).onSuccess { places ->
            Log.d(TAG, "Found ${places.size} nearby places")
            places.forEach { place ->
                Log.d(TAG, "Place: ${place.address}, Distance: ${place.distanceWithinMeters}m")
            }
            runOnUiThread {
                showNearbyPlacesDialog(places, lat, lon, radius)
            }
        }.onFailure { error ->
            Log.e(TAG, "Error: ${error.message}")
            runOnUiThread {
                showToast("❌ Error: ${error.message}")
            }
        }
    }

    private suspend fun routeOverviewExample() {
        Log.d(TAG, "=== Route Overview Example ===")
        val startLat = 23.8103
        val startLon = 90.4125
        val endLat = 23.7808
        val endLon = 90.4217
        val profile = "car"

        barikoiClient.routeOverview(
            startLat = startLat,
            startLon = startLon,
            endLat = endLat,
            endLon = endLon,
            profile = profile
        ).onSuccess { routes ->
            Log.d(TAG, "Routes found: ${routes.size}")
            routes.firstOrNull()?.let { route ->
                Log.d(TAG, "Distance: ${route.distance}m")
                Log.d(TAG, "Duration: ${route.duration}s")
                Log.d(TAG, "Legs: ${route.legs?.size ?: 0}")
                runOnUiThread {
                    showRouteOverviewDialog(routes, startLat, startLon, endLat, endLon, profile)
                }
            } ?: runOnUiThread {
                showToast("❌ No route found")
            }
        }.onFailure { error ->
            Log.e(TAG, "Error: ${error.message}")
            runOnUiThread {
                showToast("❌ Error: ${error.message}")
            }
        }
    }

    private suspend fun searchPlaceExample() {
        Log.d(TAG, "=== Search Place Example ===")
        barikoiClient.searchPlace(
            query = "Bashundhara"
        ).onSuccess { response ->
            Log.d(TAG, "Session ID: ${response.sessionId}")
            Log.d(TAG, "Found ${response.places?.size ?: 0} places")

            runOnUiThread {
                showToast("✅ Found ${response.places?.size ?: 0} places for 'Bashundhara'")
            }

            // Get details of first place if available
            response.places?.firstOrNull()?.placeCode?.let { placeCode ->
                response.sessionId?.let { sessionId ->
                    getPlaceDetails(placeCode, sessionId)
                }
            }
        }.onFailure { error ->
            Log.e(TAG, "Error: ${error.message}")
            runOnUiThread {
                showToast("❌ Error: ${error.message}")
            }
        }
    }

    private fun getPlaceDetails(placeCode: String, sessionId: String) {
        Log.d(TAG, "=== Place Details Example ===")
        Log.d(TAG, "Opening details page for place: $placeCode")

        runOnUiThread {
            // Open PlaceDetailsActivity with the place code and session ID
            PlaceDetailsActivity.start(
                context = this@MainActivity,
                placeCode = placeCode,
                sessionId = sessionId
            )
            showToast("Opening place details...")
        }
    }

    private suspend fun snapToRoadExample() {
        Log.d(TAG, "=== Snap to Road Example ===")
        barikoiClient.snapToRoad(
            latitude = 23.8103,
            longitude = 90.4125
        ).onSuccess { response ->
            Log.d(TAG, "Snapped to: ${response.snappedLatitude}, ${response.snappedLongitude}")
            Log.d(TAG, "Distance from original: ${response.distance}m")
            runOnUiThread {
                showToast("✅ Snapped to road, ${response.distance}m away")
            }
        }.onFailure { error ->
            Log.e(TAG, "Error: ${error.message}")
            runOnUiThread {
                showToast("❌ Error: ${error.message}")
            }
        }
    }

    private suspend fun checkNearbyExample() {
        Log.d(TAG, "=== Check Nearby (Geofence) Example ===")
        barikoiClient.checkNearby(
            currentLat = 23.8103,
            currentLon = 90.4125,
            destinationLat = 23.8110,
            destinationLon = 90.4130,
            radiusMeters = 1000.0
        ).onSuccess { response ->
            Log.d(TAG, "Message: ${response.message}")
            Log.d(TAG, "Is inside radius: ${response.isInside}")
            Log.d(TAG, "Geofence data: ${response.data}")

            val status = if (response.isInside) "Inside" else "Outside"
            val radiusInfo = response.data?.radius ?: "1000"

            runOnUiThread {
                showToast("✅ $status geofence (${radiusInfo}m radius)")
            }
        }.onFailure { error ->
            Log.e(TAG, "Error: ${error.message}")
            runOnUiThread {
                showToast("❌ Error: ${error.message}")
            }
        }
    }

    private suspend fun rupantorGeocodeExample() {
        Log.d(TAG, "=== Rupantor Geocode Example ===")

        // Get current location first
        val location = currentLocation ?: run {
            runOnUiThread {
                showToast("❌ Please wait, getting location...")
            }
            getCurrentLocation()
            currentLocation
        }

        if (location == null) {
            runOnUiThread {
                showToast("❌ Could not get current location")
            }
            return
        }

        val lat = location.latitude
        val lon = location.longitude

        // For Rupantor, we need an address string - we'll use the reverse geocode result
        barikoiClient.reverseGeocode(latitude = lat, longitude = lon)
            .onSuccess { reversePlace ->
                val addressQuery = reversePlace.address ?: "Current Location"

                // Now call Rupantor with the address
                lifecycleScope.launch {
                    barikoiClient.rupantorGeocode(
                        query = addressQuery,
                        thana = true,
                        district = true,
                        bangla = true
                    ).onSuccess { rupantorResponse ->
                        val place = rupantorResponse.geocodedAddress
                        Log.d(TAG, "Formatted Address: ${place?.addressAlt ?: place?.address}")
                        Log.d(TAG, "Coordinates: ${place?.latitude}, ${place?.longitude}")
                        Log.d(TAG, "Thana: ${place?.thana}")
                        Log.d(TAG, "District: ${place?.district}")
                        Log.d(TAG, "Fixed Address: ${rupantorResponse.fixedAddress}")
                        Log.d(TAG, "Address Status: ${rupantorResponse.addressStatus}")
                        Log.d(TAG, "Confidence Score: ${rupantorResponse.confidenceScorePercentage}%")
                        runOnUiThread {
                            showRupantorDialog(rupantorResponse, lat, lon, addressQuery)
                        }
                    }.onFailure { error ->
                        Log.e(TAG, "Error: ${error.message}")
                        runOnUiThread {
                            showToast("❌ Error: ${error.message}")
                        }
                    }
                }
            }.onFailure { error ->
                Log.e(TAG, "Error getting address for Rupantor: ${error.message}")
                runOnUiThread {
                    showToast("❌ Error: ${error.message}")
                }
            }
    }

    private suspend fun calculateRouteExample() {
        Log.d(TAG, "=== Calculate Route (GraphHopper) Example ===")
        val startLat = 23.8103
        val startLon = 90.4125
        val endLat = 23.7808
        val endLon = 90.4217
        val profile = "car"

        barikoiClient.calculateRoute(
            startLat = startLat,
            startLon = startLon,
            endLat = endLat,
            endLon = endLon,
            profile = profile
        ).onSuccess { response ->
            Log.d(TAG, "GraphHopper Response:")
            response.paths?.firstOrNull()?.let { path ->
                Log.d(TAG, "Distance: ${path.distance}m")
                Log.d(TAG, "Time: ${path.time}ms")
                Log.d(TAG, "Instructions: ${path.instructions?.size ?: 0} steps")
                path.instructions?.forEach { Log.d(TAG, "  - ${it.text}") }
                runOnUiThread {
                    showCalculateRouteDialog(response, startLat, startLon, endLat, endLon, profile)
                }
            } ?: runOnUiThread {
                showToast("❌ No route path found")
            }
        }.onFailure { error ->
            Log.e(TAG, "Error: ${error.message}")
            runOnUiThread {
                showToast("❌ Error: ${error.message}")
            }
        }
    }

    private suspend fun optimizeRouteExample() {
        Log.d(TAG, "=== Optimize Route Example ===")
        val sourceLat = 23.8103
        val sourceLon = 90.4125
        val destinationLat = 23.7516
        val destinationLon = 90.3888
        val profile = "car"
        val waypoints = listOf(
            Pair(23.8200, 90.4200),
            Pair(23.7900, 90.4100),
            Pair(23.7700, 90.4300)
        )

        barikoiClient.optimizeRoute(
            sourceLat = sourceLat,
            sourceLon = sourceLon,
            destinationLat = destinationLat,
            destinationLon = destinationLon,
            waypoints = waypoints,
            profile = profile
        ).onSuccess { response ->
            Log.d(TAG, "Optimized route created")
            Log.d(TAG, "Paths: ${response.paths?.size ?: 0}")
            response.paths?.firstOrNull()?.let {
                Log.d(TAG, "Distance: ${it.distance}m, Time: ${it.time}ms")
                Log.d(TAG, "Instructions: ${it.instructions?.size ?: 0} steps")
            }
            runOnUiThread {
                showOptimizeRouteDialog(
                    response, sourceLat, sourceLon,
                    destinationLat, destinationLon, waypoints, profile
                )
            }
        }.onFailure { error ->
            Log.e(TAG, "Error: ${error.message}")
            runOnUiThread {
                showToast("❌ Error: ${error.message}")
            }
        }
    }

    private fun showOptimizeRouteDialog(
        response: com.barikoi.sdk.models.OptimizedRouteResponse,
        sourceLat: Double,
        sourceLon: Double,
        destinationLat: Double,
        destinationLon: Double,
        waypoints: List<Pair<Double, Double>>,
        profile: String
    ) {
        // Actual API returns paths[] identical to GraphHopper calculateRoute
        val path = response.paths?.firstOrNull()

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_optimize_route)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // ── Summary ──────────────────────────────────────────────────
        val distanceKm = (path?.distance ?: 0.0) / 1000
        val durationMs = path?.time ?: 0L
        val durationMin = durationMs / 60000
        val durationText = if (durationMin >= 60) {
            "${durationMin / 60}h ${durationMin % 60}m"
        } else {
            "$durationMin min"
        }
        // Count waypoint markers in instructions (sign == 5)
        val waypointCount = path?.instructions?.count { it.sign == 5 } ?: waypoints.size
        val totalStops = waypointCount + 2 // +source +destination

        dialog.findViewById<TextView>(R.id.tvOptDistance).text = "%.2f km".format(distanceKm)
        dialog.findViewById<TextView>(R.id.tvOptDuration).text = durationText
        dialog.findViewById<TextView>(R.id.tvOptStops).text = "$totalStops"

        // ── Route Points ─────────────────────────────────────────────
        dialog.findViewById<TextView>(R.id.tvOptSource).text =
            "🟢 Source:      %.6f, %.6f".format(sourceLat, sourceLon)
        dialog.findViewById<TextView>(R.id.tvOptDestination).text =
            "🔴 Destination: %.6f, %.6f".format(destinationLat, destinationLon)
        dialog.findViewById<TextView>(R.id.tvOptProfile).text =
            "🚗 Profile: ${profile.replaceFirstChar { it.uppercase() }}"

        // ── Waypoints (from instructions sign==5) ────────────────────
        val waypointInstructions = path?.instructions?.filter { it.sign == 5 }
        if (!waypointInstructions.isNullOrEmpty()) {
            dialog.findViewById<MaterialCardView>(R.id.cardWaypoints).visibility = View.VISIBLE
            dialog.findViewById<TextView>(R.id.tvWaypointsHeader).text =
                "📌 Waypoints (${waypointInstructions.size})"
            val llWaypoints = dialog.findViewById<LinearLayout>(R.id.llWaypoints)
            llWaypoints.removeAllViews()
            waypointInstructions.forEachIndexed { index, wp ->
                val tv = TextView(this).apply {
                    text = "${index + 1}. ${wp.text ?: "Waypoint ${index + 1}"}"
                    textSize = 13f
                    setPadding(0, 4, 0, 4)
                }
                llWaypoints.addView(tv)
            }
        } else if (waypoints.isNotEmpty()) {
            // Fallback: show input waypoints if API didn't return waypoint markers
            dialog.findViewById<MaterialCardView>(R.id.cardWaypoints).visibility = View.VISIBLE
            dialog.findViewById<TextView>(R.id.tvWaypointsHeader).text =
                "📌 Waypoints (${waypoints.size})"
            val llWaypoints = dialog.findViewById<LinearLayout>(R.id.llWaypoints)
            llWaypoints.removeAllViews()
            waypoints.forEachIndexed { index, (lat, lon) ->
                val tv = TextView(this).apply {
                    text = "${index + 1}. %.6f, %.6f".format(lat, lon)
                    textSize = 13f
                    setPadding(0, 4, 0, 4)
                }
                llWaypoints.addView(tv)
            }
        }

        // ── Turn-by-Turn Instructions (reuse cardTrips / llTrips) ────
        val instructions = path?.instructions?.filter { it.sign != 5 } // exclude waypoint markers
        if (!instructions.isNullOrEmpty()) {
            dialog.findViewById<MaterialCardView>(R.id.cardTrips).visibility = View.VISIBLE
            dialog.findViewById<TextView>(R.id.tvTripsHeader).text =
                "🧭 Turn-by-Turn Instructions (${instructions.size} steps)"
            val llTrips = dialog.findViewById<LinearLayout>(R.id.llTrips)
            llTrips.removeAllViews()
            instructions.forEachIndexed { index, instruction ->
                val stepDistKm = (instruction.distance ?: 0.0) / 1000
                val icon = when (instruction.sign) {
                    -98 -> "↩️"
                    -3 -> "↰"
                    -2 -> "⬅️"
                    -1 -> "↖️"
                    0 -> "⬆️"
                    1 -> "↗️"
                    2 -> "➡️"
                    3 -> "↱"
                    4 -> "🏁"
                    6 -> "⭕"  // roundabout
                    7 -> "↗️"  // keep right/slight
                    -7 -> "↖️"  // keep left/slight
                    else -> "▶️"
                }
                val tv = TextView(this).apply {
                    text = "$icon ${index + 1}. ${instruction.text ?: "—"}  (%.2f km)".format(stepDistKm)
                    textSize = 12f
                    setPadding(0, 6, 0, 6)
                }
                llTrips.addView(tv)
                if (index < instructions.size - 1) {
                    llTrips.addView(View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1
                        )
                        setBackgroundColor(0xFFEEEEEE.toInt())
                    })
                }
            }
        }

        dialog.findViewById<Button>(R.id.btnCloseOptRoute).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // ========== Location Helper Methods ==========

    private fun checkAndRequestLocationPermission() {
        when {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                getCurrentLocation()
            }

            else -> {
                // Request permissions
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        lifecycleScope.launch {
            try {
                val cancellationToken = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationToken.token
                ).await()

                currentLocation = location
                Log.d(TAG, "Location obtained: ${location.latitude}, ${location.longitude}")
                showToast("📍 Location: ${location.latitude}, ${location.longitude}")
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location: ${e.message}")
                showToast("❌ Error getting location: ${e.message}")
            }
        }
    }

    // ========== Dialog Helper Methods ==========

    private fun showReverseGeocodeDialog(place: Place, lat: Double, lon: Double) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_api_result)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set dialog title
        dialog.findViewById<TextView>(R.id.tvDialogTitle).text = "Reverse Geocode Result"

        // Set current location
        dialog.findViewById<TextView>(R.id.tvCurrentLocation).text =
            "📍 Lat: %.6f, Lon: %.6f".format(lat, lon)

        // Set response data
        dialog.findViewById<TextView>(R.id.tvAddress).text =
            "Address: ${place.address ?: "N/A"}"

        val addressBn = if (place.addressBn?.isNotEmpty() == true) {
            "Address (Bangla): ${place.addressBn}"
        } else {
            ""
        }
        dialog.findViewById<TextView>(R.id.tvAddressBangla).apply {
            text = addressBn
            visibility = if (addressBn.isNotEmpty()) View.VISIBLE else View.GONE
        }

        dialog.findViewById<TextView>(R.id.tvArea).text =
            "Area: ${place.area ?: "N/A"}"

        dialog.findViewById<TextView>(R.id.tvCity).text =
            "City: ${place.city ?: "N/A"}"

        dialog.findViewById<TextView>(R.id.tvDistrict).text =
            "District: ${place.district ?: "N/A"}"

        dialog.findViewById<TextView>(R.id.tvThana).text =
            "Thana: ${place.thana ?: "N/A"}"

        dialog.findViewById<TextView>(R.id.tvPostCode).text =
            "Post Code: ${place.postCode?.toString() ?: place.postcode ?: "N/A"}"

        dialog.findViewById<TextView>(R.id.tvPlaceType).text =
            "Type: ${place.pType ?: "N/A"} | Location Type: ${place.locationType ?: "N/A"}"

        // Additional info
        if (place.uCode != null || place.placeCode != null) {
            dialog.findViewById<MaterialCardView>(R.id.cardAdditionalInfo).visibility = View.VISIBLE
            val additionalInfo = buildString {
                place.placeCode?.let { append("Place Code: $it\n") }
                place.uCode?.let { append("Unique Code: $it\n") }
                place.division?.let { append("Division: $it\n") }
                place.country?.let { append("Country: $it") }
            }
            dialog.findViewById<TextView>(R.id.tvAdditionalInfo).text = additionalInfo
        }

        // Close button
        dialog.findViewById<Button>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showRupantorDialog(
        rupantorResponse: com.barikoi.sdk.models.RupantorGeocodeResponse,
        lat: Double,
        lon: Double,
        originalQuery: String
    ) {
        val place = rupantorResponse.geocodedAddress
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_api_result)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set dialog title
        dialog.findViewById<TextView>(R.id.tvDialogTitle).text = "Rupantor Geocode Result"

        // Set current location
        dialog.findViewById<TextView>(R.id.tvCurrentLocation).text =
            "📍 Current: %.6f, %.6f\n🎯 Formatted: %.6f, %.6f".format(
                lat, lon,
                place?.latitude ?: lat,
                place?.longitude ?: lon
            )

        // Set response data - prefer Address (capitalized) field from geocoded_address
        dialog.findViewById<TextView>(R.id.tvAddress).text =
            "Formatted Address: ${place?.addressAlt ?: place?.address ?: rupantorResponse.fixedAddress ?: "N/A"}"

        val addressBn = when {
            rupantorResponse.banglaAddress?.isNotEmpty() == true -> "বাংলা ঠিকানা: ${rupantorResponse.banglaAddress}"
            place?.addressBn?.isNotEmpty() == true -> "বাংলা ঠিকানা: ${place.addressBn}"
            else -> ""
        }
        dialog.findViewById<TextView>(R.id.tvAddressBangla).apply {
            text = addressBn
            visibility = if (addressBn.isNotEmpty()) View.VISIBLE else View.GONE
        }

        dialog.findViewById<TextView>(R.id.tvArea).text =
            "Area: ${place?.area ?: "N/A"}${if (place?.areaBn != null) " (${place.areaBn})" else ""}"

        dialog.findViewById<TextView>(R.id.tvCity).text =
            "City: ${place?.city ?: "N/A"}${if (place?.cityBn != null) " (${place.cityBn})" else ""}"

        dialog.findViewById<TextView>(R.id.tvDistrict).text =
            "District: ${place?.district ?: "N/A"}"

        dialog.findViewById<TextView>(R.id.tvThana).text =
            "Thana: ${place?.thana ?: "N/A"}${if (place?.thanaBn != null) " (${place.thanaBn})" else ""}"

        dialog.findViewById<TextView>(R.id.tvPostCode).text =
            "Post Code: ${place?.postCode?.toString() ?: place?.postcode ?: "N/A"}"

        dialog.findViewById<TextView>(R.id.tvPlaceType).text =
            "Type: ${place?.pType ?: "N/A"}"

        // Additional info
        dialog.findViewById<MaterialCardView>(R.id.cardAdditionalInfo).visibility = View.VISIBLE
        val additionalInfo = buildString {
            append("Original Query: $originalQuery\n\n")
            rupantorResponse.givenAddress?.let { append("Given Address: $it\n") }
            rupantorResponse.fixedAddress?.let { append("Fixed Address: $it\n") }
            rupantorResponse.addressStatus?.let { append("Address Status: $it\n") }
            rupantorResponse.confidenceScorePercentage?.let { append("Confidence Score: $it%\n\n") }
            place?.uCode?.let { append("uCode: $it\n") }
            place?.holdingNumber?.let { append("Holding No: $it\n") }
            place?.roadNameNumber?.let { append("Road: $it\n") }
            place?.subArea?.let { append("Sub-Area: $it\n") }
            place?.placeCode?.let { append("Place Code: $it\n") }
            place?.subDistrict?.let { append("Sub-District: $it\n") }
            place?.union?.let { append("Union: $it\n") }
            place?.pauroshova?.let { append("Pauroshova: $it\n") }
            place?.division?.let { append("Division: $it\n") }
            place?.country?.let { append("Country: $it") }
        }
        dialog.findViewById<TextView>(R.id.tvAdditionalInfo).text = additionalInfo

        // Close button
        dialog.findViewById<Button>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showRouteOverviewDialog(
        routes: List<com.barikoi.sdk.models.Route>,
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        profile: String
    ) {
        val route = routes.firstOrNull() ?: return

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_route_overview)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Distance & Duration
        val distanceKm = (route.distance ?: 0.0) / 1000
        val durationMin = (route.duration ?: 0.0) / 60
        val durationText = if (durationMin >= 60) {
            val hours = (durationMin / 60).toInt()
            val mins = (durationMin % 60).toInt()
            "${hours}h ${mins}m"
        } else {
            "%.0f min".format(durationMin)
        }

        dialog.findViewById<TextView>(R.id.tvDistance).text = "%.2f km".format(distanceKm)
        dialog.findViewById<TextView>(R.id.tvDuration).text = durationText

        // Origin & Destination
        dialog.findViewById<TextView>(R.id.tvOrigin).text =
            "🟢 Start: %.6f, %.6f".format(startLat, startLon)
        dialog.findViewById<TextView>(R.id.tvDestination).text =
            "🔴 End:   %.6f, %.6f".format(endLat, endLon)
        dialog.findViewById<TextView>(R.id.tvProfile).text =
            "🚗 Profile: ${profile.replaceFirstChar { it.uppercase() }}"

        // Route Legs
        val legs = route.legs
        if (!legs.isNullOrEmpty()) {
            dialog.findViewById<MaterialCardView>(R.id.cardLegs).visibility = View.VISIBLE
            val llLegs = dialog.findViewById<LinearLayout>(R.id.llLegs)
            llLegs.removeAllViews()
            legs.forEachIndexed { index, leg ->
                val legDistKm = (leg.distance ?: 0.0) / 1000
                val legDurMin = (leg.duration ?: 0.0) / 60
                val stepsCount = leg.steps?.size ?: 0
                val legView = TextView(this).apply {
                    text = "Leg ${index + 1}: %.2f km · %.0f min · %d steps".format(
                        legDistKm, legDurMin, stepsCount
                    )
                    textSize = 13f
                    setPadding(0, 4, 0, 4)
                }
                llLegs.addView(legView)
            }
        }

        // Additional info (number of routes, weight)
        val infoBuilder = buildString {
            append("Routes returned: ${routes.size}\n")
            route.weight?.let { append("Weight: $it\n") }
            route.geometry?.let { append("Has geometry: Yes") }
        }
        if (infoBuilder.isNotBlank()) {
            dialog.findViewById<MaterialCardView>(R.id.cardRouteInfo).visibility = View.VISIBLE
            dialog.findViewById<TextView>(R.id.tvRouteInfo).text = infoBuilder.trim()
        }

        dialog.findViewById<Button>(R.id.btnCloseRoute).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showCalculateRouteDialog(
        response: com.barikoi.sdk.models.GraphHopperRouteResponse,
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        profile: String
    ) {
        val path = response.paths?.firstOrNull() ?: return

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_calculate_route)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Distance
        val distanceKm = (path.distance ?: 0.0) / 1000
        dialog.findViewById<TextView>(R.id.tvCalcDistance).text = "%.2f km".format(distanceKm)

        // Duration
        val durationMs = path.time ?: 0L
        val durationMin = durationMs / 60000
        val durationText = if (durationMin >= 60) {
            "${durationMin / 60}h ${durationMin % 60}m"
        } else {
            "$durationMin min"
        }
        dialog.findViewById<TextView>(R.id.tvCalcDuration).text = durationText

        // Steps count
        val stepsCount = path.instructions?.size ?: 0
        dialog.findViewById<TextView>(R.id.tvCalcSteps).text = "$stepsCount"

        // Origin & Destination
        dialog.findViewById<TextView>(R.id.tvCalcOrigin).text =
            "🟢 Start: %.6f, %.6f".format(startLat, startLon)
        dialog.findViewById<TextView>(R.id.tvCalcDestination).text =
            "🔴 End:   %.6f, %.6f".format(endLat, endLon)
        dialog.findViewById<TextView>(R.id.tvCalcProfile).text =
            "🚗 Profile: ${profile.replaceFirstChar { it.uppercase() }}"

        // Turn-by-turn instructions
        val instructions = path.instructions
        if (!instructions.isNullOrEmpty()) {
            dialog.findViewById<MaterialCardView>(R.id.cardInstructions).visibility = View.VISIBLE
            dialog.findViewById<TextView>(R.id.tvInstructionsHeader).text =
                "🧭 Turn-by-Turn Instructions (${instructions.size} steps)"
            val llInstructions = dialog.findViewById<LinearLayout>(R.id.llInstructions)
            llInstructions.removeAllViews()
            instructions.forEachIndexed { index, instruction ->
                val stepDistKm = (instruction.distance ?: 0.0) / 1000
                val stepView = TextView(this).apply {
                    text = "${index + 1}. ${instruction.text ?: "—"}  (%.2f km)".format(stepDistKm)
                    textSize = 12f
                    setPadding(0, 6, 0, 6)
                }
                llInstructions.addView(stepView)
                // Divider between steps (except last)
                if (index < instructions.size - 1) {
                    val divider = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1
                        )
                        setBackgroundColor(0xFFEEEEEE.toInt())
                    }
                    llInstructions.addView(divider)
                }
            }
        }

        dialog.findViewById<Button>(R.id.btnCloseCalcRoute).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showNearbyPlacesDialog(places: List<Place>, lat: Double, lon: Double, radius: Double) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_nearby_places)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Set search center
        dialog.findViewById<TextView>(R.id.tvSearchCenter).text =
            "📍 Lat: %.6f, Lon: %.6f\n🔍 Radius: %.1f km".format(lat, lon, radius)

        // Set results summary
        dialog.findViewById<TextView>(R.id.tvResultsSummary).text =
            "Found ${places.size} places within %.1f km".format(radius)

        // Get places list container
        val placesList = dialog.findViewById<LinearLayout>(R.id.llPlacesList)
        placesList.removeAllViews()

        // Add each place to the list (max 5 for dialog)
        places.take(5).forEachIndexed { index, place ->
            val placeCard = layoutInflater.inflate(
                R.layout.item_nearby_place,
                placesList,
                false
            ) as MaterialCardView

            // Set place data
            placeCard.findViewById<TextView>(R.id.tvPlaceNumber).text = "${index + 1}"
            placeCard.findViewById<TextView>(R.id.tvPlaceName).text =
                place.name ?: place.address ?: "Unknown Place"
            placeCard.findViewById<TextView>(R.id.tvPlaceAddress).text =
                place.address ?: "No address"
            placeCard.findViewById<TextView>(R.id.tvPlaceArea).text =
                "${place.area ?: "N/A"}, ${place.city ?: "N/A"}"

            // Distance
            val distance = place.distanceWithinMeters ?: place.distanceInMeters?.toDoubleOrNull()
            if (distance != null) {
                val distanceText = if (distance < 1000) {
                    "%.0f m".format(distance)
                } else {
                    "%.2f km".format(distance / 1000)
                }
                placeCard.findViewById<TextView>(R.id.tvDistance).text = "📍 $distanceText"
            }

            // Place type
            place.pType?.let {
                placeCard.findViewById<TextView>(R.id.tvPlaceType).text = it
            }

            // Click listener to show more details
            placeCard.setOnClickListener {
                dialog.dismiss()
                showPlaceQuickInfo(place)
            }

            placesList.addView(placeCard)
        }

        // View All button
        dialog.findViewById<Button>(R.id.btnViewAll).setOnClickListener {
            dialog.dismiss()
            showAllNearbyPlaces(places, lat, lon, radius)
        }

        // Close button
        dialog.findViewById<Button>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPlaceQuickInfo(place: Place) {
        val message = buildString {
            place.name?.let { append("Name: $it\n\n") }
            place.address?.let { append("Address: $it\n\n") }
            place.area?.let { append("Area: $it\n") }
            place.city?.let { append("City: $it\n") }
            place.district?.let { append("District: $it\n") }
            place.thana?.let { append("Thana: $it\n") }
            place.pType?.let { append("Type: $it\n") }
            append("\n")
            if (place.latitude != null && place.longitude != null) {
                append("Coordinates:\n${place.latitude}, ${place.longitude}")
            }

            val distance = place.distanceWithinMeters ?: place.distanceInMeters?.toDoubleOrNull()
            distance?.let {
                append("\n\nDistance: ")
                if (it < 1000) {
                    append("%.0f meters".format(it))
                } else {
                    append("%.2f km".format(it / 1000))
                }
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(place.name ?: "Place Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Copy Coordinates") { _, _ ->
                val coords = "${place.latitude}, ${place.longitude}"
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("coordinates", coords)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Coordinates copied!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showAllNearbyPlaces(places: List<Place>, lat: Double, lon: Double, radius: Double) {
        val items = places.map { place ->
            val name = place.name ?: place.address ?: "Unknown"
            val distance = place.distanceWithinMeters ?: place.distanceInMeters?.toDoubleOrNull()
            val distText = if (distance != null) {
                if (distance < 1000) "%.0f m".format(distance) else "%.2f km".format(distance / 1000)
            } else "N/A"
            "$name ($distText)"
        }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("All Nearby Places (${places.size})")
            .setItems(items) { _, which ->
                showPlaceQuickInfo(places[which])
            }
            .setNegativeButton("Close", null)
            .show()
    }

    companion object {
        private const val TAG = "BarikoiSDK"
    }
}

