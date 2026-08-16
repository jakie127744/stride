package com.stride.app.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose routes (2.8+). One object per screen from the wireframes —
 * see the Stride Wireframes artifact for what each of these actually looks like. Onboarding,
 * PreRunEnvironment, ActiveRun, and RunSummary are real (a minimal but functional vertical
 * slice, ahead of their full Phase 4/5/6 scope); History, Insights, and LiveTrack are still
 * placeholders, reachable via Home's dev-only preview links until their phase lands.
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
