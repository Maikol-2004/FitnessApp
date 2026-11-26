package com.maikol.fitnessapp.feature.nutrition.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maikol.fitnessapp.R
import com.maikol.fitnessapp.core.database.FoodSource
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.tracking.formatInput
import com.maikol.fitnessapp.feature.nutrition.data.MealGroup
import com.maikol.fitnessapp.feature.nutrition.data.Nutrient
import com.maikol.fitnessapp.feature.nutrition.data.NutrientSum
import com.maikol.fitnessapp.feature.nutrition.data.QtyUnit
import com.maikol.fitnessapp.feature.nutrition.data.Totals
import com.maikol.fitnessapp.feature.nutrition.data.Coverage
import com.maikol.fitnessapp.feature.nutrition.data.Portionable
import com.maikol.fitnessapp.feature.tracking.DatePickerSheet
import com.maikol.fitnessapp.feature.tracking.PickerField
import com.maikol.fitnessapp.feature.tracking.dayLabel
import com.maikol.fitnessapp.feature.training.ui.GlassButton
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import com.maikol.fitnessapp.feature.training.ui.MenuAction
import com.maikol.fitnessapp.feature.training.ui.OverflowMenu
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PortionRoute(
    onBack: () -> Unit,
    onLogged: (String) -> Unit,
    onIngredient: (String) -> Unit,
    editFood: (String) -> Unit,
    viewModel: PortionViewModel = viewModel(factory = PortionViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()
    val loggedText = (event as? PortionEvent.Logged)?.let { e ->
        stringResource(R.string.n_logged_message, e.name, quantityText(e.quantity, e.unit), stringResource(e.group.label()))
    }
    LaunchedEffect(event) {
        when (val e = event) {
            is PortionEvent.Logged -> { viewModel.consumed(); onLogged(loggedText.orEmpty()) }
            is PortionEvent.Ingredient -> { viewModel.consumed(); onIngredient(e.encoded) }
            null -> Unit
        }
    }
    LaunchedEffect(state.missing) { if (state.missing) onBack() }
    val src = state.source ?: return
    var tab by rememberSaveable { mutableStateOf(0) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()
    val food = state.food

    NutritionPage(
        title = src.name,
        subtitle = listOfNotNull(src.brand, stringResource(src.preparation.label()), sourceLabel(src.source)).joinToString(" · "),
        onBack = onBack,
        actions = {
            if (food != null) {
                IconButton(onClick = viewModel::toggleFavorite) {
                    Icon(
                        painterResource(if (food.favorite) R.drawable.ic_star else R.drawable.ic_star_border),
                        stringResource(if (food.favorite) R.string.n_unfavorite else R.string.n_make_favorite),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                OverflowMenu(
                    description = stringResource(R.string.record_actions, food.name),
                    actions = listOf(
                        MenuAction(if (food.source == FoodSource.MANUAL) R.string.n_edit_food else R.string.n_edit_copy, R.drawable.ic_edit) { editFood(food.id) },
                        MenuAction(R.string.n_archive, R.drawable.ic_archive) { viewModel.archiveFood(onBack) },
                    ),
                )
            }
        },
        bottomBar = {
            InlineMessage(visible = state.failed, text = stringResource(R.string.record_save_error))
            PrimaryButton(
                text = stringResource(
                    when (state.mode) {
                        PortionMode.NEW -> R.string.n_add_to_diary
                        PortionMode.EDIT_ITEM -> R.string.n_save_changes
                        PortionMode.INGREDIENT -> R.string.n_add_to_recipe
                    },
                ),
                onClick = viewModel::save,
                busy = state.saving,
                enabled = state.forQuantity != null && !state.needsBasis,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        GlassSegmented(listOf(stringResource(R.string.n_tab_quantity), stringResource(R.string.n_tab_info)), tab, { tab = it })
        if (state.needsBasis) BasisReview(viewModel::confirmBasis)
        if (tab == 0) {
            QuantitySection(state, src, viewModel)
            if (state.mode != PortionMode.INGREDIENT) {
                Text(stringResource(R.string.n_destination), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                GlassChoices(MealGroup.choosable, state.group.takeIf { it != MealGroup.UNSPECIFIED }, { stringResource(it.label()) }, viewModel::onGroup)
                if (state.mode == PortionMode.NEW) {
                    PickerField(stringResource(R.string.n_date), dayLabel(state.date, today), onClick = { pickDate = true })
                } else {
                    Text(stringResource(R.string.n_edit_date_note, dayLabel(state.date, today)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            InfoSection(src, state)
        }
    }
    if (pickDate) DatePickerSheet(state.date, today, onPick = { viewModel.onDate(it); pickDate = false }, onDismiss = { pickDate = false })
}

/** Producto de volumen publicado en campos «por 100 g»: el usuario confirma la base con su etiqueta. */
@Composable
private fun BasisReview(onConfirm: (QtyUnit) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(com.maikol.fitnessapp.core.designsystem.theme.Spacing.s)) {
        InlineMessage(visible = true, text = stringResource(R.string.n_basis_review), iconRes = R.drawable.ic_info, color = MaterialTheme.colorScheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(com.maikol.fitnessapp.core.designsystem.theme.Spacing.s)) {
            GlassButton(stringResource(R.string.n_basis_100ml), null, { onConfirm(QtyUnit.ML) }, Modifier.weight(1f), centered = true)
            GlassButton(stringResource(R.string.n_basis_100g), null, { onConfirm(QtyUnit.G) }, Modifier.weight(1f), centered = true)
        }
    }
}

@Composable
private fun QuantitySection(state: PortionUiState, src: Portionable, vm: PortionViewModel) {
    val units = src.basis.units()
    GlassNumberField(
        state.quantityText,
        vm::onQuantity,
        stringResource(R.string.n_quantity),
        suffix = unitLabel(state.unit),
        error = stringResource(R.string.n_quantity_error).takeIf { state.quantityError },
        imeAction = ImeAction.Done,
    )
    if (units.size > 1) {
        GlassChoices(units, state.unit, { u -> unitChoiceLabel(u, src) }, vm::onUnit)
    }
    val q = (com.maikol.fitnessapp.feature.training.data.parseDecimal(state.quantityText) as? com.maikol.fitnessapp.feature.training.data.Parsed.Value)?.value
    if (q != null && !state.quantityError) {
        src.basis.equivalence(q, state.unit)?.let { (v, u) ->
            Text(stringResource(R.string.n_equivalence, quantityText(q, state.unit), quantityText(v, u)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (src.basis.unit == QtyUnit.ML || src.basis.servingUnit == QtyUnit.ML) {
        Text(stringResource(R.string.n_no_density), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (src.recipeId != null && src.basis.servingAmount == null) {
        Text(stringResource(R.string.n_recipe_no_grams), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(stringResource(R.string.n_for_quantity), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    val n = state.forQuantity
    val totals = n?.let { Totals.of(it, src.partialMask) }
    Column(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        Text(n?.kcal?.let(::kcalText) ?: "— kcal", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text(
            stringResource(R.string.n_for_quantity_of, src.name, quantityText(q ?: 0.0, state.unit)).takeIf { n != null } ?: stringResource(R.string.n_quantity_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (totals != null) {
        MacroRow(
            values = mapOf(Nutrient.PROTEIN to totals.protein, Nutrient.CARBS to totals.carbs, Nutrient.FAT to totals.fat),
            goals = emptyMap(),
            stacked = LocalDensity.current.fontScale > 1.3f,
        )
        MissingNote(src)
    }
}

@Composable
private fun unitChoiceLabel(u: QtyUnit, src: Portionable): String = when (u) {
    QtyUnit.SERVING -> {
        val a = src.basis.servingAmount
        val su = src.basis.servingUnit
        if (src.basis.unit != QtyUnit.SERVING && a != null && su != null) stringResource(R.string.n_serving_of, "${formatInput(a)} ${su.code}") else stringResource(R.string.n_unit_serving)
    }
    else -> u.code
}

/** Nutrientes ausentes: se nombran y el registro queda parcial, nunca como cero. */
@Composable
private fun MissingNote(src: Portionable) {
    val missing = Nutrient.entries.filter { src.perBasis[it] == null }
    val partial = Nutrient.entries.filter { src.partialMask and it.bit != 0 }
    if (missing.isNotEmpty()) {
        Text(
            stringResource(R.string.n_missing, missing.map { stringResource(it.label()).lowercase() }.joinToString(", ")),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (partial.isNotEmpty()) {
        Text(stringResource(R.string.n_partial_recipe), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoSection(src: Portionable, state: PortionUiState) {
    val f = state.food
    Text(stringResource(R.string.n_values_basis, basisLabel(src.basis.amount, src.basis.unit)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    Column {
        Nutrient.entries.forEachIndexed { i, n ->
            if (i > 0) HorizontalDivider(color = FitnessThemeExtras.colors.surfaceRaised)
            val sum = Totals.of(src.perBasis, src.partialMask)[n]
            InfoRow(stringResource(n.label()), nutrientValue(n, sum))
        }
    }
    src.basis.servingAmount?.let { a ->
        val su = src.basis.servingUnit
        if (su != null) InfoRow(stringResource(R.string.n_serving_def), (src.servingName?.let { "$it · " } ?: "") + "${formatInput(a)} ${su.code}")
    }
    InfoRow(stringResource(R.string.n_preparation), stringResource(src.preparation.label()))
    InfoRow(stringResource(R.string.n_source), sourceLabel(src.source))
    if (f != null) {
        (f.barcode ?: f.sourceId)?.let { InfoRow(stringResource(R.string.n_code_label), it) }
        f.fetchedAt?.let {
            InfoRow(stringResource(R.string.n_fetched), DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", com.maikol.fitnessapp.feature.training.data.AppLocale).format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())))
        }
        if (f.kcalFromKj) Text(stringResource(R.string.n_kcal_from_kj), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (f.source == FoodSource.OFF) {
            Text(stringResource(R.string.n_off_values_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            f.sourceUrl?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(stringResource(R.string.n_off_attribution), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        f.derivedFromId?.let { Text(stringResource(R.string.n_private_copy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    state.item?.let { Text(stringResource(R.string.n_snapshot_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun nutrientValue(n: Nutrient, sum: NutrientSum): String = when {
    sum.value == null -> stringResource(R.string.n_not_reported)
    sum.coverage == Coverage.PARTIAL -> nutrientText(n, sum.value) + " · " + stringResource(R.string.n_partial)
    else -> nutrientText(n, sum.value)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
