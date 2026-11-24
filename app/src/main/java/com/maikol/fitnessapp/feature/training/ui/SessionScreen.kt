package com.maikol.fitnessapp.feature.training.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.database.SessionStatus
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.LoadingRegion
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.component.SecondaryButton
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.motion.MotionTokens
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.feature.training.data.ClockReading
import com.maikol.fitnessapp.feature.training.data.PerformedSet
import com.maikol.fitnessapp.feature.training.data.Session
import com.maikol.fitnessapp.feature.training.data.SessionExercise
import com.maikol.fitnessapp.feature.training.data.SessionTimer
import com.maikol.fitnessapp.feature.training.data.SessionTiming
import com.maikol.fitnessapp.feature.training.data.formatClock
import com.maikol.fitnessapp.feature.training.data.formatCountdown
import com.maikol.fitnessapp.feature.training.data.formatLoad
import com.maikol.fitnessapp.feature.training.data.loadUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun SessionRoute(
    onBack: () -> Unit,
    onFinished: (sessionId: String) -> Unit,
    onOpenSummary: (sessionId: String) -> Unit,
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
        SessionScreen(state, session, viewModel, onBack)
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

@Composable
private fun SessionScreen(state: SessionUiState, session: Session, vm: SessionViewModel, onBack: () -> Unit) {
    // Atrás deja la sesión abierta y recuperable; no la pausa ni la descarta.
    BackHandler(onBack = onBack)
    val exerciseIndex = session.currentExercise.coerceIn(0, (session.exercises.size - 1).coerceAtLeast(0))
    val exercise = session.exercises.getOrNull(exerciseIndex)
    val uncertain = remember(session.timer) { SessionTiming.delta(session.timer, vm.now()).uncertain }
    // Poca altura (horizontal): el descanso pasa al contenido desplazable para no tapar la tabla de series.
    val compactHeight = LocalConfiguration.current.screenHeightDp < 520

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
            verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
        ) {
            SessionHeader(session, vm::now)
            if (compactHeight) RestDock(session, vm, Modifier.clip(MaterialTheme.shapes.large))
            if (uncertain) {
                FitnessCard {
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
                    state = state,
                    vm = vm,
                )
            }
        }
        if (!compactHeight) RestDock(session, vm)
        BottomActionBar {
            if (session.isPaused) {
                SecondaryButton(text = stringResource(R.string.session_resume), onClick = vm::resume, iconRes = R.drawable.ic_play, modifier = Modifier.weight(1f))
            } else {
                SecondaryButton(text = stringResource(R.string.session_pause), onClick = vm::pause, iconRes = R.drawable.ic_pause, modifier = Modifier.weight(1f))
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

/** Tiempo efectivo grande (se actualiza una vez por segundo solo aquí) y aro de progreso M07. */
@Composable
private fun SessionHeader(session: Session, now: () -> ClockReading) {
    FitnessCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.session_elapsed).uppercase(), style = OverlineStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.xs))
                ElapsedClock(session.timer, now)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    stringResource(R.string.session_progress, session.completedSets, session.totalSets),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            val description = stringResource(R.string.session_progress_description, session.completedSets, session.totalSets)
            ProgressRing(
                progress = if (session.totalSets == 0) 0f else session.completedSets.toFloat() / session.totalSets,
                size = 84.dp,
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
            ) {
                Text("${session.completedSets}/${session.totalSets}", style = NumberStyleCell, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun ElapsedClock(timer: SessionTimer, now: () -> ClockReading) {
    val elapsed = rememberTicker(timer, now, frames = false) { SessionTiming.elapsedMs(timer, it) }
    Text(formatClock(elapsed.longValue), style = NumberStyleLarge, color = MaterialTheme.colorScheme.onSurface)
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

@Composable
private fun ExerciseSection(exercise: SessionExercise, index: Int, count: Int, state: SessionUiState, vm: SessionViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Spacing.s)) {
            Text(
                stringResource(R.string.session_exercise_position, index + 1, count).uppercase(),
                style = OverlineStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { vm.goToExercise(index - 1) }, enabled = index > 0) {
                Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.session_prev_exercise))
            }
            IconButton(onClick = { vm.goToExercise(index + 1) }, enabled = index < count - 1) {
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(R.string.session_next_exercise))
            }
        }
        Text(exercise.name, style = MaterialTheme.typography.headlineMedium)
        if (exercise.notes.isNotBlank()) {
            Text(exercise.notes, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SetTable(exercise, state, vm)
        TextButton(onClick = { vm.addSet(exercise.id) }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(stringResource(R.string.session_add_set), style = MaterialTheme.typography.labelLarge)
        }
        if (index < count - 1) {
            SecondaryButton(
                text = stringResource(R.string.session_next_named, state.session?.exercises?.getOrNull(index + 1)?.name.orEmpty()),
                onClick = { vm.goToExercise(index + 1) },
                iconRes = R.drawable.ic_chevron_right,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val SetNumberWidth = 44.dp

@Composable
private fun SetTable(exercise: SessionExercise, state: SessionUiState, vm: SessionViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(FitnessThemeExtras.colors.card)
            .padding(vertical = Spacing.s),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = Spacing.m, vertical = Spacing.xs)
                .semantics(mergeDescendants = true) {},
        ) {
            HeaderLabel(stringResource(R.string.session_col_set), Modifier.width(SetNumberWidth))
            HeaderLabel(state.unit.loadUnit(), Modifier.weight(1f))
            HeaderLabel(stringResource(R.string.session_col_reps), Modifier.weight(1f))
            HeaderLabel(stringResource(R.string.session_col_done), Modifier.width(MinTouchTarget))
        }
        exercise.sets.forEachIndexed { i, set ->
            SessionSetRow(
                number = i + 1,
                exerciseName = exercise.name,
                set = set,
                draft = state.draftFor(set),
                unit = state.unit,
                pending = set.id in state.pending,
                failed = set.id in state.failed,
                showErrors = set.id in state.attempted,
                onReps = { vm.onRepsChange(set, it) },
                onLoad = { vm.onLoadChange(set, it) },
                onToggle = { if (set.completed) vm.uncomplete(set) else vm.complete(set) },
            )
        }
    }
}

/**
 * Fila de serie. M06: solo cuando Room confirma la serie, el fondo pasa a champán 8 % (180 ms) y la casilla
 * se rellena con el check (160 ms); desmarcar revierte en 140 ms. Sin saltos de fila, zoom ni confeti.
 * Los campos siguen editables y su posición no cambia al escribir.
 */
@Composable
private fun SessionSetRow(
    number: Int,
    exerciseName: String,
    set: PerformedSet,
    draft: SetDraft,
    unit: UnitSystem,
    pending: Boolean,
    failed: Boolean,
    showErrors: Boolean,
    onReps: (String) -> Unit,
    onLoad: (String) -> Unit,
    onToggle: () -> Unit,
) {
    val motion = LocalMotionEnabled.current
    val rowColor by animateColorAsState(
        targetValue = if (set.completed) champagneTint() else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(if (motion) 180 else MotionTokens.REDUCED_FADE_MS),
        label = "setRow",
    )
    val haptics = LocalHapticFeedback.current
    var wasCompleted by remember { androidx.compose.runtime.mutableStateOf(set.completed) }
    LaunchedEffect(set.completed) {
        // Vibración breve del sistema solo en la transición a completada (no al cargar ni al rotar).
        if (set.completed && !wasCompleted) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        wasCompleted = set.completed
    }
    val loadInvalid = draft.loadValue() is com.maikol.fitnessapp.feature.training.data.Parsed.Invalid
    val repsInvalid = draft.repsValue() == null
    val loadDesc = stringResource(R.string.session_set_field, number, exerciseName, unit.loadUnit())
    val repsDesc = stringResource(R.string.session_set_field, number, exerciseName, stringResource(R.string.session_col_reps))
    Column(Modifier.background(rowColor)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs + 2.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(SetNumberWidth)) {
                Text(
                    if (set.warmup) stringResource(R.string.editor_warmup_short) else "$number",
                    style = NumberStyleCell,
                    color = if (set.warmup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            NumberCell(
                value = draft.load.text,
                onValueChange = onLoad,
                description = loadDesc,
                decimal = true,
                isError = loadInvalid && (showErrors || draft.load.text.isNotEmpty()),
                placeholder = formatLoad(set.targetLoadKg, unit)?.substringBefore(' ') ?: "—",
                background = if (set.completed) FitnessThemeExtras.colors.surfaceRaised.copy(alpha = 0.7f) else FitnessThemeExtras.colors.surfaceRaised,
                modifier = Modifier.weight(1f),
            )
            NumberCell(
                value = draft.reps,
                onValueChange = onReps,
                description = repsDesc,
                isError = repsInvalid && (showErrors || draft.reps.isNotEmpty()),
                placeholder = set.targetReps?.toString().orEmpty(),
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                background = if (set.completed) FitnessThemeExtras.colors.surfaceRaised.copy(alpha = 0.7f) else FitnessThemeExtras.colors.surfaceRaised,
                modifier = Modifier.weight(1f),
            )
            SetCheck(number, set.completed, pending, onToggle)
        }
        if (failed || (showErrors && (repsInvalid || loadInvalid))) {
            InlineMessage(
                visible = true,
                text = stringResource(
                    when {
                        failed -> R.string.session_set_failed
                        repsInvalid -> R.string.error_reps
                        else -> R.string.error_load
                    },
                ),
                modifier = Modifier.padding(start = Spacing.m, end = Spacing.m, bottom = Spacing.s),
            )
        }
    }
}

/** Casilla circular de 48 dp: contorno → relleno champán con check (160 ms), desmarcar 140 ms. */
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
    val outline = MaterialTheme.colorScheme.outline
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val action = stringResource(if (completed) R.string.session_uncomplete_set else R.string.session_complete_set, number)
    val stateText = stringResource(if (completed) R.string.session_set_completed else R.string.session_set_pending)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(MinTouchTarget)
            .toggleable(value = completed, enabled = !pending, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics {
                contentDescription = action
                stateDescription = stateText
            },
    ) {
        Canvas(Modifier.size(32.dp)) {
            val stroke = 2.dp.toPx()
            drawCircle(outline, radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke), alpha = 1f - fill)
            if (fill > 0f) drawCircle(primary, radius = (size.minDimension / 2) * (0.6f + 0.4f * fill), alpha = fill)
        }
        Icon(
            painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = onPrimary,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { alpha = fill },
        )
    }
}

