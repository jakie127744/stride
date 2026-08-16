package com.stride.core.common

/**
 * Asked at the start of every session (see docs/foundation.md — "Environment: outdoor vs.
 * treadmill"). Drives whether GPS + weather are used at all: OUTDOOR pulls both, TREADMILL
 * pulls neither and falls back to belt-speed/manual pace input.
 */
enum class RunEnvironment {
    OUTDOOR,
    TREADMILL,
}
