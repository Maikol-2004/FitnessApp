package com.maikol.fitnessapp.feature.nutrition.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.maikol.fitnessapp.core.designsystem.component.PrimaryButton
import com.maikol.fitnessapp.core.designsystem.theme.FitnessThemeExtras
import com.maikol.fitnessapp.core.designsystem.theme.Spacing
import com.maikol.fitnessapp.core.tracking.formatInput
import com.maikol.fitnessapp.feature.nutrition.data.Coverage
import com.maikol.fitnessapp.feature.nutrition.data.Ingredient
import com.maikol.fitnessapp.feature.nutrition.data.IngredientDraft
import com.maikol.fitnessapp.feature.nutrition.data.MAX_FINAL_WEIGHT_G
import com.maikol.fitnessapp.feature.nutrition.data.MAX_SERVINGS
import com.maikol.fitnessapp.feature.nutrition.data.Nutrient
import com.maikol.fitnessapp.feature.nutrition.data.NutritionRepository
import com.maikol.fitnessapp.feature.nutrition.data.Portionable
import com.maikol.fitnessapp.feature.nutrition.data.QtyUnit
import com.maikol.fitnessapp.feature.nutrition.data.RecipeDraft
import com.maikol.fitnessapp.feature.nutrition.data.RecipeTotals
import com.maikol.fitnessapp.feature.nutrition.data.nutrientsFor
import com.maikol.fitnessapp.feature.nutrition.data.portionable
import com.maikol.fitnessapp.feature.nutrition.data.recipeTotals
import com.maikol.fitnessapp.feature.nutrition.data.validQuantity
import com.maikol.fitnessapp.feature.training.data.Parsed
import com.maikol.fitnessapp.feature.training.data.parseDecimal
import com.maikol.fitnessapp.feature.training.ui.GlassButton
import com.maikol.fitnessapp.feature.training.ui.GlassTextField
import com.maikol.fitnessapp.feature.training.ui.InlineMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Línea del borrador: referencia para recuperar el snapshot (alimento actual, ingrediente guardado o consumo). */
data class IngredientLine(val id: String, val ref: String, val quantity: String, val unit: QtyUnit) {
    fun encode() = listOf(id, ref, quantity, unit.code).joinToString("\u001F")

    companion object {
        fun decode(s: String): IngredientLine? = s.split('\u001F').takeIf { it.size == 4 }?.let { IngredientLine(it[0], it[1], it[2], QtyUnit.of(it[3]) ?: QtyUnit.G) }
    }
}

enum class RecipeField { NAME, SERVINGS, WEIGHT, INGREDIENTS }

data class RecipeUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val servings: String = "1",
    val finalWeight: String = "",
    val notes: String = "",
    val lines: List<IngredientLine> = emptyList(),
    val sources: Map<String, Portionable> = emptyMap(),
    val totals: RecipeTotals? = null,
    val badLines: Set<String> = emptySet(),
    val errors: Set<RecipeField> = emptySet(),
    val saving: Boolean = false,
    val failed: Boolean = false,
)

/**
 * Receta propia. El borrador (texto y referencias de ingredientes) vive en SavedStateHandle; los snapshots se
 * recuperan de Room por referencia. Guardar no registra consumo y crea una versión nueva.
 */
class RecipeEditViewModel(private val handle: SavedStateHandle, private val repository: NutritionRepository) : ViewModel() {
    private val recipeId: String? = handle["recipeId"]
    private val fromEntry: String? = handle["fromEntry"]
    private val id: String = recipeId ?: handle.get<String>(KEY_ID) ?: repository.newId().also { handle[KEY_ID] = it }
    private val fields = handle.getStateFlow(KEY_FIELDS, arrayListOf("", "1", "", ""))
    private val lines = handle.getStateFlow(KEY_LINES, arrayListOf<String>())
    private val sources = MutableStateFlow<Map<String, Portionable>>(emptyMap())
    private val status = MutableStateFlow(RecipeUiState())
    private val done = MutableStateFlow<String?>(null)
    val savedEvent: StateFlow<String?> = done

