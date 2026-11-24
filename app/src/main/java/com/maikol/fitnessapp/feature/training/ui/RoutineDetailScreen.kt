package com.maikol.fitnessapp.feature.training.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.LoadingRegion
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.feature.training.data.PlannedSet
import com.maikol.fitnessapp.feature.training.data.Routine
import com.maikol.fitnessapp.feature.training.data.RoutineExercise
import com.maikol.fitnessapp.feature.training.data.formatLoad

@Composable
fun RoutineDetailRoute(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenRoutine: (String) -> Unit,
    viewModel: RoutineDetailViewModel = viewModel(factory = RoutineDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val suffix = stringResource(R.string.duplicate_suffix)
    LaunchedEffect(state.sessionToOpen) {
        state.sessionToOpen?.let {
            viewModel.onSessionOpened()
            onOpenSession(it)
        }
    }
    LoadingRegion(isLoading = state.loading, message = stringResource(R.string.preparing_workout), modifier = Modifier.fillMaxSize()) {
        val routine = state.routine
        if (routine == null) {
            Column(Modifier.fillMaxSize()) {
                TrainingTopBar(title = "", onBack = onBack)
                Text(stringResource(R.string.detail_not_found), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(Spacing.l))
            }
            return@LoadingRegion
        }
        RoutineDetailScreen(
            state = state,
            routine = routine,
            onBack = onBack,
            onStart = { if (state.openSession != null) onOpenSession(state.openSession!!.id) else viewModel.start() },
            menu = buildList {
                add(MenuAction(R.string.action_edit, R.drawable.ic_edit) { onEdit(routine.id) })
                if (!routine.archived) {
                    add(
                        MenuAction(
                            if (state.isNext) R.string.action_unset_next else R.string.action_set_next,
                            if (state.isNext) R.drawable.ic_bookmark else R.drawable.ic_bookmark_filled,
                        ) { viewModel.toggleNext() },
                    )
                }
                add(MenuAction(R.string.action_duplicate, R.drawable.ic_copy) { viewModel.duplicate(suffix, onOpenRoutine) })
                if (routine.archived) add(MenuAction(R.string.action_restore, R.drawable.ic_unarchive) { viewModel.setArchived(false) })
                else add(MenuAction(R.string.action_archive, R.drawable.ic_archive) { viewModel.setArchived(true) })
            },
        )
    }
}

@Composable
internal fun RoutineDetailScreen(
    state: DetailUiState,
    routine: Routine,
    onBack: () -> Unit,
    onStart: () -> Unit,
    menu: List<MenuAction>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                TrainingHero(
                    title = routine.name,
                    onBack = onBack,
                    minHeight = 236.dp,
                    actions = { OverflowMenu(description = stringResource(R.string.training_routine_actions, routine.name), actions = menu) },
                    below = {
                        Spacer(Modifier.height(Spacing.s))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            Text(
                                countsLabel(routine.exercises.size, routine.setCount),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            )
                            if (state.isNext) Pill(stringResource(R.string.detail_next_selected), iconRes = R.drawable.ic_bookmark_filled)
                        }
                    },
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
                    modifier = Modifier.padding(horizontal = Spacing.l).padding(top = Spacing.l, bottom = Spacing.xl),
                ) {
                    if (routine.notes.isNotBlank()) {
                        Text(routine.notes, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (routine.archived) {
                        InlineMessage(visible = true, text = stringResource(R.string.detail_archived), iconRes = R.drawable.ic_archive, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    routine.exercises.forEachIndexed { i, exercise -> ExercisePlanCard(i + 1, exercise, state.unit) }
                }
            }
            StatusBarScrim()
        }
        BottomActionBar {
            Column(Modifier.weight(1f)) {
                InlineMessage(visible = state.actionFailed, text = stringResource(R.string.training_action_failed), modifier = Modifier.padding(bottom = Spacing.s))
                if (state.blockedByOtherSession) {
                    InlineMessage(
                        visible = true,
                        text = stringResource(R.string.detail_other_session, state.openSession?.routineName.orEmpty()),
                        iconRes = R.drawable.ic_timer,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.s),
                    )
                }
                PrimaryButton(
                    text = stringResource(if (state.openSession != null) R.string.training_continue else R.string.detail_start),
                    onClick = onStart,
                    busy = state.starting,
                    enabled = !routine.archived || state.openSession != null,
                    iconRes = R.drawable.ic_play,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Ejercicio del plan: número, nombre, notas y series agrupadas con su descanso. Sin fotos por fila. */
@Composable
private fun ExercisePlanCard(number: Int, exercise: RoutineExercise, unit: UnitSystem) {
    FitnessCard {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp).background(champagneTint(0.14f), MaterialTheme.shapes.small),
            ) {
                Text("$number", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                if (exercise.notes.isNotBlank()) {
                    Text(exercise.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(Spacing.s))
                exercise.sets.grouped().forEach { group -> SetGroupLine(group, unit) }
                restLabel(exercise.sets)?.let {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(stringResource(R.string.detail_rest, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SetGroupLine(group: SetGroup, unit: UnitSystem) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            "${group.count} × ${group.reps}",
            style = NumberStyleCell.copy(textAlign = TextAlign.Start),
            color = if (group.warmup) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            " · " + (formatLoad(group.loadKg, unit) ?: stringResource(R.string.bodyweight)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (group.warmup) {
            Spacer(Modifier.width(Spacing.s))
            Pill(stringResource(R.string.summary_warmup), filled = false)
        }
    }
}

/** «90 s» si todas las series descansan igual; «60–90 s» si varían; null sin descanso. */
private fun restLabel(sets: List<PlannedSet>): String? {
    val rests = sets.map { it.restSeconds }.filter { it > 0 }
    if (rests.isEmpty()) return null
    val min = rests.min()
    val max = rests.max()
    return if (min == max) "$min s" else "$min–$max s"
}

@Preview(name = "Detalle DEMO", showBackground = true, backgroundColor = 0xFF121416, widthDp = 393, heightDp = 1000)
@Composable
private fun DetailPreview() {
    val routine = Routine(
        "r", "Tren superior DEMO", "Enfoque en técnica.", false,
        listOf(
            RoutineExercise("e1", "k1", "Press con mancuernas", "", listOf(PlannedSet("s0", 12, 10.0, 60, true), PlannedSet("s1", 10, 24.0, 90, false), PlannedSet("s2", 10, 24.0, 90, false))),
            RoutineExercise("e2", "k2", "Dominadas", "Agarre prono", listOf(PlannedSet("s3", 8, null, 120, false), PlannedSet("s4", 8, null, 120, false))),
        ),
    )
    FitnessTheme { RoutineDetailScreen(DetailUiState(loading = false, routine = routine, isNext = true), routine, {}, {}, emptyList()) }
}
