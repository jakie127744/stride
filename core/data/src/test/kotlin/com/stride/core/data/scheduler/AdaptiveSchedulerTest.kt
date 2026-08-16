package com.stride.core.data.scheduler

import com.stride.core.database.entity.PlanSessionEntity
import com.stride.core.database.entity.PlanSessionStatus
import com.stride.core.database.entity.PlanSessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class AdaptiveSchedulerTest {

    private fun session(
        id: Long,
        date: LocalDate,
        status: PlanSessionStatus = PlanSessionStatus.PLANNED,
    ) = PlanSessionEntity(
        id = id,
        planId = 1,
        weekNumber = 1,
        scheduledDate = date,
        sessionType = PlanSessionType.EASY_RUN,
        status = status,
    )

    // A fixed Monday so "current week" boundaries in every test are unambiguous.
    private val monday = LocalDate.of(2026, 8, 17).also { assertEquals(DayOfWeek.MONDAY, it.dayOfWeek) }

    @Test
    fun `no missed sessions returns null`() {
        val sessions = listOf(session(1, monday), session(2, monday.plusDays(2)))
        assertNull(AdaptiveScheduler.reconcile(today = monday, sessions = sessions, recentSessionsFeltHard = false))
    }

    @Test
    fun `a single missed session with room this week compresses to today`() {
        val today = monday.plusDays(2) // Wednesday
        val missed = session(1, monday) // Monday, missed
        val sessions = listOf(missed)

        val result = AdaptiveScheduler.reconcile(today, sessions, recentSessionsFeltHard = false)

        requireNotNull(result)
        assertEquals(AdaptiveScheduler.Strategy.COMPRESS, result.strategy)
        assertEquals(1, result.updatedSessions.size)
        assertEquals(today, result.updatedSessions.first().scheduledDate)
        assertEquals(PlanSessionStatus.RESCHEDULED, result.updatedSessions.first().status)
    }

    @Test
    fun `compress skips a date that's already occupied by another session`() {
        val today = monday.plusDays(2) // Wednesday
        val missed = session(1, monday)
        val alreadyOnWednesday = session(2, today) // occupies today
        val sessions = listOf(missed, alreadyOnWednesday)

        val result = AdaptiveScheduler.reconcile(today, sessions, recentSessionsFeltHard = false)

        requireNotNull(result)
        assertEquals(AdaptiveScheduler.Strategy.COMPRESS, result.strategy)
        assertEquals(today.plusDays(1), result.updatedSessions.first().scheduledDate) // Thursday, not Wednesday
    }

    @Test
    fun `missed sessions that outnumber the remaining days this week shift instead`() {
        val today = monday.plusDays(6) // Sunday — only today itself is left this week
        val firstMissed = session(1, monday)
        val secondMissed = session(2, monday.plusDays(1)) // two missed, only one slot available
        val sessions = listOf(firstMissed, secondMissed)

        val result = AdaptiveScheduler.reconcile(today, sessions, recentSessionsFeltHard = false)

        requireNotNull(result)
        assertEquals(AdaptiveScheduler.Strategy.SHIFT, result.strategy)
        assertEquals(today, result.updatedSessions.first { it.id == 1L }.scheduledDate)
    }

    @Test
    fun `shift moves every remaining planned session forward by the same amount, not just the missed one`() {
        val today = monday.plusDays(6) // Sunday
        val firstMissed = session(1, monday)
        val secondMissed = session(2, monday.plusDays(1)) // forces SHIFT over COMPRESS (see test above)
        val stillUpcoming = session(3, monday.plusDays(10)) // more than a week out, still PLANNED
        val sessions = listOf(firstMissed, secondMissed, stillUpcoming)

        val result = AdaptiveScheduler.reconcile(today, sessions, recentSessionsFeltHard = false)

        requireNotNull(result)
        assertEquals(AdaptiveScheduler.Strategy.SHIFT, result.strategy)
        val shiftDays = 6L // Monday -> Sunday, from the earliest missed session
        val updatedById = result.updatedSessions.associateBy { it.id }
        assertEquals(today, updatedById.getValue(1).scheduledDate)
        assertEquals(monday.plusDays(10 + shiftDays), updatedById.getValue(3).scheduledDate)
    }

    @Test
    fun `shift does not touch sessions already before the missed one`() {
        val today = monday.plusDays(6)
        val alreadyCompleted = session(1, monday.minusDays(3), status = PlanSessionStatus.COMPLETED)
        val missed = session(2, monday)
        val sessions = listOf(alreadyCompleted, missed)

        val result = AdaptiveScheduler.reconcile(today, sessions, recentSessionsFeltHard = false)

        requireNotNull(result)
        // Only the missed session should appear in the update set — the completed one is untouched.
        assertEquals(1, result.updatedSessions.size)
        assertEquals(2L, result.updatedSessions.first().id)
    }

    @Test
    fun `a long gap regresses instead of compressing even though there's room this week`() {
        val today = monday.plusDays(11) // 11 days after the missed session — past the regress threshold
        val missed = session(1, monday)
        val sessions = listOf(missed)

        val result = AdaptiveScheduler.reconcile(today, sessions, recentSessionsFeltHard = false)

        requireNotNull(result)
        assertEquals(AdaptiveScheduler.Strategy.REGRESS, result.strategy)
        assertEquals(today, result.updatedSessions.first().scheduledDate)
    }

    @Test
    fun `a hard RPE trend regresses even for a short, otherwise-compressible gap`() {
        val today = monday.plusDays(1) // Tuesday — would easily compress on its own
        val missed = session(1, monday)
        val sessions = listOf(missed)

        val result = AdaptiveScheduler.reconcile(today, sessions, recentSessionsFeltHard = true)

        requireNotNull(result)
        assertEquals(AdaptiveScheduler.Strategy.REGRESS, result.strategy)
    }

    @Test
    fun `multiple missed sessions all get placed when compressing`() {
        val today = monday.plusDays(3) // Thursday
        val sessions = listOf(session(1, monday), session(2, monday.plusDays(1)))

        val result = AdaptiveScheduler.reconcile(today, sessions, recentSessionsFeltHard = false)

        requireNotNull(result)
        assertEquals(AdaptiveScheduler.Strategy.COMPRESS, result.strategy)
        val dates = result.updatedSessions.map { it.scheduledDate }.sorted()
        assertEquals(listOf(today, today.plusDays(1)), dates)
    }

    @Test
    fun `a skipped session's original date does not block compression`() {
        val today = monday.plusDays(2) // Wednesday
        val missed = session(1, monday)
        val skipped = session(2, today, status = PlanSessionStatus.SKIPPED) // shouldn't occupy today
        val sessions = listOf(missed, skipped)

        val result = AdaptiveScheduler.reconcile(today, sessions, recentSessionsFeltHard = false)

        requireNotNull(result)
        assertTrue(result.updatedSessions.any { it.id == 1L && it.scheduledDate == today })
    }
}
