package com.maikol.fitnessapp.feature.upcoming

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.PhotoBackground
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.designsystem.theme.Spacing

/** Secciones cuya función llega en una fase posterior: imagen, título y explicación concreta. */
enum class UpcomingFeature(
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
    @param:DrawableRes val iconRes: Int,
    @param:DrawableRes val photoRes: Int,
) {
    TRAINING(R.string.destination_training, R.string.upcoming_training_body, R.drawable.ic_dumbbell, R.drawable.training_gym_hero),
    PROGRESS(R.string.destination_progress, R.string.upcoming_progress_body, R.drawable.ic_trending_up, R.drawable.h1_progress),
    HEALTH(R.string.destination_health, R.string.upcoming_health_body, R.drawable.ic_heart, R.drawable.h1_health),
    NUTRITION(R.string.category_nutrition, R.string.upcoming_nutrition_body, R.drawable.ic_restaurant, R.drawable.h1_nutrition),
    HYDRATION(R.string.category_hydration, R.string.upcoming_hydration_body, R.drawable.ic_water_drop, R.drawable.h1_hydration),
    SLEEP(R.string.category_sleep, R.string.upcoming_sleep_body, R.drawable.ic_moon, R.drawable.h1_sleep),
}

/**
 * Estado final de disponibilidad. [onBack]: flecha Atrás (destinos secundarios).
 * [onBackHome]: único botón permitido en H1 (Entrenar). No hay acciones Guardar/Registrar/Crear.
 */
@Composable
fun UpcomingFeatureScreen(
    feature: UpcomingFeature,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onBackHome: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        PhotoBackground(
            photo = feature.photoRes,
            photoAlignment = Alignment.CenterEnd,
            horizontalScrimEnd = 1f,
            bottomScrimAlpha = 0.8f,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp),
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = Spacing.s, vertical = Spacing.s),
            ) {
                Box(Modifier.size(48.dp)) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xxl + Spacing.xl))
                Text(
                    stringResource(feature.titleRes),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .padding(horizontal = Spacing.s)
                        .padding(bottom = Spacing.s)
                        .semantics { heading() },
                )
            }
        }
        Column(Modifier.padding(Spacing.screenHorizontal)) {
            FitnessCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(feature.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(Spacing.s))
                    Text(
                        stringResource(R.string.upcoming_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(Spacing.s))
                Text(
                    stringResource(feature.bodyRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onBackHome != null) {
                PrimaryButton(
                    text = stringResource(R.string.upcoming_back_home),
                    onClick = onBackHome,
                    modifier = Modifier.padding(top = Spacing.l),
                )
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
    // Protección fija de la barra de estado: el texto desplazado no se cruza con los iconos del sistema.
    Box(
        Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
    )
    }
}

@Preview(name = "Próximamente · Salud", showBackground = true, backgroundColor = 0xFF121416, widthDp = 393, heightDp = 760)
@Composable
private fun UpcomingHealthPreview() {
    FitnessTheme { UpcomingFeatureScreen(UpcomingFeature.HEALTH) }
}

@Preview(name = "Próximamente · Nutrición (secundario)", showBackground = true, backgroundColor = 0xFF121416, widthDp = 393, heightDp = 760)
@Composable
private fun UpcomingNutritionPreview() {
    FitnessTheme { UpcomingFeatureScreen(UpcomingFeature.NUTRITION, onBack = {}) }
}
