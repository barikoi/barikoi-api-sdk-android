package com.barikoi.barikoiapis

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.barikoi.barikoiapis.databinding.ActivitySearchBinding
import com.barikoi.sdk.BarikoiClient
import com.barikoi.sdk.models.Place
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val barikoiClient = BarikoiClient()
    private lateinit var adapter: SearchResultsAdapter

    private var searchJob: Job? = null
    private var currentSessionId: String? = null
    private var isAutocompleteMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        setupToolbar()
        setupRecyclerView()
        setupSearchInput()
        setupToggleButtons()
        setupSearchButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = SearchResultsAdapter { place ->
            onPlaceClicked(place)
        }

        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = adapter
    }

    private fun setupSearchInput() {
        // Real-time search for autocomplete
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""

                if (isAutocompleteMode && query.length >= 2) {
                    // Debounce: cancel previous job and start new one
                    searchJob?.cancel()
                    searchJob = lifecycleScope.launch {
                        delay(300) // Wait 300ms after user stops typing
                        performAutocomplete(query)
                    }
                }
            }
        })

        // Handle search action from keyboard
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun setupToggleButtons() {
        binding.toggleSearchType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnAutocomplete -> {
                        isAutocompleteMode = true
                        binding.searchInputLayout.hint = "Type to autocomplete..."
                        updateStatus("Autocomplete mode: Type to search")
                    }
                    R.id.btnSearchPlace -> {
                        isAutocompleteMode = false
                        binding.searchInputLayout.hint = "Enter place name..."
                        updateStatus("Search Place mode: Tap Search button")
                    }
                }
                clearResults()
            }
        }
    }

    private fun setupSearchButton() {
        binding.btnSearch.setOnClickListener {
            performSearch()
        }
    }

    private fun performSearch() {
        val query = binding.etSearch.text?.toString()?.trim()

        if (query.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show()
            return
        }

        if (isAutocompleteMode) {
            performAutocomplete(query)
        } else {
            performSearchPlace(query)
        }
    }

    private fun performAutocomplete(query: String) {
        showLoading()

        lifecycleScope.launch {
            barikoiClient.autocomplete(query, bangla = true)
                .onSuccess { places ->
                    hideLoading()
                    if (places.isEmpty()) {
                        updateStatus("No results found for '$query'")
                        showResults(emptyList())
                    } else {
                        updateStatus("Found ${places.size} results")
                        showResults(places)
                    }
                }
                .onFailure { error ->
                    hideLoading()
                    updateStatus("Error: ${error.message}")
                    Toast.makeText(
                        this@SearchActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun performSearchPlace(query: String) {
        showLoading()

        lifecycleScope.launch {
            barikoiClient.searchPlace(query)
                .onSuccess { response ->
                    hideLoading()
                    currentSessionId = response.sessionId

                    val places = response.places ?: emptyList()
                    if (places.isEmpty()) {
                        updateStatus("No results found for '$query'")
                        showResults(emptyList())
                    } else {
                        updateStatus("Found ${places.size} results (Session ID: ${response.sessionId})")
                        showResults(places)
                    }
                }
                .onFailure { error ->
                    hideLoading()
                    updateStatus("Error: ${error.message}")
                    Toast.makeText(
                        this@SearchActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun onPlaceClicked(place: Place) {
        // If we have a session ID and place code, get detailed information
        val placeCode = place.placeCode
        if (!isAutocompleteMode && currentSessionId != null && placeCode != null) {
            // Open PlaceDetailsActivity with session
            PlaceDetailsActivity.start(
                context = this,
                placeCode = placeCode,
                sessionId = currentSessionId!!
            )
        } else {
            // Show quick info dialog for autocomplete results
            showQuickInfoDialog(place)
        }
    }

    private fun showQuickInfoDialog(place: Place) {
        val message = buildString {
            place.name?.let { append("Name: $it\n") }
            place.address?.let { append("Address: $it\n") }
            place.area?.let { append("Area: $it\n") }
            place.city?.let { append("City: $it\n") }
            place.district?.let { append("District: $it\n") }
            if (place.latitude != null && place.longitude != null) {
                append("Coordinates: ${place.latitude}, ${place.longitude}")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(place.name ?: place.address ?: "Place Details")
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

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvResults.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun updateStatus(message: String) {
        binding.tvStatus.text = message
        binding.tvStatus.visibility = View.VISIBLE
    }

    private fun showResults(places: List<Place>) {
        if (places.isEmpty()) {
            binding.rvResults.visibility = View.GONE
            binding.tvStatus.visibility = View.VISIBLE
        } else {
            adapter.submitList(places)
            binding.rvResults.visibility = View.VISIBLE
            binding.tvStatus.visibility = View.GONE
        }
    }

    private fun clearResults() {
        adapter.submitList(emptyList())
        binding.rvResults.visibility = View.GONE
        binding.tvStatus.visibility = View.VISIBLE
    }

    companion object {
        fun start(context: android.content.Context) {
            val intent = android.content.Intent(context, SearchActivity::class.java)
            context.startActivity(intent)
        }
    }
}
