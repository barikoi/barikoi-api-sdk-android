package com.barikoi.barikoiapis

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.barikoi.sdk.models.Place

class SearchResultsAdapter(
    private val onItemClick: (Place) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.ViewHolder>() {

    private var places: List<Place> = emptyList()

    fun submitList(newPlaces: List<Place>) {
        places = newPlaces
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(places[position])
    }

    override fun getItemCount(): Int = places.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPlaceName: TextView = itemView.findViewById(R.id.tvPlaceName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvAreaCity: TextView = itemView.findViewById(R.id.tvAreaCity)
        private val tvPlaceType: TextView = itemView.findViewById(R.id.tvPlaceType)
        private val tvCoordinates: TextView = itemView.findViewById(R.id.tvCoordinates)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(places[position])
                }
            }
        }

        fun bind(place: Place) {
            // Place Name - use name, or address, or "Unknown Place"
            tvPlaceName.text = place.name ?: place.address ?: "Unknown Place"

            // Address - show full address or alternative
            val addressText = place.address ?: place.addressAlt ?: "No address available"
            tvAddress.text = addressText
            tvAddress.visibility = if (addressText.isNotEmpty()) View.VISIBLE else View.GONE

            // Area and City
            val areaCity = buildString {
                if (place.area?.isNotEmpty() == true) {
                    append(place.area)
                }
                if (place.city?.isNotEmpty() == true) {
                    if (isNotEmpty()) append(", ")
                    append(place.city)
                }
            }
            tvAreaCity.text = if (areaCity.isNotEmpty()) areaCity else "Location not specified"

            // Place Type
            val placeType = place.pType ?: place.locationType ?: "Unknown"
            tvPlaceType.text = placeType
            tvPlaceType.visibility = View.VISIBLE

            // Coordinates
            if (place.latitude != null && place.longitude != null) {
                tvCoordinates.text = "📍 %.6f, %.6f".format(place.latitude, place.longitude)
                tvCoordinates.visibility = View.VISIBLE
            } else {
                tvCoordinates.visibility = View.GONE
            }
        }
    }
}
