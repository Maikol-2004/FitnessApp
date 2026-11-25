package com.maikol.fitnessapp.feature.training.ui

import com.maikol.fitnessapp.core.navigation.dockBottomPadding
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.FitnessCard
import com.maikol.fitnessapp.core.designsystem.component.LoadingRegion
import com.maikol.fitnessapp.core.designsystem.component.PhotoBackground
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.component.SecondaryButton
import com.maikol.fitnessapp.core.designsystem.motion.LocalMotionEnabled
import com.maikol.fitnessapp.core.designsystem.motion.pressScale
import com.maikol.fitnessapp.core.designsystem.theme.FitnessTheme
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.feature.training.data.HistoryEntry
import com.maikol.fitnessapp.feature.training.data.OpenSession
import com.maikol.fitnessapp.feature.training.data.RoutineSummary
import com.maikol.fitnessapp.feature.training.data.formatDuration
import com.maikol.fitnessapp.feature.training.data.formatSessionDate
import com.maikol.fitnessapp.feature.training.data.formatVolume

/** Acciones de navegación de la raíz de Entrenar. */
class TrainingNavigation(
    val createRoutine: () -> Unit,
    val openRoutine: (String) -> Unit,
    val editRoutine: (String) -> Unit,
    val openSession: (String) -> Unit,
    val openSummary: (String) -> Unit,
    val openHistory: () -> Unit,
)

@Composable
fun TrainingRoute(nav: TrainingNavigation, viewModel: TrainingViewModel = viewModel(factory = TrainingViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val suffix = stringResource(R.string.duplicate_suffix)
    LoadingRegion(isLoading = state.loading, message = stringResource(R.string.loading_data), modifier = Modifier.fillMaxSize()) {
        TrainingScreen(
            state = state,
            nav = nav,
            onDuplicate = { viewModel.duplicate(it, suffix) },
            onArchive = viewModel::archive,
            onRestore = viewModel::restore,
            onToggleNext = viewModel::toggleNext,
            onDismissError = viewModel::dismissError,
        )
    }
}

@Composable
fun TrainingScreen(
    state: TrainingUiState,
    nav: TrainingNavigation,
    onDuplicate: (String) -> Unit,
    onArchive: (String) -> Unit,
    onRestore: (String) -> Unit,
    onToggleNext: (String) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showArchived by rememberSaveable { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = Spacing.xl + dockBottomPadding()),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "hero") {
                TrainingHero(title = stringResource(R.string.destination_training), subtitle = stringResource(R.string.training_subtitle))
            }
            state.openSession?.let { open ->
                item(key = "open") {
                    OpenSessionCard(open, onContinue = { nav.openSession(open.id) }, modifier = Modifier.padding(horizontal = Spacing.l).padding(top = Spacing.l))
                }
            }
            item(key = "routines-header") {
                SectionHeader(
                    title = stringResource(R.string.training_routines_title),
                    modifier = Modifier.padding(start = Spacing.l, end = Spacing.l, top = Spacing.xl, bottom = Spacing.s),
                ) {
                    if (state.routines.isNotEmpty()) {
                        PrimaryButton(text = stringResource(R.string.training_create), onClick = nav.createRoutine, iconRes = R.drawable.ic_add)
                    }
                }
            }
            if (state.actionFailed) {
                item(key = "error") {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Spacing.l)) {
                        InlineMessage(visible = true, text = stringResource(R.string.training_action_failed), modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismissError) { Text(stringResource(R.string.action_close)) }
                    }
                }
            }
            if (state.routines.isEmpty()) {
                item(key = "empty") { EmptyRoutines(onCreate = nav.createRoutine, modifier = Modifier.padding(horizontal = Spacing.l)) }
            }
            items(state.routines, key = { "r-${it.id}" }) { routine ->
                RoutineCard(
                    routine = routine,
                    isNext = routine.id == state.nextRoutineId,
                    onOpen = { nav.openRoutine(routine.id) },
                    menu = listOf(
                        MenuAction(R.string.action_edit, R.drawable.ic_edit) { nav.editRoutine(routine.id) },
                        MenuAction(
                            if (routine.id == state.nextRoutineId) R.string.action_unset_next else R.string.action_set_next,
                            if (routine.id == state.nextRoutineId) R.drawable.ic_bookmark else R.drawable.ic_bookmark_filled,
                        ) { onToggleNext(routine.id) },
                        MenuAction(R.string.action_duplicate, R.drawable.ic_copy) { onDuplicate(routine.id) },
                        MenuAction(R.string.action_archive, R.drawable.ic_archive) { onArchive(routine.id) },
                    ),
                    modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.cardGap / 2),
                )
            }
            if (state.archived.isNotEmpty()) {
                item(key = "archived-header") {
                    ArchivedHeader(count = state.archived.size, expanded = showArchived, onToggle = { showArchived = !showArchived })
                }
                if (showArchived) {
                    items(state.archived, key = { "a-${it.id}" }) { routine ->
                        ArchivedRow(routine, onRestore = { onRestore(routine.id) }, modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xs))
                    }
                }
            }
            item(key = "history-header") {
                SectionHeader(
                    title = stringResource(R.string.training_history_title),
                    modifier = Modifier.padding(start = Spacing.l, end = Spacing.l, top = Spacing.xl, bottom = Spacing.s),
                )
            }
            if (state.recentHistory.isEmpty()) {
                item(key = "history-empty") {
                    Text(
                        stringResource(R.string.training_history_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.l),
                    )
                }
            }
            items(state.recentHistory, key = { "h-${it.id}" }) { entry ->
                HistoryRow(entry, state.unit, onClick = { nav.openSummary(entry.id) }, modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xs))
            }
            if (state.historyCount > 0) {
                item(key = "history-all") {
                    SecondaryButton(
                        text = stringResource(R.string.training_history_all),
                        onClick = nav.openHistory,
                        iconRes = R.drawable.ic_history,
                        modifier = Modifier.padding(horizontal = Spacing.l).padding(top = Spacing.m).fillMaxWidth(),
                    )
                }
            }
        }
        StatusBarScrim()
    }
}

