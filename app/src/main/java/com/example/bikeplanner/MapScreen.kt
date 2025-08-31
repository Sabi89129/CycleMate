package com.example.bikeplanner

import android.Manifest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.content.Context
import android.location.Location
import androidx.compose.ui.platform.LocalContext
import android.location.LocationListener
import android.location.LocationManager
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.LocalLifecycleOwner

/*
private val T.latitude: Double
private val T.longitude: Double
private val compose: Any
private val compose: Any
private val compose: Any
*/
@Composable
fun BikeRoutingScreen() {
    // 1. Definiere  Berechtigung und Standort
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 2. Erstelle einen Launcher für die Berechtigungsabfrage
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        locationPermissionGranted = isGranted
    }

    // 3. Starte die Abfrage, sobald das Composable aktiv wird
    LaunchedEffect(Unit) {
        // Starte die Berechtigungsabfrage, sobald die App sichtbar wird
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // 4. Starte den Location Listener, wenn die Berechtigung erteilt wurde
    DisposableEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    // Update den Zustand mit der neuen Position
                    currentLocation = location
                }
            }
            try {
                // Registriere den Listener für Standort-Updates
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L, 5f, locationListener
                )
            } catch (e: SecurityException) {
                // Sollte nicht passieren, aber zur Sicherheit
            }
            onDispose {
                // Bei Verlassen des Screens den Listener deregistrieren
                lm.removeUpdates(locationListener)
            }
        } else {
            onDispose {} // Nichts zu tun, wenn keine Berechtigung
        }
    }

    AndroidView(
        factory = { ctx: Context ->
            // MapLibre Instanz initialisieren
            MapLibre.getInstance(ctx)

            // IDs für die "Du bist hier"-Anzeige
            val hereSourceId = "here-src"
            val hereLayerId = "here-layer"

            // MapView erstellen und Lifecycle-Events hinzufügen
            MapView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                onCreate(null)

                // Karte asynchron laden und konfigurieren
                getMapAsync { map: MapLibreMap ->
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
            }
        },
        update = { mapView ->
            // Dieser Block wird jedes Mal ausgeführt, wenn sich 'currentLocation' ändert
            currentLocation?.let { loc ->
                mapView.getMapAsync { map ->
                    val style = map.style ?: return@getMapAsync

                    // Definiere die IDs für Quelle und Layer hier, um sie im Lambda verfügbar zu machen
                    val hereSourceId = "here-src"
                    val hereLayerId = "here-layer"

                    // Den GeoJson-Punkt auf der Karte aktualisieren
                    val src = style.getSourceAs<GeoJsonSource>(hereSourceId)
                    src?.setGeoJson(Point.fromLngLat(loc.longitude, loc.latitude))

                    // Hier kommt die fehlende Zentrierlogik
                    // Du brauchst eine Variable, die in einem übergeordneten Scope definiert ist
                    // z.B. var didCenterOnce by remember { mutableStateOf(false) }

                    var didCenterOnce = false
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
    )



}