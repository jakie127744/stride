package com.stride.core.maps

import java.util.Locale
import kotlin.math.floor

data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * A single downloadable/cacheable PMTiles extract, addressed by a coarse lat/lng grid cell
 * rather than a real place name — "which city/county is this GPS fix in" isn't a
 * client-computable concept without a geocoding lookup service this app doesn't have a backend
 * for. A grid cell is simple, deterministic, and matches how the extracts actually get built and
 * uploaded to R2 (see docs/foundation.md "Route mapping" for the hosting decision) — the upload
 * pipeline outside this app names each extract file by the same [id] scheme.
 */
data class MapRegion(
    val id: String,
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
) {
    fun contains(point: GeoPoint): Boolean =
        point.latitude in minLatitude..maxLatitude && point.longitude in minLongitude..maxLongitude

    /** File name (and R2 object key) for this region's PMTiles extract. */
    val fileName: String get() = "$id.pmtiles"
}

/** Degrees per side of a region cell. 1.0° is a rough middle ground between "small enough that a
 * single extract stays a few MB" and "large enough a runner doesn't cross a cell boundary and
 * trigger a fresh download every few km" — at mid-latitudes that's on the order of 80-110km per
 * side, roughly county-to-small-metro sized. Not tuned against real extract file sizes yet; that
 * needs actual PMTiles builds to measure against, not a guess. */
const val DEFAULT_REGION_CELL_SIZE_DEGREES = 1.0

/**
 * Deterministically maps a GPS fix to the region cell that should cover it. Pure function
 * (no I/O, no Android dependency) so it's trivially unit-testable — see [MapRegionTest].
 */
fun regionFor(point: GeoPoint, cellSizeDegrees: Double = DEFAULT_REGION_CELL_SIZE_DEGREES): MapRegion {
    val minLat = floor(point.latitude / cellSizeDegrees) * cellSizeDegrees
    val minLng = floor(point.longitude / cellSizeDegrees) * cellSizeDegrees
    val id = "cell_${formatCoord(minLat)}_${formatCoord(minLng)}"
    return MapRegion(
        id = id,
        minLatitude = minLat,
        maxLatitude = minLat + cellSizeDegrees,
        minLongitude = minLng,
        maxLongitude = minLng + cellSizeDegrees,
    )
}

/** "-74.0" -> "n74_0" — a region id needs to be a safe file name / URL path segment, not a
 * signed decimal (no `-`, no `.` ambiguity with the `.pmtiles` extension). */
private fun formatCoord(value: Double): String {
    val formatted = String.format(Locale.US, "%.1f", value)
    return if (formatted.startsWith("-")) {
        "n${formatted.removePrefix("-")}"
    } else {
        formatted
    }.replace(".", "_")
}
