package com.maikol.fitnessapp.feature.training.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maikol.fitnessapp.FitnessApplication
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.component.SecondaryButton
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.core.preferences.UserPreferencesRepository
import com.maikol.fitnessapp.feature.training.catalog.CatalogExercise
import com.maikol.fitnessapp.feature.training.catalog.ExerciseMedia
import com.maikol.fitnessapp.feature.training.catalog.LoadSemantics
import com.maikol.fitnessapp.feature.training.catalog.loadSemantics
import com.maikol.fitnessapp.feature.training.catalog.mediaAssets
import com.maikol.fitnessapp.feature.training.catalog.usesDumbbells
import com.maikol.fitnessapp.feature.training.data.ExerciseProgress
import com.maikol.fitnessapp.feature.training.data.ExerciseSessionEntry
import com.maikol.fitnessapp.feature.training.data.ProgressPeriod
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import com.maikol.fitnessapp.feature.training.data.exerciseProgress
import com.maikol.fitnessapp.feature.training.data.formatLoad
import com.maikol.fitnessapp.feature.training.data.formatSessionDate
import com.maikol.fitnessapp.feature.training.data.formatVolume
import com.maikol.fitnessapp.feature.training.data.kgToDisplay
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * G2: ficha del ejercicio con Guía, Progreso e Historial. Capa a pantalla completa (editor, selector y sesión):
 * no añade rutas ni toca el temporizador de la sesión. La secuencia de posiciones es honesta: tres
 * ilustraciones fijas con avance manual y bucle opcional; nunca se presenta como vídeo.
 */

/** Acción de la ficha cuando se abre desde el selector. */
internal data class GuideSelection(val selected: Boolean, val inRoutine: Boolean, val onToggle: () -> Unit)

private enum class DetailTab(val label: Int) { GUIDE(R.string.detail_tab_guide), PROGRESS(R.string.detail_tab_progress), HISTORY(R.string.detail_tab_history) }

private val PrimaryMuscleColor = Color(0xFFD8C3A5)
private val SecondaryMuscleColor = Color(0xFFA77B47)

