package com.example.locationinfo

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.locationinfo.ui.theme.LocationInfoTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {

    // Location client to get the device's last known location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set the UI content with the custom theme and location screen composable
        setContent {
            LocationInfoTheme {
                LocationInfoScreen(fusedLocationClient)
            }
        }
    }
}

// Composable function to display the location info and map
@Composable
fun LocationInfoScreen(
    fusedLocationClient: FusedLocationProviderClient
) {
    val context = LocalContext.current // Context needed for permissions and geocoder
    val scope = rememberCoroutineScope() // Coroutine scope for background tasks

    // State to track if location permission has been granted
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Launcher to request location permission from the user
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted // Update state based on user response
    }

    // Request location permission once when the composable is first launched
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // State to hold user's current location coordinates
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    // State to hold the address string corresponding to the location
    var addressText by remember { mutableStateOf("Waiting for location...") }

    // Camera state for controlling Google Maps camera position
    val cameraPositionState = rememberCameraPositionState()

    // Mutable state for user's location marker on the map
    val userMarkerState = remember { MarkerState() }

    // List to hold custom marker positions added by the user
    val customMarkers = remember { mutableStateListOf<LatLng>() }

    // Fetch device location and update UI when permission is granted
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val fine = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (fine) {
                // Get last known device location asynchronously
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        val latLng = LatLng(loc.latitude, loc.longitude)
                        userLocation = latLng
                        userMarkerState.position = latLng

                        // Set camera position to user's current location
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(latLng, 15f)

                        // Perform reverse geocoding on background thread to get address
                        scope.launch {
                            addressText = reverseGeocode(context, latLng) ?: "Address not found"
                        }
                    }
                }
            }
        }
    }

    // UI layout for the screen
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Blue)
    ) {
        // Title text
        Text(
            "Location Information",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )

        // Address or status text
        Text(
            addressText,
            color = Color.LightGray,
            modifier = Modifier.padding(start = 16.dp)
        )

        // Map container that fills remaining space
        Box(modifier = Modifier.weight(1f)) {

            // Show map and markers if permission granted and location available
            if (hasPermission && userLocation != null) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { position ->
                        customMarkers.add(position) // Add custom markers on map tap
                    }
                ) {
                    // User location marker
                    Marker(
                        state = userMarkerState,
                        title = "You are here"
                    )

                    // Custom markers added by user
                    customMarkers.forEach { pos ->
                        Marker(
                            state = MarkerState(position = pos),
                            title = "Custom Marker"
                        )
                    }
                }
            } else {
                // Message when permission not granted or location unavailable
                Text(
                    "Location permission required",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
        }
    }
}
// Function to get the address string from latitude and longitude using Geocoder
suspend fun reverseGeocode(
    context: android.content.Context,
    latLng: LatLng
): String? =
    withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val result = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            result?.firstOrNull()?.getAddressLine(0) // Return first address line if available
        } catch (e: Exception) {
            null // Return null if geocoding fails
        }
    }
