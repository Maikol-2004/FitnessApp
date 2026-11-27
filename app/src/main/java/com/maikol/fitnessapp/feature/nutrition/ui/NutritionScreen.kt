package com.maikol.fitnessapp.feature.nutrition.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.MinTouchTarget
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.database.MealMode
import com.maikol.fitnessapp.feature.nutrition.data.Coverage
import com.maikol.fitnessapp.feature.nutrition.data.DayPoint
import com.maikol.fitnessapp.feature.nutrition.data.DiaryEntry
import com.maikol.fitnessapp.feature.nutrition.data.DiaryItem
import com.maikol.fitnessapp.feature.nutrition.data.MealGroup
import com.maikol.fitnessapp.feature.nutrition.data.Nutrient
import com.maikol.fitnessapp.feature.nutrition.data.goalDifference
import com.maikol.fitnessapp.feature.tracking.DatePickerSheet
import com.maikol.fitnessapp.feature.tracking.DeleteDialog
import com.maikol.fitnessapp.feature.tracking.dayLabel
import com.maikol.fitnessapp.feature.tracking.formatDay
import com.maikol.fitnessapp.feature.tracking.formatTime
import com.maikol.fitnessapp.feature.training.ui.GlassButton
import com.maikol.fitnessapp.feature.training.ui.GlassIconButton
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import com.maikol.fitnessapp.feature.training.ui.MenuAction
import com.maikol.fitnessapp.feature.training.ui.OverflowMenu
import com.maikol.fitnessapp.feature.training.ui.editorGlass
import java.time.LocalDate

class NutritionRootNav(
    val addFood: (LocalDate, MealGroup?) -> Unit,
    val quickLog: (LocalDate, MealGroup?) -> Unit,
    val openEntry: (String) -> Unit,
    val editTotals: (String) -> Unit,
    val editItem: (String) -> Unit,
    val openGoals: () -> Unit,
    val logRecipe: (String, LocalDate) -> Unit,
    val editRecipe: (String?) -> Unit,
)

/** Lo que se va a copiar/mover/borrar desde el menú de una fila. */
private sealed interface RowTarget {
    data class Item(val item: DiaryItem, val group: MealGroup) : RowTarget
    data class Entry(val entry: DiaryEntry) : RowTarget
}

@Composable
fun NutritionRoute(
    savedState: SavedStateHandle,
    onBack: () -> Unit,
    nav: NutritionRootNav,
    viewModel: NutritionViewModel = viewModel(factory = NutritionViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val message by savedState.getStateFlow<String?>(NutritionRoutes.KEY_MESSAGE, null).collectAsStateWithLifecycle()
    LaunchedEffect(message) {
        message?.let {
            savedState[NutritionRoutes.KEY_MESSAGE] = null
            snackbar.showSnackbar(it)
        }
    }
    UndoSnackbar(snackbar, state.undo != null, state.undoRestoring, state.undoFailed, viewModel::undo, viewModel::dismissUndo)
    var copying by remember { mutableStateOf<RowTarget?>(null) }
    var moving by remember { mutableStateOf<RowTarget?>(null) }
    var deleting by remember { mutableStateOf<DiaryEntry?>(null) }
    var pickDate by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        NutritionPage(
            title = stringResource(R.string.category_nutrition),
            subtitle = stringResource(R.string.n_subtitle),
            onBack = onBack,
            withDock = true,
            actions = { GlassIconButton(R.drawable.ic_target, stringResource(R.string.n_goals_title), nav.openGoals) },
            bottomBar = when (state.tab) {
                NutritionTab.DIARY -> ({
                    PrimaryButton(stringResource(R.string.n_log_food), { nav.addFood(state.date, null) }, Modifier.fillMaxWidth(), iconRes = R.drawable.ic_add)
                })
                NutritionTab.RECIPES -> ({
                    PrimaryButton(stringResource(R.string.n_recipe_new), { nav.editRecipe(null) }, Modifier.fillMaxWidth(), iconRes = R.drawable.ic_add)
                })
                NutritionTab.TRENDS -> null
            },
        ) {
            if (state.tab != NutritionTab.TRENDS) {
                DateBar(state.date, state.today, viewModel::selectDate, onPick = { pickDate = true })
                Summary(state, nav.openGoals)
            }
            GlassSegmented(
                listOf(stringResource(R.string.n_tab_diary), stringResource(R.string.n_tab_recipes), stringResource(R.string.n_tab_trends)),
                state.tab.ordinal,
                { viewModel.selectTab(NutritionTab.entries[it]) },
            )
            InlineMessage(visible = state.failed, text = stringResource(R.string.record_save_error))
            when (state.tab) {
                NutritionTab.DIARY -> Diary(
                    state, nav,
                    onAddToGroup = { nav.addFood(state.date, it) },
                    onMenu = { target, action ->
                        when (action) {
                            RowAction.EDIT -> when (target) {
                                is RowTarget.Item -> nav.editItem(target.item.entity.id)
                                is RowTarget.Entry -> nav.editTotals(target.entry.id)
                            }
                            RowAction.MOVE -> moving = target
                            RowAction.COPY -> copying = target
                            RowAction.DELETE -> when (target) {
                                is RowTarget.Item -> viewModel.deleteItem(target.item.entity.id)
                                is RowTarget.Entry -> deleting = target.entry
                            }
                        }
                    },
                )
                NutritionTab.RECIPES -> Recipes(state, onLog = { nav.logRecipe(it, state.date) }, onEdit = nav.editRecipe, onArchive = viewModel::archiveRecipe)
                NutritionTab.TRENDS -> state.trends?.let { Trends(it, viewModel::selectPeriod, viewModel::openDay, nav.openGoals) }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 160.dp))
    }

    if (pickDate) {
        DatePickerSheet(state.date, state.today, onPick = { viewModel.selectDate(it); pickDate = false }, onDismiss = { pickDate = false })
    }
    copying?.let { target ->
        val opId = remember(target) { viewModel.newOpId() }
        CopyDialog(
            candidates = listOf(target.candidate()),
            today = state.today,
            initialDate = state.today,
            initialGroup = target.group(),
            opId = opId,
            onCopy = viewModel::copy,
            onDismiss = { copying = null },
        )
    }
    moving?.let { target ->
        MoveDialog(target.title(), target.group(), onMove = { g ->
            when (target) {
                is RowTarget.Item -> viewModel.moveItem(target.item.entity.id, g)
                is RowTarget.Entry -> viewModel.moveEntry(target.entry.id, g)
            }
            moving = null
        }, onDismiss = { moving = null })
    }
    deleting?.let { e ->
        DeleteDialog(e.meal.name, onConfirm = { deleting = null; viewModel.deleteEntry(e.id) }, onDismiss = { deleting = null })
    }
}

