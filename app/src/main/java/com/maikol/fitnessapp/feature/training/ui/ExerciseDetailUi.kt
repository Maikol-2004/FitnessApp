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
import androidx.compose.ui.semantics.role
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
import com.maikol.fitnessapp.feature.training.catalog.AssistMode
import com.maikol.fitnessapp.feature.training.catalog.CatalogExercise
import com.maikol.fitnessapp.feature.training.catalog.ExerciseCatalog
import com.maikol.fitnessapp.feature.training.catalog.ExerciseMedia
import com.maikol.fitnessapp.feature.training.catalog.LoadSemantics
import com.maikol.fitnessapp.feature.training.catalog.loadSemantics
import com.maikol.fitnessapp.feature.training.catalog.mediaAssets
import com.maikol.fitnessapp.feature.training.catalog.usesDumbbells
import com.maikol.fitnessapp.feature.training.data.ChartPoint
import com.maikol.fitnessapp.feature.training.data.ExerciseProgress
import com.maikol.fitnessapp.feature.training.data.ExerciseSessionEntry
import com.maikol.fitnessapp.feature.training.data.ProgressPeriod
import com.maikol.fitnessapp.feature.training.data.TrainingRepository
import com.maikol.fitnessapp.feature.training.data.exerciseProgress
import com.maikol.fitnessapp.feature.training.data.formatLoad
import com.maikol.fitnessapp.feature.training.data.formatLoadInput
import com.maikol.fitnessapp.feature.training.data.formatSessionDate
import com.maikol.fitnessapp.feature.training.data.formatVolume
import com.maikol.fitnessapp.feature.training.data.kgToDisplay
import com.maikol.fitnessapp.feature.training.data.loadUnit
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

/** G2.1: prefijo del identificador de ficha de un ejercicio personalizado (estado guardado del editor y la sesión). */
internal const val CUSTOM_GUIDE_PREFIX = "custom:"

internal fun customGuideToken(exerciseKey: String) = CUSTOM_GUIDE_PREFIX + exerciseKey

/** Qué muestra la ficha: una definición del catálogo o un ejercicio personalizado, identificado por su exerciseKey. */
internal sealed interface DetailSubject {
    /** Clave estable para el estado de la ficha y su ViewModel. */
    val key: String
    val name: String
    val semantics: LoadSemantics
    val usesDumbbells: Boolean

    data class Catalog(val exercise: CatalogExercise) : DetailSubject {
        override val key: String get() = exercise.id
        override val name: String get() = exercise.name
        override val semantics: LoadSemantics get() = exercise.loadSemantics
        override val usesDumbbells: Boolean get() = exercise.usesDumbbells
    }

    /** Sin imágenes ni mapa: nunca se le asignan los medios de otro ejercicio. */
    data class Custom(val exerciseKey: String, override val name: String, val notes: String) : DetailSubject {
        override val key: String get() = customGuideToken(exerciseKey)
        override val semantics: LoadSemantics get() = LoadSemantics.EXTERNAL
        override val usesDumbbells: Boolean get() = false
    }

    companion object {
        /** Resuelve el identificador guardado: catalogId o «custom:<exerciseKey>» buscado en [customs] (clave → nombre, notas). */
        fun resolve(token: String?, customs: (String) -> Pair<String, String>?): DetailSubject? {
            if (token == null) return null
            if (token.startsWith(CUSTOM_GUIDE_PREFIX)) {
                val exerciseKey = token.removePrefix(CUSTOM_GUIDE_PREFIX)
                return customs(exerciseKey)?.let { (name, notes) -> Custom(exerciseKey, name, notes) }
            }
            return ExerciseCatalog.byId(token)?.let { Catalog(it) }
        }
    }
}

/** Acción de la ficha cuando se abre desde el selector. */
internal data class GuideSelection(val selected: Boolean, val inRoutine: Boolean, val onToggle: () -> Unit)

private enum class DetailTab(val label: Int) { GUIDE(R.string.detail_tab_guide), PROGRESS(R.string.detail_tab_progress), HISTORY(R.string.detail_tab_history) }

private val PrimaryMuscleColor = Color(0xFFD8C3A5)
private val SecondaryMuscleColor = Color(0xFFA77B47)

/** Ficha de una definición del catálogo (selector, editor y sesión). */
@Composable
internal fun ExerciseGuideScreen(
    exercise: CatalogExercise,
    onClose: () -> Unit,
    selection: GuideSelection? = null,
    onOpenSession: ((String) -> Unit)? = null,
) = ExerciseDetailScreen(DetailSubject.Catalog(exercise), onClose, selection, onOpenSession)

