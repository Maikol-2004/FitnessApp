package com.maikol.fitnessapp.feature.training.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maikol.fitnessapp.core.designsystem.component.PhotoBackground
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.database.SessionStatus
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.LoadingRegion
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.component.SecondaryButton
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.motion.MotionTokens
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.feature.training.data.PerformedSet
import com.maikol.fitnessapp.feature.training.data.Session
import com.maikol.fitnessapp.feature.training.data.SessionExercise
import com.maikol.fitnessapp.feature.training.data.SessionTimer
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import com.maikol.fitnessapp.feature.training.data.formatDuration
import com.maikol.fitnessapp.feature.training.data.formatLoad
import com.maikol.fitnessapp.feature.training.data.formatSessionDate
import com.maikol.fitnessapp.feature.training.data.formatVolume
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SummaryUiState(val loading: Boolean = true, val session: Session? = null, val unit: UnitSystem = UnitSystem.METRIC)

class SummaryViewModel(
    savedState: SavedStateHandle,
    repository: TrainingRepository,
    preferences: UserPreferencesRepository,
) : ViewModel() {
    private val sessionId: String = checkNotNull(savedState[ARG_SESSION_ID])
    val celebrate: Boolean = savedState[ARG_CELEBRATE] ?: false

    val state: StateFlow<SummaryUiState> = combine(repository.observeSession(sessionId), preferences.userPreferences) { s, p ->
        SummaryUiState(loading = false, session = s?.takeIf { it.status == SessionStatus.COMPLETED }, unit = p.unitSystem)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryUiState())

    companion object {
        const val ARG_SESSION_ID = "sessionId"
        const val ARG_CELEBRATE = "celebrate"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApplication).container
                SummaryViewModel(createSavedStateHandle(), container.trainingRepository, container.userPreferencesRepository)
            }
        }
    }
}

