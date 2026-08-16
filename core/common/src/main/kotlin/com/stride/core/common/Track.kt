package com.stride.core.common

/**
 * Which track a plan/session belongs to. A runner can switch tracks at any time — this is
 * stored per-plan, not per-account, so switching never discards history. See docs/foundation.md
 * ("Core value proposition" — one continuous journey, not two apps).
 */
enum class Track {
    BEGINNER,
    PRO,
}