    init {
        viewModelScope.launch {
            if (handle.get<Boolean>(KEY_LOADED) != true) {
                handle[KEY_LOADED] = true
                recipeId?.let { repository.getRecipe(it) }?.let { r ->
                    handle[KEY_FIELDS] = arrayListOf(r.recipe.name, formatInput(r.recipe.servings), r.recipe.finalWeightG?.let(::formatInput).orEmpty(), r.recipe.notes)
                    handle[KEY_LINES] = ArrayList(r.ingredients.sortedBy { it.position }.map { IngredientLine(it.id, "R:${r.recipe.id}:${it.id}", formatInput(it.quantity), QtyUnit.of(it.quantityUnit) ?: QtyUnit.G).encode() })
                }
                // Guardar como receta: solo alimentos reales con cantidad (las raciones de receta no se anidan).
                fromEntry?.let { repository.getEntry(it) }?.let { e ->
                    handle[KEY_LINES] = ArrayList(e.items.filter { it.entity.recipeId == null }.map {
                        IngredientLine(repository.newId(), "M:${it.entity.id}", formatInput(it.entity.quantity), it.unit).encode()
                    })
                }
            }
            resolveAll()
            status.update { it.copy(loading = false) }
        }
    }

    private suspend fun resolve(ref: String): Portionable? = when {
        ref.startsWith("F:") -> repository.getFood(ref.removePrefix("F:"))?.portionable()
        ref.startsWith("M:") -> repository.getItemWithEntry(ref.removePrefix("M:"))?.first?.portionable()
        ref.startsWith("R:") -> {
            val (rid, iid) = ref.removePrefix("R:").split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            repository.getRecipe(rid)?.ingredients?.firstOrNull { it.id == iid }?.portionable()
        }
        else -> null
    }

    private suspend fun resolveAll() {
        val refs = lines.value.mapNotNull { IngredientLine.decode(it)?.ref }.filter { it !in sources.value }
        val found = refs.mapNotNull { r -> resolve(r)?.let { r to it } }
        sources.update { it + found }
    }

    val state: StateFlow<RecipeUiState> = combine(fields, lines, sources, status) { f, l, src, s ->
        val parsedLines = l.mapNotNull(IngredientLine::decode)
        val servings = (parseDecimal(f[1]) as? Parsed.Value)?.value?.takeIf { it > 0 && it <= MAX_SERVINGS }
        val weight = (parseDecimal(f[2]) as? Parsed.Value)?.value?.takeIf { it > 0 && it <= MAX_FINAL_WEIGHT_G }
        val bad = parsedLines.filter { line ->
            val p = src[line.ref]
            val q = (parseDecimal(line.quantity) as? Parsed.Value)?.value
            p == null || q == null || !validQuantity(q, line.unit) || p.basis.factor(q, line.unit) == null
        }.map { it.id }.toSet()
        val ingredients = parsedLines.filter { it.id !in bad }.map { line ->
            val p = src.getValue(line.ref)
            Ingredient(p.perBasis, p.basis, (parseDecimal(line.quantity) as Parsed.Value).value, line.unit)
        }
        s.copy(
            isNew = recipeId == null,
            name = f[0], servings = f[1], finalWeight = f[2], notes = f[3],
            lines = parsedLines, sources = src, badLines = bad,
            totals = if (servings != null && ingredients.isNotEmpty()) recipeTotals(ingredients, servings, weight) else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeUiState())

    private fun setField(i: Int, v: String) {
        val f = ArrayList(fields.value)
        f[i] = v
        handle[KEY_FIELDS] = f
        status.update { it.copy(errors = emptySet(), failed = false) }
    }

    fun name(v: String) = setField(0, v.take(60))
    fun servings(v: String) = setField(1, v)
    fun weight(v: String) = setField(2, v)
    fun notes(v: String) = setField(3, v.take(1000))

    private fun setLines(transform: (List<IngredientLine>) -> List<IngredientLine>) {
        handle[KEY_LINES] = ArrayList(transform(lines.value.mapNotNull(IngredientLine::decode)).map { it.encode() })
        status.update { it.copy(errors = emptySet(), failed = false) }
    }

    fun quantity(lineId: String, text: String) = setLines { l -> l.map { if (it.id == lineId) it.copy(quantity = text) else it } }

    fun unit(lineId: String, u: QtyUnit) = setLines { l ->
        l.map { line ->
            if (line.id != lineId) return@map line
            val p = sources.value[line.ref]
            val q = (parseDecimal(line.quantity) as? Parsed.Value)?.value
            val conv = if (p != null && q != null) p.basis.convert(q, line.unit, u) else null
            line.copy(unit = u, quantity = conv?.let(::formatInput) ?: line.quantity)
        }
    }

    fun remove(lineId: String) = setLines { l -> l.filterNot { it.id == lineId } }

    /** Ingrediente elegido en búsqueda/cantidad: «foodId|cantidad|unidad». */
    fun addFromPick(encoded: String) {
        val parts = encoded.split('|')
        if (parts.size != 3) return
        val q = parts[1].toDoubleOrNull() ?: return
        val u = QtyUnit.of(parts[2]) ?: return
        setLines { it + IngredientLine(repository.newId(), "F:${parts[0]}", formatInput(q), u) }
        viewModelScope.launch { resolveAll() }
    }

    fun save() {
        val s = state.value
        if (s.saving || s.loading) return
        val errors = mutableSetOf<RecipeField>()
        if (s.name.isBlank()) errors += RecipeField.NAME
        val servings = (parseDecimal(s.servings) as? Parsed.Value)?.value?.takeIf { it > 0 && it <= MAX_SERVINGS }
        if (servings == null) errors += RecipeField.SERVINGS
        val weight = when (val p = parseDecimal(s.finalWeight)) {
            Parsed.Empty -> null
            is Parsed.Value -> p.value.takeIf { it > 0 && it <= MAX_FINAL_WEIGHT_G } ?: null.also { errors += RecipeField.WEIGHT }
            Parsed.Invalid -> null.also { errors += RecipeField.WEIGHT }
        }
        if (s.lines.isEmpty() || s.badLines.isNotEmpty()) errors += RecipeField.INGREDIENTS
        if (errors.isNotEmpty()) {
            status.update { it.copy(errors = errors) }
            return
        }
        val draft = RecipeDraft(
            s.name, servings!!, weight, s.notes,
            s.lines.map { IngredientDraft(it.id, s.sources.getValue(it.ref), (parseDecimal(it.quantity) as Parsed.Value).value, it.unit) },
        )
        status.update { it.copy(saving = true, failed = false) }
        viewModelScope.launch {
            try {
                repository.saveRecipe(id, draft)
                status.update { it.copy(saving = false) }
                done.value = draft.name
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                status.update { it.copy(saving = false, failed = true) }
            }
        }
    }

    companion object {
        private const val KEY_ID = "recipe_id"
        private const val KEY_FIELDS = "recipe_fields"
        private const val KEY_LINES = "recipe_lines"
        private const val KEY_LOADED = "recipe_loaded"

        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as FitnessApplication).container
                RecipeEditViewModel(createSavedStateHandle(), c.nutritionRepository)
            }
        }
    }
}

