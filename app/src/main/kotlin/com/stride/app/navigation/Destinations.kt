package com.stride.app.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose routes (2.8+). One object per screen from the wireframes —
 * see the Stride Wireframes artifact for what each of these actually looks like. Real screens
 * move into their :feature:* module as each ships; these placeholders exist so the nav graph
 * and DI wiring can be proven end-to-end in Phase 3 rather than left unverified.
 */
sealed interface Destination {
    @Serializable
    data object Home : Destination

    @Serializable
    data object Onboarding : Destination

    @Serializable
    data object PreRunEnvironment : Destination

    @Serializable
    data class ActiveRun(val planSessionId: Long?) : Destination

    @Serializable
    data class RunSummary(val runId: Long) : Destination

    @Serializable
    data object History : Destination

    @Serializable
    data object Insights : Destination

    @Serializable
    data object LiveTrack : Destination
}
