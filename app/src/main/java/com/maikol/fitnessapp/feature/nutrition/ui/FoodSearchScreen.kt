package com.maikol.fitnessapp.feature.nutrition.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
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
import com.maikol.fitnessapp.feature.nutrition.data.MealGroup
import com.maikol.fitnessapp.feature.nutrition.data.Preparation
import com.maikol.fitnessapp.feature.nutrition.data.QtyUnit
import com.maikol.fitnessapp.feature.nutrition.data.RemoteHit
import com.maikol.fitnessapp.feature.nutrition.data.RemoteResult
import com.maikol.fitnessapp.feature.nutrition.data.totals
import com.maikol.fitnessapp.feature.tracking.dayLabel
import com.maikol.fitnessapp.feature.training.ui.GlassButton
import com.maikol.fitnessapp.feature.training.ui.GlassIconButton
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import com.maikol.fitnessapp.feature.training.ui.editorGlass
import java.time.LocalDate

@Composable
fun FoodSearchRoute(
    savedState: SavedStateHandle,
    onBack: () -> Unit,
    openFood: (String, LocalDate, MealGroup?, PickTarget) -> Unit,
    openRecipe: (String, LocalDate, MealGroup?) -> Unit,
    createFood: (String?, LocalDate, MealGroup?, PickTarget) -> Unit,
    quickLog: (LocalDate, MealGroup?) -> Unit,
    scan: () -> Unit,
    viewModel: FoodSearchViewModel = viewModel(factory = FoodSearchViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scanned by savedState.getStateFlow<String?>(NutritionRoutes.KEY_SCANNED, null).collectAsStateWithLifecycle()
    LaunchedEffect(scanned) {
        scanned?.let {
            savedState[NutritionRoutes.KEY_SCANNED] = null
            viewModel.lookupCode(it)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is SearchEvent.OpenFood -> openFood(e.id, viewModel.date, viewModel.group, viewModel.target)
            }
        }
    }
    var manualCode by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val ingredient = viewModel.target == PickTarget.INGREDIENT
    val today = LocalDate.now()

    NutritionPage(
        title = stringResource(if (ingredient) R.string.n_add_ingredient else R.string.n_add_food),
        subtitle = if (ingredient) stringResource(R.string.n_ingredient_hint) else
            listOfNotNull(dayLabel(viewModel.date, today), viewModel.group?.let { stringResource(it.label()) }).joinToString(" · "),
        onBack = onBack,
        bottomBar = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                GlassButton(stringResource(R.string.n_create_food), R.drawable.ic_add, { createFood(null, viewModel.date, viewModel.group, viewModel.target) }, Modifier.weight(1f), centered = true)
                if (!ingredient) {
                    GlassButton(stringResource(R.string.n_quick_log), R.drawable.ic_bolt, { quickLog(viewModel.date, viewModel.group) }, Modifier.weight(1f), centered = true)
                }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            SearchField(
                state.query,
                viewModel::onQuery,
                onSearch = { focus.clearFocus(); viewModel.searchOnline() },
                modifier = Modifier.weight(1f),
            )
            GlassIconButton(R.drawable.ic_barcode, stringResource(R.string.n_scan), scan, size = 56.dp)
        }
        if (state.query.isBlank()) {
            GlassSegmented(
                listOf(stringResource(R.string.n_filter_recent), stringResource(R.string.n_filter_favorites), stringResource(R.string.n_filter_mine)),
                state.filter.ordinal,
                { viewModel.onFilter(LocalFilter.entries[it]) },
            )
        }
        GlassButton(
            stringResource(R.string.n_search_online),
            R.drawable.ic_globe,
            { focus.clearFocus(); viewModel.searchOnline() },
            Modifier.fillMaxWidth(),
            enabled = state.query.trim().length >= 2 && state.remote !is RemoteState.Loading,
            centered = true,
        )
        TextAction(stringResource(R.string.n_enter_code), { manualCode = true }, iconRes = R.drawable.ic_edit)
        CodeStatus(state.code, onClear = viewModel::clearCode, onCreate = { createFood(it, viewModel.date, viewModel.group, viewModel.target) }, onRetry = viewModel::lookupCode)

        // Resultados locales: siempre disponibles, también sin red.
        LocalResults(state, viewModel.target, onFood = { openFood(it, viewModel.date, viewModel.group, viewModel.target) }, onRecipe = { openRecipe(it, viewModel.date, viewModel.group) })
        RemoteSection(state, viewModel::openRemote, viewModel::cancelOnline, viewModel::searchOnline, viewModel::clearOpenError)
    }
    if (manualCode) {
        ManualCodeDialog(onDismiss = { manualCode = false }, onConfirm = { manualCode = false; viewModel.lookupCode(it) })
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.n_search_label)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier.semantics { contentDescription = label },
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.editorGlass().heightIn(min = 56.dp).padding(horizontal = Spacing.l)) {
                Icon(painterResource(R.drawable.ic_search), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.m))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
                if (value.isNotEmpty()) {
                    Icon(
                        painterResource(R.drawable.ic_close), stringResource(R.string.n_clear),
                        modifier = Modifier.size(MinTouchTarget).clickable(role = Role.Button) { onChange("") }.padding(12.dp),
                    )
                }
            }
        },
    )
}

