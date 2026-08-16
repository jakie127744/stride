package com.stride.core.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PmtilesCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun cache(): PmtilesCache = PmtilesCache(tempFolder.newFolder("pmtiles"))

    private val region = MapRegion(
        id = "cell_40_0_n74_0",
        minLatitude = 40.0,
        maxLatitude = 41.0,
        minLongitude = -74.0,
        maxLongitude = -73.0,
    )

    @Test
    fun `region is not cached until its file exists`() {
        val cache = cache()
        assertFalse(cache.isCached(region))
    }

    @Test
    fun `region is cached once a non-empty file is written`() {
        val cache = cache()
        cache.fileFor(region).writeText("fake pmtiles bytes")
        assertTrue(cache.isCached(region))
    }

    @Test
    fun `an empty file does not count as cached`() {
        val cache = cache()
        cache.fileFor(region).createNewFile()
        assertFalse(cache.isCached(region))
    }

    @Test
    fun `cachedRegionIds lists only pmtiles files by their id`() {
        val cache = cache()
        cache.fileFor(region).writeText("data")
        cache.fileFor(region.copy(id = "cell_41_0_n74_0")).writeText("data")
        // Not a .pmtiles file — must not show up as a cached region.
        cache.fileFor(region).parentFile?.resolve("notes.txt")?.writeText("irrelevant")

        assertEquals(setOf("cell_40_0_n74_0", "cell_41_0_n74_0"), cache.cachedRegionIds().toSet())
    }

    @Test
    fun `delete removes a cached region's file`() {
        val cache = cache()
        cache.fileFor(region).writeText("data")
        assertTrue(cache.delete(region))
        assertFalse(cache.isCached(region))
    }

    @Test
    fun `delete on an uncached region is a harmless no-op`() {
        val cache = cache()
        assertFalse(cache.delete(region))
    }
}
