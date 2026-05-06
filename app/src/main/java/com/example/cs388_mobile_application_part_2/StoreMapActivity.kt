package com.example.cs388_mobile_application_part_2

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import java.util.Locale

@SuppressLint("SetTextI18n")
class StoreMapActivity : AppCompatActivity(), OnMapReadyCallback {

    // map + location clients
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private var gameName: String = "Board Game"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            enableLocationAndSearch()
        } else {
            Toast.makeText(this, "Location permission is required to find nearby stores", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store_map)

        gameName = intent.getStringExtra("game_name") ?: "Board Game"
        findViewById<TextView>(R.id.tvMapTitle).text = "Stores near you selling board games"

        // start Places + location services
        Places.initializeWithNewPlacesApiEnabled(applicationContext, BuildConfig.MAP_KEY)
        placesClient = Places.createClient(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val etZipCode = findViewById<EditText>(R.id.etZipCode)
        val btnSearchZip = findViewById<Button>(R.id.btnSearchZip)

        // validate zip then search
        val doZipSearch = {
            val zip = etZipCode.text.toString().trim()
            if (zip.length == 5) searchByZipCode(zip)
            else Toast.makeText(this, "Please enter a valid 5-digit zip code", Toast.LENGTH_SHORT).show()
        }

        btnSearchZip.setOnClickListener { doZipSearch() }
        etZipCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { doZipSearch(); true } else false
        }

        // zoom controls
        findViewById<Button>(R.id.btnZoomIn).setOnClickListener {
            map.animateCamera(CameraUpdateFactory.zoomIn())
        }
        findViewById<Button>(R.id.btnZoomOut).setOnClickListener {
            map.animateCamera(CameraUpdateFactory.zoomOut())
        }
        // search current map center
        findViewById<Button>(R.id.btnSearchArea).setOnClickListener {
            val center = map.cameraPosition.target
            map.clear()
            searchNearbyStores(center)
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        checkLocationPermissionAndSearch()
    }

    private fun checkLocationPermissionAndSearch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED -> enableLocationAndSearch()
            else -> requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun enableLocationAndSearch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        map.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                Toast.makeText(this, "Could not get your location", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            val userLatLng = LatLng(location.latitude, location.longitude)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13f))
            searchNearbyStores(userLatLng)
        }
    }

    // converts zip to coordinates
    private fun searchByZipCode(zip: String) {
        val geocoder = Geocoder(this, Locale.US)
            geocoder.getFromLocationName(zip, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    runOnUiThread {
                        if (addresses.isEmpty()) {
                            Toast.makeText(this@StoreMapActivity, "Could not find location for zip code $zip", Toast.LENGTH_SHORT).show()
                        } else {
                            val location = addresses[0]
                            val latLng = LatLng(location.latitude, location.longitude)
                            updateMapForZip(latLng)
                        }
                    }
                }
                override fun onError(errorMessage: String?) {
                    runOnUiThread {
                        Toast.makeText(this@StoreMapActivity, "Error looking up zip code", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun updateMapForZip(latLng: LatLng) {
        map.clear()
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
        searchNearbyStores(latLng)
    }

    private fun searchNearbyStores(center: LatLng) {
        val placeFields = listOf(Place.Field.DISPLAY_NAME, Place.Field.LOCATION, Place.Field.FORMATTED_ADDRESS)
        // 50km search radius
        val circle = CircularBounds.newInstance(center, 50000.0)
        val request = SearchNearbyRequest.builder(circle, placeFields)
            .setIncludedTypes(listOf("store"))
            .setMaxResultCount(20)
            .build()

        placesClient.searchNearby(request).addOnSuccessListener { response ->
            // drop blue marker per result
            for (place in response.places) {
                val latLng = place.location ?: continue
                map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(place.displayName)
                        .snippet(place.formattedAddress)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )
            }
            if (response.places.isEmpty()) {
                Toast.makeText(this, "No stores found in this area", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Search failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