/**
 * Descanso integrado (M08) entre el contenido y la barra de acciones: no tapa la edición. El aro se vacía
 * de forma lineal según el deadline; con movimiento reducido se actualiza una vez por segundo. Al llegar a
 * cero cambia a «Descanso terminado» y destaca una sola vez (160 ms).
 */
@Composable
private fun RestDock(session: Session, vm: SessionViewModel, modifier: Modifier = Modifier) {
    val timer = session.timer
    val motion = LocalMotionEnabled.current
    AnimatedVisibility(
        visible = timer.hasRest,
        enter = fadeIn(tween(if (motion) 160 else MotionTokens.REDUCED_FADE_MS)),
        exit = fadeOut(tween(if (motion) 140 else MotionTokens.REDUCED_FADE_MS)),
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .background(FitnessThemeExtras.colors.card)
                .padding(horizontal = Spacing.l, vertical = Spacing.m),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(92.dp)
                    .clearAndSetSemantics {
                        contentDescription = if (done) "" else description
                    },
            ) {
                Canvas(Modifier.size(92.dp)) {
                    val w = 6.dp.toPx()
                    val arcSize = Size(size.width - w, size.height - w)
                    val topLeft = Offset(w / 2, w / 2)
                    drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(w))
                    // Lectura del tiempo confinada al dibujo: el aro avanza sin recomponer la pantalla.
                    val fraction = (remaining.longValue.toFloat() / total).coerceIn(0f, 1f)
                    if (fraction > 0f) drawArc(fill, -90f, 360f * fraction, false, topLeft, arcSize, style = Stroke(w, cap = StrokeCap.Round))
                }
                Text(countdown, style = NumberStyleMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
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
                    val narrow = maxWidth / LocalDensity.current.fontScale < 200.dp
                    if (done) {
                        SecondaryButton(text = stringResource(R.string.action_close), onClick = vm::skipRest, iconRes = R.drawable.ic_close)
                    } else if (narrow) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            RestAdjustRow(vm)
                            SecondaryButton(text = stringResource(R.string.rest_skip), onClick = vm::skipRest, iconRes = R.drawable.ic_skip)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                            RestAdjustRow(vm)
                            SecondaryButton(text = stringResource(R.string.rest_skip), onClick = vm::skipRest, iconRes = R.drawable.ic_skip)
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

@Composable
private fun RestChip(text: String, description: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = MinTouchTarget)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .toggleable(value = false, role = Role.Button, onValueChange = { onClick() })
            .semantics { contentDescription = description }
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
