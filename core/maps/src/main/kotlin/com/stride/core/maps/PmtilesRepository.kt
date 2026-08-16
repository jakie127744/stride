package com.stride.core.maps

import java.io.File

/**
 * Download-and-cache half of "download based on where you're running" (docs/foundation.md
 * "Route mapping"). Deliberately does NOT touch MapLibre or render anything — turning a cached
 * `.pmtiles` file into map tiles MapLibre can draw needs a local PMTiles-format reader and tile
 * server this repository doesn't implement (see the doc's "Client-side reading" note on why
 * that's real remaining work, not done here).
 */
interface PmtilesRepository {
    /** Which region cell a GPS fix falls in — pure, no I/O. */
    fun regionFor(point: GeoPoint): MapRegion

    fun isCached(region: MapRegion): Boolean

    /** Null if [region] hasn't been downloaded (or was evicted) yet. */
    fun cachedFileFor(region: MapRegion): File?

    /**
     * Downloads [region]'s extract and caches it on-device. Returns the cached file on success.
     * Network/HTTP failures are reported as a failed [Result] rather than thrown — a failed map
     * download should never crash a run in progress, same principle as
     * [com.stride.core.weather.WeatherRepository] returning null on failure instead of throwing.
     */
    suspend fun downloadRegion(region: MapRegion): Result<File>
}
