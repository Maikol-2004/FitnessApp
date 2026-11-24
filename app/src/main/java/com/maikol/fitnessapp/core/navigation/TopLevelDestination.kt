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

/** Rutas del flujo de Entrenar (H2). Todas pertenecen a la pestaña Entrenar. */
object TrainingRoutes {
    const val EDITOR = "routine-editor?routineId={routineId}"
    const val DETAIL = "routine/{routineId}"
    const val SESSION = "session/{sessionId}"
    const val SUMMARY = "summary/{sessionId}?celebrate={celebrate}"
    const val HISTORY = "history"

    fun editor(routineId: String? = null) = if (routineId == null) "routine-editor" else "routine-editor?routineId=$routineId"
    fun detail(routineId: String) = "routine/$routineId"
    fun session(sessionId: String) = "session/$sessionId"
    fun summary(sessionId: String, celebrate: Boolean = false) = "summary/$sessionId?celebrate=$celebrate"

    private val all = setOf(EDITOR, DETAIL, SESSION, SUMMARY, HISTORY)

    /** Flujos de concentración: sin barra inferior (editor, detalle, sesión y resumen). */
    private val immersive = setOf(EDITOR, DETAIL, SESSION, SUMMARY)

    fun owns(route: String?) = route in all
    fun isImmersive(route: String?) = route in immersive
}

/** Pestaña a la que pertenece cualquier ruta; los secundarios de Inicio y el resto, a Inicio. */
fun tabOf(route: String?): TopLevelDestination =
    TopLevelDestination.fromRoute(route) ?: if (TrainingRoutes.owns(route)) TopLevelDestination.TRAINING else TopLevelDestination.HOME
