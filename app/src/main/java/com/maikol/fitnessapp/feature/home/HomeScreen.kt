package com.maikol.fitnessapp.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.LoadingRegion
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.motion.MotionTokens
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.designsystem.theme.Spacing

private val MinTwoColumnCardWidth = 152.dp

@Composable
fun HomeRoute(
    onOpenCategory: (HomeCategory) -> Unit,
    onOpenHealth: () -> Unit,
    onOpenTraining: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoadingRegion(
        isLoading = uiState is HomeUiState.Loading,
        message = stringResource(R.string.loading_data),
        modifier = Modifier.fillMaxSize(),
    ) {
        val ready = uiState as? HomeUiState.Ready ?: return@LoadingRegion
        HomeScreen(
            displayName = ready.displayName,
            activity = null,
            onOpenCategory = onOpenCategory,
            onOpenHealth = onOpenHealth,
            onOpenTraining = onOpenTraining,
            onOpenProfile = onOpenProfile,
        )
    }
}

@Composable
fun HomeScreen(
    displayName: String?,
    activity: ActivityMetrics?,
    onOpenCategory: (HomeCategory) -> Unit,
    onOpenHealth: () -> Unit,
    onOpenTraining: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    demoLabel: Boolean = false,
) {
    val entrance = rememberHomeFirstEntrance()
    val offsetPx = with(LocalDensity.current) { 8.dp.toPx() }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Dos columnas si cada tarjeta deja sitio al título más largo («Entrenamiento») sin partirlo;
        // si el ancho o la escala de fuente no lo permiten, una columna en el mismo orden.
        val fontScale = LocalDensity.current.fontScale
        val cardWidth = (maxWidth - Spacing.screenHorizontal * 2 - Spacing.cardGap) / 2
        val compact = cardWidth < MinTwoColumnCardWidth * fontScale
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Inset fuera del scroll: el contenido no pasa por debajo de la barra de estado.
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(top = Spacing.l, bottom = Spacing.xl)
                .graphicsLayer {
                    alpha = entrance.value
                    translationY = (1f - entrance.value) * offsetPx
                },
        ) {
            HomeHeader(displayName, demoLabel, onOpenProfile)
            Spacer(Modifier.height(Spacing.xl))
            ActivitySummaryCard(metrics = activity, onViewHealth = onOpenHealth, compact = compact)
            Spacer(Modifier.height(Spacing.xl))
            CategoryGrid(singleColumn = compact, onOpenCategory = onOpenCategory)
            Spacer(Modifier.height(Spacing.cardGap))
            NextWorkoutCard(onViewTraining = onOpenTraining)
        }
    }
}

/**
 * M02: la primera vez que se compone Inicio entra con alpha 180 ms y 8 dp de desplazamiento.
 * El indicador se guarda en el estado de la pestaña: volver a Inicio, hacer scroll o recomponer no
 * lo relanza. Con movimiento reducido el contenido aparece sin desplazamiento.
 */
@Composable
private fun rememberHomeFirstEntrance(): Animatable<Float, *> {
    var played by rememberSaveable { mutableStateOf(false) }
    val motionEnabled = LocalMotionEnabled.current
    val progress = remember { Animatable(if (played || !motionEnabled) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            played = true
            if (progress.value < 1f) {
                progress.animateTo(1f, tween(MotionTokens.HOME_FIRST_ENTER_MS, easing = MotionTokens.EaseOut))
            }
        }
    }
    return progress
}

@Composable
private fun HomeHeader(displayName: String?, demoLabel: Boolean, onOpenProfile: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (displayName == null) stringResource(R.string.home_greeting)
                    else stringResource(R.string.home_greeting_named, displayName),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .semantics { heading() },
                )
                if (demoLabel) DemoBadge(Modifier.padding(start = Spacing.s))
            }
            Text(
                stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.padding(start = Spacing.m))
        ProfileAvatar(displayName = displayName, onClick = onOpenProfile)
    }
}

@Composable
private fun CategoryGrid(singleColumn: Boolean, onOpenCategory: (HomeCategory) -> Unit) {
    val categories = HomeCategory.entries
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
        if (singleColumn) {
            categories.forEach { category ->
                CategoryPhotoCard(
                    category = category,
                    onClick = { onOpenCategory(category) },
                    descriptionWidthFraction = 0.7f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            categories.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap),
                ) {
                    pair.forEach { category ->
                        CategoryPhotoCard(
                            category = category,
                            onClick = { onOpenCategory(category) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

/** Etiqueta para previews y capturas con datos sintéticos. No se usa en la app real. */
@Composable
private fun DemoBadge(modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            "DEMO",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = 2.dp),
        )
    }
}

@Preview(name = "Inicio vacío", showBackground = true, backgroundColor = 0xFF121416, widthDp = 393, heightDp = 1500)
@Composable
private fun HomeEmptyPreview() {
    FitnessTheme {
        HomeScreen(null, null, {}, {}, {}, {})
    }
}

@Preview(name = "Inicio DEMO", showBackground = true, backgroundColor = 0xFF121416, widthDp = 393, heightDp = 1500)
@Composable
private fun HomeDemoPreview() {
    FitnessTheme {
        HomeScreen(
            displayName = "Demo",
            activity = ActivityMetrics(steps = 6240, stepGoal = 8000, distanceKm = 4.2, activeKcal = 320, activeMinutes = 28),
            onOpenCategory = {}, onOpenHealth = {}, onOpenTraining = {}, onOpenProfile = {},
            demoLabel = true,
        )
    }
}

@Preview(name = "Inicio fuente 200 %", fontScale = 2f, showBackground = true, backgroundColor = 0xFF121416, widthDp = 360, heightDp = 3200)
@Composable
private fun HomeLargeFontPreview() {
    FitnessTheme {
        HomeScreen(null, null, {}, {}, {}, {})
    }
}
