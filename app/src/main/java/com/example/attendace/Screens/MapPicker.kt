package com.example.attendace.Screens

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.attendace.MainActivity
import com.example.attendace.ui.theme.AttendAceTheme
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.OverlayWithIW
import android.util.Log

class LocationPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MyMap", "Reached on the MyMap")

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm_prefs", MODE_PRIVATE))

        setContent {
            AttendAceTheme {
                MapScreen(
                    sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
                )
            }
        }
    }
}

@Composable
fun MapScreen(sharedPreferences: SharedPreferences) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            MapView(ctx).apply {
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(48.8566, 2.3522)) // Default to Paris
            }
        }, update = { mapView ->
            // Add a custom overlay to handle single taps
            val singleTapOverlay = object : Overlay() {
                override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
                    val geoPoint = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                    val latitude = geoPoint.latitude
                    val longitude = geoPoint.longitude

                    // Display the picked location
                    Toast.makeText(context, "Picked Location: $latitude, $longitude", Toast.LENGTH_SHORT).show()

                    // Add a marker at the selected location
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(latitude, longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.clear()
                    mapView.overlays.add(marker)
                    mapView.invalidate()

                    // Save coordinates to SharedPreferences
                    saveCoordinates(sharedPreferences, latitude, longitude)
                    Log.d("MyMap", "coordinates saved")

                    // Navigate to MainActivity after saving
                    coroutineScope.launch {
                        sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply()
                        Log.d("MyMap", "Switching to MainActivity")
                        context.startActivity(Intent(context, MainActivity::class.java))
                        (context as ComponentActivity).finish()
                    }

                    return true
                }
            }
            mapView.overlays.add(singleTapOverlay)
        })
    }
}

fun saveCoordinates(sharedPreferences: SharedPreferences, latitude: Double, longitude: Double) {
    sharedPreferences.edit()
        .putFloat("classroom_latitude", latitude.toFloat())
        .putFloat("classroom_longitude", longitude.toFloat())
        .apply()
}
