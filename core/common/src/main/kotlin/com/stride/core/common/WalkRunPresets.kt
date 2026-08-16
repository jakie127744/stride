package com.stride.core.common

/**
 * A named walk/run ratio a beginner-track session can be built from — e.g. "4:00 walk @ 3.0mph,
 * then 1:00 run @ 6.0mph, repeated". Presets exist so a runner is never handed a blank pace
 * field: :feature:plan (Phase 6) picks a default preset from recent RPE + completed-week
 * progress, shows it as an editable suggestion, and lets the runner override before starting —
 * see docs/foundation.md "Session pacing".
 */
data class WalkRunPreset(
    val id: String,
    val label: String,
    val walkDurationSeconds: Int,
    val walkSpeedMph: Double,
    val runDurationSeconds: Int,
    val runSpeedMph: Double,
    /** How many walk/run pairs make up a full session at this preset — combined with the
     * runner's chosen session length to decide the actual rep count at generation time. */
    val suggestedWeekRange: IntRange,
)

/**
 * A representative slice of the beginner progression, not the full 8–9 week table (that's real
 * plan-authoring content for Phase 6). Each step gets a little harder: longer runs, shorter
 * walks, faster run pace — never a jump a runner couldn't reasonably feel ready for.
 */
val BeginnerWalkRunPresets: List<WalkRunPreset> = listOf(
    WalkRunPreset(
        id = "week1_2",
        label = "Getting started",
        walkDurationSeconds = 4 * 60,
        walkSpeedMph = 3.0,
        runDurationSeconds = 1 * 60,
        runSpeedMph = 5.0,
        suggestedWeekRange = 1..2,
    ),
    WalkRunPreset(
        id = "week3_4",
        label = "Building",
        walkDurationSeconds = 3 * 60,
        walkSpeedMph = 3.2,
        runDurationSeconds = 90,
        runSpeedMph = 5.5,
        suggestedWeekRange = 3..4,
    ),
    WalkRunPreset(
        id = "week5_6",
        label = "Steady progress",
        walkDurationSeconds = 2 * 60,
        walkSpeedMph = 3.2,
        runDurationSeconds = 3 * 60,
        runSpeedMph = 6.0,
        suggestedWeekRange = 5..6,
    ),
    WalkRunPreset(
        id = "week7_8",
        label = "Nearly continuous",
        walkDurationSeconds = 60,
        walkSpeedMph = 3.5,
        runDurationSeconds = 8 * 60,
        runSpeedMph = 6.0,
        suggestedWeekRange = 7..8,
    ),
)

/**
 * Naive recommendation: current week clamps into the nearest preset's range. The real version
 * (Phase 6) also weighs recent RPE — repeatedly-hard sessions hold at the current preset an
 * extra week instead of advancing, same spirit as the adaptive scheduler's regress-and-repeat.
 */
fun recommendedPresetForWeek(week: Int): WalkRunPreset =
    BeginnerWalkRunPresets.firstOrNull { week in it.suggestedWeekRange }
        ?: if (week < BeginnerWalkRunPresets.first().suggestedWeekRange.first) {
            BeginnerWalkRunPresets.first()
        } else {
            BeginnerWalkRunPresets.last()
        }