@Composable
fun RecipeEditRoute(
    savedState: SavedStateHandle,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    addIngredient: () -> Unit,
    viewModel: RecipeEditViewModel = viewModel(factory = RecipeEditViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saved by viewModel.savedEvent.collectAsStateWithLifecycle()
    val savedText = saved?.let { stringResource(R.string.n_recipe_saved, it) }
    LaunchedEffect(saved) { savedText?.let(onSaved) }
    val picked by savedState.getStateFlow<String?>(NutritionRoutes.KEY_INGREDIENT, null).collectAsStateWithLifecycle()
    LaunchedEffect(picked) {
        picked?.let {
            savedState[NutritionRoutes.KEY_INGREDIENT] = null
            viewModel.addFromPick(it)
        }
    }
    if (state.loading) return
    var per100 by rememberSaveable { mutableStateOf(false) }
    val e = state.errors
    val invalid = stringResource(R.string.field_invalid)
    val stacked = LocalDensity.current.fontScale > 1.3f

    NutritionPage(
        title = stringResource(if (state.isNew) R.string.n_recipe_new else R.string.n_recipe_edit),
        subtitle = stringResource(R.string.n_recipe_subtitle),
        onBack = onBack,
        bottomBar = {
            InlineMessage(visible = state.failed, text = stringResource(R.string.record_save_error))
            PrimaryButton(stringResource(R.string.n_recipe_save), viewModel::save, Modifier.fillMaxWidth(), busy = state.saving, iconRes = R.drawable.ic_check)
            Text(stringResource(R.string.n_recipe_save_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    ) {
        GlassTextField(state.name, viewModel::name, stringResource(R.string.n_recipe_name), error = stringResource(R.string.n_food_name_error).takeIf { RecipeField.NAME in e },
            capitalization = KeyboardCapitalization.Sentences)
        val servingsField = @Composable { m: Modifier ->
            GlassNumberField(state.servings, viewModel::servings, stringResource(R.string.n_servings_label), m, error = stringResource(R.string.n_servings_error).takeIf { RecipeField.SERVINGS in e })
        }
        val weightField = @Composable { m: Modifier ->
            GlassNumberField(state.finalWeight, viewModel::weight, stringResource(R.string.n_final_weight), m, suffix = "g", error = invalid.takeIf { RecipeField.WEIGHT in e })
        }
        if (stacked) {
            servingsField(Modifier.fillMaxWidth()); weightField(Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) { servingsField(Modifier.weight(1f)); weightField(Modifier.weight(1f)) }
        }
        Text(stringResource(R.string.n_final_weight_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text(stringResource(R.string.n_ingredients), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        if (state.lines.isEmpty()) {
            Text(stringResource(R.string.n_ingredients_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column {
            state.lines.forEachIndexed { i, line ->
                if (i > 0) HorizontalDivider(color = FitnessThemeExtras.colors.surfaceRaised)
                IngredientRow(line, state.sources[line.ref], line.id in state.badLines, viewModel)
            }
        }
        InlineMessage(visible = RecipeField.INGREDIENTS in e, text = stringResource(R.string.n_ingredients_error))
        GlassButton(stringResource(R.string.n_add_ingredient), R.drawable.ic_add, addIngredient, Modifier.fillMaxWidth(), centered = true)

        val t = state.totals
        if (t != null) {
            GlassSegmented(listOf(stringResource(R.string.n_per_serving_tab), stringResource(R.string.n_per_100g_tab)), if (per100 && t.per100g != null) 1 else 0, { per100 = it == 1 })
            val shown = if (per100) t.per100g else t.perServing
            if (shown == null) {
                Text(stringResource(R.string.n_per_100g_needs_weight), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(Modifier.semantics(mergeDescendants = true) {}) {
                    Text(
                        if (per100) stringResource(R.string.n_per_100g_tab) else t.servingMassG?.let { stringResource(R.string.n_per_serving_mass, gramsText(it)) } ?: stringResource(R.string.n_per_serving_tab),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(shown.kcal.value?.let(::kcalText) ?: "— kcal", style = MaterialTheme.typography.displaySmall)
                    Text(stringResource(R.string.n_calculated_from_ingredients), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                MacroRow(mapOf(Nutrient.PROTEIN to shown.protein, Nutrient.CARBS to shown.carbs, Nutrient.FAT to shown.fat), emptyMap(), stacked = stacked)
                if (Nutrient.entries.any { t.total[it].coverage == Coverage.PARTIAL || t.total[it].coverage == Coverage.UNKNOWN }) {
                    Text(stringResource(R.string.n_recipe_partial), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(stringResource(R.string.n_recipe_total, t.total.kcal.value?.let(::kcalText) ?: "— kcal"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        GlassTextField(state.notes, viewModel::notes, stringResource(R.string.n_preparation_notes), singleLine = false)
    }
}

@Composable
private fun IngredientRow(line: IngredientLine, src: Portionable?, bad: Boolean, vm: RecipeEditViewModel) {
    Column(Modifier.padding(vertical = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(src?.let { listOfNotNull(it.name, it.brand).joinToString(" · ") } ?: stringResource(R.string.n_ingredient_missing), style = MaterialTheme.typography.titleMedium)
                if (src != null) {
                    val q = (parseDecimal(line.quantity) as? Parsed.Value)?.value
                    val n = q?.let { nutrientsFor(src.perBasis, src.basis, it, line.unit) }
                    Text(
                        listOfNotNull(stringResource(src.preparation.label()), n?.let { it.kcal?.let(::kcalText) ?: "— kcal" }).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = { vm.remove(line.id) }) {
                Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.n_remove_ingredient, src?.name.orEmpty()))
            }
        }
        if (src != null) {
            GlassNumberField(line.quantity, { vm.quantity(line.id, it) }, stringResource(R.string.n_quantity_of, src.name), suffix = unitLabel(line.unit),
                error = stringResource(R.string.n_quantity_error).takeIf { bad })
            val units = src.basis.units()
            if (units.size > 1) GlassChoices(units, line.unit, { unitLabel(it) }, { vm.unit(line.id, it) })
        }
    }
}