/** Ficha completa. Sin [selection] (editor, sesión) solo informa; [onOpenSession] enlaza al resumen de una sesión. */
@Composable
internal fun ExerciseGuideScreen(
    exercise: CatalogExercise,
    onClose: () -> Unit,
    selection: GuideSelection? = null,
    onOpenSession: ((String) -> Unit)? = null,
) {
    BackHandler(onBack = onClose)
    var tab by rememberSaveable(exercise.id) { mutableStateOf(DetailTab.GUIDE) }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TrainingTopBar(title = exercise.name, subtitle = exercise.metaLine(), onBack = onClose)
            DetailTabs(tab, onSelect = { tab = it }, modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xs))
            Box(Modifier.weight(1f)) {
                when (tab) {
                    DetailTab.GUIDE -> GuideTab(exercise)
                    DetailTab.PROGRESS -> ProgressTab(exercise)
                    DetailTab.HISTORY -> HistoryTab(exercise, onOpenSession)
                }
            }
            if (selection != null) {
                BottomActionBar {
                    when {
                        selection.inRoutine -> SecondaryButton(
                            text = stringResource(R.string.picker_in_routine),
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.weight(1f),
                        )
                        selection.selected -> SecondaryButton(
                            text = stringResource(R.string.guide_selected),
                            onClick = selection.onToggle,
                            iconRes = R.drawable.ic_check,
                            modifier = Modifier.weight(1f),
                        )
                        else -> PrimaryButton(
                            text = stringResource(R.string.guide_add),
                            onClick = selection.onToggle,
                            iconRes = R.drawable.ic_add,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** Pestañas en cápsula (como la referencia aprobada) con rol de pestaña para TalkBack. */
@Composable
private fun DetailTabs(selected: DetailTab, onSelect: (DetailTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FitnessThemeExtras.colors.card)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        DetailTab.entries.forEach { t ->
            val isSelected = t == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MinTouchTarget)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(t) }),
            ) {
                Text(
                    stringResource(t.label),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.s),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Guía

@Composable
private fun GuideTab(exercise: CatalogExercise) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.l)
            .padding(top = Spacing.s, bottom = Spacing.xl),
    ) {
        FrameSequence(exercise)
        MusclesCard(exercise)
        FitnessCard {
            Fact(stringResource(R.string.guide_equipment), equipmentText(exercise))
            Fact(stringResource(R.string.guide_difficulty), "${exercise.difficulty.label}. ${exercise.difficulty.description}", last = true)
        }
        GuideSection(stringResource(R.string.guide_preparation)) {
            Text(exercise.preparation, style = MaterialTheme.typography.bodyLarge)
        }
        GuideSection(stringResource(R.string.guide_steps)) {
            exercise.steps.forEachIndexed { i, step ->
                Row(Modifier.semantics(mergeDescendants = true) {}.padding(vertical = Spacing.xs)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp).background(champagneTint(0.14f), CircleShape)) {
                        Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(Spacing.m))
                    Text(step, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
        }
        GuideSection(stringResource(R.string.guide_breathing)) {
            Text(exercise.breathing, style = MaterialTheme.typography.bodyLarge)
        }
        GuideSection(stringResource(R.string.guide_mistakes)) {
            exercise.mistakes.forEach { mistake ->
                Row(Modifier.padding(vertical = Spacing.xs).semantics(mergeDescendants = true) {}) {
                    Icon(painterResource(R.drawable.ic_error), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp).size(18.dp))
                    Spacer(Modifier.width(Spacing.m))
                    Text(mistake, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun equipmentText(exercise: CatalogExercise): String {
    val main = exercise.equipment.joinToString(" / ") { it.label }
    if (exercise.accessories.isEmpty()) return main
    return "$main · " + stringResource(R.string.guide_accessory, exercise.accessories.joinToString(", ") { it.label.lowercase() })
}

/** Alto de la secuencia: proporción 650×593 del fotograma, máximo 380 dp y 55 % del alto de la ventana. */
@Composable
private fun sequenceHeight(): Dp {
    val window = LocalWindowInfo.current.containerSize
    val (width, height) = with(LocalDensity.current) { window.width.toDp() to window.height.toDp() }
    val byWidth = (width - Spacing.l * 2) * (593f / 650f)
    return minOf(byWidth, 380.dp, height * 0.55f)
}

/**
 * Tres posiciones fijas: deslizar o flechas (manual), y bucle opcional 1→2→3→2→1 a 1,2 s por posición con
 * cambio directo de imagen (sin interpolar ni simular un vídeo). Empieza en pausa (autoplayDefault = false);
 * se pausa al salir de la pestaña, de la ficha o al pasar la app a segundo plano. Movimiento reducido: solo manual.
 */
@Composable
private fun FrameSequence(exercise: CatalogExercise) {
    val media = exercise.mediaAssets
    val motion = LocalMotionEnabled.current
    val pager = rememberPagerState(pageCount = { media.frames.size })
    val scope = rememberCoroutineScope()
    var playing by remember { mutableStateOf(false) }
    if (!motion) playing = false

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) playing = false }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // Un único bucle activo: se cancela al pausar o al salir de la composición.
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        val seq = media.sequence
        var step = seq.indexOf(pager.currentPage).coerceAtLeast(0)
        while (true) {
            delay(ExerciseMedia.FRAME_DURATION_MS)
            step = (step + 1) % seq.size
            pager.scrollToPage(seq[step])
        }
    }
    val phases = listOf(R.string.sequence_phase_1, R.string.sequence_phase_2, R.string.sequence_phase_3).map { stringResource(it) }
    val page = pager.currentPage

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(FitnessThemeExtras.colors.card),
    ) {
        Box {
            HorizontalPager(
                state = pager,
                modifier = Modifier
                    .fillMaxWidth()
                    // Altura explícita: proporción del fotograma en vertical; en horizontal, la figura completa
                    // cabe en pantalla junto a los controles (la imagen se ajusta con Fit, sin recortes).
                    .height(sequenceHeight()),
            ) { index ->
                ExerciseAssetImage(
                    path = media.frames[index],
                    contentDescription = stringResource(R.string.sequence_frame_description, exercise.name, index + 1, phases[index]),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                stringResource(R.string.sequence_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Spacing.s)
                    .background(ExerciseMediaBackground.copy(alpha = 0.82f), RoundedCornerShape(8.dp))
                    .padding(horizontal = Spacing.s, vertical = Spacing.xs),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs)) {
            IconButton(onClick = { playing = false; scope.launch { pager.scrollToPage((page - 1).coerceAtLeast(0)) } }, enabled = page > 0) {
                Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.sequence_prev))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${page + 1} · ${phases[page]}", style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.sequence_position, page + 1, media.frames.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { playing = false; scope.launch { pager.scrollToPage((page + 1).coerceAtMost(media.frames.size - 1)) } }, enabled = page < media.frames.size - 1) {
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(R.string.sequence_next))
            }
        }
        if (motion) {
            // Con fuente grande, un botón debajo de otro para que «Reproducir» no se parta.
            val stacked = LocalDensity.current.fontScale >= 1.3f
            val buttons: @Composable (Modifier) -> Unit = { buttonModifier ->
                SecondaryButton(
                    text = stringResource(if (playing) R.string.sequence_pause else R.string.sequence_play),
                    onClick = { playing = !playing },
                    iconRes = if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                    modifier = buttonModifier,
                )
                SecondaryButton(
                    text = stringResource(R.string.sequence_replay),
                    onClick = {
                        scope.launch {
                            playing = false
                            pager.scrollToPage(0)
                            playing = true
                        }
                    },
                    iconRes = R.drawable.ic_replay,
                    modifier = buttonModifier,
                )
            }
            val padding = Modifier.padding(start = Spacing.m, end = Spacing.m, bottom = Spacing.m)
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s), modifier = padding) { buttons(Modifier.fillMaxWidth()) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = padding) { buttons(Modifier.weight(1f)) }
            }
        } else {
            Text(
                stringResource(R.string.sequence_reduced_motion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.m, end = Spacing.m, bottom = Spacing.m),
            )
        }
        Text(
            stringResource(R.string.media_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.m, end = Spacing.m, bottom = Spacing.m),
        )
    }
}