/** Ficha completa. Sin [selection] (editor, sesión) solo informa; [onOpenSession] enlaza al resumen de una sesión. */
@Composable
internal fun ExerciseDetailScreen(
    subject: DetailSubject,
    onClose: () -> Unit,
    selection: GuideSelection? = null,
    onOpenSession: ((String) -> Unit)? = null,
) {
    BackHandler(onBack = onClose)
    var tab by rememberSaveable(subject.key) { mutableStateOf(DetailTab.GUIDE) }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            val subtitle = when (subject) {
                is DetailSubject.Catalog -> subject.exercise.metaLine()
                is DetailSubject.Custom -> stringResource(R.string.custom_subtitle)
            }
            TrainingTopBar(title = subject.name, subtitle = subtitle, onBack = onClose)
            DetailTabs(tab, onSelect = { tab = it }, modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xs))
            Box(Modifier.weight(1f)) {
                when (tab) {
                    DetailTab.GUIDE -> when (subject) {
                        is DetailSubject.Catalog -> GuideTab(subject.exercise)
                        is DetailSubject.Custom -> CustomGuideTab(subject)
                    }
                    DetailTab.PROGRESS -> ProgressTab(subject)
                    DetailTab.HISTORY -> HistoryTab(subject, onOpenSession)
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
private fun CustomGuideTab(subject: DetailSubject.Custom) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.l)
            .padding(top = Spacing.s, bottom = Spacing.xl),
    ) {
        FitnessCard {
            Icon(painterResource(R.drawable.ic_edit), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(Spacing.s))
            Text(stringResource(R.string.custom_guide_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.custom_guide_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        GuideSection(stringResource(R.string.custom_notes_title)) {
            if (subject.notes.isBlank()) {
                Text(stringResource(R.string.custom_notes_empty), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(subject.notes, style = MaterialTheme.typography.bodyLarge)
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

/**
 * Alto de la secuencia (G2.1, más compacta como la referencia aprobada): máximo 236 dp y 30 % del alto de la
 * ventana, sin superar la proporción 650×593 del fotograma. La imagen se ajusta con Fit: la figura completa,
 * sin recortes, con el fondo del recurso a los lados.
 */
@Composable
private fun sequenceHeight(): Dp {
    val window = LocalWindowInfo.current.containerSize
    val (width, height) = with(LocalDensity.current) { window.width.toDp() to window.height.toDp() }
    val byWidth = (width - Spacing.l * 2) * (593f / 650f)
    return minOf(byWidth, 236.dp, height * 0.30f)
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
        // Una sola fila de controles (48 dp): anterior, posición, siguiente y, con movimiento, bucle y repetir.
        val positionDescription = stringResource(R.string.sequence_position, page + 1, media.frames.size) + ": " + phases[page]
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Spacing.xs)) {
            IconButton(onClick = { playing = false; scope.launch { pager.scrollToPage((page - 1).coerceAtLeast(0)) } }, enabled = page > 0) {
                Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.sequence_prev))
            }
            Text(
                stringResource(R.string.sequence_position_short, page + 1, media.frames.size, phases[page]),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).semantics { contentDescription = positionDescription },
            )
            IconButton(onClick = { playing = false; scope.launch { pager.scrollToPage((page + 1).coerceAtMost(media.frames.size - 1)) } }, enabled = page < media.frames.size - 1) {
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(R.string.sequence_next))
            }
            if (motion) {
                SequenceIconButton(
                    iconRes = if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                    description = stringResource(if (playing) R.string.sequence_pause_description else R.string.sequence_play_description),
                    onClick = { playing = !playing },
                )
                SequenceIconButton(
                    iconRes = R.drawable.ic_replay,
                    description = stringResource(R.string.sequence_replay_description),
                    onClick = {
                        scope.launch {
                            playing = false
                            pager.scrollToPage(0)
                            playing = true
                        }
                    },
                )
            }
        }
        Text(
            if (motion) stringResource(R.string.media_disclaimer)
            else stringResource(R.string.sequence_reduced_motion) + " " + stringResource(R.string.media_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.m, end = Spacing.m, bottom = Spacing.s),
        )
    }
}

