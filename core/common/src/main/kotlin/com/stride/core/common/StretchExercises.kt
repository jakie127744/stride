package com.stride.core.common

enum class StretchPhase { WARM_UP, COOL_DOWN }

/**
 * A single named stretch/mobility move. [holdSeconds] drives the guided hold-timer cue
 * ("hold for 20 seconds… and switch sides") — see docs/foundation.md "Warm-up, cool-down,
 * and stretching". This is deliberately just a timer+voice guide, not a claim of sensed form
 * verification; a camera-assisted version is a distinct, explicitly-scoped later enhancement.
 */
data class StretchExercise(
    val id: String,
    val name: String,
    val phase: StretchPhase,
    val holdSeconds: Int,
    /** Most stretches alternate sides; a few (e.g. torso twists done standing) don't. */
    val perSide: Boolean = true,
)

val DefaultStretchRoutine: List<StretchExercise> = listOf(
    // Warm-up: dynamic, movement-based — never static holds before the muscle is warm.
    StretchExercise("leg_swings", "Leg swings", StretchPhase.WARM_UP, holdSeconds = 20),
    StretchExercise("walking_lunges", "Walking lunges", StretchPhase.WARM_UP, holdSeconds = 30, perSide = false),
    StretchExercise("high_knees", "High knees", StretchPhase.WARM_UP, holdSeconds = 20, perSide = false),

    // Cool-down: static holds — the muscle is warm now, this is where holding is appropriate.
    StretchExercise("calf_stretch", "Calf stretch", StretchPhase.COOL_DOWN, holdSeconds = 20),
    StretchExercise("quad_stretch", "Quad stretch", StretchPhase.COOL_DOWN, holdSeconds = 20),
    StretchExercise("hamstring_stretch", "Hamstring stretch", StretchPhase.COOL_DOWN, holdSeconds = 20),
)

fun stretchRoutineFor(phase: StretchPhase): List<StretchExercise> =
    DefaultStretchRoutine.filter { it.phase == phase }

/** Looks a stretch back up by its display name — used where only a [SessionStepEntity]'s
 * `label` survived the round trip through Room (e.g. cue generation in `RunSessionEngine`),
 * to recover [StretchExercise.perSide] without needing a dedicated database column for it. */
fun findStretchExercise(name: String): StretchExercise? =
    DefaultStretchRoutine.firstOrNull { it.name == name }
