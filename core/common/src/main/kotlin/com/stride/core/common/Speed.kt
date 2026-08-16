package com.stride.core.common

import kotlin.math.roundToInt

/**
 * Canonical unit for any stored/computed speed is meters/second — every display unit (mph,
 * min/km pace, min/mile pace) is a conversion at the UI edge, never a second source of truth.
 * Keeps unit bugs from creeping in when a value crosses the GPS → Room → UI boundary.
 */
private const val METERS_PER_MILE = 1609.344
private const val SECONDS_PER_HOUR = 3600.0

fun Double.mphToMetersPerSecond(): Double = (this * METERS_PER_MILE) / SECONDS_PER_HOUR

fun Double.metersPerSecondToMph(): Double = (this * SECONDS_PER_HOUR) / METERS_PER_MILE

/** Whole seconds to cover one kilometer at this speed — the "6:42 /km" the run screen shows.
 * Rounds rather than truncates — a value that's algebraically exactly 600 can land at
 * 599.9999… after floating-point division, and truncation would display a wrong pace. */
fun Double.metersPerSecondToPaceSecondsPerKm(): Int? =
    if (this <= 0.0) null else (1000.0 / this).roundToInt()

fun Double.metersPerSecondToPaceSecondsPerMile(): Int? =
    if (this <= 0.0) null else (METERS_PER_MILE / this).roundToInt()
