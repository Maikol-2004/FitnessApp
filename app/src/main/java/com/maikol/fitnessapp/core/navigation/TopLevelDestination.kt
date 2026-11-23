package com.maikol.fitnessapp.core.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.maikol.fitnessapp.R

/**
 * Contrato de los cinco destinos raíz aprobados (docs/02_DESIGN_UX.md), en este orden.
 * No añadir ni renombrar destinos sin decisión explícita.
 */
enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    HOME("home", R.string.destination_home, R.drawable.ic_home),
    TRAINING("training", R.string.destination_training, R.drawable.ic_dumbbell),
    PROGRESS("progress", R.string.destination_progress, R.drawable.ic_bar_chart),
    HEALTH("health", R.string.destination_health, R.drawable.ic_heart),
    PROFILE("profile", R.string.destination_profile, R.drawable.ic_person),
    ;

    companion object {
        fun fromRoute(route: String?): TopLevelDestination? = entries.firstOrNull { it.route == route }
    }
}

/** Destinos secundarios: se abren desde Inicio y vuelven a él con Atrás. */
enum class SecondaryDestination(val route: String) {
    NUTRITION("nutrition"),
    HYDRATION("hydration"),
    SLEEP("sleep"),
}
