package com.barikoi.barikoiapis

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.barikoi.barikoiapis.databinding.ActivityPlaceDetailsBinding
import com.barikoi.sdk.BarikoiClient
import com.barikoi.sdk.models.Place
import kotlinx.coroutines.launch

class PlaceDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaceDetailsBinding
    private val barikoiClient = BarikoiClient()
    private var currentPlace: Place? = null

    companion object {
        private const val EXTRA_PLACE_CODE = "place_code"
        private const val EXTRA_SESSION_ID = "session_id"

        fun start(context: Context, placeCode: String, sessionId: String) {
            val intent = Intent(context, PlaceDetailsActivity::class.java).apply {
                putExtra(EXTRA_PLACE_CODE, placeCode)
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPlaceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get intent extras
        val placeCode = intent.getStringExtra(EXTRA_PLACE_CODE)
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)

        if (placeCode == null || sessionId == null) {
            showError("Invalid parameters")
            return
        }


        // Setup UI
        setupButtons()

        // Load place details
        loadPlaceDetails(placeCode, sessionId)
    }

    private fun setupButtons() {
        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.btnCopyCoordinates.setOnClickListener {
            currentPlace?.let { place ->
                val coordinates = "${place.latitude}, ${place.longitude}"
                copyToClipboard(coordinates)
                Toast.makeText(this, "Coordinates copied: $coordinates", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPlaceDetails(placeCode: String, sessionId: String) {
        showLoading()

        lifecycleScope.launch {
            barikoiClient.placeDetails(placeCode, sessionId)
                .onSuccess { place ->
                    currentPlace = place
                    displayPlaceDetails(place)
                }
                .onFailure { error ->
                    showError("Failed to load place details: ${error.message}")
                }
        }
    }

    private fun displayPlaceDetails(place: Place) {
        hideLoading()
        binding.contentContainer.visibility = View.VISIBLE

        // Basic Information
        binding.tvPlaceName.text = place.name ?: "Unnamed Place"
        binding.tvAddress.text = place.address ?: place.addressAlt ?: "No address available"

        if (place.addressBn?.isNotEmpty() == true) {
            binding.tvAddressBn.visibility = View.VISIBLE
            binding.tvAddressBn.text = "বাংলা: ${place.addressBn}"
        } else {
            binding.tvAddressBn.visibility = View.GONE
        }

        binding.tvPlaceCode.text = "Place Code: ${place.placeCode ?: "N/A"} | ID: ${place.id ?: "N/A"}"

        val placeType = buildString {
            append("Type: ${place.pType ?: "N/A"}")
            if (place.subType != null) {
                append(" | Sub-Type: ${place.subType}")
            }
            if (place.locationType != null) {
                append(" | Location: ${place.locationType}")
            }
        }
        binding.tvPlaceType.text = placeType

        // Location Information
        binding.tvCoordinates.text = "📍 Coordinates: ${place.latitude ?: "N/A"}, ${place.longitude ?: "N/A"}"
        binding.tvArea.text = "Area: ${place.area ?: "N/A"}${if (place.areaBn != null) " (${place.areaBn})" else ""}"
        binding.tvCity.text = "City: ${place.city ?: "N/A"}${if (place.cityBn != null) " (${place.cityBn})" else ""}"
        binding.tvDistrict.text = "District: ${place.district ?: "N/A"}"
        binding.tvDivision.text = "Division: ${place.division ?: "N/A"}"
        binding.tvCountry.text = "Country: ${place.country ?: "N/A"}"

        // Administrative Details
        binding.tvThana.text = "Thana: ${place.thana ?: "N/A"}${if (place.thanaBn != null) " (${place.thanaBn})" else ""}"
        binding.tvSubDistrict.text = "Sub-District: ${place.subDistrict ?: "N/A"}"
        binding.tvUnion.text = "Union: ${place.union ?: "N/A"}"
        binding.tvPauroshova.text = "Pauroshova: ${place.pauroshova ?: "N/A"}"

        val postCode = place.postCode?.toString() ?: place.postcode ?: "N/A"
        binding.tvPostCode.text = "Post Code: $postCode"

        // Additional Information (show only if data available)
        val hasAdditionalInfo = place.uCode != null ||
                                place.subType != null ||
                                place.distanceWithinMeters != null ||
                                place.distanceInMeters != null ||
                                place.addressComponents != null

        if (hasAdditionalInfo) {
            binding.tvAdditionalTitle.visibility = View.VISIBLE
            binding.cardAdditional.visibility = View.VISIBLE

            if (place.uCode != null) {
                binding.tvUCode.visibility = View.VISIBLE
                binding.tvUCode.text = "Unique Code: ${place.uCode}"
            } else {
                binding.tvUCode.visibility = View.GONE
            }

            if (place.subType != null) {
                binding.tvSubType.visibility = View.VISIBLE
                binding.tvSubType.text = "Sub Type: ${place.subType}"
            } else {
                binding.tvSubType.visibility = View.GONE
            }

            val distance = place.distanceWithinMeters ?: place.distanceInMeters?.toDoubleOrNull()
            if (distance != null) {
                binding.tvDistance.visibility = View.VISIBLE
                binding.tvDistance.text = "Distance: %.2f meters".format(distance)
            } else {
                binding.tvDistance.visibility = View.GONE
            }

            // Address Components
            if (place.addressComponents != null) {
                binding.tvAddressComponents.visibility = View.VISIBLE
                val addrComp = place.addressComponents
                val areaComp = place.areaComponents
                val components = buildString {
                    append("Address Components:\n")
                    addrComp?.placeName?.let { append("• Place: $it\n") }
                    addrComp?.house?.let { append("• House: $it\n") }
                    addrComp?.road?.let { append("• Road: $it\n") }

                    if (areaComp != null) {
                        append("\nArea Components:\n")
                        areaComp.area?.let { append("• Area: $it\n") }
                        areaComp.subArea?.let { append("• Sub-Area: $it") }
                    }
                }
                binding.tvAddressComponents.text = components.trim()
            } else {
                binding.tvAddressComponents.visibility = View.GONE
            }
        } else {
            binding.tvAdditionalTitle.visibility = View.GONE
            binding.cardAdditional.visibility = View.GONE
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.GONE
        binding.tvError.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showError(message: String) {
        hideLoading()
        binding.contentContainer.visibility = View.GONE
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = message
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("coordinates", text)
        clipboard.setPrimaryClip(clip)
    }
}