@Composable
fun SummaryRoute(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onBackHome: () -> Unit,
    viewModel: SummaryViewModel = viewModel(factory = SummaryViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoadingRegion(isLoading = state.loading, message = stringResource(R.string.preparing_workout), modifier = Modifier.fillMaxSize()) {
        val session = state.session
        if (session == null) {
            Column(Modifier.fillMaxSize()) {
                TrainingTopBar(title = stringResource(R.string.summary_title), onBack = onBack)
                Text(stringResource(R.string.session_not_found), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(Spacing.l))
            }
            return@LoadingRegion
        }
        SummaryScreen(session, state.unit, viewModel.celebrate, onBack, onOpenHistory, onBackHome)
    }
}

@Composable
fun SummaryScreen(
    session: Session,
    unit: UnitSystem,
    celebrate: Boolean,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Cabecera de logro: fotografía a sangre, medalla con halo champán y el nombre de la rutina.
            PhotoBackground(
                photo = R.drawable.training_gym_hero,
                photoAlignment = Alignment.Center,
                horizontalScrimEnd = 0f,
                bottomScrimAlpha = 1f,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.45f)))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = Spacing.xs)
                        .padding(top = Spacing.xs, bottom = Spacing.xl),
                ) {
                    Row(Modifier.fillMaxWidth()) { BackButton(onBack) }
                    Medallion(celebrate)
                    Spacer(Modifier.height(Spacing.m))
                    Text(
                        stringResource(R.string.summary_completed_title),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 36.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = Spacing.l).semantics { heading() },
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        session.routineName + (session.endedAt?.let { " · " + formatSessionDate(it) } ?: ""),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = Spacing.l),
                    )
                }
            }
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
            modifier = Modifier
                .padding(horizontal = Spacing.l)
                .padding(top = Spacing.s, bottom = Spacing.xl),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                StatTile(stringResource(R.string.summary_duration), formatDuration(session.timer.activeMs), Modifier.weight(1f))
                StatTile(stringResource(R.string.summary_volume), formatVolume(session.volumeKg, unit), Modifier.weight(1f), highlight = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                val done = session.exercises.count { e -> e.sets.any { it.completed } }
                StatTile(stringResource(R.string.summary_exercises), "$done", Modifier.weight(1f))
                StatTile(stringResource(R.string.summary_sets), "${session.completedSets}", Modifier.weight(1f))
            }
            Text(
                stringResource(R.string.summary_volume_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SectionHeader(stringResource(R.string.summary_logged_title), Modifier.padding(top = Spacing.s))
            session.exercises.forEach { SummaryExerciseCard(it, unit) }
        }
        }
        StatusBarScrim()
        }
        BottomActionBar {
            SecondaryButton(text = stringResource(R.string.summary_view_history), onClick = onOpenHistory, iconRes = R.drawable.ic_history, modifier = Modifier.weight(1f))
            PrimaryButton(text = stringResource(R.string.summary_back_home), onClick = onBackHome, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Medalla de cierre (M09): el aro se cierra 0→100 % en 280 ms y el check aparece en 150 ms, solo la primera
 * vez que se llega tras finalizar. Desde el historial, al rotar o con movimiento reducido es estática.
 * Los datos ya están en pantalla: nunca esperan a la animación.
 */
@Composable
private fun Medallion(celebrate: Boolean) {
    val motion = LocalMotionEnabled.current
    var played by rememberSaveable { mutableStateOf(false) }
    val animate = celebrate && motion && !played
    val ring = remember { Animatable(if (animate) 0f else 1f) }
    val check = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!animate) return@LaunchedEffect
        played = true
        ring.animateTo(1f, tween(280, easing = MotionTokens.EaseOut))
        check.animateTo(1f, tween(150))
    }
    val primary = MaterialTheme.colorScheme.primary
    val track = FitnessThemeExtras.colors.surfaceRaised
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(168.dp)
            .background(Brush.radialGradient(listOf(champagneTint(0.28f), Color.Transparent)), CircleShape)
            .clearAndSetSemantics {},
    ) {
        Canvas(Modifier.size(112.dp)) {
            val w = 6.dp.toPx()
            val arcSize = Size(size.width - w, size.height - w)
            val topLeft = Offset(w / 2, w / 2)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(w))
            drawArc(primary, -90f, 360f * ring.value, false, topLeft, arcSize, style = Stroke(w, cap = StrokeCap.Round))
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer { alpha = check.value }
                .background(champagneTint(0.16f), CircleShape),
        ) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = primary, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun SummaryExerciseCard(exercise: SessionExercise, unit: UnitSystem) {
    val done = exercise.sets.filter { it.completed }
    FitnessCard {
        Column(Modifier.semantics(mergeDescendants = true) {}) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.s))
            if (done.isEmpty()) {
                Text(stringResource(R.string.summary_no_sets), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            done.forEachIndexed { i, set -> SummarySetLine(i + 1, set, unit) }
        }
    }
}

@Composable
private fun SummarySetLine(number: Int, set: PerformedSet, unit: UnitSystem) {
    val reps = set.reps ?: 0
    val load = formatLoad(set.loadKg, unit)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp).background(FitnessThemeExtras.colors.surfaceRaised, CircleShape),
        ) {
            Text(
                if (set.warmup) stringResource(R.string.editor_warmup_short) else "$number",
                style = MaterialTheme.typography.labelMedium,
                color = if (set.warmup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.width(Spacing.m))
        Text(
            if (load != null) stringResource(R.string.summary_set_line, load, reps) else stringResource(R.string.summary_set_bodyweight, reps),
            style = NumberStyleCell.copy(textAlign = TextAlign.Start),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (set.warmup) {
            Spacer(Modifier.width(Spacing.s))
            Pill(stringResource(R.string.summary_warmup), filled = false)
        }
    }
}

@Preview(name = "Resumen DEMO", showBackground = true, backgroundColor = 0xFF121416, widthDp = 393, heightDp = 1000)
@Composable
private fun SummaryPreview() {
    val sets = listOf(
        PerformedSet("a", 0, 10, 20.0, 90, false, 10, 20.0, true),
        PerformedSet("b", 1, 10, 20.0, 90, false, 8, 20.0, true),
    )
    val session = Session(
        "s", "r", "Tren superior DEMO", SessionStatus.COMPLETED, 0, 3_600_000,
        SessionTimer(2_710_000, null, null, null, null), 0,
        listOf(SessionExercise("e", "k", "Press con mancuernas", "", sets)),
    )
    FitnessTheme { SummaryScreen(session, UnitSystem.METRIC, celebrate = false, {}, {}, {}) }
}
