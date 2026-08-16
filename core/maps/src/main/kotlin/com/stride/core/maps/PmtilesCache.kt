package com.stride.core.maps

import java.io.File

/**
 * Where downloaded PMTiles extracts live on-device, and whether a given region is already
 * cached. Pure file-system bookkeeping — no network, no MapLibre, no Android [android.content.Context]
 * dependency (that's resolved once, in [com.stride.core.maps.di.MapsModule], to keep this class
 * plain-JVM-testable — see PmtilesCacheTest).
 */
class PmtilesCache(private val cacheDir: File) {
    init {
        cacheDir.mkdirs()
    }

    fun fileFor(region: MapRegion): File = File(cacheDir, region.fileName)

    fun isCached(region: MapRegion): Boolean = fileFor(region).let { it.exists() && it.length() > 0 }

    /** Every region currently on disk, regardless of whether it's the one covering the runner's
     * *current* GPS fix — used for a "manage downloaded areas" settings surface later, and for
     * cache-eviction policy once one exists (none yet: extracts are a few MB each and nothing
     * proactively deletes them today). */
    fun cachedRegionIds(): List<String> =
        cacheDir.listFiles { file -> file.extension == "pmtiles" }
            ?.map { it.nameWithoutExtension }
            .orEmpty()

    fun delete(region: MapRegion): Boolean = fileFor(region).let { it.exists() && it.delete() }
}