internal class MenuAction(val labelRes: Int, @param:DrawableRes val iconRes: Int, val onClick: () -> Unit)

@Composable
private fun OpenSessionCard(open: OpenSession, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FitnessThemeExtras.colors.card)
            .background(champagneTint(0.10f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), shape)
            .padding(Spacing.l),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(R.drawable.ic_timer)
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.training_open_session_title).uppercase(), style = OverlineStyle, color = MaterialTheme.colorScheme.primary)
                Text(open.routineName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (open.paused) Pill(stringResource(R.string.training_open_session_paused), filled = false)
        }
        Spacer(Modifier.height(Spacing.m))
        PrimaryButton(text = stringResource(R.string.training_continue), onClick = onContinue, iconRes = R.drawable.ic_play, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun IconBadge(@DrawableRes iconRes: Int, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .background(FitnessThemeExtras.colors.surfaceRaised, MaterialTheme.shapes.medium),
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun EmptyRoutines(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    FitnessCard(modifier = modifier) {
        IconBadge(R.drawable.ic_dumbbell)
        Spacer(Modifier.height(Spacing.m))
        Text(stringResource(R.string.training_empty_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Spacing.xs))
        Text(stringResource(R.string.training_empty_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.l))
        PrimaryButton(text = stringResource(R.string.training_create), onClick = onCreate, iconRes = R.drawable.ic_add)
    }
}

/**
 * Tarjeta de rutina sobre la foto del gimnasio atenuada. Zona principal (abre el detalle) y botón de menú
 * son hermanos, no anidados: cada uno tiene su propio objetivo táctil y su anuncio.
 */
@Composable
private fun RoutineCard(
    routine: RoutineSummary,
    isNext: Boolean,
    onOpen: () -> Unit,
    menu: List<MenuAction>,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    PhotoBackground(
        photo = R.drawable.training_gym_hero,
        photoAlignment = Alignment.CenterEnd,
        horizontalScrimEnd = 1.15f,
        bottomScrimAlpha = 0.5f,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, LocalMotionEnabled.current)
            .clip(MaterialTheme.shapes.large)
            .then(if (isNext) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), MaterialTheme.shapes.large) else Modifier),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 112.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 112.dp)
                    .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onOpen)
                    .padding(start = Spacing.l, top = Spacing.l, bottom = Spacing.l),
            ) {
                Column(Modifier.weight(1f)) {
                    if (isNext) {
                        Pill(stringResource(R.string.training_next_badge), iconRes = R.drawable.ic_bookmark_filled)
                        Spacer(Modifier.height(Spacing.s))
                    }
                    Text(
                        routine.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, lineHeight = 27.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        countsLabel(routine.exerciseCount, routine.setCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    )
                }
                // Fondos circulares translúcidos: legibles sobre cualquier zona de la fotografía.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f), CircleShape),
                ) {
                    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Box(
                Modifier
                    .padding(start = Spacing.xs, end = Spacing.s)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f), CircleShape),
            ) {
                OverflowMenu(description = stringResource(R.string.training_routine_actions, routine.name), actions = menu)
            }
        }
    }
}