@Composable
private fun MusclesCard(exercise: CatalogExercise) {
    val primary = exercise.primary.label
    val secondary = exercise.secondary.joinToString(", ") { it.label }
    val none = stringResource(R.string.muscles_none)
    FitnessCard {
        Text(stringResource(R.string.muscles_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }.padding(bottom = Spacing.s))
        ExerciseAssetImage(
            path = exercise.mediaAssets.anatomy,
            contentDescription = stringResource(R.string.muscles_map_description, primary, secondary.ifEmpty { none }),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 340.dp)
                .aspectRatio(650f / 594f)
                .clip(MaterialTheme.shapes.medium),
        )
        Spacer(Modifier.height(Spacing.m))
        LegendRow(PrimaryMuscleColor, stringResource(R.string.muscles_primary, primary))
        LegendRow(
            SecondaryMuscleColor,
            if (secondary.isEmpty()) stringResource(R.string.muscles_secondary_none) else stringResource(R.string.muscles_secondary, secondary),
        )
        Text(stringResource(R.string.muscles_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.xs))
    }
}

@Composable
private fun LegendRow(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = Spacing.xs)) {
        Box(Modifier.size(16.dp).background(color, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(Spacing.s))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Fact(label: String, value: String, last: Boolean = false) {
    Column(Modifier.padding(bottom = if (last) 0.dp else Spacing.m).semantics(mergeDescendants = true) {}) {
        Text(label.uppercase(), style = OverlineStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xs))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun GuideSection(title: String, content: @Composable () -> Unit) {
    FitnessCard {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }.padding(bottom = Spacing.s))
        content()
    }
}

// ---------------------------------------------------------------- Progreso

data class ExerciseProgressUiState(
    val loading: Boolean = true,
    val unit: UnitSystem = UnitSystem.METRIC,
    val progress: ExerciseProgress? = null,
)

/** Progreso de una definición del catálogo con los registros reales; se actualiza solo al cambiar la base. */
class ExerciseProgressViewModel(
    catalogId: String,
    semantics: LoadSemantics,
    repository: TrainingRepository,
    preferences: UserPreferencesRepository,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val period = MutableStateFlow(ProgressPeriod.ALL)

    val state: StateFlow<ExerciseProgressUiState> = combine(
        repository.observeCatalogExerciseSets(catalogId),
        period,
        preferences.userPreferences.map { it.unitSystem },
    ) { rows, p, unit ->
        ExerciseProgressUiState(loading = false, unit = unit, progress = exerciseProgress(rows, semantics, p, nowMs()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseProgressUiState())

    fun setPeriod(value: ProgressPeriod) {
        period.value = value
    }

    companion object {
        fun factory(exercise: CatalogExercise) = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApplication).container
                ExerciseProgressViewModel(exercise.id, exercise.loadSemantics, container.trainingRepository, container.userPreferencesRepository)
            }
        }
    }
}

