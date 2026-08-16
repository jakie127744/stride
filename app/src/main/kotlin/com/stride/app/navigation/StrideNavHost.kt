package com.stride.app.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.stride.app.ui.ActiveRunPlaceholderScreen
import com.stride.app.ui.HistoryPlaceholderScreen
import com.stride.app.ui.HomeScreen
import com.stride.app.ui.InsightsPlaceholderScreen
import com.stride.app.ui.LiveTrackPlaceholderScreen
import com.stride.app.ui.OnboardingPlaceholderScreen
import com.stride.app.ui.PreRunEnvironmentPlaceholderScreen
import com.stride.app.ui.RunSummaryPlaceholderScreen
import com.stride.core.designsystem.LocalReducedMotion
import com.stride.core.designsystem.StrideMotion

@Composable
fun StrideNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: Destination = Destination.Home,
) {
    val reducedMotion = LocalReducedMotion.current

    val enter: () -> EnterTransition = {
        if (reducedMotion) {
            fadeIn(tween(StrideMotion.DURATION_SHORT))
        } else {
            fadeIn(tween(StrideMotion.DURATION_MEDIUM, easing = StrideMotion.StandardEasing)) +
                slideInHorizontally(
                    animationSpec = tween(StrideMotion.DURATION_MEDIUM, easing = StrideMotion.EmphasizedEasing),
                    initialOffsetX = { it / 6 },
                )
        }
    }
    val exit: () -> ExitTransition = {
        if (reducedMotion) {
            fadeOut(tween(StrideMotion.DURATION_SHORT))
        } else {
            fadeOut(tween(StrideMotion.DURATION_SHORT, easing = StrideMotion.StandardEasing))
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { enter() },
        exitTransition = { exit() },
        popEnterTransition = { enter() },
        popExitTransition = { exit() },
    ) {
        composable<Destination.Home> {
            HomeScreen(
                onStartSession = { navController.navigate(Destination.PreRunEnvironment) },
                onOpenHistory = { navController.navigate(Destination.History) },
                onOpenInsights = { navController.navigate(Destination.Insights) },
            )
        }
        composable<Destination.Onboarding> { OnboardingPlaceholderScreen() }
        composable<Destination.PreRunEnvironment> {
            PreRunEnvironmentPlaceholderScreen(
                onContinue = { navController.navigate(Destination.ActiveRun(planSessionId = null)) },
            )
        }
        composable<Destination.ActiveRun> { backStackEntry ->
            val route: Destination.ActiveRun = backStackEntry.toRoute()
            ActiveRunPlaceholderScreen(planSessionId = route.planSessionId)
        }
        composable<Destination.RunSummary> { backStackEntry ->
            val route: Destination.RunSummary = backStackEntry.toRoute()
            RunSummaryPlaceholderScreen(runId = route.runId)
        }
        composable<Destination.History> { HistoryPlaceholderScreen() }
        composable<Destination.Insights> { InsightsPlaceholderScreen() }
        composable<Destination.LiveTrack> { LiveTrackPlaceholderScreen() }
    }
}
