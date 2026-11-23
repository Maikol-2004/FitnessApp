package com.maikol.fitnessapp.feature.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.navigation.SecondaryDestination
import com.maikol.fitnessapp.core.navigation.TopLevelDestination

/** Destino de una categoría: una raíz (cambia de pestaña) o un secundario de Inicio. */
sealed interface CategoryTarget {
    data class Root(val destination: TopLevelDestination) : CategoryTarget
    data class Secondary(val destination: SecondaryDestination) : CategoryTarget
}

/** Las seis categorías de Inicio, en el orden aprobado (docs/07_H1_H2_DELIVERY.md). */
enum class HomeCategory(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:DrawableRes val iconRes: Int,
    @param:DrawableRes val photoRes: Int,
    /** Encuadre por foto para que el sujeto quede visible a la derecha. */
    val photoAlignment: Alignment,
    val target: CategoryTarget,
) {
    TRAINING(
        R.string.category_training, R.string.category_training_desc, R.drawable.ic_dumbbell,
        R.drawable.h1_training, Alignment.CenterEnd, CategoryTarget.Root(TopLevelDestination.TRAINING),
    ),
    HEALTH(
        R.string.category_health, R.string.category_health_desc, R.drawable.ic_heart,
        R.drawable.h1_health, Alignment.CenterEnd, CategoryTarget.Root(TopLevelDestination.HEALTH),
    ),
    NUTRITION(
        R.string.category_nutrition, R.string.category_nutrition_desc, R.drawable.ic_restaurant,
        R.drawable.h1_nutrition, Alignment.CenterEnd, CategoryTarget.Secondary(SecondaryDestination.NUTRITION),
    ),
    HYDRATION(
        R.string.category_hydration, R.string.category_hydration_desc, R.drawable.ic_water_drop,
        R.drawable.h1_hydration, Alignment.CenterEnd, CategoryTarget.Secondary(SecondaryDestination.HYDRATION),
    ),
    SLEEP(
        R.string.category_sleep, R.string.category_sleep_desc, R.drawable.ic_moon,
        R.drawable.h1_sleep, Alignment.CenterEnd, CategoryTarget.Secondary(SecondaryDestination.SLEEP),
    ),
    PROGRESS(
        R.string.category_progress, R.string.category_progress_desc, R.drawable.ic_trending_up,
        R.drawable.h1_progress, Alignment.CenterEnd, CategoryTarget.Root(TopLevelDestination.PROGRESS),
    ),
}
