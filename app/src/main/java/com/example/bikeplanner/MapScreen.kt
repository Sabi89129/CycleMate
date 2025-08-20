package com.example.bikeplanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Point

@Composable
fun BikeRoutingScreen() {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier,
        factory = { ctx: Context ->
            // MapLibre Instanz initialisieren
            MapLibre.getInstance(ctx)

            // MapView erstellen und Lifecycle-Events hinzufügen
            val mapView = MapView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                onCreate(null)
            }

            // IDs für die "Du bist hier"-Anzeige
            val hereSourceId = "here-src"
            val hereLayerId = "here-layer"

            // Location Infrastruktur
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var didCenterOnce = false

            // Helper: "You are here"-Punkt im Kartenstil aktualisieren
            fun updateHere(style: Style, lat: Double, lon: Double) {
                val src = style.getSourceAs<GeoJsonSource>(hereSourceId)
                if (src != null) {
                    src.setGeoJson(Point.fromLngLat(lon, lat))
                }
            }

            // LocationListener, der bei Standortänderungen aufgerufen wird
            val locationListener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    mapView.getMapAsync { map: MapLibreMap ->
                        val style = map.style ?: return@getMapAsync
                        updateHere(style, loc.latitude, loc.longitude)

                        // Karte nur einmal auf den Standort zentrieren, um Wackeln zu vermeiden
                        if (!didCenterOnce) {
                            didCenterOnce = true
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(loc.latitude, loc.longitude))
                                .zoom(15.0)
                                .build()
                        }
                    }
                }
            }

            // Sicherstellen, dass die MapView-Lifecycle-Events mit dem Composable-Lifecycle synchronisiert werden
            val observer = LifecycleEventObserver { _, e ->
                when (e) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            // Karte asynchron laden und konfigurieren
            mapView.getMapAsync { map: MapLibreMap ->
                map.setMinZoomPreference(2.0)
                map.setMaxZoomPreference(22.0)

                // Stil aus OpenCycleMap definieren
                val ocmStyle = """
                    {
                    "version": 8,
                    "name": "OCM",
                    "sources": {
                    "ocm": {
                    "type": "raster",
                    "tiles": ["https://tile.thunderforest.com/cycle/{z}/{x}/{y}.png?apikey=${BuildConfig.THUNDERFOREST_KEY}"],
                    "tileSize": 256,
                    "attribution": "Maps © Thunderforest, Data © OpenStreetMap contributors"
                    }
                },
                "layers": [
                    { "id": "ocm-layer", "type": "raster", "source": "ocm" }
                    ]
                }
                """.trimIndent()

                map.setStyle(Style.Builder().fromJson(ocmStyle)) { style ->
                    // "You are here"-Quelle und -Layer anlegen (mit Dummy-Koordinaten)
                    if (style.getSource(hereSourceId) == null) {
                        style.addSource(
                            GeoJsonSource(hereSourceId, Point.fromLngLat(0.0, 0.0))
                        )
                    }
                    if (style.getLayer(hereLayerId) == null) {
                        style.addLayer(
                            CircleLayer(hereLayerId, hereSourceId).withProperties(
                                circleColor("#2196F3"),
                                circleRadius(6f),
                                circleStrokeColor("#FFFFFF"),
                                circleStrokeWidth(2f)
                            )
                        )
                    }
                }
            }

            // Location-Updates sofort abonnieren, direkt im factory Block
            // Überprüfen der Location-Berechtigungen
            val fineGranted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (fineGranted || coarseGranted) {
                // Updates abonnieren
                try {
                    lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        2000L, 5f, locationListener
                    )
                } catch (_: SecurityException) {
                    // Falls die Berechtigungen zur Laufzeit entzogen wurden
                }

                try {
                    lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        2000L, 5f, locationListener
                    )
                } catch (_: SecurityException) {
                    // Falls die Berechtigungen zur Laufzeit entzogen wurden
                }

                // Optional: sofortiger Punkt mit LastKnownLocation
                (lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))?.let { last ->
                    mapView.getMapAsync { map: MapLibreMap ->
                        val style = map.style
                        if (style != null) {
                            updateHere(style, last.latitude, last.longitude)
                            if (!didCenterOnce) {
                                didCenterOnce = true
                                map.cameraPosition = CameraPosition.Builder()
                                    .target(LatLng(last.latitude, last.longitude))
                                    .zoom(15.0)
                                    .build()
                            }
                        }
                    }
                }
            }
            mapView
        },
        update = { /* Keine Updates hier nötig */ }
    )
}