package com.maikol.fitnessapp.core.navigation

import androidx.annotation.StringRes
import com.maikol.fitnessapp.R

/**
 * Contrato de los cinco destinos raíz aprobados (docs/02_DESIGN_UX.md), en este orden.
 * No añadir ni renombrar destinos sin decisión explícita. La barra inferior se construye en H1.
 */
enum class TopLevelDestination(val route: String, @param:StringRes val labelRes: Int) {
    HOME("home", R.string.destination_home),
    TRAINING("training", R.string.destination_training),
    PROGRESS("progress", R.string.destination_progress),
    HEALTH("health", R.string.destination_health),
    PROFILE("profile", R.string.destination_profile),
}