/** Botón redondo de la secuencia: 48 dp de área táctil, con fondo champán tenue para distinguirlo de las flechas. */
@Composable
private fun SequenceIconButton(iconRes: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp).background(champagneTint(0.16f), CircleShape)) {
            Icon(painterResource(iconRes), contentDescription = description, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun MusclesCard(exercise: CatalogExercise) {
    val primary = exercise.primary.label
    val secondary = exercise.secondary.joinToString(", ") { it.label }
    val none = stringResource(R.string.muscles_none)
    val description = stringResource(R.string.muscles_map_description, primary, secondary.ifEmpty { none })
    val legend: @Composable () -> Unit = {
        LegendRow(PrimaryMuscleColor, stringResource(R.string.muscles_primary, primary))
        LegendRow(
            SecondaryMuscleColor,
            if (secondary.isEmpty()) stringResource(R.string.muscles_secondary_none) else stringResource(R.string.muscles_secondary, secondary),
        )
        Text(stringResource(R.string.muscles_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.xs))
    }
    FitnessCard {
        Text(stringResource(R.string.muscles_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }.padding(bottom = Spacing.s))
        if (LocalDensity.current.fontScale >= 1.3f) {
            // Fuente grande: mapa encima y leyenda a todo el ancho.
            // Altura fija y ancho derivado: con fillMaxWidth el aspecto no se cumplía y la leyenda se solapaba.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnatomyImage(exercise, description, Modifier.height(240.dp))
            }
            Spacer(Modifier.height(Spacing.s))
            legend()
        } else {
            // Como la referencia aprobada: mapa a la izquierda y leyenda a la derecha.
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnatomyImage(exercise, description, Modifier.weight(0.56f))
                Spacer(Modifier.width(Spacing.m))
                Column(Modifier.weight(0.44f)) { legend() }
            }
        }
    }
}

@Composable
private fun AnatomyImage(exercise: CatalogExercise, description: String, modifier: Modifier) {
    ExerciseAssetImage(
        path = exercise.mediaAssets.anatomy,
        contentDescription = description,
        modifier = modifier
            .aspectRatio(617f / 626f)
            .clip(MaterialTheme.shapes.medium),
    )
}