@Composable
private fun LocalResults(state: SearchUiState, target: PickTarget, onFood: (String) -> Unit, onRecipe: (String) -> Unit) {
    val title = when {
        state.query.isNotBlank() -> stringResource(R.string.n_local_results)
        state.filter == LocalFilter.RECENT -> stringResource(R.string.n_filter_recent)
        state.filter == LocalFilter.FAVORITES -> stringResource(R.string.n_filter_favorites)
        else -> stringResource(R.string.n_filter_mine)
    }
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    if (state.rows.isEmpty()) {
        Text(
            stringResource(
                when {
                    !state.hasAnyLocal -> if (target == PickTarget.INGREDIENT) R.string.n_local_none_ingredient else R.string.n_local_none
                    state.query.isNotBlank() -> R.string.n_local_no_match
                    state.filter == LocalFilter.RECENT -> R.string.n_recent_empty
                    state.filter == LocalFilter.FAVORITES -> R.string.n_favorites_empty
                    else -> R.string.n_mine_empty
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column {
        state.rows.forEachIndexed { i, row ->
            if (i > 0) HorizontalDivider(color = FitnessThemeExtras.colors.surfaceRaised)
            when (row) {
                is SearchRow.Food -> {
                    val f = row.food
                    val prep = stringResource(Preparation.of(f.preparation).label())
                    ResultRow(
                        title = listOfNotNull(f.name, f.brand).joinToString(" · "),
                        detail = prep,
                        right1 = basisLabel(f.basisAmount, QtyUnit.of(f.basisUnit) ?: QtyUnit.G),
                        right2 = sourceLabel(f.source),
                        favorite = f.favorite,
                        onClick = { onFood(f.id) },
                    )
                }
                is SearchRow.Recipe -> {
                    val t = row.recipe.totals()
                    ResultRow(
                        title = row.recipe.recipe.name,
                        detail = t.perServing.kcal.value?.let { stringResource(R.string.n_kcal_per_serving, kcalText(it)) } ?: stringResource(R.string.n_kcal_unknown_short),
                        right1 = stringResource(R.string.n_per_serving),
                        right2 = stringResource(R.string.n_source_recipe),
                        favorite = row.recipe.recipe.favorite,
                        onClick = { onRecipe(row.recipe.recipe.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultRow(title: String, detail: String, right1: String, right2: String, favorite: Boolean, onClick: () -> Unit, busy: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.s)
            .semantics(mergeDescendants = true) {},
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (favorite) {
                    Icon(painterResource(R.drawable.ic_star), stringResource(R.string.n_favorite), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(Spacing.xs))
                }
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
        Spacer(Modifier.width(Spacing.s))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(120.dp)) {
            Text(right1, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(right2, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
        if (busy) {
            CircularProgressIndicator(Modifier.padding(start = Spacing.s).size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RemoteSection(state: SearchUiState, onOpen: (RemoteHit) -> Unit, onCancel: () -> Unit, onRetry: () -> Unit, onClearOpenError: () -> Unit) {
    val r = state.remote
    if (r == RemoteState.Idle) return
    Text(stringResource(R.string.n_online_results), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    when (r) {
        is RemoteState.Loading -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(Spacing.m))
            Text(stringResource(R.string.n_searching, r.query), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextAction(stringResource(R.string.action_cancel), onCancel)
        }
        is RemoteState.Error -> {
            InlineMessage(visible = true, text = remoteErrorText(r.result))
            TextAction(stringResource(R.string.n_retry), onRetry)
        }
        is RemoteState.Results -> {
            if (r.hits.isEmpty()) {
                Text(stringResource(R.string.n_online_empty, r.query), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.openError?.let {
                InlineMessage(visible = true, text = remoteErrorText(it))
                TextAction(stringResource(R.string.n_dismiss), onClearOpenError)
            }
            Column {
                r.hits.forEachIndexed { i, hit ->
                    if (i > 0) HorizontalDivider(color = FitnessThemeExtras.colors.surfaceRaised)
                    ResultRow(
                        title = listOfNotNull(hit.name, hit.brand).joinToString(" · "),
                        detail = listOfNotNull(hit.quantity, stringResource(R.string.n_code_value, hit.code)).joinToString(" · "),
                        right1 = stringResource(R.string.n_source_off),
                        right2 = stringResource(R.string.n_values_on_open),
                        favorite = false,
                        busy = state.opening == hit.code,
                        onClick = { onOpen(hit) },
                    )
                }
            }
            Text(stringResource(R.string.n_off_attribution), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RemoteState.Idle -> Unit
    }
}

@Composable
internal fun remoteErrorText(r: RemoteResult<Nothing>): String = when (r) {
    RemoteResult.Offline -> stringResource(R.string.n_err_offline)
    RemoteResult.Timeout -> stringResource(R.string.n_err_timeout)
    RemoteResult.RateLimited -> stringResource(R.string.n_err_429)
    RemoteResult.Unavailable -> stringResource(R.string.n_err_503)
    is RemoteResult.Throttled -> stringResource(R.string.n_err_throttled, r.waitSeconds)
    RemoteResult.NotFound -> stringResource(R.string.n_code_not_found)
    RemoteResult.NoNutrition -> stringResource(R.string.n_code_no_nutrition)
    else -> stringResource(R.string.n_err_generic)
}

@Composable
private fun CodeStatus(code: CodeState, onClear: () -> Unit, onCreate: (String) -> Unit, onRetry: (String) -> Unit) {
    when (code) {
        CodeState.Idle -> Unit
        CodeState.Invalid -> {
            InlineMessage(visible = true, text = stringResource(R.string.n_code_invalid))
        }
        is CodeState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(Spacing.m))
            Text(stringResource(R.string.n_code_looking, code.code), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextAction(stringResource(R.string.action_cancel), onClear)
        }
        is CodeState.NotFound -> CodeProblem(stringResource(R.string.n_code_not_found_value, code.code), code.code, onCreate, onClear, null)
        is CodeState.NoNutrition -> CodeProblem(stringResource(R.string.n_code_no_nutrition_value, code.code), code.code, onCreate, onClear, null)
        is CodeState.Error -> CodeProblem(remoteErrorText(code.result) + " · " + stringResource(R.string.n_code_value, code.code), code.code, onCreate, onClear) { onRetry(code.code) }
    }
}

@Composable
private fun CodeProblem(text: String, code: String, onCreate: (String) -> Unit, onClear: () -> Unit, onRetry: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        InlineMessage(visible = true, text = text, iconRes = R.drawable.ic_info, color = MaterialTheme.colorScheme.onSurface)
        GlassButton(stringResource(R.string.n_create_with_code), R.drawable.ic_add, { onCreate(code) }, Modifier.fillMaxWidth())
        Row {
            if (onRetry != null) TextAction(stringResource(R.string.n_retry), onRetry)
            TextAction(stringResource(R.string.n_dismiss), onClear)
        }
    }
}

/** Alternativa sin cámara: escribir el código de barras (8, 12, 13 o 14 dígitos; se conservan los ceros). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManualCodeDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = FitnessThemeExtras.colors.card) {
            Column(Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Text(stringResource(R.string.n_enter_code), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                GlassNumberField(code, { code = it.take(14) }, stringResource(R.string.n_code_label), integer = true, imeAction = ImeAction.Done)
                Text(stringResource(R.string.n_code_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PrimaryButton(stringResource(R.string.n_code_search), { onConfirm(code) }, Modifier.fillMaxWidth(), enabled = code.length >= 8)
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
                    Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