private fun RowTarget.group(): MealGroup = when (this) {
    is RowTarget.Item -> group
    is RowTarget.Entry -> entry.group
}

private fun RowTarget.title(): String = when (this) {
    is RowTarget.Item -> item.entity.name
    is RowTarget.Entry -> entry.meal.name
}

@Composable
private fun RowTarget.candidate(): CopyCandidate = when (this) {
    is RowTarget.Item -> CopyCandidate(item.entity.id, false, item.entity.name, itemDetail(item))
    is RowTarget.Entry -> CopyCandidate(entry.id, true, entry.meal.name, entry.totals.kcal.value?.let(::kcalText) ?: "— kcal")
}

@Composable
internal fun itemDetail(item: DiaryItem): String =
    quantityText(item.entity.quantity, item.unit) + " · " + (item.entity.kcal?.let(::kcalText) ?: "— kcal")

/** Selector de fecha de cristal: anterior/siguiente (no pasa de hoy), etiqueta que abre el calendario y «Hoy». */
@Composable
internal fun DateBar(date: LocalDate, today: LocalDate, onChange: (LocalDate) -> Unit, onPick: () -> Unit) {
    val label = dayLabel(date, today)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().editorGlass(RoundedCornerShape(28.dp))) {
        IconButton(onClick = { onChange(date.minusDays(1)) }) {
            Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.day_previous))
        }
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = MinTouchTarget)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.n_pick_date)) { onPick() }
                .padding(vertical = Spacing.m),
        )
        if (date != today) {
            TextAction(stringResource(R.string.day_today), { onChange(today) })
        }
        IconButton(onClick = { onChange(date.plusDays(1)) }, enabled = date < today) {
            Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.day_next))
        }
    }
}