@Composable
private fun progressViewModel(exercise: CatalogExercise): ExerciseProgressViewModel =
    viewModel(key = "exercise-progress-${exercise.id}", factory = ExerciseProgressViewModel.factory(exercise))

private val ShortDate = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-ES"))
private val ShortDateYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-ES"))

internal fun formatShortDate(epochMs: Long, zone: ZoneId = ZoneId.systemDefault(), nowMs: Long = System.currentTimeMillis()): String {
    val date = Instant.ofEpochMilli(epochMs).atZone(zone)
    val sameYear = date.year == Instant.ofEpochMilli(nowMs).atZone(zone).year
    return (if (sameYear) ShortDate else ShortDateYear).format(date).replace(".", "")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressTab(exercise: CatalogExercise) {
    val vm = progressViewModel(exercise)
    val state by vm.state.collectAsStateWithLifecycle()
    val progress = state.progress ?: return
    val unit = state.unit
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.l)
            .padding(top = Spacing.s, bottom = Spacing.xl),
    ) {
        if (!progress.hasPractice) {
            EmptyCard(stringResource(R.string.progress_empty_title), stringResource(R.string.progress_empty_body))
            return@Column
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            listOf(
                ProgressPeriod.ALL to R.string.progress_period_all,
                ProgressPeriod.DAYS_90 to R.string.progress_period_90,
                ProgressPeriod.DAYS_30 to R.string.progress_period_30,
            ).forEach { (p, label) ->
                FilterChip(
                    selected = progress.period == p,
                    onClick = { vm.setPeriod(p) },
                    label = { Text(stringResource(label)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                )
            }
        }
        Text(
            pluralStringResource(
                R.plurals.progress_summary, progress.sessionsInPeriod, progress.sessionsInPeriod,
                pluralStringResource(R.plurals.progress_work_sets, progress.workSetsInPeriod, progress.workSetsInPeriod),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (progress.sessionsInPeriod == 0) {
            EmptyCard(stringResource(R.string.progress_period_empty), null)
        } else if (progress.semantics == LoadSemantics.EXTERNAL) {
            val noLoad = stringResource(R.string.progress_no_load)
            MetricTiles { tileModifier ->
                MetricTile(
                    stringResource(R.string.progress_max_load),
                    progress.maxLoad?.let { formatLoad(it.loadKg, unit) } ?: "—",
                    progress.maxLoad?.let { stringResource(R.string.progress_reps_date, it.reps, formatShortDate(it.endedAt)) } ?: noLoad,
                    R.drawable.ic_dumbbell,
                    tileModifier,
                )
                MetricTile(
                    stringResource(R.string.progress_best_volume),
                    progress.bestVolume?.let { formatVolume(it.volumeKg, unit) } ?: "—",
                    progress.bestVolume?.let { pluralStringResource(R.plurals.progress_sets_date, it.sets, it.sets, formatShortDate(it.endedAt)) } ?: noLoad,
                    R.drawable.ic_bar_chart,
                    tileModifier,
                )
                MetricTile(
                    stringResource(R.string.progress_avg_load),
                    progress.averageLoad?.let { formatLoad(it.loadKg, unit) } ?: "—",
                    progress.averageLoad?.let { pluralStringResource(R.plurals.progress_sets, it.sets, it.sets) } ?: noLoad,
                    R.drawable.ic_trending_up,
                    tileModifier,
                )
            }
            if (progress.chart.isNotEmpty()) {
                ChartCard(stringResource(R.string.progress_chart_load), stringResource(R.string.progress_chart_load_sub), progress, unit)
            }
        } else {
            if (progress.chart.isNotEmpty()) {
                ChartCard(stringResource(R.string.progress_chart_assist), stringResource(R.string.progress_chart_assist_sub), progress, unit)
            }
        }
        FitnessCard {
            Text(stringResource(R.string.progress_marks), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }.padding(bottom = Spacing.s))
            progress.firstPractice?.let { MarkRow(R.drawable.ic_bookmark, stringResource(R.string.progress_first), formatSessionDate(it.endedAt)) }
            progress.lastPractice?.let { MarkRow(R.drawable.ic_history, stringResource(R.string.progress_last), formatSessionDate(it.endedAt)) }
            Text(stringResource(R.string.progress_global_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.xs))
        }
        RuleNote(stringResource(R.string.progress_rules))
        when {
            progress.semantics == LoadSemantics.ASSISTANCE -> RuleNote(stringResource(R.string.progress_assist_rule))
            exercise.usesDumbbells -> RuleNote(stringResource(R.string.progress_dumbbell_rule))
            progress.maxLoad == null -> RuleNote(stringResource(R.string.progress_bodyweight_rule))
        }
    }
}

@Composable
private fun EmptyCard(title: String, body: String?) {
    FitnessCard {
        Icon(painterResource(R.drawable.ic_trending_up), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(Spacing.s))
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Tres marcas en fila de igual ancho; con fuente grande, una debajo de otra. */
@Composable
private fun MetricTiles(content: @Composable (Modifier) -> Unit) {
    if (LocalDensity.current.fontScale >= 1.3f) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) { content(Modifier.fillMaxWidth()) }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.height(IntrinsicSize.Max)) {
            content(Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, detail: String, icon: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(FitnessThemeExtras.colors.card)
            .padding(Spacing.m)
            .semantics(mergeDescendants = true) {},
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(Spacing.xs))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = NumberStyleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, softWrap = false, autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = NumberStyleMedium.fontSize))
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MarkRow(icon: Int, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = Spacing.xs).semantics(mergeDescendants = true) {}) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.m))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RuleNote(text: String) {
    Row(Modifier.semantics(mergeDescendants = true) {}) {
        Icon(painterResource(R.drawable.ic_info), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp).size(16.dp))
        Spacer(Modifier.width(Spacing.s))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Puntos reales por sesión en orden de fecha; sin días inventados. Un único punto se dibuja solo. */
@Composable
private fun ChartCard(title: String, subtitle: String, progress: ExerciseProgress, unit: UnitSystem) {
    val points = progress.chart
    val values = points.map { kgToDisplay(it.valueKg, unit) }
    val minV = values.min()
    val maxV = values.max()
    val description = if (points.size == 1) {
        stringResource(R.string.progress_chart_single, formatLoad(points.first().valueKg, unit).orEmpty())
    } else {
        pluralStringResource(R.plurals.progress_chart_description, points.size, title, points.size, formatLoad(points.first().valueKg, unit).orEmpty(), formatLoad(points.last().valueKg, unit).orEmpty())
    }
    val line = MaterialTheme.colorScheme.primary
    val grid = FitnessThemeExtras.colors.surfaceRaised
    FitnessCard {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.m))
        Row(Modifier.clearAndSetSemantics { contentDescription = description }) {
            val top = formatLoad(points.maxBy { it.valueKg }.valueKg, unit).orEmpty()
            val bottom = formatLoad(points.minBy { it.valueKg }.valueKg, unit).orEmpty()
            // Un único valor: una sola etiqueta, centrada como el punto.
            Column(Modifier.height(160.dp), verticalArrangement = if (top == bottom) Arrangement.Center else Arrangement.SpaceBetween) {
                Text(top, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (top != bottom) Text(bottom, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(Spacing.s))
            Canvas(Modifier.weight(1f).height(160.dp)) {
                val pad = 10.dp.toPx()
                val w = size.width - 2 * pad
                val h = size.height - 2 * pad
                listOf(0f, 0.5f, 1f).forEach { f ->
                    drawLine(grid, Offset(0f, pad + h * f), Offset(size.width, pad + h * f), strokeWidth = 1.dp.toPx())
                }
                val range = (maxV - minV).takeIf { it > 0 } ?: 1.0
                val offsets = values.mapIndexed { i, v ->
                    val x = if (values.size == 1) size.width / 2 else pad + w * i / (values.size - 1)
                    val y = if (maxV == minV) pad + h / 2 else pad + h * (1 - ((v - minV) / range)).toFloat()
                    Offset(x, y)
                }
                if (offsets.size > 1) {
                    val path = Path().apply {
                        moveTo(offsets.first().x, offsets.first().y)
                        offsets.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, line, style = Stroke(width = 2.5.dp.toPx()))
                }
                offsets.forEach { drawCircle(line, radius = 5.dp.toPx(), center = it) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = Spacing.xs)) {
            Text(formatShortDate(points.first().endedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            if (points.size > 1) {
                Text(formatShortDate(points.last().endedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------------------------------------------------------- Historial

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryTab(exercise: CatalogExercise, onOpenSession: ((String) -> Unit)?) {
    val vm = progressViewModel(exercise)
    val state by vm.state.collectAsStateWithLifecycle()
    val progress = state.progress ?: return
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.l)
            .padding(top = Spacing.s, bottom = Spacing.xl),
    ) {
        if (progress.history.isEmpty()) {
            EmptyCard(stringResource(R.string.history_empty), null)
            return@Column
        }
        // Marca global (todo el historial), independiente del filtro de la pestaña Progreso.
        val bestKg = progress.history.mapNotNull { it.maxLoadKg }.maxOrNull()
        progress.history.forEach { entry ->
            HistoryCard(entry, state.unit, progress.semantics, isBest = progress.semantics == LoadSemantics.EXTERNAL && bestKg != null && entry.maxLoadKg == bestKg, onOpenSession)
        }
        if (progress.history.any { e -> e.sets.any { it.warmup } }) RuleNote(stringResource(R.string.history_warmup_legend))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryCard(
    entry: ExerciseSessionEntry,
    unit: UnitSystem,
    semantics: LoadSemantics,
    isBest: Boolean,
    onOpenSession: ((String) -> Unit)?,
) {
    FitnessCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(formatSessionDate(entry.endedAt), style = MaterialTheme.typography.titleMedium)
                Text(entry.routineName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isBest) {
                Text(
                    stringResource(R.string.history_best),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)).padding(horizontal = Spacing.s, vertical = Spacing.xs),
                )
            }
        }
        Spacer(Modifier.height(Spacing.s))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            entry.sets.forEach { set ->
                val reps = set.reps ?: 0
                val load = formatLoad(set.loadKg, unit)
                val text = when {
                    load == null -> stringResource(R.string.history_set_no_load, reps)
                    semantics == LoadSemantics.ASSISTANCE -> stringResource(R.string.history_set_assist, load, reps)
                    else -> stringResource(R.string.history_set, load, reps)
                }
                Text(
                    if (set.warmup) "${stringResource(R.string.history_warmup)} · $text" else text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (set.warmup) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .background(FitnessThemeExtras.colors.surfaceRaised, RoundedCornerShape(8.dp))
                        .padding(horizontal = Spacing.s, vertical = Spacing.xs),
                )
            }
        }
        entry.volumeKg?.let {
            Text(stringResource(R.string.history_volume, formatVolume(it, unit)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = Spacing.s))
        }
        if (onOpenSession != null) {
            TextButton(onClick = { onOpenSession(entry.sessionId) }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.history_open_summary), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(Spacing.xs))
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
