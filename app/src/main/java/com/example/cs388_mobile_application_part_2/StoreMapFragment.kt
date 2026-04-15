package com.example.cs388_mobile_application_part_2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.SearchNearbyRequest

class StoreMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var tvStatus: TextView
    private lateinit var progress: ProgressBar
    private var googleMap: GoogleMap? = null

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            locateAndSearch()
        } else {
            showStatus("Location permission denied")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_store_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvStatus = view.findViewById(R.id.tvMapStatus)
        progress = view.findViewById(R.id.mapProgress)
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        if (!Places.isInitialized()) {
            Places.initialize(requireContext().applicationContext, BuildConfig.MAPS_API_KEY)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        checkPermissionsAndSearch()
    }

    private fun checkPermissionsAndSearch() {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            locateAndSearch()
        } else {
            requestPermission.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun locateAndSearch() {
        val map = googleMap ?: return
        try {
            map.isMyLocationEnabled = true
        } catch (e: SecurityException) { }

        progress.visibility = View.VISIBLE
        showStatus("Finding your location...")

        val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        try {
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location == null) {
                    progress.visibility = View.GONE
                    showStatus("Could not get location. Try moving outside or enabling GPS.")
                    return@addOnSuccessListener
                }
                val userLatLng = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13f))
                searchNearbyStores(userLatLng)
            }.addOnFailureListener {
                progress.visibility = View.GONE
                showStatus("Location error: ${it.message}")
            }
        } catch (e: SecurityException) {
            progress.visibility = View.GONE
            showStatus("Location permission error")
        }
    }

    private fun searchNearbyStores(center: LatLng) {
        showStatus("Searching for board game stores...")
        val placesClient = Places.createClient(requireContext())

        val placeFields = listOf(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val request = SearchNearbyRequest.builder(
            com.google.android.libraries.places.api.model.CircularBounds.newInstance(center, 5000.0), placeFields
        ).setIncludedTypes(listOf("game_store", "hobby_store", "book_store", "toy_store"))
            .setMaxResultCount(20)
            .build()

        placesClient.searchNearby(request)
            .addOnSuccessListener { response ->
                progress.visibility = View.GONE
                val places = response.places
                if (places.isEmpty()) {
                    showStatus("No board game stores found nearby")
                    return@addOnSuccessListener
                }
                tvStatus.visibility = View.GONE
                for (place in places) {
                    val latLng = place.latLng ?: continue
                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(place.name)
                            .snippet(place.address)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                }
            }
            .addOnFailureListener { e ->
                progress.visibility = View.GONE
                showStatus("Search failed: ${e.message}")
            }
    }

    private fun showStatus(msg: String) {
        tvStatus.text = msg
        tvStatus.visibility = View.VISIBLE
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
}