@Composable
internal fun countsLabel(exercises: Int, sets: Int): String = stringResource(
    R.string.training_counts,
    pluralStringResource(R.plurals.exercise_count, exercises, exercises),
    pluralStringResource(R.plurals.set_count, sets, sets),
)

@Composable
internal fun OverflowMenu(description: String, actions: List<MenuAction>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { expanded = true }, modifier = Modifier.semantics { this.contentDescription = description }) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = FitnessThemeExtras.colors.surfaceRaised,
            shape = MaterialTheme.shapes.medium,
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(stringResource(action.labelRes), style = MaterialTheme.typography.bodyLarge) },
                    leadingIcon = { Icon(painterResource(action.iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                )
            }
        }
    }
}

@Composable
private fun ArchivedHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_archive), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.s))
            Text(
                stringResource(R.string.training_archived_title, count),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painterResource(if (expanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        if (expanded) {
            Text(
                stringResource(R.string.training_archived_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}

@Composable
private fun ArchivedRow(routine: RoutineSummary, onRestore: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(FitnessThemeExtras.colors.card, MaterialTheme.shapes.large)
            .padding(start = Spacing.l, end = Spacing.s, top = Spacing.s, bottom = Spacing.s),
    ) {
        Column(Modifier.weight(1f)) {
            Text(routine.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(countsLabel(routine.exerciseCount, routine.setCount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onRestore, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Icon(painterResource(R.drawable.ic_unarchive), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(stringResource(R.string.action_restore), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Fila de historial: fecha, nombre y cifras reales guardadas. Abre el resumen persistido. */
@Composable
internal fun HistoryRow(entry: HistoryEntry, unit: UnitSystem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(FitnessThemeExtras.colors.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Spacing.l),
    ) {
        IconBadge(R.drawable.ic_calendar)
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(entry.routineName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(formatSessionDate(entry.endedAt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(
                    R.string.history_line,
                    formatDuration(entry.durationMs),
                    pluralStringResource(R.plurals.set_count, entry.completedSets, entry.completedSets),
                    formatVolume(entry.volumeKg, unit),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val previewNav = TrainingNavigation({}, {}, {}, {}, {}, {})

@Preview(name = "Entrenar vacío", showBackground = true, backgroundColor = 0xFF121416, widthDp = 393, heightDp = 900)
@Composable
private fun TrainingEmptyPreview() {
    FitnessTheme { TrainingScreen(TrainingUiState(loading = false), previewNav, {}, {}, {}, {}, {}) }
}

@Preview(name = "Entrenar DEMO", showBackground = true, backgroundColor = 0xFF121416, widthDp = 393, heightDp = 1300)
@Composable
private fun TrainingDemoPreview() {
    FitnessTheme {
        TrainingScreen(
            TrainingUiState(
                loading = false,
                routines = listOf(RoutineSummary("1", "Tren superior DEMO", 4, 12, false), RoutineSummary("2", "Piernas DEMO", 3, 9, false)),
                archived = listOf(RoutineSummary("3", "Antigua DEMO", 2, 6, true)),
                nextRoutineId = "1",
                openSession = OpenSession("s", "1", "Tren superior DEMO", paused = true),
                recentHistory = listOf(HistoryEntry("h", "Tren superior DEMO", 1_764_000_000_000, 2_280_000, 12, 4, 2400.0)),
                historyCount = 1,
            ),
            previewNav, {}, {}, {}, {}, {},
        )
    }
}

@Preview(name = "Entrenar fuente 200 %", fontScale = 2f, showBackground = true, backgroundColor = 0xFF121416, widthDp = 360, heightDp = 1600)
@Composable
private fun TrainingLargeFontPreview() {
    FitnessTheme { TrainingScreen(TrainingUiState(loading = false), previewNav, {}, {}, {}, {}, {}) }
}