@Composable
private fun Summary(state: NutritionUiState, openGoals: () -> Unit) {
    val kcal = state.totals.kcal
    val goals = state.goal?.values
    val goalKcal = goals?.kcal
    val stacked = LocalDensity.current.fontScale > 1.3f
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (goalKcal != null && kcal.value != null && !stacked) {
            KcalRing((kcal.value / goalKcal).toFloat(), kcalNumber(kcal.value) + "\nkcal")
            Spacer(Modifier.width(Spacing.l))
        }
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
            Text(kcal.value?.let(::kcalText) ?: "— kcal", style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(if (kcal.coverage == Coverage.EMPTY) R.string.n_no_records_day else R.string.n_logged),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (kcal.coverage == Coverage.PARTIAL) {
                Text(stringResource(R.string.n_kcal_partial), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (kcal.coverage == Coverage.UNKNOWN) {
                Text(stringResource(R.string.n_kcal_unknown), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (goalKcal != null) {
                Text(stringResource(R.string.n_goal_kcal, kcalText(goalKcal.toDouble())), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                goalDifference(kcal.value, goalKcal)?.let { diff ->
                    Text(
                        if (diff >= 0) stringResource(R.string.n_goal_remaining, kcalText(diff)) else stringResource(R.string.n_goal_over, kcalText(-diff)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    if (goals == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (state.anyGoal) R.string.n_goal_none_day else R.string.n_goal_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (!state.anyGoal) TextAction(stringResource(R.string.n_goal_set), openGoals)
        }
    }
    MacroRow(
        values = mapOf(Nutrient.PROTEIN to state.totals.protein, Nutrient.CARBS to state.totals.carbs, Nutrient.FAT to state.totals.fat),
        goals = mapOf(Nutrient.PROTEIN to goals?.proteinG, Nutrient.CARBS to goals?.carbsG, Nutrient.FAT to goals?.fatG),
        stacked = stacked,
    )
}

internal enum class RowAction { EDIT, MOVE, COPY, DELETE }

@Composable
private fun Diary(state: NutritionUiState, nav: NutritionRootNav, onAddToGroup: (MealGroup) -> Unit, onMenu: (RowTarget, RowAction) -> Unit) {
    val byGroup = state.entries.groupBy { it.group }
    val groups = MealGroup.choosable + if (byGroup.containsKey(MealGroup.UNSPECIFIED)) listOf(MealGroup.UNSPECIFIED) else emptyList()
    Column {
        groups.forEachIndexed { i, g ->
            if (i > 0) HorizontalDivider(color = FitnessThemeExtras.colors.surfaceRaised)
            GroupSection(g, byGroup[g].orEmpty(), nav, onAdd = { onAddToGroup(g) }, onMenu = onMenu)
        }
    }
}

@Composable
private fun GroupSection(group: MealGroup, entries: List<DiaryEntry>, nav: NutritionRootNav, onAdd: () -> Unit, onMenu: (RowTarget, RowAction) -> Unit) {
    var expanded by rememberSaveable(group) { mutableStateOf(true) }
    val totals = com.maikol.fitnessapp.feature.nutrition.data.Totals.sum(entries.map { it.totals })
    val foodEntry = entries.firstOrNull { it.isFoodItems && it.items.isNotEmpty() }
    val name = stringResource(group.label())
    val subtotal = when (totals.kcal.coverage) {
        Coverage.EMPTY -> null
        Coverage.UNKNOWN -> "— kcal"
        else -> kcalText(totals.kcal.value!!)
    }
    Column(Modifier.padding(vertical = Spacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
            Icon(painterResource(group.icon()), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(Spacing.m))
            Column(
                Modifier
                    .weight(1f)
                    .then(if (foodEntry != null) Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.n_open_meal, name)) { nav.openEntry(foodEntry.id) } else Modifier)
                    .semantics(mergeDescendants = true) { heading() },
            ) {
                Text(listOfNotNull(name, subtotal).joinToString(" · "), style = MaterialTheme.typography.titleMedium)
                if (totals.kcal.coverage == Coverage.PARTIAL) {
                    Text(stringResource(R.string.n_partial), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            GlassIconButton(R.drawable.ic_add, stringResource(R.string.n_add_to, name), onAdd, size = 44.dp)
            if (entries.isNotEmpty()) {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.semantics { stateDescription = if (expanded) "expandido" else "contraído" },
                ) {
                    Icon(
                        painterResource(R.drawable.ic_chevron_right),
                        stringResource(if (expanded) R.string.n_collapse else R.string.n_expand, name),
                        modifier = Modifier.rotate(if (expanded) 270f else 90f),
                    )
                }
            }
        }
        if (entries.isEmpty()) {
            Text(stringResource(R.string.n_no_records), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 36.dp))
        } else if (expanded) {
            entries.forEach { e ->
                if (e.isFoodItems) {
                    e.items.forEach { item -> ItemRow(item, onClick = { nav.editItem(item.entity.id) }) { a -> onMenu(RowTarget.Item(item, group), a) } }
                } else {
                    TotalsRow(e, onClick = { nav.editTotals(e.id) }) { a -> onMenu(RowTarget.Entry(e), a) }
                }
            }
        }
    }
}

@Composable
internal fun ItemRow(item: DiaryItem, onClick: () -> Unit, onAction: (RowAction) -> Unit) {
    val e = item.entity
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Column(
            Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = 36.dp, top = Spacing.xs, bottom = Spacing.xs)
                .semantics(mergeDescendants = true) {},
        ) {
            Text(listOfNotNull(e.name, e.brand).joinToString(" · "), style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(quantityText(e.quantity, item.unit), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(e.kcal?.let(::kcalText) ?: "— kcal", style = MaterialTheme.typography.bodyLarge)
            if (item.totals.kcal.coverage == Coverage.PARTIAL) Text(stringResource(R.string.n_partial), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RowMenu(e.name, onAction)
    }
}

@Composable
private fun TotalsRow(entry: DiaryEntry, onClick: () -> Unit, onAction: (RowAction) -> Unit) {
    val m = entry.meal
    val kind = stringResource(if (m.mode == MealMode.LEGACY_TOTALS) R.string.n_legacy_totals else R.string.n_quick_totals)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Column(
            Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = 36.dp, top = Spacing.xs, bottom = Spacing.xs)
                .semantics(mergeDescendants = true) {},
        ) {
            Text(m.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${formatTime(entry.time)} · $kind", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(m.kcal?.let(::kcalText) ?: "— kcal", style = MaterialTheme.typography.bodyLarge)
        RowMenu(m.name, onAction)
    }
}

@Composable
internal fun RowMenu(name: String, onAction: (RowAction) -> Unit) {
    OverflowMenu(
        description = stringResource(R.string.record_actions, name),
        actions = listOf(
            MenuAction(R.string.action_edit, R.drawable.ic_edit) { onAction(RowAction.EDIT) },
            MenuAction(R.string.n_move, R.drawable.ic_swap) { onAction(RowAction.MOVE) },
            MenuAction(R.string.n_copy, R.drawable.ic_copy) { onAction(RowAction.COPY) },
            MenuAction(R.string.action_delete, R.drawable.ic_delete) { onAction(RowAction.DELETE) },
        ),
    )
}

// ---- Recetas ----

@Composable
private fun Recipes(state: NutritionUiState, onLog: (String) -> Unit, onEdit: (String?) -> Unit, onArchive: (String) -> Unit) {
    if (state.recipes.isEmpty()) {
        Text(stringResource(R.string.n_recipes_empty), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column {
        state.recipes.forEachIndexed { i, row ->
            if (i > 0) HorizontalDivider(color = FitnessThemeExtras.colors.surfaceRaised)
            val r = row.recipe.recipe
            val per = row.totals.perServing.kcal
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.n_recipe_log)) { onLog(r.id) }
                        .padding(vertical = Spacing.s)
                        .semantics(mergeDescendants = true) {},
                ) {
                    Text(r.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(
                            quantityText(r.servings, com.maikol.fitnessapp.feature.nutrition.data.QtyUnit.SERVING),
                            per.value?.let { stringResource(R.string.n_kcal_per_serving, kcalText(it)) } ?: stringResource(R.string.n_kcal_unknown_short),
                            stringResource(R.string.n_partial).takeIf { per.coverage == Coverage.PARTIAL },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OverflowMenu(
                    description = stringResource(R.string.record_actions, r.name),
                    actions = listOf(
                        MenuAction(R.string.action_edit, R.drawable.ic_edit) { onEdit(r.id) },
                        MenuAction(R.string.n_archive, R.drawable.ic_archive) { onArchive(r.id) },
                    ),
                )
            }
        }
    }
    Text(stringResource(R.string.n_recipe_save_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---- Tendencias ----

@Composable
private fun Trends(t: TrendsState, onPeriod: (Int) -> Unit, onDay: (LocalDate) -> Unit, openGoals: () -> Unit) {
    GlassSegmented(
        listOf(stringResource(R.string.n_days_7), stringResource(R.string.n_days_30)),
        if (t.period == 7) 0 else 1,
        { onPeriod(if (it == 0) 7 else 30) },
    )
    val s = t.summary
    Text(
        stringResource(R.string.n_period, formatDay(t.from, t.to), formatDay(t.to, t.to)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(pluralStringResource(R.plurals.n_days_with_records, s.daysWithRecords, s.daysWithRecords, s.periodDays), style = MaterialTheme.typography.headlineSmall)
        val kcalDays = s.recordedDays[Nutrient.KCAL] ?: 0
        val mean = s.meanOfRecordedDays[Nutrient.KCAL]
        if (mean != null) {
            Text(stringResource(R.string.n_mean_kcal, kcalText(mean)), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(pluralStringResource(R.plurals.n_mean_denominator, kcalDays, kcalDays, s.periodDays), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(stringResource(R.string.n_no_kcal_period), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val partial = s.partialDays[Nutrient.KCAL] ?: 0
        if (partial > 0) {
            Text(pluralStringResource(R.plurals.n_partial_days, partial, partial), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    TrendChart(t.points, t.period, onDay)
    Text(stringResource(R.string.n_macros_mean), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    listOf(Nutrient.PROTEIN, Nutrient.CARBS, Nutrient.FAT).forEach { n ->
        val days = s.recordedDays[n] ?: 0
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(n.label()), style = MaterialTheme.typography.bodyLarge)
                Text(pluralStringResource(R.plurals.n_mean_denominator, days, days, s.periodDays), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(s.meanOfRecordedDays[n]?.let(::gramsText) ?: "—", style = MaterialTheme.typography.titleMedium)
        }
    }
    Row(verticalAlignment = Alignment.Top) {
        Icon(painterResource(R.drawable.ic_info), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.s))
        Text(stringResource(R.string.n_trend_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    GlassButton(stringResource(R.string.n_goals_edit), R.drawable.ic_target, openGoals)
    Text(stringResource(R.string.n_trend_list), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    Column {
        t.points.reversed().forEach { p -> DayRow(p, t.to, onDay) }
    }
}

/** Gráfico de barras por día: los días sin registros son huecos rotulados, nunca una barra de 0. */
@Composable
private fun TrendChart(points: List<DayPoint>, period: Int, onDay: (LocalDate) -> Unit) {
    val max = points.mapNotNull { it.totals.kcal.value }.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val fill = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    val showLabels = period <= 7
    Row(horizontalArrangement = Arrangement.spacedBy(if (showLabels) Spacing.s else 2.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().height(180.dp)) {
        points.forEach { p ->
            val v = p.totals.kcal.value
            val partial = p.totals.kcal.coverage == Coverage.PARTIAL
            val desc = dayDescription(p)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.n_view_diary)) { onDay(p.date) }
                    .semantics(mergeDescendants = true) { contentDescription = desc },
            ) {
                if (v != null) {
                    if (showLabels) Text(kcalNumber(v), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight((v / max).toFloat().coerceIn(0.02f, 1f) * 0.8f)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .then(if (partial) Modifier.border(1.5.dp, fill, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)) else Modifier.background(fill)),
                    )
                } else {
                    if (showLabels) {
                        Text(stringResource(R.string.n_gap_short), style = MaterialTheme.typography.labelSmall, color = outline, textAlign = TextAlign.Center, maxLines = 2)
                    }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(outline.copy(alpha = 0.5f)))
                }
                if (showLabels) {
                    Text(dayInitial(p.date), style = MaterialTheme.typography.labelSmall, color = outline, maxLines = 1)
                }
            }
        }
    }
    Text(stringResource(R.string.n_chart_legend), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun dayInitial(d: LocalDate): String =
    d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, com.maikol.fitnessapp.feature.training.data.AppLocale).take(3).replaceFirstChar { it.titlecase() }

@Composable
private fun dayDescription(p: DayPoint): String {
    val day = formatDay(p.date, p.date)
    val k = p.totals.kcal
    return when (k.coverage) {
        Coverage.EMPTY -> stringResource(R.string.n_day_empty, day)
        Coverage.UNKNOWN -> stringResource(R.string.n_day_unknown, day)
        Coverage.PARTIAL -> stringResource(R.string.n_day_partial, day, kcalText(k.value!!))
        Coverage.KNOWN -> stringResource(R.string.n_day_known, day, kcalText(k.value!!))
    }
}

@Composable
private fun DayRow(p: DayPoint, today: LocalDate, onDay: (LocalDate) -> Unit) {
    val k = p.totals.kcal
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.n_view_diary)) { onDay(p.date) }
            .semantics(mergeDescendants = true) {},
    ) {
        Text(dayLabel(p.date, today), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            when (k.coverage) {
                Coverage.EMPTY -> stringResource(R.string.n_gap_short)
                Coverage.UNKNOWN -> "— kcal"
                Coverage.PARTIAL -> kcalText(k.value!!) + " · " + stringResource(R.string.n_partial)
                Coverage.KNOWN -> kcalText(k.value!!)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (k.coverage == Coverage.EMPTY) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}
