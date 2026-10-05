package com.fourdfit.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fourdfit.app.ui.home.HomeScreen
import com.fourdfit.app.ui.horoscope.HoroscopeScreen
import com.fourdfit.app.ui.nutrition.NutritionScreen
import com.fourdfit.app.ui.profile.EditProfileScreen
import com.fourdfit.app.ui.profile.ProfileScreen
import com.fourdfit.app.ui.progress.ProgressScreen
import com.fourdfit.app.ui.settings.LegalScreen
import com.fourdfit.app.ui.settings.NotificationSettingsScreen
import com.fourdfit.app.ui.settings.SettingsScreen
import com.fourdfit.app.ui.theme.FourD
import com.fourdfit.app.ui.workout.ExerciseDetailScreen
import com.fourdfit.app.ui.workout.ProgramDetailScreen
import com.fourdfit.app.ui.workout.WorkoutLibraryScreen
import com.fourdfit.app.ui.workout.WorkoutPlayerScreen

/**
 * Single NavHost for the signed-in app. Screens move with a depth transition (scale + fade):
 * forward navigation pushes the old screen "into" the glass, back pulls it out again.
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val topLevel = TopLevelDestination.entries.firstOrNull { it.route == currentRoute }
    val reduce = FourD.reduceMotion

    val enter: EnterTransition = if (reduce) fadeIn(tween(180)) else fadeIn(tween(320)) + scaleIn(tween(360), initialScale = 0.94f)
    val exit: ExitTransition = if (reduce) fadeOut(tween(140)) else fadeOut(tween(220)) + scaleOut(tween(260), targetScale = 1.05f)
    val popEnter: EnterTransition = if (reduce) fadeIn(tween(180)) else fadeIn(tween(320)) + scaleIn(tween(360), initialScale = 1.05f)
    val popExit: ExitTransition = if (reduce) fadeOut(tween(140)) else fadeOut(tween(220)) + scaleOut(tween(260), targetScale = 0.94f)

    val back: () -> Unit = { navController.popBackStack() }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { enter },
            exitTransition = { exit },
            popEnterTransition = { popEnter },
            popExitTransition = { popExit },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenProgram = { navController.navigate(Routes.program(it)) },
                    onStartProgram = { navController.navigate(Routes.player(Routes.KIND_PROGRAM, it)) },
                    onOpenProgress = { navController.navigate(Routes.PROGRESS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenProfile = { navigateTopLevel(navController, TopLevelDestination.PROFILE) },
                    onOpenNutrition = { navigateTopLevel(navController, TopLevelDestination.NUTRITION) },
                )
            }
            composable(Routes.WORKOUT) {
                WorkoutLibraryScreen(
                    onOpenExercise = { navController.navigate(Routes.exercise(it)) },
                    onOpenProgram = { navController.navigate(Routes.program(it)) },
                )
            }
            composable(Routes.NUTRITION) { NutritionScreen() }
            composable(Routes.HOROSCOPE) { HoroscopeScreen() }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                    onOpenProgress = { navController.navigate(Routes.PROGRESS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(
                Routes.PROGRAM,
                arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString(Routes.ARG_ID).orEmpty()
                ProgramDetailScreen(
                    programId = id,
                    onBack = back,
                    onOpenExercise = { navController.navigate(Routes.exercise(it)) },
                    onStart = { navController.navigate(Routes.player(Routes.KIND_PROGRAM, id)) },
                )
            }
            composable(
                Routes.EXERCISE,
                arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString(Routes.ARG_ID).orEmpty()
                ExerciseDetailScreen(
                    exerciseId = id,
                    onBack = back,
                    onStart = { navController.navigate(Routes.player(Routes.KIND_EXERCISE, id)) },
                )
            }
            composable(
                Routes.PLAYER,
                arguments =
                    listOf(
                        navArgument(Routes.ARG_KIND) { type = NavType.StringType },
                        navArgument(Routes.ARG_ID) { type = NavType.StringType },
                    ),
            ) { entry ->
                WorkoutPlayerScreen(
                    kind = entry.arguments?.getString(Routes.ARG_KIND).orEmpty(),
                    id = entry.arguments?.getString(Routes.ARG_ID).orEmpty(),
                    onClose = back,
                )
            }
            composable(Routes.PROGRESS) { ProgressScreen(onBack = back) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = back,
                    onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenLegal = { navController.navigate(Routes.legal(it)) },
                )
            }
            composable(Routes.EDIT_PROFILE) { EditProfileScreen(onBack = back) }
            composable(Routes.NOTIFICATIONS) { NotificationSettingsScreen(onBack = back) }
            composable(
                Routes.LEGAL,
                arguments = listOf(navArgument(Routes.ARG_DOC) { type = NavType.StringType }),
            ) { entry ->
                LegalScreen(doc = entry.arguments?.getString(Routes.ARG_DOC).orEmpty(), onBack = back)
            }
        }

        AnimatedVisibility(
            visible = topLevel != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(320)) { it } + fadeIn(),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(),
        ) {
            FourDBottomBar(
                current = topLevel,
                onSelect = { navigateTopLevel(navController, it) },
            )
        }
    }
}

private fun navigateTopLevel(
    navController: NavController,
    destination: TopLevelDestination,
) {
    navController.navigate(destination.route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
