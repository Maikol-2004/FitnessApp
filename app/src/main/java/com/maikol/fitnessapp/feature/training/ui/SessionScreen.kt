package com.maikol.fitnessapp.feature.training.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.database.SessionStatus
import com.maikol.fitnessapp.core.designsystem.component.LoadingRegion
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.motion.MotionTokens
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.feature.training.catalog.ExerciseCatalog
import com.maikol.fitnessapp.feature.training.catalog.LoadSemantics
import com.maikol.fitnessapp.feature.training.catalog.loadSemantics
import com.maikol.fitnessapp.feature.training.catalog.mediaAssets
import com.maikol.fitnessapp.feature.training.data.ClockReading
import com.maikol.fitnessapp.feature.training.data.Parsed
import com.maikol.fitnessapp.feature.training.data.PerformedSet
import com.maikol.fitnessapp.feature.training.data.PreviousSession
import com.maikol.fitnessapp.feature.training.data.PreviousSet
import com.maikol.fitnessapp.feature.training.data.Session
import com.maikol.fitnessapp.feature.training.data.SessionExercise
import com.maikol.fitnessapp.feature.training.data.SessionTimer
import com.maikol.fitnessapp.feature.training.data.SessionTiming
import com.maikol.fitnessapp.feature.training.data.formatClock
import com.maikol.fitnessapp.feature.training.data.formatCountdown
import com.maikol.fitnessapp.feature.training.data.formatExplicitLoad
import com.maikol.fitnessapp.feature.training.data.loadUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun SessionRoute(
    onBack: () -> Unit,
    onFinished: (sessionId: String) -> Unit,
    onOpenSummary: (sessionId: String) -> Unit,
    onOpenPastSession: (sessionId: String) -> Unit = {},
    viewModel: SessionViewModel = viewModel(factory = SessionViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished, state.discarded) {
        when {
            state.finished -> {
                viewModel.onNavigationHandled()
                onFinished(viewModel.sessionId)
            }
            state.discarded -> {
                viewModel.onNavigationHandled()
                onBack()
            }
        }
    }
    LoadingRegion(isLoading = state.loading, message = stringResource(R.string.preparing_workout), modifier = Modifier.fillMaxSize()) {
        val session = state.session
        if (session == null || !session.isOpen) {
            ClosedSession(session, onBack, onOpenSummary)
            return@LoadingRegion
        }
        SessionScreen(state, session, viewModel, onBack, onOpenPastSession)
    }
    SessionDialogs(state, viewModel)
}

@Composable
private fun ClosedSession(session: Session?, onBack: () -> Unit, onOpenSummary: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TrainingTopBar(title = stringResource(R.string.session_title), onBack = onBack)
        Column(Modifier.padding(Spacing.l)) {
            Text(stringResource(R.string.session_not_found), style = MaterialTheme.typography.bodyLarge)
            if (session?.status == SessionStatus.COMPLETED) {
                Spacer(Modifier.height(Spacing.l))
                PrimaryButton(text = stringResource(R.string.summary_title), onClick = { onOpenSummary(session.id) })
            }
        }
    }
}

