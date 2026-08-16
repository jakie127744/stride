package com.stride.app.ui

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.stride.app.BuildConfig
import com.stride.app.location.TrackPoint
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * A real interactive basemap under the GPS path, not just a raw polyline on a blank canvas.
 * Honest scope note: the style URL points at MapLibre's own public demo tile server
 * (https://demotiles.maplibre.org) — free to use for exactly this kind of development, but not
 * meant for production traffic. Before shipping, this switches to self-hosted Protomaps PMTiles
 * per docs/foundation.md's "Route mapping" spec — that's a data-hosting decision, not a code
 * change here. `MapLibre.getInstance()` is called once in StrideApplication.onCreate().
 *
 * Known simplification: this MapView is rebuilt from scratch whenever the Track tab unmounts
 * and remounts (switching tabs on Active Run) — full onCreate/onDestroy cycling each switch,
 * camera state not preserved. Not incorrect, just not optimized; fine for how this screen is
 * actually used (glance at the track, glance away, not rapid tab-flipping).
 *
 * The style URL itself lives in [BuildConfig.MAP_STYLE_URL] (set in app/build.gradle.kts), not
 * hardcoded here — one place to swap in a self-hosted Protomaps URL later. A release build still
 * pointed at the demo server by omission is a real risk worth flagging loudly, not silently.
 */
private const val TRACK_SOURCE_ID = "stride-track-source"
private const val TRACK_LAYER_ID = "stride-track-layer"

@Composable
fun TrackMapView(points: List<TrackPoint>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        if (!BuildConfig.DEBUG && BuildConfig.MAP_STYLE_URL.contains("demotiles.maplibre.org")) {
            Log.w(
                "TrackMapView",
                "Release build is still pointed at MapLibre's public demo tile server — " +
                    "swap BuildConfig.MAP_STYLE_URL to a self-hosted Protomaps URL before shipping.",
            )
        }
        MapView(context).apply {
            onCreate(Bundle())
            getMapAsync { map ->
                map.setStyle(Style.Builder().fromUri(BuildConfig.MAP_STYLE_URL)) { style ->
                    style.addSource(GeoJsonSource(TRACK_SOURCE_ID))
                    style.addLayer(
                        LineLayer(TRACK_LAYER_ID, TRACK_SOURCE_ID).withProperties(
                            PropertyFactory.lineColor(AndroidColor.WHITE),
                            PropertyFactory.lineWidth(4f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        ),
                    )
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            if (points.size < 2) return@AndroidView
            view.getMapAsync { map ->
                val latLngs = points.map { LatLng(it.latitude, it.longitude) }
                val geoPoints = points.map { Point.fromLngLat(it.longitude, it.latitude) }
                map.style?.getSourceAs<GeoJsonSource>(TRACK_SOURCE_ID)
                    ?.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(geoPoints)))

                val bounds = LatLngBounds.Builder().apply { latLngs.forEach { include(it) } }.build()
                runCatching {
                    map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80), 400)
                }
            }
        },
    )
}
