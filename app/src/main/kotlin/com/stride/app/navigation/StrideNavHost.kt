package com.stride.app.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.stride.app.ui.ActiveRunScreen
import com.stride.app.ui.HistoryScreen
import com.stride.app.ui.HomeScreen
import com.stride.app.ui.InsightsScreen
import com.stride.app.ui.LiveTrackPlaceholderScreen
import com.stride.app.ui.OnboardingScreen
import com.stride.app.ui.PreRunEnvironmentScreen
import com.stride.app.ui.RunSummaryScreen
import com.stride.core.common.RunEnvironment
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
                onGetStarted = { navController.navigate(Destination.Onboarding) },
                onStartSession = { planSessionId ->
                    navController.navigate(Destination.PreRunEnvironment(planSessionId))
                },
                onOpenHistory = { navController.navigate(Destination.History) },
                onOpenInsights = { navController.navigate(Destination.Insights) },
                onPreviewDestination = { route ->
                    val destination: Destination? = when (route) {
                        "run" -> Destination.ActiveRun(planSessionId = null)
                        "summary" -> Destination.RunSummary(runId = 0)
                        "livetrack" -> Destination.LiveTrack
                        else -> null
                    }
                    destination?.let(navController::navigate)
                },
            )
        }
        composable<Destination.Onboarding> {
            OnboardingScreen(
                onTrackSelected = { planSessionId ->
                    navController.navigate(Destination.PreRunEnvironment(planSessionId)) {
                        popUpTo<Destination.Home>()
                    }
                },
            )
        }
        composable<Destination.PreRunEnvironment> { backStackEntry ->
            val route: Destination.PreRunEnvironment = backStackEntry.toRoute()
            PreRunEnvironmentScreen(
                planSessionId = route.planSessionId,
                onContinue = { environment ->
                    navController.navigate(
                        Destination.ActiveRun(
                            planSessionId = route.planSessionId,
                            outdoor = environment == RunEnvironment.OUTDOOR,
                        ),
                    )
                },
            )
        }
        composable<Destination.ActiveRun> {
            ActiveRunScreen(
                onFinished = { runId ->
                    navController.navigate(Destination.RunSummary(runId)) {
                        popUpTo<Destination.Home>()
                    }
                },
            )
        }
        composable<Destination.RunSummary> {
            RunSummaryScreen(
                onDone = {
                    navController.navigate(Destination.Home) {
                        popUpTo<Destination.Home> { inclusive = true }
                    }
                },
            )
        }
        composable<Destination.History> { HistoryScreen() }
        composable<Destination.Insights> { InsightsScreen() }
        composable<Destination.LiveTrack> { LiveTrackPlaceholderScreen() }
    }
}