/**
 * V2 (Liquid Glass abierto, como el editor V1): ejercicio y series directamente sobre el fondo; el cristal solo
 * está en campos y controles. El descanso queda fijo entre el contenido y las acciones, sin tapar las series.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionScreen(
    state: SessionUiState,
    session: Session,
    vm: SessionViewModel,
    onBack: () -> Unit,
    onOpenPastSession: (String) -> Unit,
) {
    // Atrás deja la sesión abierta y recuperable; no la pausa ni la descarta.
    BackHandler(onBack = onBack)
    val exerciseIndex = session.currentExercise.coerceIn(0, (session.exercises.size - 1).coerceAtLeast(0))
    val exercise = session.exercises.getOrNull(exerciseIndex)
    val uncertain = remember(session.timer) { SessionTiming.delta(session.timer, vm.now()).uncertain }
    // Poca altura (horizontal): el descanso pasa al contenido desplazable para no tapar las series.
    val compactHeight = LocalConfiguration.current.screenHeightDp < 520
    // G1: ficha del ejercicio como capa local (sobrevive a la rotación); el temporizador sigue en el ViewModel.
    var guideId by rememberSaveable { mutableStateOf<String?>(null) }
    // Con el teclado abierto, Pausar/Finalizar se ocultan y el descanso pasa al contenido desplazable: todo el
    // hueco libre queda para las series que se están editando (sigue visible al desplazar y sigue contando).
    val keyboard = WindowInsets.isImeVisible
    val restInContent = compactHeight || keyboard

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            TrainingTopBar(
                title = stringResource(R.string.session_title),
                subtitle = session.routineName,
                onBack = onBack,
                actions = {
                    OverflowMenu(
                        description = stringResource(R.string.session_more),
                        actions = listOf(MenuAction(R.string.session_discard, R.drawable.ic_delete) { vm.requestDiscard() }),
                    )
                },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.l)
                    .padding(top = Spacing.xs, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.l),
            ) {
                SessionProgress(session, vm::now)
                if (restInContent) RestPanel(session, vm)
                if (uncertain) {
                    Column {
                        InlineMessage(visible = true, text = stringResource(R.string.session_time_uncertain), iconRes = R.drawable.ic_timer, color = MaterialTheme.colorScheme.onSurface)
                        TextButton(onClick = vm::confirmTime, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                            Text(stringResource(R.string.session_time_confirm), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                InlineMessage(
                    visible = session.isPaused,
                    text = stringResource(R.string.session_paused_banner),
                    iconRes = R.drawable.ic_pause,
                    color = MaterialTheme.colorScheme.primary,
                )
                InlineMessage(visible = state.finishFailed, text = stringResource(R.string.session_finish_failed))
                if (exercise != null) {
                    ExerciseSection(
                        exercise = exercise,
                        index = exerciseIndex,
                        count = session.exercises.size,
                        nextName = session.exercises.getOrNull(exerciseIndex + 1)?.name,
                        state = state,
                        vm = vm,
                        onOpenGuide = { guideId = it },
                    )
                }
            }
            if (!restInContent) {
                RestPanel(session, vm, Modifier.padding(horizontal = Spacing.l), divider = true)
            }
            if (!keyboard) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = Spacing.l)
                        .padding(top = Spacing.s, bottom = Spacing.m),
                ) {
                    if (session.isPaused) {
                        GlassButton(stringResource(R.string.session_resume), R.drawable.ic_play, vm::resume, Modifier.weight(1f).heightIn(min = 50.dp), centered = true)
                    } else {
                        GlassButton(stringResource(R.string.session_pause), R.drawable.ic_pause, vm::pause, Modifier.weight(1f).heightIn(min = 50.dp), centered = true)
                    }
                    PrimaryButton(
                        text = stringResource(R.string.session_finish),
                        onClick = vm::requestFinish,
                        busy = state.finishing,
                        iconRes = R.drawable.ic_stop,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        DetailSubject.resolve(guideId) { key -> session.exercises.firstOrNull { it.key == key && it.catalogId == null }?.let { it.name to it.notes } }
            ?.let { ExerciseDetailScreen(it, onClose = { guideId = null }, onOpenSession = onOpenPastSession) }
    }
}

/** V2: progreso compacto sobre el fondo: tiempo efectivo, series reales completadas y aro M07 pequeño. */
@Composable
private fun SessionProgress(session: Session, now: () -> ClockReading) {
    val description = stringResource(R.string.session_progress_description, session.completedSets, session.totalSets)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.session_elapsed).uppercase(), style = OverlineStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ElapsedClock(session.timer, now)
        }
        Text(
            pluralStringResource(R.plurals.v2_sets_progress, session.totalSets, session.completedSets, session.totalSets),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        )
        Spacer(Modifier.width(Spacing.m))
        ProgressRing(
            progress = if (session.totalSets == 0) 0f else session.completedSets.toFloat() / session.totalSets,
            size = 44.dp,
            stroke = 5.dp,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

@Composable
private fun ElapsedClock(timer: SessionTimer, now: () -> ClockReading) {
    val elapsed = rememberTicker(timer, now, frames = false) { SessionTiming.elapsedMs(timer, it) }
    Text(formatClock(elapsed.longValue), style = NumberStyleMedium, color = MaterialTheme.colorScheme.onSurface)
}

/**
 * Reloj de UI basado en el ancla persistida (nunca en contar recomposiciones). Solo corre con la pantalla al
 * menos STARTED. [frames]: cada frame (aro suave) o alineado al segundo (texto y movimiento reducido).
 */
@Composable
private fun rememberTicker(
    timer: SessionTimer,
    now: () -> ClockReading,
    frames: Boolean,
    compute: (ClockReading) -> Long,
): androidx.compose.runtime.MutableLongState {
    val value = remember { mutableLongStateOf(compute(now())) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(timer, frames) {
        value.longValue = compute(now())
        if (!timer.running) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                val reading = now()
                value.longValue = compute(reading)
                if (frames) {
                    androidx.compose.runtime.withFrameMillis { }
                } else {
                    delay(1_000 - (SessionTiming.elapsedMs(timer, reading) % 1_000))
                }
            }
        }
    }
    return value
}

