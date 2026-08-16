package com.stride.app.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose routes (2.8+). One object per screen from the wireframes —
 * see the Stride Wireframes artifact for what each of these actually looks like. Onboarding,
 * PreRunEnvironment, ActiveRun, RunSummary, History, Insights, and LiveTrack are all real now
 * (see docs/roadmap.md's "pulled forward" vertical slice) — ahead of their full Phase 4/5/6
 * scope, but reading and writing actual Room data, not placeholders. Only ActiveRun/RunSummary
 * still lean on Home's dev-only preview links for a way in without a real scheduled session.
 */
sealed interface Destination {
    @Serializable
    data object Home : Destination

    @Serializable
    data object Onboarding : Destination

    @Serializable
    data class PreRunEnvironment(val planSessionId: Long?) : Destination

    @Serializable
    data class ActiveRun(val planSessionId: Long?, val outdoor: Boolean = true, val shoeId: Long? = null) : Destination

    @Serializable
    data class RunSummary(val runId: Long) : Destination

    @Serializable
    data object History : Destination

    @Serializable
    data object Insights : Destination

    @Serializable
    data object LiveTrack : Destination
}