@Composable
private fun LegendRow(color: Color, text: String) {
    Row(modifier = Modifier.padding(vertical = Spacing.xs)) {
        Box(Modifier.padding(top = 3.dp).size(14.dp).background(color, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(Spacing.s))
        Text(text, style = MaterialTheme.typography.bodyMedium)
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

/**
 * Progreso con los registros reales; se actualiza solo al cambiar la base. Catálogo: por [catalogId] en cualquier
 * rutina. Personalizado o anterior a G1 ([catalogId] null): por su [exerciseKey], sin mezclar con el catálogo.
 */
class ExerciseProgressViewModel(
    catalogId: String?,
    exerciseKey: String?,
    semantics: LoadSemantics,
    repository: TrainingRepository,
    preferences: UserPreferencesRepository,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val period = MutableStateFlow(ProgressPeriod.ALL)

    val state: StateFlow<ExerciseProgressUiState> = combine(
        if (catalogId != null) repository.observeCatalogExerciseSets(catalogId) else repository.observeCustomExerciseSets(requireNotNull(exerciseKey)),
        period,
        preferences.userPreferences.map { it.unitSystem },
    ) { rows, p, unit ->
        ExerciseProgressUiState(loading = false, unit = unit, progress = exerciseProgress(rows, semantics, p, nowMs()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseProgressUiState())

    fun setPeriod(value: ProgressPeriod) {
        period.value = value
    }

    companion object {
        internal fun factory(subject: DetailSubject) = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FitnessApplication).container
                ExerciseProgressViewModel(
                    catalogId = (subject as? DetailSubject.Catalog)?.exercise?.id,
                    exerciseKey = (subject as? DetailSubject.Custom)?.exerciseKey,
                    semantics = subject.semantics,
                    repository = container.trainingRepository,
                    preferences = container.userPreferencesRepository,
                )
            }
        }
    }
}

@Composable
private fun progressViewModel(subject: DetailSubject): ExerciseProgressViewModel =
    viewModel(key = "exercise-progress-${subject.key}", factory = ExerciseProgressViewModel.factory(subject))

/** Asistencia para lectura: a diferencia de [formatLoad], 0 es un dato («0 kg» = sin ayuda), no una ausencia. */
private fun formatAssist(kg: Double, unit: UnitSystem): String = "${formatLoadInput(kg, unit)} ${unit.loadUnit()}"

private val ShortDate = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-ES"))
private val ShortDateYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-ES"))

internal fun formatShortDate(epochMs: Long, zone: ZoneId = ZoneId.systemDefault(), nowMs: Long = System.currentTimeMillis()): String {
    val date = Instant.ofEpochMilli(epochMs).atZone(zone)
    val sameYear = date.year == Instant.ofEpochMilli(nowMs).atZone(zone).year
    return (if (sameYear) ShortDate else ShortDateYear).format(date).replace(".", "")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressTab(subject: DetailSubject) {
    val vm = progressViewModel(subject)
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
                ChartCard(stringResource(R.string.progress_chart_load), stringResource(R.string.progress_chart_load_sub), progress.chart) { formatLoad(it, unit).orEmpty() }
            }
        } else {
            // Una gráfica por modalidad: los kg de máquina y la ayuda de una banda no se comparan entre sí.
            progress.assistSeries.forEach { series ->
                val title = stringResource(if (series.mode == AssistMode.MACHINE) R.string.progress_chart_assist_machine else R.string.progress_chart_assist_band)
                ChartCard(title, stringResource(R.string.progress_chart_assist_sub), series.points) { formatAssist(it, unit) }
            }
            if (progress.unknownAssistSessions > 0) {
                RuleNote(pluralStringResource(R.plurals.progress_assist_unknown, progress.unknownAssistSessions, progress.unknownAssistSessions))
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
            subject.usesDumbbells -> RuleNote(stringResource(R.string.progress_dumbbell_rule))
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
private fun ChartCard(title: String, subtitle: String, points: List<ChartPoint>, format: (Double) -> String) {
    // El orden del eje sale de los kg guardados; la unidad solo cambia el texto (la escala es proporcional).
    val values = points.map { it.valueKg }
    val minV = values.min()
    val maxV = values.max()
    val description = if (points.size == 1) {
        stringResource(R.string.progress_chart_single, format(points.first().valueKg))
    } else {
        pluralStringResource(R.plurals.progress_chart_description, points.size, title, points.size, format(points.first().valueKg), format(points.last().valueKg))
    }
    val line = MaterialTheme.colorScheme.primary
    val grid = FitnessThemeExtras.colors.surfaceRaised
    FitnessCard {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.m))
        Row(Modifier.clearAndSetSemantics { contentDescription = description }) {
            val top = format(maxV)
            val bottom = format(minV)
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
private fun HistoryTab(subject: DetailSubject, onOpenSession: ((String) -> Unit)?) {
    val vm = progressViewModel(subject)
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
                val text = if (semantics == LoadSemantics.ASSISTANCE) {
                    // 0 kg de ayuda es un dato; solo null es «sin anotar». Cada serie dice su modalidad.
                    val kg = set.loadKg
                    when {
                        kg == null -> stringResource(R.string.history_set_assist_missing, reps)
                        set.assistMode == AssistMode.MACHINE -> stringResource(R.string.history_set_assist_machine, formatAssist(kg, unit), reps)
                        set.assistMode == AssistMode.BAND -> stringResource(R.string.history_set_assist_band, formatAssist(kg, unit), reps)
                        else -> stringResource(R.string.history_set_assist_unknown, formatAssist(kg, unit), reps)
                    }
                } else {
                    formatLoad(set.loadKg, unit)?.let { stringResource(R.string.history_set, it, reps) }
                        ?: stringResource(R.string.history_set_no_load, reps)
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

// ---------------------------------------------------------------- Modalidad de asistencia (G2.1)

/**
 * Máquina o banda para la dominada asistida (editor y sesión). Tocar la opción marcada la desmarca: «sin
 * indicar» solo lo decide la persona; la app nunca adivina la modalidad de registros anteriores.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AssistModeSelector(mode: AssistMode?, onChange: (AssistMode?) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(stringResource(R.string.assist_mode_title), style = MaterialTheme.typography.labelLarge, modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.selectableGroup()) {
            listOf(AssistMode.MACHINE to R.string.assist_mode_machine, AssistMode.BAND to R.string.assist_mode_band).forEach { (m, label) ->
                FilterChip(
                    selected = mode == m,
                    onClick = { onChange(if (mode == m) null else m) },
                    label = { Text(stringResource(label)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.heightIn(min = MinTouchTarget).semantics { role = Role.RadioButton },
                )
            }
        }
        Text(
            stringResource(if (mode == null) R.string.assist_mode_unknown_hint else R.string.assist_mode_known_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
