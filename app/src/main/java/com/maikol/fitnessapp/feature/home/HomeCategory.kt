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
    /** Encuadre: las fotos v2 ya llevan el sujeto completo en el tercio derecho (design/polish-v2). */
    val photoAlignment: Alignment,
    val target: CategoryTarget,
) {
    TRAINING(
        R.string.category_training, R.string.category_training_desc, R.drawable.ic_dumbbell,
        R.drawable.category_training_right_v2, Alignment.Center, CategoryTarget.Root(TopLevelDestination.TRAINING),
    ),
    HEALTH(
        R.string.category_health, R.string.category_health_desc, R.drawable.ic_heart,
        R.drawable.category_health_right_v2, Alignment.Center, CategoryTarget.Root(TopLevelDestination.HEALTH),
    ),
    NUTRITION(
        R.string.category_nutrition, R.string.category_nutrition_desc, R.drawable.ic_restaurant,
        R.drawable.category_nutrition_right_v2, Alignment.Center, CategoryTarget.Secondary(SecondaryDestination.NUTRITION),
    ),
    HYDRATION(
        R.string.category_hydration, R.string.category_hydration_desc, R.drawable.ic_water_drop,
        R.drawable.category_hydration_right_v2, Alignment.Center, CategoryTarget.Secondary(SecondaryDestination.HYDRATION),
    ),
    SLEEP(
        R.string.category_sleep, R.string.category_sleep_desc, R.drawable.ic_moon,
        R.drawable.category_sleep_right_v2, Alignment.Center, CategoryTarget.Secondary(SecondaryDestination.SLEEP),
    ),
    PROGRESS(
        R.string.category_progress, R.string.category_progress_desc, R.drawable.ic_trending_up,
        R.drawable.category_progress_right_v2, Alignment.Center, CategoryTarget.Root(TopLevelDestination.PROGRESS),
    ),
}
