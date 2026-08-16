package com.stride.core.data.scheduler

import com.stride.core.database.entity.PlanSessionEntity
import com.stride.core.database.entity.PlanSessionStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Reconciles missed sessions per docs/foundation.md "Adaptive Scheduling": compress -> shift ->
 * regress-and-repeat, in that preference order. Pure and stateless — no I/O, no database, no
 * clock reads (today is passed in) — [com.stride.core.data.repository.PlanRepositoryImpl] is
 * what actually reads/writes; this only decides. That split is what makes every branch here
 * directly unit-testable without a real Room database — see AdaptiveSchedulerTest.
 *
 * Honest scope note: full multi-week regression (skipping back to *repeat an earlier week's
 * content*, distinct from just moving dates around) needs real multi-week plan authoring, which
 * doesn't exist yet — `OnboardingViewModel` generates a single starter week today (see its own
 * doc comment). Until multi-week plans are real, REGRESS and SHIFT both move dates forward; what
 * actually distinguishes them here is the *trigger* (a long absence or a hard-RPE trend regresses
 * instead of just shifting) — the runner coming back from a bad stretch gets the same
 * date-shifting relief either way, which is the part of the spec that's meaningful before
 * multi-week plans exist to regress *into*.
 */
object AdaptiveScheduler {

    /** A gap this long stops being "shift the schedule a bit" and starts being "this runner
     * needs a reset, not just a nudge" — ten days is over a full week beyond the missed session
     * itself, not just a busy weekend. */
    private const val REGRESS_GAP_DAYS = 10L

    enum class Strategy { COMPRESS, SHIFT, REGRESS }

    data class Reconciliation(val strategy: Strategy, val updatedSessions: List<PlanSessionEntity>)

    /**
     * @param sessions every session for the plan, any status — needed to know which dates are
     *   already occupied and where the "current week" boundary falls.
     * @param recentSessionsFeltHard true if the runner's last couple of completed sessions on
     *   this plan were logged with a high RPE — same regress-trigger signal the walk/run preset
     *   recommendation already uses (see `recommendedPresetForWeek`'s doc comment), applied here
     *   to scheduling instead of pace.
     * @return null if there's nothing to reconcile (no missed sessions).
     */
    fun reconcile(
        today: LocalDate,
        sessions: List<PlanSessionEntity>,
        recentSessionsFeltHard: Boolean,
    ): Reconciliation? {
        val missed = sessions
            .filter { it.status == PlanSessionStatus.PLANNED && it.scheduledDate.isBefore(today) }
            .sortedBy { it.scheduledDate }
        if (missed.isEmpty()) return null

        val gapDays = ChronoUnit.DAYS.between(missed.first().scheduledDate, today)
        val strategy = when {
            gapDays >= REGRESS_GAP_DAYS || recentSessionsFeltHard -> Strategy.REGRESS
            fitsWithinCurrentWeek(today, sessions, missed) -> Strategy.COMPRESS
            else -> Strategy.SHIFT
        }

        val updated = when (strategy) {
            Strategy.COMPRESS -> compress(today, sessions, missed)
            Strategy.SHIFT, Strategy.REGRESS -> shift(today, sessions, missed)
        }
        return Reconciliation(strategy, updated)
    }

    /** True if every missed session can land on a free day between today and the end of this
     * calendar week (Monday-start) without colliding with an already-occupied date. */
    private fun fitsWithinCurrentWeek(
        today: LocalDate,
        sessions: List<PlanSessionEntity>,
        missed: List<PlanSessionEntity>,
    ): Boolean {
        val weekEnd = today.with(DayOfWeek.SUNDAY).let { if (it.isBefore(today)) it.plusWeeks(1) else it }
        val occupied = occupiedDatesExcluding(sessions, missed)
        var cursor = today
        var placed = 0
        while (!cursor.isAfter(weekEnd) && placed < missed.size) {
            if (cursor !in occupied) {
                occupied.add(cursor)
                placed++
            }
            cursor = cursor.plusDays(1)
        }
        return placed == missed.size
    }

    /** Slots each missed session into the next free day from today onward, staying as close to
     * today as possible without colliding with another already-scheduled date. */
    private fun compress(
        today: LocalDate,
        sessions: List<PlanSessionEntity>,
        missed: List<PlanSessionEntity>,
    ): List<PlanSessionEntity> {
        val occupied = occupiedDatesExcluding(sessions, missed)
        var cursor = today
        return missed.map { session ->
            while (cursor in occupied) cursor = cursor.plusDays(1)
            occupied.add(cursor)
            session.copy(scheduledDate = cursor, status = PlanSessionStatus.RESCHEDULED).also { cursor = cursor.plusDays(1) }
        }
    }

    /** Slides every remaining PLANNED session (the missed ones and everything still ahead of
     * them) forward by the same number of days, preserving their relative spacing — the whole
     * remaining plan moves together rather than compressing into gaps. */
    private fun shift(
        today: LocalDate,
        sessions: List<PlanSessionEntity>,
        missed: List<PlanSessionEntity>,
    ): List<PlanSessionEntity> {
        val shiftDays = ChronoUnit.DAYS.between(missed.first().scheduledDate, today)
        return sessions
            .filter { it.status == PlanSessionStatus.PLANNED && !it.scheduledDate.isBefore(missed.first().scheduledDate) }
            .map { it.copy(scheduledDate = it.scheduledDate.plusDays(shiftDays), status = PlanSessionStatus.RESCHEDULED) }
    }

    private fun occupiedDatesExcluding(
        sessions: List<PlanSessionEntity>,
        excluded: List<PlanSessionEntity>,
    ): MutableSet<LocalDate> {
        val excludedIds = excluded.map { it.id }.toSet()
        return sessions
            .asSequence()
            .filter { it.status != PlanSessionStatus.SKIPPED && it.id !in excludedIds }
            .map { it.scheduledDate }
            .toMutableSet()
    }
}
