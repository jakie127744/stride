package com.stride.core.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapRegionTest {

    @Test
    fun `positive coordinates map to a region that contains the point`() {
        val point = GeoPoint(latitude = 40.7128, longitude = -74.0060) // New York City
        val region = regionFor(point)
        assertTrue(region.contains(point))
        assertEquals(40.0, region.minLatitude, 0.0)
        assertEquals(41.0, region.maxLatitude, 0.0)
        // floor(-74.0060) is -75.0, not -74.0 — floor rounds toward negative infinity, which is
        // exactly what a "cell covering this point" needs for a negative coordinate.
        assertEquals(-75.0, region.minLongitude, 0.0)
        assertEquals(-74.0, region.maxLongitude, 0.0)
    }

    @Test
    fun `negative coordinates produce a safe file-name-friendly id`() {
        val region = regionFor(GeoPoint(latitude = 40.7128, longitude = -74.0060))
        assertTrue("id was: ${region.id}", !region.id.contains("-"))
        assertTrue("id was: ${region.id}", !region.id.contains("."))
        assertEquals("cell_40_0_n75_0", region.id)
    }

    @Test
    fun `southern and western hemisphere coordinates still resolve to a containing region`() {
        val point = GeoPoint(latitude = -33.8688, longitude = 151.2093) // Sydney
        val region = regionFor(point)
        assertTrue(region.contains(point))
    }

    @Test
    fun `two points in the same cell resolve to the same region id`() {
        val a = regionFor(GeoPoint(40.71, -74.00))
        val b = regionFor(GeoPoint(40.99, -73.01))
        assertEquals(a.id, b.id)
    }

    @Test
    fun `a point just across a cell boundary resolves to a different region`() {
        val a = regionFor(GeoPoint(40.99, -74.00))
        val b = regionFor(GeoPoint(41.01, -74.00))
        assertTrue(a.id != b.id)
    }

    @Test
    fun `custom cell size is honored`() {
        val region = regionFor(GeoPoint(40.7128, -74.0060), cellSizeDegrees = 0.5)
        assertEquals(40.5, region.minLatitude, 0.0)
        assertEquals(-74.5, region.minLongitude, 0.0)
    }

    @Test
    fun `fileName appends the pmtiles extension`() {
        val region = regionFor(GeoPoint(40.7128, -74.0060))
        assertEquals("${region.id}.pmtiles", region.fileName)
    }
}
