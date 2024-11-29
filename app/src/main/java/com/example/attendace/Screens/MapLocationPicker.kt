package com.example.attendace.Screens

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.attendace.SharedPrefHelper
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickLocationScreen(navController: NavController? = null, sharedPreferences: SharedPreferences) {
    val context = LocalContext.current

    // Initialize OSM
    Configuration.getInstance().load(context, sharedPreferences)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Pick A Location",
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize(),
                contentAlignment = Alignment.Center
        ) {
            // Add OSM Map
            AndroidView(factory = { ctx ->
                MapView(ctx).apply {
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(29.86971, 77.89503))
                }
            },update = { mapView ->
                val singleTapOverlay = object : Overlay() {
                    override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
                        val projection = mapView.projection
                        val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                        val latitude = geoPoint.latitude
                        val longitude = geoPoint.longitude

                        // Save the picked location in SharedPreferences
                        SharedPrefHelper.saveFloat(sharedPreferences, "picked_latitude", latitude.toFloat())
                        SharedPrefHelper.saveFloat(sharedPreferences, "picked_longitude", longitude.toFloat())
                        SharedPrefHelper.saveBoolean(sharedPreferences, "isFirstLaunch", false)

                        // Display the picked location as a Toast
                        Toast.makeText(context, "Picked Location: $latitude, $longitude", Toast.LENGTH_SHORT).show()

                        // Add a marker at the picked location
                        val marker = Marker(mapView).apply {
                            position = geoPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            textLabelFontSize = 20
                        }
                        mapView.overlays.clear()
                        mapView.overlays.add(marker)
                        mapView.invalidate()

                        navController?.navigate("home")

                        return true
                    }
                }
                mapView.overlays.add(singleTapOverlay)
            })
        }
        padding
    }
}