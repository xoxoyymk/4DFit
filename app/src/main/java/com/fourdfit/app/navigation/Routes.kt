package com.fourdfit.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val WORKOUT = "workout"
    const val NUTRITION = "nutrition"
    const val HOROSCOPE = "horoscope"
    const val PROFILE = "profile"

    const val EXERCISE = "exercise/{id}"
    const val PROGRAM = "program/{id}"
    const val PLAYER = "player/{kind}/{id}"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val EDIT_PROFILE = "edit_profile"
    const val NOTIFICATIONS = "notifications"
    const val LEGAL = "legal/{doc}"

    const val ARG_ID = "id"
    const val ARG_KIND = "kind"
    const val ARG_DOC = "doc"

    const val KIND_PROGRAM = "program"
    const val KIND_EXERCISE = "exercise"

    fun exercise(id: String) = "exercise/$id"

    fun program(id: String) = "program/$id"

    fun player(
        kind: String,
        id: String,
    ) = "player/$kind/$id"

    fun legal(doc: String) = "legal/$doc"
}

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    WORKOUT(Routes.WORKOUT, "Workout", Icons.Rounded.FitnessCenter, Icons.Outlined.FitnessCenter),
    NUTRITION(Routes.NUTRITION, "Nutrition", Icons.Rounded.Restaurant, Icons.Outlined.Restaurant),
    HOROSCOPE(Routes.HOROSCOPE, "Horoscope", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
    PROFILE(Routes.PROFILE, "Profile", Icons.Rounded.Person, Icons.Outlined.Person),
}