private val ThumbnailSize = 64.dp
private val ThumbnailShape = RoundedCornerShape(14.dp)
private val SetMarkerWidth = 44.dp

@Composable
private fun ExerciseSection(
    exercise: SessionExercise,
    index: Int,
    count: Int,
    nextName: String?,
    state: SessionUiState,
    vm: SessionViewModel,
    onOpenGuide: (String) -> Unit,
) {
    val guide = ExerciseCatalog.byId(exercise.catalogId)
    // «Anterior»: null mientras carga (sin parpadeo de «Sin registro anterior»).
    val previous by remember(exercise.id, exercise.key, exercise.catalogId, exercise.assistMode) { vm.previous(exercise) }
        .collectAsStateWithLifecycle(initialValue = null)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.session_exercise_position, index + 1, count).uppercase(),
                style = OverlineStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            GlassIconButton(R.drawable.ic_chevron_left, stringResource(R.string.session_prev_exercise), { vm.goToExercise(index - 1) }, enabled = index > 0)
            Spacer(Modifier.width(Spacing.s))
            GlassIconButton(R.drawable.ic_chevron_right, stringResource(R.string.session_next_exercise), { vm.goToExercise(index + 1) }, enabled = index < count - 1)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (guide != null) {
                ExerciseAssetImage(
                    path = guide.mediaAssets.thumbnail,
                    contentDescription = null,
                    opaque = true,
                    modifier = Modifier.size(ThumbnailSize).clip(ThumbnailShape),
                )
            } else {
                // Personalizado: sin foto de otro ejercicio.
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ThumbnailSize).editorGlass(ThumbnailShape)) {
                    Icon(
                        painterResource(R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.v1_custom_no_image),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
                Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                if (guide != null) {
                    Text(guide.metaLine(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(Spacing.s))
            // La ficha es una capa local: no toca el temporizador ni el descanso del ViewModel. Personalizados
            // (G2.1): ficha sin imágenes, con su progreso por exerciseKey.
            GlassIconButton(
                R.drawable.ic_info,
                stringResource(R.string.session_open_guide, exercise.name),
                { onOpenGuide(guide?.id ?: customGuideToken(exercise.key)) },
            )
        }
        val assistance = guide?.loadSemantics == LoadSemantics.ASSISTANCE
        if (assistance) {
            AssistModeSelector(exercise.assistMode, onChange = { vm.setAssistMode(exercise.id, it) })
        }
        if (exercise.notes.isNotBlank()) {
            Text(exercise.notes, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        previous?.let { ref ->
            val text = ref.session?.let { stringResource(R.string.v2_previous_session, formatShortDate(it.endedAt)) }
                ?: stringResource(R.string.v2_previous_none)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_history), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (ref.session == null && assistance && exercise.assistMode == null) {
                    Text(stringResource(R.string.v2_previous_assist_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        SetList(exercise, state, previous?.session, vm)
        GlassButton(stringResource(R.string.session_add_set), R.drawable.ic_add, { vm.addSet(exercise.id) })
        if (nextName != null) {
            GlassButton(
                stringResource(R.string.session_next_named, nextName),
                R.drawable.ic_chevron_right,
                { vm.goToExercise(index + 1) },
                Modifier.fillMaxWidth(),
                centered = true,
            )
        }
    }
}

@Composable
private fun SetList(exercise: SessionExercise, state: SessionUiState, previous: PreviousSession?, vm: SessionViewModel) {
    // Serie actual: la primera sin completar del ejercicio visible.
    val currentId = exercise.sets.firstOrNull { !it.completed }?.id
    BoxWithConstraints {
        // Fuente grande o ancho estrecho: cada serie en dos líneas con etiquetas (como el editor V1).
        val stacked = maxWidth / LocalDensity.current.fontScale < 300.dp
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            if (!stacked) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                ) {
                    HeaderLabel(stringResource(R.string.session_col_set), Modifier.width(SetMarkerWidth))
                    HeaderLabel(state.unit.loadUnit(), Modifier.weight(1f))
                    HeaderLabel(stringResource(R.string.session_col_reps), Modifier.weight(1f))
                    HeaderLabel(stringResource(R.string.session_col_done), Modifier.width(MinTouchTarget))
                }
            }
            exercise.sets.forEachIndexed { i, set ->
                val ordinal = exercise.sets.take(i).count { it.warmup == set.warmup }
                SessionSetRow(
                    number = i + 1,
                    exerciseName = exercise.name,
                    set = set,
                    current = set.id == currentId,
                    previous = previous?.let { PreviousSlot(it.counterpart(set.warmup, ordinal)) },
                    draft = state.draftFor(set),
                    unit = state.unit,
                    pending = set.id in state.pending,
                    failed = set.id in state.failed,
                    showErrors = set.id in state.attempted,
                    stacked = stacked,
                    onReps = { vm.onRepsChange(set, it) },
                    onLoad = { vm.onLoadChange(set, it) },
                    onToggle = { if (set.completed) vm.uncomplete(set) else vm.complete(set) },
                )
            }
        }
    }
}

/** Hueco de la referencia en una fila: [set] null = la sesión anterior no tuvo serie equivalente. */
private class PreviousSlot(val set: PreviousSet?)

/** «40 kg × 10», «12 reps» o «0 kg × 8»: la carga 0 registrada se muestra; solo la ausente se omite. */
@Composable
private fun previousText(set: PreviousSet, unit: UnitSystem): String? {
    val load = formatExplicitLoad(set.loadKg, unit)
    val reps = set.reps
    return when {
        load != null && reps != null -> stringResource(R.string.v2_previous_load_reps, load, reps)
        reps != null -> pluralStringResource(R.plurals.v2_previous_reps, reps, reps)
        else -> load
    }
}

/**
 * Fila de serie sobre el fondo. Estados con texto e icono, no solo color: «Serie actual» (barra lateral y
 * número destacado), «Pendiente», «Hecha» (check relleno) y «Calentamiento» (C). M06: el check se rellena solo
 * cuando Room confirma la serie (160 ms; desmarcar 140 ms) con vibración breve. Los campos siguen editables.
 */
@Composable
private fun SessionSetRow(
    number: Int,
    exerciseName: String,
    set: PerformedSet,
    current: Boolean,
    previous: PreviousSlot?,
    draft: SetDraft,
    unit: UnitSystem,
    pending: Boolean,
    failed: Boolean,
    showErrors: Boolean,
    stacked: Boolean,
    onReps: (String) -> Unit,
    onLoad: (String) -> Unit,
    onToggle: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var wasCompleted by remember { mutableStateOf(set.completed) }
    LaunchedEffect(set.completed) {
        // Vibración breve del sistema solo en la transición a completada (no al cargar ni al rotar).
        if (set.completed && !wasCompleted) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        wasCompleted = set.completed
    }
    val loadInvalid = draft.loadValue() is Parsed.Invalid
    val repsInvalid = draft.repsValue() == null
    val loadDesc = stringResource(R.string.session_set_field, number, exerciseName, unit.loadUnit())
    val repsDesc = stringResource(R.string.session_set_field, number, exerciseName, stringResource(R.string.session_col_reps))
    val stateText = stringResource(
        when {
            set.completed -> R.string.v2_state_done
            current -> R.string.v2_state_current
            else -> R.string.v2_state_pending
        },
    )
    val caption = buildList {
        if (set.warmup) add(stringResource(R.string.v2_state_warmup))
        add(stateText)
        previous?.let { slot ->
            add(slot.set?.let { p -> previousText(p, unit)?.let { stringResource(R.string.v2_previous, it) } } ?: stringResource(R.string.v2_previous_no_counterpart))
        }
    }.joinToString(" · ")

    val marker: @Composable (Modifier) -> Unit = { m -> SetMarker(number, set.warmup, current, set.completed, m) }
    val load: @Composable (Modifier) -> Unit = { m ->
        NumberCell(
            value = draft.load.text,
            onValueChange = onLoad,
            description = loadDesc,
            modifier = m.editorGlass(),
            decimal = true,
            isError = loadInvalid && (showErrors || draft.load.text.isNotEmpty()),
            placeholder = formatExplicitLoad(set.targetLoadKg, unit)?.substringBefore(' ') ?: "—",
            background = Color.Transparent,
            shape = GlassShape,
        )
    }
    val reps: @Composable (Modifier) -> Unit = { m ->
        NumberCell(
            value = draft.reps,
            onValueChange = onReps,
            description = repsDesc,
            modifier = m.editorGlass(),
            isError = repsInvalid && (showErrors || draft.reps.isNotEmpty()),
            placeholder = set.targetReps?.toString().orEmpty(),
            imeAction = ImeAction.Done,
            background = Color.Transparent,
            shape = GlassShape,
        )
    }
    val captionColor = if (current && !set.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (!stacked) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                marker(Modifier.width(SetMarkerWidth))
                load(Modifier.weight(1f))
                reps(Modifier.weight(1f))
                SetCheck(number, set.completed, pending, onToggle)
            }
            Text(caption, style = MaterialTheme.typography.bodyMedium, color = captionColor, modifier = Modifier.padding(start = SetMarkerWidth + Spacing.s))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                marker(Modifier.width(SetMarkerWidth))
                Spacer(Modifier.width(Spacing.s))
                Text(caption, style = MaterialTheme.typography.bodyMedium, color = captionColor, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.Bottom) {
                LabeledCell(unit.loadUnit(), Modifier.weight(1f)) { load(Modifier.fillMaxWidth()) }
                LabeledCell(stringResource(R.string.session_col_reps), Modifier.weight(1f)) { reps(Modifier.fillMaxWidth()) }
                SetCheck(number, set.completed, pending, onToggle)
            }
        }
        // Un fallo solo se muestra mientras la serie sigue sin completar en Room (nunca junto al check).
        if ((failed && !set.completed) || (showErrors && (repsInvalid || loadInvalid))) {
            InlineMessage(
                visible = true,
                text = stringResource(
                    when {
                        failed && !set.completed -> R.string.session_set_failed
                        repsInvalid -> R.string.error_reps
                        else -> R.string.error_load
                    },
                ),
                modifier = Modifier.padding(start = SetMarkerWidth + Spacing.s),
            )
        }
    }
}

/** Número de serie (o «C»): la actual lleva barra champán y número destacado; la hecha se atenúa. */
@Composable
private fun SetMarker(number: Int, warmup: Boolean, current: Boolean, completed: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val highlight = current && !completed
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .clearAndSetSemantics { }
            .drawBehind {
                if (highlight) {
                    val w = 3.dp.toPx()
                    drawRoundRect(primary, topLeft = Offset(0f, size.height * 0.2f), size = Size(w, size.height * 0.6f), cornerRadius = CornerRadius(w / 2))
                }
            },
    ) {
        Text(
            if (warmup) stringResource(R.string.editor_warmup_short) else "$number",
            style = if (highlight) NumberStyleMedium else NumberStyleCell,
            color = when {
                completed -> MaterialTheme.colorScheme.onSurfaceVariant
                warmup || highlight -> primary
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

/**
 * Botón de completar de 48 dp, separado de los campos: cristal con check visible en contorno; al confirmarse en
 * Room se rellena de champán (160 ms; desmarcar 140 ms). Movimiento reducido: sin interpolación.
 */
@Composable
private fun SetCheck(number: Int, completed: Boolean, pending: Boolean, onToggle: () -> Unit) {
    val motion = LocalMotionEnabled.current
    val fill by animateFloatAsState(
        targetValue = if (completed) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (!motion) 0 else if (completed) 160 else 140,
            easing = MotionTokens.EaseOut,
        ),
        label = "setCheck",
    )
    val primary = MaterialTheme.colorScheme.primary
    val tint = lerp(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.onPrimary, fill)
    val action = stringResource(if (completed) R.string.session_uncomplete_set else R.string.session_complete_set, number)
    val stateText = stringResource(if (completed) R.string.session_set_completed else R.string.session_set_pending)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(MinTouchTarget)
            .alpha(if (pending) 0.6f else 1f)
            .editorGlass(CircleShape)
            .toggleable(value = completed, enabled = !pending, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics {
                contentDescription = action
                stateDescription = stateText
            },
    ) {
        if (fill > 0f) {
            Canvas(Modifier.size(MinTouchTarget)) {
                drawCircle(primary, radius = (size.minDimension / 2) * (0.6f + 0.4f * fill), alpha = fill)
            }
        }
        Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/**
 * Descanso integrado (M08) sobre el fondo, entre el contenido y las acciones: no tapa la edición. Mismo reloj
 * persistido (sobrevive al cierre del proceso). El aro se vacía de forma lineal según el deadline; con movimiento
 * reducido se actualiza una vez por segundo. Al llegar a cero cambia a «Descanso terminado».
 */
@Composable
private fun RestPanel(session: Session, vm: SessionViewModel, modifier: Modifier = Modifier, divider: Boolean = false) {
    val timer = session.timer
    val motion = LocalMotionEnabled.current
    AnimatedVisibility(
        visible = timer.hasRest,
        enter = fadeIn(tween(if (motion) 160 else MotionTokens.REDUCED_FADE_MS)),
        exit = fadeOut(tween(if (motion) 140 else MotionTokens.REDUCED_FADE_MS)),
        modifier = modifier,
    ) {
        val total = timer.restTotalMs ?: 1L
        val remaining = rememberTicker(timer, vm::now, frames = motion) { SessionTiming.restRemainingMs(timer, it) ?: 0L }
        val seconds by remember { derivedStateOf { (remaining.longValue + 999) / 1_000 } }
        val done = seconds == 0L
        val labelColor by animateColorAsState(
            targetValue = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(160),
            label = "restDone",
        )
        val track = FitnessThemeExtras.colors.surfaceRaised
        val fill = MaterialTheme.colorScheme.primary
        val countdown = formatCountdown(seconds * 1_000)
        val description = stringResource(R.string.rest_description, countdown)
        // El aro crece con la fuente grande para que la cuenta atrás no se salga.
        val ringSize = 76.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
        Column {
            if (divider) HorizontalDivider(color = FitnessThemeExtras.colors.surfaceRaised)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.m)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(ringSize)
                        .clearAndSetSemantics { contentDescription = if (done) "" else description },
                ) {
                    Canvas(Modifier.size(ringSize)) {
                        val w = 5.dp.toPx()
                        val arcSize = Size(size.width - w, size.height - w)
                        val topLeft = Offset(w / 2, w / 2)
                        drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(w))
                        // Lectura del tiempo confinada al dibujo: el aro avanza sin recomponer la pantalla.
                        val fraction = (remaining.longValue.toFloat() / total).coerceIn(0f, 1f)
                        if (fraction > 0f) drawArc(fill, -90f, 360f * fraction, false, topLeft, arcSize, style = Stroke(w, cap = StrokeCap.Round))
                    }
                    Text(countdown, style = NumberStyleCell.copy(fontSize = 20.sp), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.width(Spacing.l))
                Column(Modifier.weight(1f)) {
                    Text(
                        (if (done) stringResource(R.string.rest_done) else stringResource(R.string.rest_title)).uppercase(),
                        style = OverlineStyle,
                        color = labelColor,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Spacer(Modifier.height(Spacing.s))
                    BoxWithConstraints {
                        val narrow = maxWidth / LocalDensity.current.fontScale < 260.dp
                        if (done) {
                            GlassButton(stringResource(R.string.action_close), R.drawable.ic_close, vm::skipRest)
                        } else if (narrow) {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                RestAdjustRow(vm)
                                GlassButton(stringResource(R.string.rest_skip), R.drawable.ic_skip, vm::skipRest)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                                RestAdjustRow(vm)
                                GlassButton(stringResource(R.string.rest_skip), R.drawable.ic_skip, vm::skipRest)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RestAdjustRow(vm: SessionViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        RestChip(stringResource(R.string.rest_minus), stringResource(R.string.rest_minus_description)) { vm.adjustRest(-15) }
        RestChip(stringResource(R.string.rest_plus), stringResource(R.string.rest_plus_description)) { vm.adjustRest(15) }
    }
}

/** ±15 s de cristal: el texto es corto y el lector anuncia la acción completa. */
@Composable
private fun RestChip(text: String, description: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = MinTouchTarget)
            .editorGlass()
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick { onClick(); true }
            }
            .padding(horizontal = Spacing.m),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SessionDialogs(state: SessionUiState, vm: SessionViewModel) {
    val session = state.session ?: return
    when (state.dialog) {
        SessionDialog.NONE -> Unit
        SessionDialog.FINISH -> FitnessDialog(
            title = stringResource(R.string.session_finish_title),
            body = stringResource(R.string.session_finish_body, session.completedSets, session.totalSets),
            confirmLabel = stringResource(R.string.session_finish_confirm),
            onConfirm = vm::confirmFinish,
            dismissLabel = stringResource(R.string.session_keep),
            onDismiss = vm::dismissDialog,
        )
        SessionDialog.FINISH_EMPTY -> FitnessDialog(
            title = stringResource(R.string.session_finish_empty_title),
            body = stringResource(R.string.session_finish_empty_body),
            confirmLabel = stringResource(R.string.session_discard),
            onConfirm = vm::confirmDiscard,
            dismissLabel = stringResource(R.string.session_keep),
            onDismiss = vm::dismissDialog,
            destructive = true,
        )
        SessionDialog.INVALID_DRAFTS -> FitnessDialog(
            title = stringResource(R.string.session_invalid_title),
            body = stringResource(R.string.session_invalid_body),
            confirmLabel = stringResource(R.string.session_invalid_discard),
            onConfirm = vm::discardInvalidDrafts,
            dismissLabel = stringResource(R.string.session_invalid_review),
            onDismiss = vm::dismissDialog,
        )
        SessionDialog.DISCARD -> FitnessDialog(
            title = stringResource(R.string.session_discard_title),
            body = stringResource(R.string.session_discard_body),
            confirmLabel = stringResource(R.string.session_discard_confirm),
            onConfirm = vm::confirmDiscard,
            dismissLabel = stringResource(R.string.session_keep),
            onDismiss = vm::dismissDialog,
            destructive = true,
        )
    }
}
