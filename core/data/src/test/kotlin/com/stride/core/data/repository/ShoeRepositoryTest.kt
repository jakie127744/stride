package com.stride.core.data.repository

import com.stride.core.database.entity.ShoeEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ShoeRepositoryTest {

    private fun shoe(total: Double, threshold: Double = 500_000.0, retiredAt: Instant? = null) = ShoeEntity(
        id = 1,
        name = "Test shoe",
        totalDistanceMeters = total,
        retirementThresholdMeters = threshold,
        retiredAt = retiredAt,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `not due below threshold`() {
        assertFalse(shoe(total = 499_999.0).isDueForRetirement())
    }

    @Test
    fun `due at exactly threshold`() {
        assertTrue(shoe(total = 500_000.0).isDueForRetirement())
    }

    @Test
    fun `due past threshold`() {
        assertTrue(shoe(total = 600_000.0).isDueForRetirement())
    }

    @Test
    fun `already retired shoe is never due again`() {
        assertFalse(shoe(total = 600_000.0, retiredAt = Instant.now()).isDueForRetirement())
    }
}
